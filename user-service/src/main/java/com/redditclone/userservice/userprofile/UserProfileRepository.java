package com.redditclone.userservice.userprofile;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface UserProfileRepository extends ReactiveMongoRepository<UserProfile, String> {

    Mono<UserProfile> findByUsername(String login);
}
