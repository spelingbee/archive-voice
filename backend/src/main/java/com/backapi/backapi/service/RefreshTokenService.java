package com.backapi.backapi.service;

import com.backapi.backapi.entity.RefreshToken;
import com.backapi.backapi.entity.User;
import com.backapi.backapi.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    /**
     * Создаёт новый Refresh Token для пользователя.
     * Сначала удаляет все предыдущие токены этого пользователя.
     */
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        // Удаляем ВСЕ старые refresh-токены этого пользователя
        refreshTokenRepository.deleteByUser(user);

        // Создаём новый токен
        RefreshToken newToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshExpiration))
                .build();

        return refreshTokenRepository.save(newToken);
    }

    /**
     * Проверяет, не истёк ли токен. Если истёк — удаляет его.
     */
    @Transactional
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh token expired. Please login again.");
        }
        return token;
    }


}