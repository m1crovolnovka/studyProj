package com.watchlist.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI watchlistOpenApi() {

		return new OpenAPI()
				.info(new Info()
						.title("Watchlist API")
						.description(
								"REST API списка фильмов и сериалов для просмотра"
						)
						.version("v1")
				)
				.components(
						new Components()
								.addSecuritySchemes(
										"bearerAuth",
										new SecurityScheme()
												.type(SecurityScheme.Type.HTTP)
												.scheme("bearer")
												.bearerFormat("JWT")
								)
				)
				.addSecurityItem(
						new SecurityRequirement()
								.addList("bearerAuth")
				);
	}
}