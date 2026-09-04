package com.watchlist.app.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Optional;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.watchlist.app.config.TestSecurityConfig;
import com.watchlist.app.domain.AppUser;
import com.watchlist.app.domain.RefreshToken;
import com.watchlist.app.domain.Role;
import com.watchlist.app.dto.RegisterRequest;
import com.watchlist.app.exception.ApiExceptionHandler;
import com.watchlist.app.exception.UsernameAlreadyExistsException;
import com.watchlist.app.repository.AppUserRepository;
import com.watchlist.app.service.AuthService;
import com.watchlist.app.service.JwtService;
import com.watchlist.app.service.RefreshTokenService;
import com.watchlist.app.service.RotatedRefreshToken;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc
@Import({ TestSecurityConfig.class, ApiExceptionHandler.class })
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AuthenticationManager authenticationManager;

	@MockitoBean
	private AppUserRepository appUserRepository;

	@MockitoBean
	private AuthService authService;

	@MockitoBean
	private JwtService jwtService;

	@MockitoBean
	private RefreshTokenService refreshTokenService;

	@Test
	void registerIsPublicAndReturnsTokens() throws Exception {
		AppUser user = new AppUser("alice", "encoded-password", Role.USER);
		when(authService.register(any(RegisterRequest.class))).thenReturn(user);
		when(jwtService.generateAccessToken(user)).thenReturn("access-token");
		when(refreshTokenService.create(user)).thenReturn("refresh-token");

		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"username":"alice","password":"password123","firstName":"Alice","lastName":"One","email":"alice@example.com","departmentId":1}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.accessToken").value("access-token"))
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.expiresIn").value(900))
				.andExpect(jsonPath("$.refreshToken").value("refresh-token"))
				.andExpect(cookie().exists("refresh_token"))
				.andExpect(header().exists(HttpHeaders.SET_COOKIE));
	}

	@Test
	void loginReturnsTokensIncludingRefreshToken() throws Exception {
		AppUser user = new AppUser("alice", "encoded-password", Role.USER);
		Authentication authentication = mock(Authentication.class);
		when(authentication.getName()).thenReturn("alice");
		when(authenticationManager.authenticate(any())).thenReturn(authentication);
		when(appUserRepository.findByUsername("alice")).thenReturn(Optional.of(user));
		when(jwtService.generateAccessToken(user)).thenReturn("access-token");
		when(refreshTokenService.create(user)).thenReturn("refresh-token");

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"username":"alice","password":"password123"}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").value("access-token"))
				.andExpect(jsonPath("$.refreshToken").value("refresh-token"))
				.andExpect(cookie().exists("refresh_token"));
	}

	@Test
	void refreshReturnsRotatedRefreshToken() throws Exception {
		AppUser user = new AppUser("alice", "encoded-password", Role.USER);
		RefreshToken refreshTokenEntity = new RefreshToken(
				"token-hash",
				user,
				Instant.now().plusSeconds(1800));
		when(refreshTokenService.rotate("old-refresh-token"))
				.thenReturn(new RotatedRefreshToken("new-refresh-token", refreshTokenEntity));
		when(jwtService.generateAccessToken(user)).thenReturn("access-token");

		mockMvc.perform(post("/api/auth/refresh")
				.cookie(new Cookie("refresh_token", "old-refresh-token")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").value("access-token"))
				.andExpect(jsonPath("$.refreshToken").value("new-refresh-token"))
				.andExpect(cookie().value("refresh_token", "new-refresh-token"));
	}

	@Test
	void refreshAcceptsTokenInBody() throws Exception {
		AppUser user = new AppUser("alice", "encoded-password", Role.USER);
		RefreshToken refreshTokenEntity = new RefreshToken(
				"token-hash",
				user,
				Instant.now().plusSeconds(1800));
		when(refreshTokenService.rotate("body-refresh-token"))
				.thenReturn(new RotatedRefreshToken("new-refresh-token", refreshTokenEntity));
		when(jwtService.generateAccessToken(user)).thenReturn("access-token");

		mockMvc.perform(post("/api/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"refreshToken":"body-refresh-token"}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").value("access-token"))
				.andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));
	}

	@Test
	void refreshWithMissingTokenReturnsBadRequest() throws Exception {
		mockMvc.perform(post("/api/auth/refresh"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Invalid request"))
				.andExpect(jsonPath("$.detail").value("Refresh token is missing"));
	}

	@Test
	void refreshWithInvalidTokenReturnsBadRequestInsteadOfServerError() throws Exception {
		when(refreshTokenService.rotate("stale-refresh-token"))
				.thenThrow(new IllegalArgumentException("Refresh token is invalid or has already been used"));

		mockMvc.perform(post("/api/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"refreshToken":"stale-refresh-token"}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Invalid request"))
				.andExpect(jsonPath("$.detail").value("Refresh token is invalid or has already been used"));
	}

	@Test
	void registerReturnsConflictWhenUsernameAlreadyExists() throws Exception {
		when(authService.register(any(RegisterRequest.class)))
				.thenThrow(new UsernameAlreadyExistsException("alice"));

		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"username":"alice","password":"password123","firstName":"Alice","lastName":"One","email":"alice@example.com","departmentId":1}
						"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.title").value("Username already exists"))
				.andExpect(jsonPath("$.detail").value("Username already exists: alice"));
	}

	@Test
	void registerReturnsBadRequestWhenValidationFails() throws Exception {
		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"username":"", "password":"123"}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Invalid request"));
	}
}