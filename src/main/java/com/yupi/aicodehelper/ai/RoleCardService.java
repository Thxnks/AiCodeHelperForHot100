package com.yupi.aicodehelper.ai;

import com.yupi.aicodehelper.role.RoleService;
import org.springframework.stereotype.Service;

@Service
public class RoleCardService {

    private final RoleService roleService;

    public RoleCardService(RoleService roleService) {
        this.roleService = roleService;
    }

    public String getRoleCard(String roleId) {
        return roleService.buildRoleCard(roleId);
    }
}
