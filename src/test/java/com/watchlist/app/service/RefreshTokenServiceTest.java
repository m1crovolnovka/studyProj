package com.watchlist.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.watchlist.app.domain.AppUser;
import com.watchlist.app.domain.RefreshToken;
import com.watchlist.app.domain.Role;
import com.watchlist.app.repository.RefreshTokenRepository;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

	@Mock
	private RefreshTokenRepository refreshTokenRepository;

	private RefreshTokenService refreshTokenService;

	private final AppUser user = new AppUser("alice", "encoded", Role.USER);

	@BeforeEach
	void setUp() {
		refreshTokenService =
				new RefreshTokenService(refreshTokenRepository, Duration.ofDays(30));
	}

	@Test
	void createSavesTokenWithHashAndFutureExpiry() {
		when(refreshTokenRepository.save(any(RefreshToken.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		String rawToken = refreshTokenService.create(user);

		assertThat(rawToken).isNotBlank();
		ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
		verify(refreshTokenRepository).save(captor.capture());

		RefreshToken saved = captor.getValue();
		assertThat(saved.getUser()).isEqualTo(user);
		assertThat(saved.getTokenHash()).isNotBlank();
		assertThat(saved.getTokenHash()).isNotEqualTo(rawToken);
		assertThat(saved.isExpired()).isFalse();
		assertThat(saved.isRevoked()).isFalse();
	}

	@Test
	void rotateRevokesOldTokenAndReturnsNewOne() {
		RefreshToken current = new RefreshToken(
				"old-hash",
				user,
				Instant.now().plus(Duration.ofDays(30)));
		when(refreshTokenRepository.findByTokenHash(anyString()))
				.thenReturn(Optional.of(current));
		when(refreshTokenRepository.save(any(RefreshToken.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		RotatedRefreshToken rotated =
				refreshTokenService.rotate("raw-old-token");

		assertThat(rotated.rawToken()).isNotEqualTo("raw-old-token");
		assertThat(rotated.token().getUser()).isEqualTo(user);
		assertThat(current.isRevoked()).isTrue();

		ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
		verify(refreshTokenRepository).save(captor.capture());
		assertThat(captor.getValue().getUser()).isEqualTo(user);
		assertThat(captor.getValue().getTokenHash()).isNotEqualTo("old-hash");
	}

	@Test
	void rotateThrowsWhenTokenNotStored() {
		when(refreshTokenRepository.findByTokenHash(anyString()))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> refreshTokenService.rotate("unknown-token"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("invalid or has already been used");
	}

	@Test
	void rotateThrowsWhenTokenAlreadyRevoked() {
		RefreshToken revoked = new RefreshToken(
				"old-hash",
				user,
				Instant.now().plus(Duration.ofDays(30)));
		revoked.revoke();
		when(refreshTokenRepository.findByTokenHash(anyString()))
				.thenReturn(Optional.of(revoked));

		assertThatThrownBy(() -> refreshTokenService.rotate("raw-old-token"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("revoked");
	}

	@Test
	void rotateThrowsWhenTokenExpired() {
		RefreshToken expired = new RefreshToken(
				"old-hash",
				user,
				Instant.now().minus(Duration.ofDays(1)));
		when(refreshTokenRepository.findByTokenHash(anyString()))
				.thenReturn(Optional.of(expired));

		assertThatThrownBy(() -> refreshTokenService.rotate("raw-old-token"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("expired");
	}

	@Test
	void revokeMarksTokenRevoked() {
		RefreshToken token = new RefreshToken(
				"old-hash",
				user,
				Instant.now().plus(Duration.ofDays(30)));
		when(refreshTokenRepository.findByTokenHash(anyString()))
				.thenReturn(Optional.of(token));

		refreshTokenService.revoke("raw-token");

		assertThat(token.isRevoked()).isTrue();
	}
}