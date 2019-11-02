package com.shadov.test.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.filter.factory.rewrite.ModifyRequestBodyGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.rewrite.RewriteFunction;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import reactor.core.publisher.Mono;

import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPMessage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

@SpringBootApplication
public class GatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}

	@Bean
	public RouteLocator myRoutes(RouteLocatorBuilder builder) {
		return builder.routes()
				.route(p -> p.path("/mediator/**")
						.filters(fs -> fs.rewritePath("/mediator/(?<RID>.*)", "/${RID}")
								.modifyRequestBody(String.class, String.class, modify())
						)
						.uri("http://localhost:5555")
				)
				.build();
	}

	private RewriteFunction<String, String> modify() {
		return (exchange, message) -> {
			InputStream is = new ByteArrayInputStream(message.getBytes());
			try {
				SOAPMessage request = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL).createMessage(null, is);

				request.getSOAPHeader().addHeaderElement(new QName("wat", "otherwat", "anotherwat"));

				ByteArrayOutputStream out = new ByteArrayOutputStream();
				request.writeTo(out);
				String strMsg = new String(out.toByteArray());

				return Mono.just(strMsg);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		};
	}
}
