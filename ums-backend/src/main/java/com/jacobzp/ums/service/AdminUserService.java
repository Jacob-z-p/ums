package com.jacobzp.ums.service;

import com.jacobzp.ums.dto.AdminUserPage;
import com.jacobzp.ums.dto.AdminUserQuery;

public interface AdminUserService {
    enum Access { ALLOWED, UNAUTHENTICATED, FORBIDDEN }

    enum DeleteResult { DELETED, NOT_FOUND, ADMIN_PROTECTED }

    Access checkAccess(long userId);

    AdminUserPage list(AdminUserQuery query);

    DeleteResult delete(long id);
}
