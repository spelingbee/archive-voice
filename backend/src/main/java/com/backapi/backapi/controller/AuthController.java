package com.backapi.backapi.controller;

import com.backapi.backapi.dto.request.LoginRequest;
import com.backapi.backapi.dto.request.RefreshTokenRequest;
import com.backapi.backapi.dto.request.RegisterRequest;
import com.backapi.backapi.dto.response.AuthResponse;
import com.backapi.backapi.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie("refreshToken", refreshToken);
        cookie.setHttpOnly(true);           // ← главное!
        cookie.setSecure(true);             // HTTPS только (в продакшене)
        cookie.setPath("/");
        cookie.setMaxAge(7 * 24 * 60 * 60); // 7 дней
        cookie.setAttribute("SameSite", "Strict"); // или "Lax"
        response.addCookie(cookie);
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response) {

        AuthResponse authResponse = authService.register(request);
        addRefreshTokenCookie(response, authResponse.getRefreshToken()); // временно берём из сервиса
        // Убираем refreshToken из ответа
        authResponse.setRefreshToken(null);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        AuthResponse authResponse = authService.login(request);
        addRefreshTokenCookie(response, authResponse.getRefreshToken());
        authResponse.setRefreshToken(null);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @RequestBody RefreshTokenRequest request,   // можно убрать тело и читать @CookieValue
            HttpServletResponse response) {

        AuthResponse authResponse = authService.refreshToken(request);
        addRefreshTokenCookie(response, authResponse.getRefreshToken());
        authResponse.setRefreshToken(null);
        return ResponseEntity.ok(authResponse);
    }
}