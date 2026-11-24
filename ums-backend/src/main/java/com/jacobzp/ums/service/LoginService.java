package com.jacobzp.ums.service;

import com.jacobzp.ums.dto.LoginRequest;
import com.jacobzp.ums.dto.LoginResponse;
import java.util.Optional;

/**
 * <p>
 * 描述：登录服务层接口
 * </p>
 * @author Jacobzp
 * @version 1.0.0
 */
public interface LoginService {
    Optional<LoginResponse> findActiveUser(long userId);
    Optional<LoginResponse> login(LoginRequest request);
}
