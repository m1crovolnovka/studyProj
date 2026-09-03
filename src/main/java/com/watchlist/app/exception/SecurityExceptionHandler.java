package com.watchlist.app.exception;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class SecurityExceptionHandler {

    @Bean
    AuthenticationEntryPoint authenticationEntryPoint(
            ObjectMapper objectMapper
    ) {
        return (request, response, exception) -> {

            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(
                    MediaType.APPLICATION_JSON_VALUE
            );

            objectMapper.writeValue(
                    response.getOutputStream(),
                    new SecurityErrorResponse(
                            401,
                            "Unauthorized",
                            "Authentication is required"
                    )
            );
        };
    }

    @Bean
    AccessDeniedHandler accessDeniedHandler(
            ObjectMapper objectMapper
    ) {
        return (request, response, exception) -> {

            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(
                    MediaType.APPLICATION_JSON_VALUE
            );

            objectMapper.writeValue(
                    response.getOutputStream(),
                    new SecurityErrorResponse(
                            403,
                            "Forbidden",
                            "Access denied"
                    )
            );
        };
    }

    public record SecurityErrorResponse(
            int status,
            String error,
            String message
    ) {
    }
}