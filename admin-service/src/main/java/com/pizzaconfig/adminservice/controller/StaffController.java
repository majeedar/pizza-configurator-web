package com.pizzaconfig.adminservice.controller;

import com.pizzaconfig.adminservice.client.StaffAccountClient.StaffAccountCreateResultDto;
import com.pizzaconfig.adminservice.client.StaffAccountClient.StaffAccountDto;
import com.pizzaconfig.adminservice.service.AdminStaffService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/staff")
public class StaffController {

    private final AdminStaffService service;

    public StaffController(AdminStaffService service) {
        this.service = service;
    }

    @GetMapping
    public List<StaffAccountDto> list() {
        return service.findAll();
    }

    @PostMapping
    public ResponseEntity<StaffAccountCreateResultDto> create(@RequestBody CreateStaffRequest request) {
        StaffAccountCreateResultDto created = service.create(request.email(), request.fullName(), request.role());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<StaffAccountDto> update(@PathVariable UUID id, @RequestBody UpdateStaffRequest request) {
        return service.update(id, request.fullName(), request.role())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        return service.delete(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    public record CreateStaffRequest(String email, String fullName, String role) {
    }

    public record UpdateStaffRequest(String fullName, String role) {
    }
}
