package com.yupi.aicodehelper.role;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yupi.aicodehelper.common.ErrorCode;
import com.yupi.aicodehelper.config.properties.AppRolesProperties;
import com.yupi.aicodehelper.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RoleService {

    private final ResourcePatternResolver resourcePatternResolver;

    private final ResourceLoader resourceLoader;

    private final ObjectMapper objectMapper;

    private final AppRolesProperties appRolesProperties;

    private final Map<String, RoleConfig> roleConfigMap = new LinkedHashMap<>();

    private final Map<String, String> roleDocCache = new ConcurrentHashMap<>();

    public RoleService(ResourcePatternResolver resourcePatternResolver,
                       ResourceLoader resourceLoader,
                       ObjectMapper objectMapper,
                       AppRolesProperties appRolesProperties) {
        this.resourcePatternResolver = resourcePatternResolver;
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
        this.appRolesProperties = appRolesProperties;
    }

    @PostConstruct
    public void init() {
        loadRoleConfigs();
    }

    public List<RoleConfig> listRoleConfigs() {
        return new ArrayList<>(roleConfigMap.values());
    }

    public List<RoleView> listRoles() {
        return roleConfigMap.values().stream().map(RoleView::from).toList();
    }

    public RoleView getRole(String roleId) {
        if (roleId == null || roleId.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "roleId 不能为空");
        }
        RoleConfig roleConfig = roleConfigMap.get(roleId);
        if (roleConfig == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "角色不存在: " + roleId);
        }
        return RoleView.from(roleConfig);
    }

    public String buildRoleCard(String roleId) {
        RoleConfig roleConfig = getRoleConfig(roleId);
        String baseCard = safeText(roleConfig.getSystemPromptTemplate());
        String roleDocPath = roleConfig.getRoleDocPath();
        if (roleDocPath == null || roleDocPath.isBlank()) {
            return baseCard;
        }
        String roleDoc = roleDocCache.computeIfAbsent(roleConfig.getRoleId(), key -> loadRoleDoc(roleDocPath));
        if (roleDoc.isBlank()) {
            return baseCard;
        }
        return baseCard + "\n\n以下是该角色的补充设定资料，请优先参考：\n" + roleDoc;
    }

    private RoleConfig getRoleConfig(String roleId) {
        String defaultRoleId = appRolesProperties.getDefaultRoleId();
        String resolvedRoleId = (roleId == null || roleId.isBlank()) ? defaultRoleId : roleId;
        RoleConfig roleConfig = roleConfigMap.get(resolvedRoleId);
        if (roleConfig != null) {
            return roleConfig;
        }
        RoleConfig defaultRole = roleConfigMap.get(defaultRoleId);
        if (defaultRole != null) {
            return defaultRole;
        }
        throw new IllegalStateException("No role configuration is available");
    }

    private void loadRoleConfigs() {
        try {
            String roleConfigPath = appRolesProperties.getConfigPath();
            Resource[] resources = resourcePatternResolver.getResources(roleConfigPath);
            List<RoleConfig> roleConfigs = new ArrayList<>();
            for (Resource resource : resources) {
                if (!resource.exists()) {
                    continue;
                }
                try (InputStream inputStream = resource.getInputStream()) {
                    RoleConfig roleConfig = objectMapper.readValue(inputStream, RoleConfig.class);
                    if (roleConfig.getEnabled() != null && !roleConfig.getEnabled()) {
                        continue;
                    }
                    if (roleConfig.getRoleId() == null || roleConfig.getRoleId().isBlank()) {
                        continue;
                    }
                    roleConfigs.add(roleConfig);
                }
            }
            roleConfigs.sort(Comparator.comparing(config -> config.getSortOrder() == null ? Integer.MAX_VALUE : config.getSortOrder()));
            roleConfigMap.clear();
            for (RoleConfig roleConfig : roleConfigs) {
                roleConfigMap.put(roleConfig.getRoleId(), roleConfig);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load role configs from " + appRolesProperties.getConfigPath(), e);
        }
    }

    private String loadRoleDoc(String path) {
        Resource resource = resourceLoader.getResource(path);
        if (!resource.exists()) {
            return "";
        }
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            return "";
        }
    }

    private String safeText(String text) {
        return text == null ? "" : text.trim();
    }
}
