package com.tibame.authentication.service.impl;

import com.tibame.authentication.service.TokenService;
import com.tibame.authentication.service.UserAuth;

import java.util.Map;

public class TokenServiceImplJwt implements TokenService {
    @Override
    public <T extends UserAuth> String setToken(T user) {
        return null;
    }

    @Override
    public UserAuth validateToken(String token) {
        return null;
    }

    @Override
    public void flashLoginExpire(String token) {

    }
}
