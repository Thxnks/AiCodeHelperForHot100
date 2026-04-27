package com.yupi.aicodehelper.auth;

import com.yupi.aicodehelper.common.ErrorCode;
import com.yupi.aicodehelper.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    public Long requireUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUserPrincipal principal)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Please login first");
        }
        return principal.userId();
    }

    public Long getCurrentUserIdOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUserPrincipal principal)) {
            return null;
        }
        return principal.userId();
    }
}

