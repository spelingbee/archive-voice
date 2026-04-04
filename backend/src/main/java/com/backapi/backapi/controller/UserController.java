package com.backapi.backapi.controller;

import com.backapi.backapi.dto.response.ApiResponseWrapper;
import com.backapi.backapi.dto.response.UserResponse;
import com.backapi.backapi.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponseWrapper<UserResponse>> getMe() {
        return ResponseEntity.ok(ApiResponseWrapper.of(userService.getCurrentUser()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<ApiResponseWrapper<UserResponse>> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseWrapper.of(userService.getUserById(id)));
    }

    @GetMapping
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<ApiResponseWrapper<List<UserResponse>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponseWrapper.of(userService.getAllUsers()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
