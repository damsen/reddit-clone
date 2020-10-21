package com.redditclone.postservice.post;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface PostRepository extends ReactiveMongoRepository<Post, String> {

    Flux<Post> findBySubredditName(String subredditName, Pageable pageable);

    Flux<Post> findByAuthor(String author, Pageable pageable);
}
