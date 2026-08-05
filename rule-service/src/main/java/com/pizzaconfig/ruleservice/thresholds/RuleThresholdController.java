package com.pizzaconfig.ruleservice.thresholds;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/rules/thresholds")
public class RuleThresholdController {

    private final RuleThresholdService service;

    public RuleThresholdController(RuleThresholdService service) {
        this.service = service;
    }

    @GetMapping
    public List<RuleThresholdResponse> getAll() {
        return service.findAll().stream().map(RuleThresholdResponse::from).toList();
    }

    @PutMapping("/{ruleId}")
    public ResponseEntity<RuleThresholdResponse> update(@PathVariable String ruleId,
                                                         @RequestBody UpdateThresholdRequest request) {
        return service.updateThreshold(ruleId, request.value())
                .map(RuleThresholdResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    public record UpdateThresholdRequest(String value) {
    }
}
