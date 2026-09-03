package com.watchlist.app.config;

import java.security.interfaces.RSAPublicKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password4j.Argon2Password4jPasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtAudienceValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			JwtDecoder jwtDecoder,
			JwtAuthenticationConverter jwtAuthenticationConverter,
			AuthenticationEntryPoint authenticationEntryPoint,
			AccessDeniedHandler accessDeniedHandler
	) throws Exception {

		return http
				.csrf(AbstractHttpConfigurer::disable)

				.sessionManagement(session ->
						session.sessionCreationPolicy(
								SessionCreationPolicy.STATELESS
						))

				.exceptionHandling(exception -> exception
						.authenticationEntryPoint(
								authenticationEntryPoint
						)
						.accessDeniedHandler(
								accessDeniedHandler
						)
				)

				.authorizeHttpRequests(auth -> auth

						.requestMatchers(
								"/api/auth/login",
								"/api/auth/refresh",
								"/api/auth/logout"
						).permitAll()

						.requestMatchers(
								"/swagger-ui.html",
								"/swagger-ui/**",
								"/v3/api-docs/**"
						).permitAll()

						.requestMatchers(
								"/api/titles",
								"/api/titles/stats",
								"/api/titles/*"
						).permitAll()

						.requestMatchers(
								"/api/departments/**",
								"/api/employees/*",
								"/api/positions/**",
								"/api/episodes/**",
								"/api/reviews/**"
						).permitAll()

						.requestMatchers(
								org.springframework.http.HttpMethod.POST,
								"/api/**"
						).hasRole("ADMIN")

						.requestMatchers(
								org.springframework.http.HttpMethod.PUT,
								"/api/**"
						).hasRole("ADMIN")

						.requestMatchers(
								org.springframework.http.HttpMethod.PATCH,
								"/api/**"
						).hasRole("ADMIN")

						.requestMatchers(
								org.springframework.http.HttpMethod.DELETE,
								"/api/**"
						).hasRole("ADMIN")

						.anyRequest().authenticated()
				)

				.oauth2ResourceServer(oauth2 ->
						oauth2.jwt(jwt ->
								jwt
										.decoder(jwtDecoder)
										.jwtAuthenticationConverter(
												jwtAuthenticationConverter
										)
						)
				)

				.httpBasic(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)

				.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new Argon2Password4jPasswordEncoder();
	}

	@Bean
	AuthenticationManager authenticationManager(
			UserDetailsService userDetailsService,
			PasswordEncoder passwordEncoder
	) {

		DaoAuthenticationProvider provider =
				new DaoAuthenticationProvider(userDetailsService);

		provider.setPasswordEncoder(passwordEncoder);

		return new ProviderManager(provider);
	}

	@Bean
	JwtAuthenticationConverter jwtAuthenticationConverter() {

		JwtGrantedAuthoritiesConverter authoritiesConverter =
				new JwtGrantedAuthoritiesConverter();

		authoritiesConverter.setAuthoritiesClaimName("role");
		authoritiesConverter.setAuthorityPrefix("ROLE_");

		JwtAuthenticationConverter converter =
				new JwtAuthenticationConverter();

		converter.setJwtGrantedAuthoritiesConverter(
				authoritiesConverter
		);

		return converter;
	}

	@Bean
	JwtDecoder jwtDecoder(
			RSAPublicKey publicKey,
			@Value("${jwt.issuer}") String issuer,
			@Value("${jwt.audience}") String audience
	) {

		NimbusJwtDecoder decoder =
				NimbusJwtDecoder
						.withPublicKey(publicKey)
						.build();

		OAuth2TokenValidator<Jwt> issuerValidator =
				JwtValidators.createDefaultWithIssuer(issuer);

		OAuth2TokenValidator<Jwt> audienceValidator =
				new JwtAudienceValidator(audience);

		decoder.setJwtValidator(
				new DelegatingOAuth2TokenValidator<>(
						issuerValidator,
						audienceValidator
				)
		);

		return decoder;
	}
}