import { useEffect, useState } from "react";
import { api, BASE_URL } from "../api/client";
import { useAuth } from "../context/AuthContext";
import type {
  Addition,
  ConfigureResponse,
  IngredientType,
  OrderResponse,
  PendingReview,
  PriceItem,
  Pizza,
  ValidationResult,
} from "../api/types";

const SIZES = ["S", "M", "L"];
const DOUGHS = ["classic", "gluten-free"];

export default function CustomerOrderPage() {
  const { phoneNumber: accountPhoneNumber } = useAuth();
  const [pizzas, setPizzas] = useState<Pizza[]>([]);
  const [prices, setPrices] = useState<PriceItem[]>([]);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [basePizzaId, setBasePizzaId] = useState("margherita");
  const [size, setSize] = useState("M");
  const [dough, setDough] = useState("classic");
  const [additions, setAdditions] = useState<Addition[]>([]);
  const [removedDefaultIds, setRemovedDefaultIds] = useState<Set<string>>(new Set());
  const [quantity, setQuantity] = useState(1);

  const [comment, setComment] = useState("");

  const [validation, setValidation] = useState<ValidationResult | null>(null);
  const [validating, setValidating] = useState(false);

  const [customNotes, setCustomNotes] = useState("");
  const [phoneNumber, setPhoneNumber] = useState(accountPhoneNumber ?? "+491234567890");
  const [order, setOrder] = useState<OrderResponse | null>(null);
  const [pendingReview, setPendingReview] = useState<PendingReview | null>(null);
  const [placingOrder, setPlacingOrder] = useState(false);
  const [orderError, setOrderError] = useState<string | null>(null);

  useEffect(() => {
    Promise.all([api.get<Pizza[]>("/v1/customer/catalog/pizzas"), api.get<PriceItem[]>("/v1/customer/pricing/prices")])
      .then(([pizzaList, priceList]) => {
        setPizzas(pizzaList);
        setPrices(priceList);
      })
      .catch(() => setLoadError("Could not reach the menu/pricing services. Is the stack running?"));
  }, []);

  const selectedPizza = pizzas.find((p) => p.id === basePizzaId);
  const knownExtraIds = new Set(selectedPizza?.allowedExtras.map((e) => e.ingredientId) ?? []);
  const extraAdditions = additions.filter((a) => !knownExtraIds.has(a.ingredientId));

  function priceFor(itemId: string): number {
    return prices.find((p) => p.itemId === itemId)?.amount ?? 0;
  }

  function getQty(ingredientId: string): number {
    return additions.find((a) => a.ingredientId === ingredientId)?.quantity ?? 0;
  }

  function setQty(ingredientId: string, type: IngredientType, quantity: number) {
    setAdditions((prev) => {
      const withoutThis = prev.filter((a) => a.ingredientId !== ingredientId);
      return quantity > 0 ? [...withoutThis, { ingredientId, type, quantity }] : withoutThis;
    });
    resetValidation();
  }

  function resetValidation() {
    if (!order) setValidation(null);
  }

  function selectPizza(pizzaId: string) {
    setBasePizzaId(pizzaId);
    setAdditions([]);
    setRemovedDefaultIds(new Set());
    resetValidation();
  }

  function computeTotal(): number {
    const base = selectedPizza?.basePrice ?? 0;
    const sizePrice = priceFor(`size-${size.toLowerCase()}`);
    const doughPrice = priceFor(dough === "gluten-free" ? "dough-gluten-free" : "dough-classic");
    const cheeseUnit = priceFor("topping-cheese");
    const pineappleUnit = priceFor("topping-pineapple");
    const cheeseTotal = additions.filter((a) => a.type === "CHEESE").reduce((sum, a) => sum + a.quantity, 0) * cheeseUnit;
    const pineappleTotal = getQty("pineapple") * pineappleUnit;
    const perPizza = base + sizePrice + doughPrice + cheeseTotal + pineappleTotal;
    return perPizza * quantity;
  }

  async function handleValidate() {
    setValidating(true);
    setValidation(null);
    setOrderError(null);
    try {
      const result = await api.post<ConfigureResponse>("/v1/customer/configurator/validate", {
        basePizzaId,
        size,
        dough,
        additions,
        removals: [...removedDefaultIds],
        comment: comment.trim() || undefined,
      });

      if (result.kitchenNote) {
        // Couldn't map the comment to a specific change — hand it to the kitchen for a
        // human resolution instead of forcing a guessed structural change through
        // validation. The button-selected config below is what gets sent along.
        const review = await api.post<PendingReview>("/v1/customer/orders/pending-reviews", {
          basePizzaId: result.basePizzaId,
          size: result.size,
          dough: result.dough,
          modifications: {
            additions: additions.map((a) => ({ id: a.ingredientId, qty: a.quantity })),
            removals: [...removedDefaultIds],
          },
          rawComment: result.kitchenNote,
          phoneNumber,
        });
        setPendingReview(review);
        return;
      }

      setBasePizzaId(result.basePizzaId);
      setSize(result.size);
      setDough(result.dough);
      setAdditions(result.additions);
      setRemovedDefaultIds(new Set(result.removals));
      setValidation(result.validation);
    } catch {
      setOrderError("Could not validate — is the gateway/rule-service reachable?");
    } finally {
      setValidating(false);
    }
  }

  async function handlePlaceOrder() {
    if (!validation || validation.outcome !== "APPROVED") return;
    setPlacingOrder(true);
    setOrderError(null);
    try {
      const perPizzaSubtotal = computeTotal() / quantity;
      const item = {
        basePizzaId,
        chosenSize: size,
        chosenDough: dough,
        modifications: {
          additions: additions.map((a) => ({ id: a.ingredientId, qty: a.quantity })),
          removals: [...removedDefaultIds],
        },
        subtotal: perPizzaSubtotal,
      };
      const placed = await api.post<OrderResponse>("/v1/customer/orders", {
        items: Array.from({ length: quantity }, () => item),
        customNotes,
        phoneNumber,
      });
      setOrder(placed);
    } catch {
      setOrderError("Could not place the order — is order-service reachable?");
    } finally {
      setPlacingOrder(false);
    }
  }

  async function handleConfirmPendingReview() {
    if (!pendingReview) return;
    setPlacingOrder(true);
    setOrderError(null);
    try {
      const placed = await api.post<OrderResponse>(`/v1/customer/orders/pending-reviews/${pendingReview.id}/confirm`);
      setOrder(placed);
      setPendingReview(null);
    } catch {
      setOrderError("Could not confirm — is order-service reachable?");
    } finally {
      setPlacingOrder(false);
    }
  }

  useEffect(() => {
    if (!order || order.status === "READY_FOR_COLLECTION") return;
    const interval = setInterval(() => {
      api
        .get<OrderResponse>(`/v1/customer/orders/${order.orderId}`)
        .then(setOrder)
        .catch(() => {
          /* transient — keep showing the last known status */
        });
    }, 4000);
    return () => clearInterval(interval);
  }, [order?.orderId, order?.status]);

  useEffect(() => {
    if (!pendingReview || pendingReview.status !== "PENDING") return;
    const interval = setInterval(() => {
      api
        .get<PendingReview>(`/v1/customer/orders/pending-reviews/${pendingReview.id}`)
        .then(setPendingReview)
        .catch(() => {
          /* transient — keep polling */
        });
    }, 3000);
    return () => clearInterval(interval);
  }, [pendingReview?.id, pendingReview?.status]);

  if (order) {
    return (
      <div className="page page--narrow">
        <h1>Order placed! 🎉</h1>
        <div className="card">
          <p className="order-number">{order.displayNumber}</p>
          <p>
            Status: <strong>{order.status.replaceAll("_", " ")}</strong>
          </p>
          <p>Total: €{order.totalPrice.toFixed(2)}</p>
          <p>
            Pickup code: <code>{order.pickupSecurityToken}</code>
          </p>
          <p className="hint">We'll text {order.phoneNumber} when it's ready. This page refreshes automatically.</p>
        </div>
        <button
          onClick={() => {
            setOrder(null);
            setPendingReview(null);
            setValidation(null);
            setAdditions([]);
            setQuantity(1);
          }}
        >
          Start a new order
        </button>
      </div>
    );
  }

  if (pendingReview) {
    return (
      <div className="page page--narrow">
        <h1>Waiting on the kitchen…</h1>
        <div className="card">
          <p>
            Your note: <em>"{pendingReview.rawComment}"</em>
          </p>

          {pendingReview.status === "PENDING" && (
            <p className="hint">
              We've sent this to the kitchen for a quick check — this page updates automatically once they respond.
            </p>
          )}

          {pendingReview.status === "RESOLVED" && pendingReview.resolvedTotalPrice != null && (
            <>
              <p>The kitchen proposed:</p>
              <ul>
                <li>
                  {pendingReview.resolvedBasePizzaId} ({pendingReview.resolvedSize}, {pendingReview.resolvedDough})
                </li>
                {pendingReview.resolvedModifications?.additions.map((a) => (
                  <li key={a.id}>
                    + {a.id} ×{a.qty}
                  </li>
                ))}
                {pendingReview.resolvedModifications?.removals.map((r) => (
                  <li key={r}>− {r}</li>
                ))}
              </ul>
              <p className="order-number">€{pendingReview.resolvedTotalPrice.toFixed(2)}</p>
              {orderError && <p className="error-banner">{orderError}</p>}
              <button onClick={handleConfirmPendingReview} disabled={placingOrder}>
                {placingOrder ? "Confirming…" : "Confirm & place order"}
              </button>
            </>
          )}
        </div>
        <button onClick={() => setPendingReview(null)}>Cancel and start over</button>
      </div>
    );
  }

  return (
    <div className="page">
      <h1>Configure your pizza</h1>
      {loadError && <p className="error-banner">{loadError}</p>}

      <div className="builder">
        <div className="card">
          <h2>1. Base pizza</h2>
          <div className="pizza-grid">
            {pizzas.map((pizza) => (
              <button
                key={pizza.id}
                className={`pizza-card ${basePizzaId === pizza.id ? "pizza-card--selected" : ""}`}
                onClick={() => selectPizza(pizza.id)}
              >
                {pizza.imageUrl && <img src={`${BASE_URL}${pizza.imageUrl}`} alt={pizza.name} className="pizza-card__image" />}
                <strong>{pizza.name}</strong>
                <span>{pizza.description}</span>
                <span className="price">€{pizza.basePrice.toFixed(2)}</span>
              </button>
            ))}
          </div>

          <div className="row">
            <label>
              Size
              <select
                value={size}
                onChange={(e) => {
                  setSize(e.target.value);
                  resetValidation();
                }}
              >
                {SIZES.map((s) => (
                  <option key={s} value={s}>
                    {s}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Dough
              <select
                value={dough}
                onChange={(e) => {
                  setDough(e.target.value);
                  resetValidation();
                }}
              >
                {DOUGHS.map((d) => (
                  <option key={d} value={d}>
                    {d}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Quantity
              <div className="stepper">
                <button
                  onClick={() => {
                    setQuantity((q) => Math.max(1, q - 1));
                    resetValidation();
                  }}
                  disabled={quantity <= 1}
                >
                  −
                </button>
                <span>{quantity}</span>
                <button
                  onClick={() => {
                    setQuantity((q) => q + 1);
                    resetValidation();
                  }}
                >
                  +
                </button>
              </div>
            </label>
          </div>

          {selectedPizza && selectedPizza.defaultIngredients.length > 0 && (
            <>
              <h3>Includes</h3>
              {selectedPizza.defaultIngredients.map((ing) => (
                <label key={ing.ingredientId} className="checkbox-row">
                  <input
                    type="checkbox"
                    checked={!removedDefaultIds.has(ing.ingredientId)}
                    disabled={!ing.removable}
                    onChange={(e) => {
                      setRemovedDefaultIds((prev) => {
                        const next = new Set(prev);
                        if (e.target.checked) next.delete(ing.ingredientId);
                        else next.add(ing.ingredientId);
                        return next;
                      });
                      resetValidation();
                    }}
                  />
                  {ing.name}
                  {!ing.removable && <span className="hint"> (always included)</span>}
                </label>
              ))}
            </>
          )}
        </div>

        <div className="card">
          <h2>2. Extras</h2>
          {selectedPizza?.allowedExtras.map((extra) => (
            <QuantityRow
              key={extra.ingredientId}
              label={`${extra.name} (${extra.unit === "PORTION" ? "portions" : "pieces"})`}
              value={getQty(extra.ingredientId)}
              onChange={(q) => setQty(extra.ingredientId, extra.type, q)}
            />
          ))}
          {extraAdditions.length > 0 && (
            <p className="hint">
              AI also suggested: {extraAdditions.map((a) => `${a.ingredientId} ×${a.quantity}`).join(", ")}
            </p>
          )}
        </div>

        <div className="card">
          <h2>3. Anything else?</h2>
          <textarea
            placeholder='e.g. "no basil, and bake it extra crispy"'
            value={comment}
            onChange={(e) => {
              setComment(e.target.value);
              resetValidation();
            }}
            rows={4}
          />
          <p className="hint">Included automatically when you validate — no separate step needed.</p>
        </div>
      </div>

      <div className="card summary">
        <h2>Summary</h2>
        <p>Estimated total: €{computeTotal().toFixed(2)}</p>
        <button onClick={handleValidate} disabled={validating}>
          {validating ? "Validating…" : "Validate"}
        </button>

        {orderError && <p className="error-banner">{orderError}</p>}

        {validation && (
          <div className={`outcome outcome--${validation.outcome.toLowerCase()}`}>
            <strong>{validation.outcome.replaceAll("_", " ")}</strong>
            {validation.failures.length > 0 && (
              <ul>
                {validation.failures.map((f) => (
                  <li key={f.ruleId}>{f.messageDe}</li>
                ))}
              </ul>
            )}
            {validation.recommendations.length > 0 && (
              <p className="hint">
                Try instead:{" "}
                {validation.recommendations.map((r, idx) => (
                  <span key={r.id}>
                    {idx > 0 && ", "}
                    <button className="link-button" onClick={() => selectPizza(r.id)}>
                      {r.name}
                    </button>
                  </span>
                ))}
              </p>
            )}
            {validation.outcome === "MANUAL_REVIEW" && <p className="hint">This needs staff review before it can be placed.</p>}
          </div>
        )}

        {validation?.outcome === "APPROVED" && (
          <div className="checkout">
            <label>
              Phone number (for pickup text)
              <input value={phoneNumber} onChange={(e) => setPhoneNumber(e.target.value)} />
            </label>
            <label>
              Notes for the kitchen (shown on the board as-is)
              <input value={customNotes} onChange={(e) => setCustomNotes(e.target.value)} />
            </label>
            <button onClick={handlePlaceOrder} disabled={placingOrder}>
              {placingOrder ? "Placing order…" : "Place order"}
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

function QuantityRow({ label, value, onChange }: { label: string; value: number; onChange: (quantity: number) => void }) {
  return (
    <div className="quantity-row">
      <span>{label}</span>
      <div className="stepper">
        <button onClick={() => onChange(Math.max(0, value - 1))} disabled={value === 0}>
          −
        </button>
        <span>{value}</span>
        <button onClick={() => onChange(value + 1)}>+</button>
      </div>
    </div>
  );
}
