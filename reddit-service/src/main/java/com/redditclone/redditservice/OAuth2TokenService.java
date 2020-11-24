package com.redditclone.redditservice;

import reactor.core.publisher.Mono;

public interface OAuth2TokenService {

    Mono<String> getToken();
}
