package com.redditclone.redditservice;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest;
import org.springframework.security.oauth2.client.endpoint.WebClientReactiveClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.stereotype.Service;
import reactor.cache.CacheMono;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class OAuth2ClientCredentialsTokenService implements OAuth2TokenService {

    private final ReactiveClientRegistrationRepository clientRepository;
    private final Cache<String, Object> tokenCache;

    @Value("${oauth2.client.registration-id}")
    private String clientRegistrationId;

    public OAuth2ClientCredentialsTokenService(ReactiveClientRegistrationRepository clientRepository) {
        this.clientRepository = clientRepository;
        this.tokenCache = Caffeine
                .newBuilder()
                .expireAfterWrite(Duration.ofMinutes(1))
                .build();
    }

    @Override
    public Mono<String> getToken() {
        var client = new WebClientReactiveClientCredentialsTokenResponseClient();
        return CacheMono
                .lookup(tokenCache.asMap(), "token", String.class)
                .onCacheMissResume(() -> clientRepository
                        .findByRegistrationId(clientRegistrationId)
                        .map(OAuth2ClientCredentialsGrantRequest::new)
                        .flatMap(client::getTokenResponse)
                        .map(OAuth2AccessTokenResponse::getAccessToken)
                        .map(OAuth2AccessToken::getTokenValue));
    }

}
