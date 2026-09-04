package com.watchlist.app.controller;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.watchlist.app.domain.AppUser;
import com.watchlist.app.dto.AuthResponse;
import com.watchlist.app.dto.LoginRequest;
import com.watchlist.app.dto.RefreshRequest;
import com.watchlist.app.dto.RegisterRequest;
import com.watchlist.app.service.AuthService;
import com.watchlist.app.service.JwtService;
import com.watchlist.app.service.RefreshTokenService;
import com.watchlist.app.service.RotatedRefreshToken;
import com.watchlist.app.repository.AppUserRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    private final AuthenticationManager authenticationManager;
    private final AppUserRepository appUserRepository;
    private final AuthService authService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    private final Duration accessTokenLifetime;
    private final Duration refreshTokenLifetime;
    private final boolean secureCookie;

    public AuthController(
            AuthenticationManager authenticationManager,
            AppUserRepository appUserRepository,
            AuthService authService,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            @Value("${jwt.access-token-lifetime}") Duration accessTokenLifetime,
            @Value("${jwt.refresh-token-lifetime}") Duration refreshTokenLifetime,
            @Value("${jwt.secure-cookie}") boolean secureCookie
    ) {
        this.authenticationManager = authenticationManager;
        this.appUserRepository = appUserRepository;
        this.authService = authService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.accessTokenLifetime = accessTokenLifetime;
        this.refreshTokenLifetime = refreshTokenLifetime;
        this.secureCookie = secureCookie;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {

        Authentication authentication =
                authenticationManager.authenticate(
                        UsernamePasswordAuthenticationToken.unauthenticated(
                                request.username(),
                                request.password()
                        )
                );

        AppUser user = appUserRepository
                .findByUsername(authentication.getName())
                .orElseThrow();

        String accessToken =
                jwtService.generateAccessToken(user);

        String refreshToken =
                refreshTokenService.create(user);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshCookie(refreshToken).toString()
                )
                .body(
                        new AuthResponse(
                                accessToken,
                                "Bearer",
                                accessTokenLifetime.toSeconds(),
                                refreshToken
                        )
                );
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {

        AppUser user = authService.register(request);

        String accessToken =
                jwtService.generateAccessToken(user);

        String refreshToken =
                refreshTokenService.create(user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshCookie(refreshToken).toString()
                )
                .body(
                        new AuthResponse(
                                accessToken,
                                "Bearer",
                                accessTokenLifetime.toSeconds(),
                                refreshToken
                        )
                );
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(
                    name = REFRESH_TOKEN_COOKIE,
                    required = false
            )
            String cookieRefreshToken,
            @Valid @RequestBody(required = false)
            RefreshRequest body
    ) {

        String refreshToken = resolveRefreshToken(
                cookieRefreshToken,
                body
        );

        RotatedRefreshToken rotated =
                refreshTokenService.rotate(refreshToken);

        AppUser user =
                rotated.token().getUser();

        String accessToken =
                jwtService.generateAccessToken(user);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshCookie(rotated.rawToken()).toString()
                )
                .body(
                        new AuthResponse(
                                accessToken,
                                "Bearer",
                                accessTokenLifetime.toSeconds(),
                                rotated.rawToken()
                        )
                );
    }

    private String resolveRefreshToken(
            String cookieRefreshToken,
            RefreshRequest body
    ) {

        if (cookieRefreshToken != null && !cookieRefreshToken.isBlank()) {
            return cookieRefreshToken;
        }

        if (body != null && body.refreshToken() != null
                && !body.refreshToken().isBlank()) {
            return body.refreshToken();
        }

        throw new IllegalArgumentException(
                "Refresh token is missing"
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(
                    name = REFRESH_TOKEN_COOKIE,
                    required = false
            )
            String refreshToken
    ) {

        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenService.revoke(refreshToken);
        }

        ResponseCookie cookie = ResponseCookie
                .from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(Duration.ZERO)
                .build();

        return ResponseEntity.noContent()
                .header(
                        HttpHeaders.SET_COOKIE,
                        cookie.toString()
                )
                .build();
    }

    private ResponseCookie refreshCookie(String token) {

        return ResponseCookie
                .from(REFRESH_TOKEN_COOKIE, token)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(refreshTokenLifetime)
                .build();
    }
}