package com.jacobzp.ums.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jacobzp.ums.entity.User;
import com.jacobzp.ums.model.dto.LoginDTO;
import com.jacobzp.ums.model.vo.LoginVO;
import com.jacobzp.ums.mapper.UserMapper;
import com.jacobzp.ums.service.LoginService;
import java.util.Optional;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class LoginServiceImpl implements LoginService {
    private final UserMapper userMapper;
    private final Pbkdf2PasswordEncoder passwordEncoder = Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();

    public LoginServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public Optional<LoginVO> login(LoginDTO request) {
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getAccount, request.getAccount()));
        if (!isActive(user)
                || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            return Optional.empty();
        }
        return Optional.of(toResponse(user));
    }

    /**
     * 根据 userId 查找未禁用的用户
     * @param userId 用户 ID
     * @return 登录响应体
     */
    @Override
    public Optional<LoginVO> findActiveUser(long userId) {
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                .select(User::getId, User::getAccount, User::getNickname, User::getAvatarUrl, User::getRole)
                .eq(User::getId, userId)
                .eq(User::getStatus, 0));
        return Optional.ofNullable(user).map(this::toResponse);
    }

    private boolean isActive(User user) {
        return user != null && Integer.valueOf(0).equals(user.getStatus());
    }

    private LoginVO toResponse(User user) {
        return new LoginVO(user.getId(), user.getAccount(), user.getNickname(),
                user.getAvatarUrl(), user.getRole());
    }
}
