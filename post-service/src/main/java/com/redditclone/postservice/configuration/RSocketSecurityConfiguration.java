package com.redditclone.postservice.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.invocation.reactive.ArgumentResolverConfigurer;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.rsocket.RSocketSecurity;
import org.springframework.security.messaging.handler.invocation.reactive.AuthenticationPrincipalArgumentResolver;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoders;
import org.springframework.security.rsocket.core.PayloadSocketAcceptorInterceptor;

@Configuration
public class RSocketSecurityConfiguration {

    @Bean
    PayloadSocketAcceptorInterceptor rSocketSecurity(RSocketSecurity rSocket) {
        return rSocket
                .authorizePayload(authz -> authz
                        .route("find.**").permitAll()
                        .anyRequest().authenticated()
                        .anyExchange().permitAll())
                .jwt(Customizer.withDefaults())
                .build();
    }

    @Bean
    ReactiveJwtDecoder jwtDecoder(@Value("${oauth2.resource-server.oidc-issuer-location}") String oidcIssuerLocation) {
        return ReactiveJwtDecoders.fromOidcIssuerLocation(oidcIssuerLocation);
    }

    @Bean
    RSocketMessageHandler messageHandler(RSocketStrategies rSocketStrategies) {
        RSocketMessageHandler messageHandler = new RSocketMessageHandler();
        messageHandler.setRSocketStrategies(rSocketStrategies);
        ArgumentResolverConfigurer args = messageHandler.getArgumentResolverConfigurer();
        args.addCustomResolver(new AuthenticationPrincipalArgumentResolver());
        return messageHandler;
    }

}
