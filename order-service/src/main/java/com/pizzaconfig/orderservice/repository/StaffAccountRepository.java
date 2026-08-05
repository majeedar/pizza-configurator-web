package com.pizzaconfig.orderservice.repository;

import com.pizzaconfig.orderservice.domain.StaffAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StaffAccountRepository extends JpaRepository<StaffAccount, UUID> {

    Optional<StaffAccount> findByEmail(String email);

    boolean existsByEmail(String email);
}
