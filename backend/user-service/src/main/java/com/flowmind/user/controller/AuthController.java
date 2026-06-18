package com.flowmind.user.controller;

import com.flowmind.common.core.ApiResponse;
import com.flowmind.user.dto.LoginRequest;
import com.flowmind.user.service.UserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService service;

    public AuthController(UserService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public ApiResponse<?> login(@RequestBody LoginRequest req) {
        try {
            return ApiResponse.success(service.login(req));
        } catch (IllegalArgumentException ex) {
            return ApiResponse.failed(ex.getMessage());
        } catch (Exception ex) {
            return ApiResponse.failed(ex.getMessage());
        }
    }

    @PostMapping("/register")
    public ApiResponse<?> register(@RequestBody LoginRequest req) {
        try {
            return ApiResponse.success(service.register(req));
        } catch (IllegalArgumentException ex) {
            return ApiResponse.failed(ex.getMessage());
        } catch (Exception ex) {
            return ApiResponse.failed(ex.getMessage());
        }
    }
}
