package com.yupi.aicodehelper.controller;

import com.yupi.aicodehelper.common.BaseResponse;
import com.yupi.aicodehelper.role.RoleService;
import com.yupi.aicodehelper.role.RoleView;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/roles")
public class RoleController {

    @Resource
    private RoleService roleService;

    @GetMapping
    public BaseResponse<List<RoleView>> listRoles() {
        return BaseResponse.success(roleService.listRoles());
    }

    @GetMapping("/{roleId}")
    public BaseResponse<RoleView> getRole(@PathVariable String roleId) {
        return BaseResponse.success(roleService.getRole(roleId));
    }
}
