package com.redditclone.subredditservice.subreddit;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface SubredditRepository extends ReactiveMongoRepository<Subreddit, String> {

    Flux<Subreddit> findAllBy(Pageable pageable);
}
