package com.yupi.aicodehelper.role;

public record RoleView(
        String roleId,
        String name,
        String category,
        String tagline,
        String description,
        String avatar,
        String avatarFallback,
        String image,
        String imageFallback,
        Integer sortOrder
) {

    public static RoleView from(RoleConfig config) {
        return new RoleView(
                config.getRoleId(),
                config.getName(),
                config.getCategory(),
                config.getTagline(),
                config.getDescription(),
                config.getAvatar(),
                config.getAvatarFallback(),
                config.getImage(),
                config.getImageFallback(),
                config.getSortOrder()
        );
    }
}
