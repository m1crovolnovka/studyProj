package com.watchlist.app.config;

import java.io.IOException;
import java.io.InputStream;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;

@Configuration
public class RsaKeyConfig {

    @Bean
    RSAPublicKey publicKey(
            @Value("${jwt.public-key}") Resource resource
    ) throws IOException {

        try (InputStream inputStream = resource.getInputStream()) {
            return RsaKeyConverters
                    .x509()
                    .convert(inputStream);
        }
    }

    @Bean
    RSAPrivateKey privateKey(
            @Value("${jwt.private-key}") Resource resource
    ) throws IOException {

        try (InputStream inputStream = resource.getInputStream()) {
            return RsaKeyConverters
                    .pkcs8()
                    .convert(inputStream);
        }
    }

    @Bean
    JwtEncoder jwtEncoder(
            RSAPublicKey publicKey,
            RSAPrivateKey privateKey
    ) {

        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID("watchlist-rsa-key-1")
                .build();

        return new NimbusJwtEncoder(
                new ImmutableJWKSet<>(new JWKSet(rsaKey))
        );
    }
}