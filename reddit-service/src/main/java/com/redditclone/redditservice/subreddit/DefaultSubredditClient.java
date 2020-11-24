package com.redditclone.redditservice.subreddit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.redditclone.redditservice.OAuth2TokenService;
import com.redditclone.redditservice.utils.RSocketUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.security.rsocket.metadata.BearerTokenAuthenticationEncoder;
import org.springframework.stereotype.Component;
import reactor.cache.CacheFlux;
import reactor.cache.CacheMono;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class DefaultSubredditClient implements SubredditClient {

    private final OAuth2TokenService tokenService;
    private final RSocketRequester requester;
    private final Cache<String, Object> subredditCache;

    public DefaultSubredditClient(OAuth2TokenService tokenService,
                                  RSocketRequester.Builder requesterBuilder,
                                  @Value("${routes.subreddit-service.host}") String host,
                                  @Value("${routes.subreddit-service.port}") Integer port) {
        this.tokenService = tokenService;
        this.requester = requesterBuilder
                .rsocketStrategies(configurer -> configurer.encoder(new BearerTokenAuthenticationEncoder()))
                .dataMimeType(MediaType.APPLICATION_CBOR)
                .tcp(host, port);
        this.subredditCache = Caffeine
                .newBuilder()
                .maximumSize(10_000L)
                .expireAfterWrite(Duration.ofMinutes(1))
                .build();
    }

    @Override
    public Flux<Subreddit> findSubreddits(SubredditRequest request) {
        return CacheFlux
                .lookup(subredditCache.asMap(), request.toCacheKey(), Subreddit.class)
                .onCacheMissResume(() -> tokenService.getToken()
                        .flatMapMany(token -> requester
                                .route("find.subreddits")
                                .metadata(RSocketUtils.addTokenToMetadata(token))
                                .data(request)
                                .retrieveFlux(Subreddit.class)));
    }

    @Override
    public Mono<Subreddit> findSubredditByName(String subredditName) {
        return CacheMono
                .lookup(subredditCache.asMap(), subredditName, Subreddit.class)
                .onCacheMissResume(() -> tokenService.getToken()
                        .flatMap(token -> requester
                                .route("find.subreddit.{subredditName}", subredditName)
                                .metadata(RSocketUtils.addTokenToMetadata(token))
                                .retrieveMono(Subreddit.class)));
    }

    @Override
    public Mono<Subreddit> createSubreddit(String username, CreateSubreddit create) {
        return tokenService.getToken()
                .flatMap(token -> requester
                        .route("create.subreddit.by.{username}", username)
                        .metadata(RSocketUtils.addTokenToMetadata(token))
                        .data(create)
                        .retrieveMono(Subreddit.class));
    }

    @Override
    public Mono<Subreddit> editSubreddit(String subredditName, String username, EditSubreddit edit) {
        return tokenService.getToken()
                .flatMap(token -> requester
                        .route("edit.subreddit.{subredditName}.by.{username}", subredditName, username)
                        .metadata(RSocketUtils.addTokenToMetadata(token))
                        .data(edit)
                        .retrieveMono(Subreddit.class));
    }

    @Override
    public Mono<Void> addSubredditMember(String subredditName, String username) {
        return tokenService.getToken()
                .flatMap(token -> requester
                        .route("add.{username}.to.{subredditName}.members", username, subredditName)
                        .metadata(RSocketUtils.addTokenToMetadata(token))
                        .retrieveMono(Void.class));
    }

    @Override
    public Mono<Void> removeSubredditMember(String subredditName, String username) {
        return tokenService.getToken()
                .flatMap(token -> requester
                        .route("remove.{username}.from.{subredditName}.members", username, subredditName)
                        .metadata(RSocketUtils.addTokenToMetadata(token))
                        .retrieveMono(Void.class));
    }
}
