package com.pizzaconfig.orderservice.service;

import com.pizzaconfig.orderservice.client.CatalogClient;
import com.pizzaconfig.orderservice.client.PricingClient;
import com.pizzaconfig.orderservice.client.RuleServiceClient;
import com.pizzaconfig.orderservice.domain.Modifications;
import com.pizzaconfig.orderservice.domain.PendingReview;
import com.pizzaconfig.orderservice.domain.PendingReviewStatus;
import com.pizzaconfig.orderservice.dto.CreateOrderItemRequest;
import com.pizzaconfig.orderservice.dto.CreateOrderRequest;
import com.pizzaconfig.orderservice.dto.CreatePendingReviewRequest;
import com.pizzaconfig.orderservice.dto.OrderResponse;
import com.pizzaconfig.orderservice.dto.ResolvePendingReviewRequest;
import com.pizzaconfig.orderservice.repository.PendingReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PendingReviewService {

    private final PendingReviewRepository repository;
    private final RuleServiceClient ruleServiceClient;
    private final PricingClient pricingClient;
    private final CatalogClient catalogClient;
    private final OrderService orderService;

    public PendingReviewService(PendingReviewRepository repository, RuleServiceClient ruleServiceClient,
                                 PricingClient pricingClient, CatalogClient catalogClient, OrderService orderService) {
        this.repository = repository;
        this.ruleServiceClient = ruleServiceClient;
        this.pricingClient = pricingClient;
        this.catalogClient = catalogClient;
        this.orderService = orderService;
    }

    @Transactional
    public PendingReview create(CreatePendingReviewRequest request, UUID customerId) {
        return repository.save(new PendingReview(
                request.basePizzaId(), request.size(), request.dough(),
                request.modifications(), request.rawComment(), request.phoneNumber(), customerId, Instant.now()));
    }

    public List<PendingReview> findPending() {
        return repository.findByStatus(PendingReviewStatus.PENDING);
    }

    public Optional<PendingReview> findById(UUID id) {
        return repository.findById(id);
    }

    // Staff propose a full structured reconfiguration here; it's validated via
    // rule-service before ever reaching the customer, so the customer's later confirm
    // step doesn't need to (and doesn't) validate again.
    @Transactional
    public Optional<ResolveOutcome> resolve(UUID id, ResolvePendingReviewRequest request) {
        Optional<PendingReview> reviewOpt = repository.findById(id);
        if (reviewOpt.isEmpty()) {
            return Optional.empty();
        }
        PendingReview review = reviewOpt.get();

        List<RuleServiceClient.AdditionDto> additions = request.additions().stream()
                .map(a -> new RuleServiceClient.AdditionDto(a.ingredientId(), a.type(), a.quantity()))
                .toList();

        RuleServiceClient.ValidationResultDto validation = ruleServiceClient.validate(new RuleServiceClient.ChangeRequestDto(
                request.basePizzaId(), request.size(), request.dough(), additions, request.removals(), "BUTTON", false));

        if (!"APPROVED".equals(validation.outcome())) {
            return Optional.of(ResolveOutcome.rejected(validation));
        }

        BigDecimal totalPrice = computeTotalPrice(request);
        Modifications resolvedModifications = new Modifications(
                request.additions().stream()
                        .map(a -> new Modifications.Addition(a.ingredientId(), a.quantity()))
                        .toList(),
                request.removals());

        review.resolve(request.basePizzaId(), request.size(), request.dough(), resolvedModifications, totalPrice, Instant.now());

        return Optional.of(ResolveOutcome.resolved(review));
    }

    @Transactional
    public Optional<OrderResponse> confirm(UUID id) {
        Optional<PendingReview> reviewOpt = repository.findById(id);
        if (reviewOpt.isEmpty() || reviewOpt.get().getStatus() != PendingReviewStatus.RESOLVED) {
            return Optional.empty();
        }
        PendingReview review = reviewOpt.get();

        CreateOrderRequest orderRequest = new CreateOrderRequest(
                List.of(new CreateOrderItemRequest(
                        review.getResolvedBasePizzaId(), review.getResolvedSize(), review.getResolvedDough(),
                        review.getResolvedModifications(), review.getResolvedTotalPrice())),
                review.getRawComment(),
                review.getPhoneNumber());

        OrderResponse orderResponse = orderService.createOrder(orderRequest, review.getCustomerId());
        review.markConfirmed();

        return Optional.of(orderResponse);
    }

    private BigDecimal computeTotalPrice(ResolvePendingReviewRequest request) {
        BigDecimal base = catalogClient.findAll().stream()
                .filter(p -> p.id().equalsIgnoreCase(request.basePizzaId()))
                .map(CatalogClient.CatalogPizzaDto::basePrice)
                .findFirst()
                .orElse(BigDecimal.ZERO);

        List<PricingClient.PriceItemDto> prices = pricingClient.findAllPrices();
        BigDecimal sizePrice = priceFor(prices, "size-" + request.size().toLowerCase());
        BigDecimal doughPrice = priceFor(prices, "gluten-free".equalsIgnoreCase(request.dough()) ? "dough-gluten-free" : "dough-classic");
        BigDecimal cheeseUnit = priceFor(prices, "topping-cheese");
        BigDecimal pineappleUnit = priceFor(prices, "topping-pineapple");

        BigDecimal cheeseTotal = request.additions().stream()
                .filter(a -> "CHEESE".equals(a.type()))
                .map(a -> cheeseUnit.multiply(BigDecimal.valueOf(a.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pineappleTotal = request.additions().stream()
                .filter(a -> "pineapple".equalsIgnoreCase(a.ingredientId()))
                .map(a -> pineappleUnit.multiply(BigDecimal.valueOf(a.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return base.add(sizePrice).add(doughPrice).add(cheeseTotal).add(pineappleTotal);
    }

    private BigDecimal priceFor(List<PricingClient.PriceItemDto> prices, String itemId) {
        return prices.stream()
                .filter(p -> p.itemId().equals(itemId))
                .map(PricingClient.PriceItemDto::amount)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }

    public record ResolveOutcome(boolean approved, PendingReview review, RuleServiceClient.ValidationResultDto validation) {
        static ResolveOutcome resolved(PendingReview review) {
            return new ResolveOutcome(true, review, null);
        }

        static ResolveOutcome rejected(RuleServiceClient.ValidationResultDto validation) {
            return new ResolveOutcome(false, null, validation);
        }
    }
}
