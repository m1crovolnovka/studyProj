package com.watchlist.app.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;

import com.watchlist.app.domain.AppUser;
import com.watchlist.app.domain.Role;

@SpringBootTest
@ActiveProfiles("test")
class JwtAccessTokenIntegrationTest {

	@Autowired
	private JwtService jwtService;

	@Autowired
	private JwtDecoder jwtDecoder;

	@Test
	void generatedAccessTokenIsAcceptedByDecoder() {
		AppUser user = new AppUser("alice", "password", Role.USER);

		String token = jwtService.generateAccessToken(user);

		var jwt = jwtDecoder.decode(token);

		assertThat(jwt.getSubject()).isEqualTo("alice");
		assertThat(jwt.getClaimAsString("role")).isEqualTo("USER");
	}

	@Test
	void accessTokenHasJwtTypHeaderRequiredByDecoder() {
		AppUser user = new AppUser("alice", "password", Role.USER);

		String token = jwtService.generateAccessToken(user);

		String headerPart = token.split("\\.")[0];
		String decodedHeader = new String(
				java.util.Base64.getUrlDecoder().decode(headerPart),
				java.nio.charset.StandardCharsets.UTF_8);

		assertThat(decodedHeader).contains("\"typ\":\"JWT\"");
	}
}