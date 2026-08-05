package com.pizzaconfig.orderservice.controller;

import com.pizzaconfig.orderservice.domain.Customer;
import com.pizzaconfig.orderservice.dto.CustomerAuthRequest;
import com.pizzaconfig.orderservice.dto.CustomerProfileResponse;
import com.pizzaconfig.orderservice.dto.CustomerRegistrationRequest;
import com.pizzaconfig.orderservice.service.CustomerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// /register is exposed publicly via the gateway's /v1/auth/register proxy route.
// /authenticate is internal — only the gateway's AuthController calls it (to verify
// credentials before minting a JWT), the same trust boundary every other internal
// service call in this system relies on already.
@RestController
@RequestMapping("/v1/orders/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping("/register")
    public ResponseEntity<CustomerProfileResponse> register(@RequestBody CustomerRegistrationRequest request) {
        Customer customer = customerService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(CustomerProfileResponse.from(customer));
    }

    @PostMapping("/authenticate")
    public ResponseEntity<CustomerProfileResponse> authenticate(@RequestBody CustomerAuthRequest request) {
        return customerService.authenticate(request.email(), request.password())
                .map(CustomerProfileResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }
}
