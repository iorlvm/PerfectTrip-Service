package com.tibame.authentication.filter;

import com.tibame.authentication.service.TokenService;
import com.tibame.authentication.service.UserAuth;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.tibame.utils.Constants.ROLE_ADMIN;

public class AdminLoginInterceptor implements HandlerInterceptor {
    @Autowired
    private TokenService tokenService;
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);

            UserAuth userAuth = tokenService.validateToken(token);
            if (userAuth != null && ROLE_ADMIN.equals(userAuth.getRole())) {
                return true;
            }
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return false;
    }
}
