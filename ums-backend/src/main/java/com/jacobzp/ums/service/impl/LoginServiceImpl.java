package com.jacobzp.ums.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jacobzp.ums.entity.User;
import com.jacobzp.ums.dto.LoginRequest;
import com.jacobzp.ums.dto.LoginResponse;
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
    public Optional<LoginResponse> login(LoginRequest request) {
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getAccount, request.getAccount()));
        if (user == null || !Integer.valueOf(0).equals(user.getStatus())
                || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            return Optional.empty();
        }
        return Optional.of(new LoginResponse(user.getId(), user.getAccount(), user.getNickname(),
                user.getAvatarUrl(), user.getRole()));
    }
}
