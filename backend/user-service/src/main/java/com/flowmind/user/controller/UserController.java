package com.flowmind.user.controller;

import com.flowmind.common.core.ApiResponse;
import com.flowmind.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @GetMapping("/me")
    public ApiResponse<?> me(HttpServletRequest request) {
        Object username = request.getAttribute("currentUsername");
        return ApiResponse.success(service.me(username == null ? "admin" : username.toString()));
    }
}
