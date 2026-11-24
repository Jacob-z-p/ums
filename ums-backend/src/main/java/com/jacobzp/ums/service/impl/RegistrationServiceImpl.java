package com.jacobzp.ums.service.impl;

import com.jacobzp.ums.entity.User;
import com.jacobzp.ums.model.dto.RegistrationDTO;
import com.jacobzp.ums.mapper.UserMapper;
import com.jacobzp.ums.exception.AccountAlreadyExistsException;
import com.jacobzp.ums.service.RegistrationService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 描述：注册服务实现类
 * </p>
 * @author Jacobzp
 * @version 1.0.0
 */
@Service
public class RegistrationServiceImpl implements RegistrationService {
    private final UserMapper userMapper;
    private final Pbkdf2PasswordEncoder passwordEncoder = Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();

    public RegistrationServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public Long register(RegistrationDTO request) {
        User user = new User();
        user.setAccount(request.getAccount());
        user.setNickname(request.getAccount()); // 注册时暂时登录账号和用户昵称一样，后面用户可以自行修改
        user.setPasswordHash(passwordEncoder.encode(request.getPassword())); // 加密保存
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException ex) {
            throw new AccountAlreadyExistsException(ex);
        }
        return user.getId();
    }
}
