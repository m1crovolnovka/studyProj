package com.watchlist.app.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.watchlist.app.domain.AppUser;
import com.watchlist.app.domain.Role;
import com.watchlist.app.repository.AppUserRepository;

@SpringBootTest
@ActiveProfiles("test")
class RefreshTokenRotationIntegrationTest {

	@Autowired
	private RefreshTokenService refreshTokenService;

	@Autowired
	private AppUserRepository appUserRepository;

	@Autowired
	private JwtService jwtService;

	@Test
	void rotatedUserCanBeUsedToGenerateAccessTokenAfterTransactionEnds() {
		AppUser user = appUserRepository.save(
				new AppUser("alice", "encoded-password", Role.USER));

		String rawRefreshToken = refreshTokenService.create(user);

		RotatedRefreshToken rotated = refreshTokenService.rotate(rawRefreshToken);

		// At this point the transaction from rotate() has already ended.
		// Accessing lazy fields of the returned user must still work.
		AppUser rotatedUser = rotated.token().getUser();
		String accessToken = jwtService.generateAccessToken(rotatedUser);

		assertThat(accessToken).isNotBlank();
		assertThat(rotatedUser.getUsername()).isEqualTo("alice");
		assertThat(rotatedUser.getRole()).isEqualTo(Role.USER);
		assertThat(rotated.rawToken()).isNotEqualTo(rawRefreshToken);
	}

	@Test
	void createdRefreshTokenCanBeRotatedOnce() {
		AppUser user = appUserRepository.save(
				new AppUser("carol", "password-password", Role.USER));

		String rawRefreshToken = refreshTokenService.create(user);
		RotatedRefreshToken first = refreshTokenService.rotate(rawRefreshToken);

		assertThat(first.rawToken()).isNotEqualTo(rawRefreshToken);

		org.assertj.core.api.Assertions.assertThatThrownBy(
						() -> refreshTokenService.rotate(rawRefreshToken))
				.isInstanceOf(IllegalArgumentException.class);

		// the replacement token is still usable
		RotatedRefreshToken second = refreshTokenService.rotate(first.rawToken());
		assertThat(second.rawToken()).isNotEqualTo(first.rawToken());
	}
}