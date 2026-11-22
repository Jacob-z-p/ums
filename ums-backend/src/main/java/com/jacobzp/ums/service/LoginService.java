package com.jacobzp.ums.service;

import com.jacobzp.ums.dto.LoginRequest;
import com.jacobzp.ums.dto.LoginResponse;
import java.util.Optional;

public interface LoginService {
    Optional<LoginResponse> login(LoginRequest request);
}
