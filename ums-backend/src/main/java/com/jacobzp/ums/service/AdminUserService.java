package com.jacobzp.ums.service;

import com.jacobzp.ums.dto.AdminUserPage;
import com.jacobzp.ums.dto.AdminUserQuery;

/**
 * <p>
 * 描述：用户管理服务层接口
 * </p>
 * @author Jacobzp
 * @version 1.0.0
 */
public interface AdminUserService {
    /**
     * <p>
     * 权限结果检查
     * </p>
     * <p>- ALLOWED：允许访问</p>
     * <p>- UNAUTHENTICATED：未通过身份认证</p>
     * <p>- FORBIDDEN：禁止访问</p>
     */
    enum Access { ALLOWED, UNAUTHENTICATED, FORBIDDEN }

    /**
     * <p>
     * 删除用户结果
     * </p>
     * <p>- DELETED：已删除</p>
     * <p>- NOT_FOUND：未找到用户</p>
     * <p>- ADMIN_PROTECTED：不允许删除，管理员受保护</p>
     */
    enum DeleteResult { DELETED, NOT_FOUND, ADMIN_PROTECTED }

    Access checkAccess(long userId);

    AdminUserPage list(AdminUserQuery query);

    DeleteResult delete(long id);
}
