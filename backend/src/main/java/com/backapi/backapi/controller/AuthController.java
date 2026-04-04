package com.backapi.backapi.controller;

import com.backapi.backapi.dto.request.LoginRequest;
import com.backapi.backapi.dto.request.RegisterRequest;
import com.backapi.backapi.dto.response.ApiResponseWrapper;
import com.backapi.backapi.dto.response.AuthResponse;
import com.backapi.backapi.dto.response.UserResponse;
import com.backapi.backapi.entity.User;
import com.backapi.backapi.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(true) // Обязательно для SameSite=None
                .path("/")
                .maxAge(7 * 24 * 60 * 60)
                .sameSite("None") // Меняем Strict на None для работы localhost + ngrok
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponseWrapper<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response) {

        AuthResponse auth = authService.register(request);
        addRefreshTokenCookie(response, auth.getRefreshToken());
        auth.setRefreshToken(null);
        return ResponseEntity.ok(ApiResponseWrapper.of(auth, "Регистрация успешна"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponseWrapper<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        AuthResponse auth = authService.login(request);
        addRefreshTokenCookie(response, auth.getRefreshToken());
        auth.setRefreshToken(null);
        return ResponseEntity.ok(ApiResponseWrapper.of(auth));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponseWrapper<AuthResponse>> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {

        String refreshToken = null;
        if (request.getCookies() != null) {
            refreshToken = Arrays.stream(request.getCookies())
                    .filter(c -> "refreshToken".equals(c.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }

        if (refreshToken == null || refreshToken.isBlank()) {
            log.warn("Refresh token отсутствует в cookies");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            AuthResponse auth = authService.refreshToken(refreshToken);
            addRefreshTokenCookie(response, auth.getRefreshToken());
            auth.setRefreshToken(null);
            return ResponseEntity.ok(ApiResponseWrapper.of(auth));
        } catch (Exception e) {
            log.error("Ошибка при обновлении токена: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest request,
            HttpServletResponse response) {

        if (request.getCookies() != null) {
            Arrays.stream(request.getCookies())
                    .filter(c -> "refreshToken".equals(c.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .ifPresent(authService::logout);
        }

        Cookie cookie = new Cookie("refreshToken", "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponseWrapper<UserResponse>> me() {
        User user = (User) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        UserResponse userResponse = UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();

        return ResponseEntity.ok(ApiResponseWrapper.of(userResponse));
    }
}