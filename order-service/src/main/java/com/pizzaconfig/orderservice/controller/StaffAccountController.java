package com.pizzaconfig.orderservice.controller;

import com.pizzaconfig.orderservice.dto.ChangePasswordRequest;
import com.pizzaconfig.orderservice.dto.StaffAccountCreateRequest;
import com.pizzaconfig.orderservice.dto.StaffAccountCreateResponse;
import com.pizzaconfig.orderservice.dto.StaffAccountResponse;
import com.pizzaconfig.orderservice.dto.StaffAccountUpdateRequest;
import com.pizzaconfig.orderservice.dto.StaffAuthRequest;
import com.pizzaconfig.orderservice.service.StaffAccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

// /authenticate is internal (called only by the gateway's AuthController, same trust
// boundary as CustomerController's /authenticate). Everything else under this path is
// reached only via the gateway's admin-service-proxied /v1/admin/staff/** route,
// except /me/password, which is a direct, self-service-only gateway route.
@RestController
@RequestMapping("/v1/orders/staff-accounts")
public class StaffAccountController {

    private final StaffAccountService service;

    public StaffAccountController(StaffAccountService service) {
        this.service = service;
    }

    @GetMapping
    public List<StaffAccountResponse> list() {
        return service.findAll().stream().map(StaffAccountResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<StaffAccountCreateResponse> create(@RequestBody StaffAccountCreateRequest request) {
        StaffAccountService.CreatedStaffAccount created = service.create(request.email(), request.fullName(), request.role());
        StaffAccountCreateResponse response = new StaffAccountCreateResponse(
                StaffAccountResponse.from(created.account()), created.temporaryPassword());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<StaffAccountResponse> update(@PathVariable UUID id, @RequestBody StaffAccountUpdateRequest request) {
        return service.updateProfile(id, request.fullName(), request.role())
                .map(StaffAccountResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        return service.delete(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @PostMapping("/authenticate")
    public ResponseEntity<StaffAccountResponse> authenticate(@RequestBody StaffAuthRequest request) {
        return service.authenticate(request.email(), request.password())
                .map(StaffAccountResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @PutMapping("/me/password")
    public ResponseEntity<StaffAccountResponse> changeOwnPassword(@RequestHeader("X-Staff-Id") UUID staffId,
                                                                    @RequestBody ChangePasswordRequest request) {
        return service.changePassword(staffId, request.newPassword())
                .map(StaffAccountResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
