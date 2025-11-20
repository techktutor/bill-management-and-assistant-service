package com.wells.bill.assistant.controller;

import com.wells.bill.assistant.entity.CardDTO;
import com.wells.bill.assistant.entity.TokenResponse;
import com.wells.bill.assistant.service.TokenizationService;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/v1/tokens")
public class TokenController {

    private final TokenizationService tokenService;

    public TokenController(TokenizationService tokenService) {
        this.tokenService = tokenService;
    }

    @PostMapping
    public TokenResponse createToken(@RequestBody CardDTO card) {
        return tokenService.tokenize(card);
    }
}
