package com.wells.bill.assistant.service;

import com.wells.bill.assistant.entity.CardDTO;
import com.wells.bill.assistant.entity.CardToken;
import com.wells.bill.assistant.entity.TokenResponse;
import com.wells.bill.assistant.repository.CardTokenRepository;
import com.wells.bill.assistant.util.CardBrandUtil;
import com.wells.bill.assistant.util.FingerprintUtil;
import com.wells.bill.assistant.util.JsonUtil;
import com.wells.bill.assistant.util.LuhnUtil;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

@Service
public class TokenizationService {

    private final CardTokenRepository cardTokenRepo;
    private final TextEncryptor encryptor =
            Encryptors.text("my-secret-key", "12345678"); // Sample only

    public TokenizationService(CardTokenRepository cardTokenRepo) {
        this.cardTokenRepo = cardTokenRepo;
    }

    public TokenResponse tokenize(CardDTO dto) {
        // 1. Validate card
        if (!LuhnUtil.isValid(dto.getCardNumber())) {
            throw new RuntimeException("Invalid card number");
        }

        String last4 = dto.getCardNumber().substring(dto.getCardNumber().length() - 4);
        String brand = CardBrandUtil.detect(dto.getCardNumber());

        // 2. Generate token
        String token = "tok_" + UUID.randomUUID();


        // 3. Encrypt payload
        String payload = JsonUtil.toJson(dto);
        byte[] encrypted = encryptor.encrypt(payload).getBytes(StandardCharsets.UTF_8);

        CardToken ct = new CardToken();
        ct.setCardId(UUID.randomUUID());
        ct.setToken(token);
        ct.setLast4(last4);
        ct.setBrand(brand);
        ct.setExpMonth(Integer.parseInt(dto.getExpMonth()));
        ct.setExpYear(Integer.parseInt(dto.getExpYear()));
        ct.setFingerprint(FingerprintUtil.hash(dto.getCardNumber()));
        ct.setEncryptedPayload(encrypted);
        ct.setCreatedAt(Instant.now());

        cardTokenRepo.save(ct);

        return new TokenResponse(token, last4, brand,
                ct.getExpMonth(), ct.getExpYear());
    }
}
