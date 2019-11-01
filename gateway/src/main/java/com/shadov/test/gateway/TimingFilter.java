package com.shadov.test.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

@Service
public class TimingFilter implements GlobalFilter, Ordered {
	private static final Logger LOGGER = LoggerFactory.getLogger(ExchangeLoggingFilter.class);

	@Override
	public Mono<Void> filter(final ServerWebExchange exchange, final GatewayFilterChain chain) {
		Instant start = Instant.now();
		return chain.filter(exchange)
				.doFinally(signal -> {
					LOGGER.info("Full time: " + Duration.between(start, Instant.now()).toMillis());
				});
	}

	@Override
	public int getOrder() {
		return -2;
	}
}
