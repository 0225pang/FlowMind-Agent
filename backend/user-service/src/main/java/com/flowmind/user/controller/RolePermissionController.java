package com.flowmind.user.controller;

import com.flowmind.common.core.ApiResponse;
import com.flowmind.user.dto.RolePermissionUpdateRequest;
import com.flowmind.user.service.RolePermissionService;
import org.springframework.web.bind.annotation.*;

@RestController
public class RolePermissionController {
    private final RolePermissionService service;

    public RolePermissionController(RolePermissionService service) {
        this.service = service;
    }

    @GetMapping("/api/roles")
    public ApiResponse<?> roles() {
        return ApiResponse.success(service.roles());
    }

    @GetMapping("/api/roles/{roleCode}/permissions")
    public ApiResponse<?> rolePermissions(@PathVariable String roleCode) {
        return ApiResponse.success(service.permissionsOfRole(roleCode));
    }

    @PutMapping("/api/roles/{roleCode}/permissions")
    public ApiResponse<?> updateRolePermissions(@PathVariable String roleCode,
                                                @RequestBody RolePermissionUpdateRequest request) {
        try {
            return ApiResponse.success(service.updateRolePermissions(roleCode, request.getPermissionCodes()));
        } catch (IllegalArgumentException ex) {
            return ApiResponse.failed(ex.getMessage());
        }
    }

    @GetMapping("/api/permissions")
    public ApiResponse<?> permissions() {
        return ApiResponse.success(service.permissions());
    }
}
