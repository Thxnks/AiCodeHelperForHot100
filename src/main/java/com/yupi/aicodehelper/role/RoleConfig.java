package com.yupi.aicodehelper.role;

import lombok.Data;

import java.util.List;

@Data
public class RoleConfig {

    private String roleId;

    private String name;

    /**
     * professional / extended
     */
    private String category;

    private String tagline;

    private String description;

    private String avatar;

    private String avatarFallback;

    private String image;

    private String imageFallback;

    private String systemPromptTemplate;

    private List<String> forbiddenBehaviors;

    private List<String> knowledgeScopes;

    private List<String> toolScopes;

    private String uiStyle;

    private String roleDocPath;

    private Integer sortOrder;

    private Boolean enabled;
}
