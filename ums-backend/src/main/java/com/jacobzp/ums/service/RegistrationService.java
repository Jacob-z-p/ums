package com.jacobzp.ums.service;

import com.jacobzp.ums.dto.RegistrationRequest;

/**
 * <p>
 * 描述：注册服务层
 * </p>
 * @author Jacobzp
 * @version 1.0.0
 */
public interface RegistrationService {
    Long register(RegistrationRequest request);
}
