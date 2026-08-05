package com.pizzaconfig.aiparserservice.controller;

import com.pizzaconfig.aiparserservice.dto.ParseRequest;
import com.pizzaconfig.aiparserservice.dto.ParsedProposal;
import com.pizzaconfig.aiparserservice.service.AiParserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/ai-parser")
public class AiParserController {

    private final AiParserService aiParserService;

    public AiParserController(AiParserService aiParserService) {
        this.aiParserService = aiParserService;
    }

    @PostMapping("/parse")
    public ParsedProposal parse(@RequestBody ParseRequest request) {
        return aiParserService.parse(request);
    }
}
