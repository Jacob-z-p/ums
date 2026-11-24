package com.jacobzp.ums.service;

import com.jacobzp.ums.model.dto.LoginDTO;
import com.jacobzp.ums.model.vo.LoginVO;
import java.util.Optional;

/**
 * <p>
 * 描述：登录服务层接口
 * </p>
 * @author Jacobzp
 * @version 1.0.0
 */
public interface LoginService {
    Optional<LoginVO> findActiveUser(long userId);
    Optional<LoginVO> login(LoginDTO request);
}
