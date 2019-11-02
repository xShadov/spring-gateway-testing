package com.shadov.test.gateway;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@EnableWebFluxSecurity
public class SecurityConfig {

	@Bean
	SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
		http
				.authorizeExchange(exchanges ->
						exchanges
								.anyExchange().permitAll()
				)
				.oauth2ResourceServer(oauth2ResourceServer ->
						oauth2ResourceServer
								.jwt(withDefaults())
				)
				.csrf().disable();
		return http.build();
	}
}