package com.pizzaconfig.ruleservice.rules;

import com.pizzaconfig.ruleservice.domain.ChangeRequest;
import com.pizzaconfig.ruleservice.domain.FailedRule;

import java.util.Optional;

public interface Rule {
    Optional<FailedRule> check(ChangeRequest request);
}
