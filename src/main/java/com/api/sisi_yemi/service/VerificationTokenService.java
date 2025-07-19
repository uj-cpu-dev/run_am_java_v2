package com.api.sisi_yemi.service;

import com.api.sisi_yemi.model.VerificationToken;
import com.api.sisi_yemi.util.dynamodb.DynamoDbHelper;
import com.api.sisi_yemi.util.dynamodb.DynamoDbHelperFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
public class VerificationTokenService {

    private final DynamoDbHelper<VerificationToken> dbHelper;

    public VerificationTokenService(DynamoDbHelperFactory factory) {
        this.dbHelper = factory.getHelper("verification_tokens", VerificationToken.class);
    }

    public void save(VerificationToken token) {
        dbHelper.save(token);
    }

    public Optional<VerificationToken> findById(String id) {
        return dbHelper.getById(id);
    }

    public Optional<VerificationToken> findByToken(String token) {
        return dbHelper.queryByGsi("token-index", "token", token).stream().findFirst();
    }

    public void deleteById(String id) {
        dbHelper.deleteById(id);
    }

    public Optional<VerificationToken> update(String id, VerificationToken updatedFields) {
        return dbHelper.updateById(id, existing -> {
            existing.setToken(updatedFields.getToken());
            existing.setUserId(updatedFields.getUserId());
            existing.setExpiryDate(updatedFields.getExpiryDate());
        });
    }

    public void saveToken(String token, String userId) {

        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setId(UUID.randomUUID().toString());
        verificationToken.setToken(token);
        verificationToken.setUserId(userId);
        verificationToken.setExpiryDate(Instant.now().plus(3, ChronoUnit.HOURS));

        dbHelper.save(verificationToken);
    }

    public void deleteByToken(String token) {
        findByToken(token).ifPresent(t -> dbHelper.deleteById(t.getId()));
    }
}
