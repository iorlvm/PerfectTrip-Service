package com.tibame.authentication.filter;

import com.tibame.authentication.service.TokenService;
import com.tibame.authentication.service.UserAuth;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Component
public class TokenParsingInterceptor implements HandlerInterceptor {
    @Autowired
    private TokenService tokenService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);

            UserAuth userAuth = tokenService.validateToken(token);
            if (userAuth != null) {
                tokenService.flashLoginExpire(token);
                HttpSession session =  request.getSession();
                session.setAttribute("username", userAuth.getUsername());
                session.setAttribute("userRole", userAuth.getRole());
            }
        }

        return true;
    }
}