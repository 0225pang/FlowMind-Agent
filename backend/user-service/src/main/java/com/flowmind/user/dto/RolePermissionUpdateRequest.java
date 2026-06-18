package com.flowmind.user.dto;

import java.util.ArrayList;
import java.util.List;

public class RolePermissionUpdateRequest {
    private List<String> permissionCodes = new ArrayList<>();

    public List<String> getPermissionCodes() {
        return permissionCodes;
    }

    public void setPermissionCodes(List<String> permissionCodes) {
        this.permissionCodes = permissionCodes == null ? new ArrayList<>() : permissionCodes;
    }
}
