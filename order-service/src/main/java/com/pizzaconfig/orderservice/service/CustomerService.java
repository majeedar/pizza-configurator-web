package com.pizzaconfig.orderservice.service;

import com.pizzaconfig.orderservice.domain.Customer;
import com.pizzaconfig.orderservice.dto.CustomerRegistrationRequest;
import com.pizzaconfig.orderservice.repository.CustomerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;

@Service
public class CustomerService {

    private final CustomerRepository repository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public CustomerService(CustomerRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Customer register(CustomerRegistrationRequest request) {
        if (repository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists.");
        }
        Customer customer = new Customer(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.fullName(),
                request.phoneNumber(),
                Instant.now());
        return repository.save(customer);
    }

    public Optional<Customer> authenticate(String email, String password) {
        return repository.findByEmail(email)
                .filter(customer -> passwordEncoder.matches(password, customer.getPasswordHash()));
    }
}
