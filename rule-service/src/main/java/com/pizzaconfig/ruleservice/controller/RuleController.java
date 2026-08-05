package com.pizzaconfig.ruleservice.controller;

import com.pizzaconfig.ruleservice.domain.ChangeRequest;
import com.pizzaconfig.ruleservice.domain.ValidationResult;
import com.pizzaconfig.ruleservice.service.RuleValidationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/rules")
public class RuleController {

    private final RuleValidationService ruleValidationService;

    public RuleController(RuleValidationService ruleValidationService) {
        this.ruleValidationService = ruleValidationService;
    }

    @PostMapping("/validate")
    public ValidationResult validate(@RequestBody ChangeRequest request) {
        return ruleValidationService.validate(request);
    }
}
