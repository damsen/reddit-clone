package com.redditclone.postservice.vote;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface VoteRepository extends ReactiveMongoRepository<Vote, String>, CustomVoteRepository {

    Mono<Vote> findVoteByPostIdAndUsername(String postId, String username);
}
