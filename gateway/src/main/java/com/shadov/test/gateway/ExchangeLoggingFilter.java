package com.shadov.test.gateway;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

//@Service
public class ExchangeLoggingFilter implements GlobalFilter, Ordered {
	private static final String MAGIC_HEADER = "x-debug";
	private static final Logger LOGGER = LoggerFactory.getLogger(ExchangeLoggingFilter.class);
	private static final String HTTP_SCHEME = "http";
	private static final String HTTPS_SCHEME = "https";

	private List<HttpMessageReader<?>> readers;

	private ExchangeLoggingFilter() {
		final ServerCodecConfigurer codecConfigurer = ServerCodecConfigurer.create();
		codecConfigurer.registerDefaults(true);

		this.readers = codecConfigurer.getReaders();
	}

	@Override
	public Mono<Void> filter(final ServerWebExchange exchange, final GatewayFilterChain chain) {
		final List<String> debugHeader = exchange.getRequest().getHeaders().get(MAGIC_HEADER);
		if (!LOGGER.isDebugEnabled() || debugHeader == null) {
			return chain.filter(exchange);
		}

		ServerHttpRequest request = exchange.getRequest();
		URI requestURI = request.getURI();
		String scheme = requestURI.getScheme();
		if ((!HTTP_SCHEME.equalsIgnoreCase(scheme) && !HTTPS_SCHEME.equals(scheme))) {
			return chain.filter(exchange);
		}

		String debugHeaderContent = debugHeader.get(0);
		if (!debugHeaderContent.equalsIgnoreCase("true")) {
			return chain.filter(exchange);
		}

		LOGGER.debug("Passed all filters, logging bodies");

		Mono<String> requestBody;
		if (exchange.getAttribute("cachedRequestBodyObject") == null) {
			LOGGER.debug("cachedRequestBodyObject is null, reading body");

			requestBody = ServerWebExchangeUtils.cacheRequestBodyAndRequest(exchange, (serverHttpRequest) ->
					ServerRequest.create(exchange.mutate().request(serverHttpRequest).build(), readers)
							.bodyToMono(String.class)
							.doOnNext((objectValue) -> exchange.getAttributes().put("cachedRequestBodyObject", objectValue)));
		} else {
			LOGGER.debug("cachedRequestBodyObject is already there");

			requestBody = Mono.just(exchange.getAttribute("cachedRequestBodyObject"));
		}

		return requestBody
				.flatMap(body -> {
					try {
						logRequest(request, body);
					} catch (Exception ex) {
						LOGGER.error("Exception during logging request body", ex);
					}

					ServerWebExchange exch = exchange;
					try {
						ServerHttpResponseDecorator logResponse = logResponse(exchange);
						exch = exchange.mutate().response(logResponse).build();
					} catch (Exception ex) {
						LOGGER.error("Failed during response logging", ex);
					}

					return chain.filter(exch);
				});
	}

	@Override
	public int getOrder() {
		return Integer.MAX_VALUE;
	}

	private void logRequest(ServerHttpRequest request, String body) {
		URI requestURI = request.getURI();
		String scheme = requestURI.getScheme();
		HttpHeaders headers = request.getHeaders();
		LOGGER.info("Request Scheme:{},Path:{}", scheme, requestURI.getPath());
		LOGGER.info("Request Method:{},IP:{},Host:{}", request.getMethod(), request.getRemoteAddress(), requestURI.getHost());
		headers.forEach((key, value) -> LOGGER.debug("Request Headers:Key->{},Value->{}", key, value));
		MultiValueMap<String, String> queryParams = request.getQueryParams();
		if (!queryParams.isEmpty()) {
			queryParams.forEach((key, value) -> LOGGER.info("Request Query Param :Key->({}),Value->({})", key, value));
		}
		MediaType contentType = headers.getContentType();
		long length = headers.getContentLength();
		LOGGER.info("Request ContentType:{},Content Length:{}", contentType, length);
		if (body != null) {
			LOGGER.info("Request Body:{}", body);
		}
	}

	private ServerHttpResponseDecorator logResponse(ServerWebExchange exchange) {
		ServerHttpResponse origResponse = exchange.getResponse();
		LOGGER.info("Response HttpStatus:{}", origResponse.getStatusCode());
		HttpHeaders headers = origResponse.getHeaders();
		headers.forEach((key, value) -> LOGGER.debug("[RequestLogFilter]Headers:Key->{},Value->{}", key, value));
		MediaType contentType = headers.getContentType();
		long length = headers.getContentLength();
		LOGGER.info("Response ContentType:{},Content Length:{}", contentType, length);
		LOGGER.info("Response Original Path:{}", exchange.getRequest().getURI().getPath());
		DataBufferFactory bufferFactory = origResponse.bufferFactory();
		return new ServerHttpResponseDecorator(origResponse) {
			@Override
			public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
				if (body instanceof Flux) {
					Flux<? extends DataBuffer> fluxBody = (Flux<? extends DataBuffer>) body;
					return super.writeWith(fluxBody.map(dataBuffer -> {
						byte[] content = new byte[dataBuffer.readableByteCount()];
						dataBuffer.read(content);
						String bodyContent = new String(content, StandardCharsets.UTF_8);
						LOGGER.info("Response:{}", bodyContent);
						return bufferFactory.wrap(content);
					}));
				}
				return super.writeWith(body);
			}
		};

	}
}
