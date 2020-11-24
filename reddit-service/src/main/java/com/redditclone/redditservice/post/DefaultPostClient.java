package com.redditclone.redditservice.post;

import com.redditclone.redditservice.VoteType;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class DefaultPostClient implements PostClient {

    @Override
    public Flux<Post> findPostsBySubreddit(String subredditName, PostRequest request) {
        return Flux.empty();
    }

    @Override
    public Flux<Post> findPostsByUsername(String username, PostRequest request) {
        return Flux.empty();
    }

    @Override
    public Mono<Post> findPostById(String postId) {
        return Mono.empty();
    }

    @Override
    public Mono<Post> createPost(String username, CreatePost create) {
        return Mono.empty();
    }

    @Override
    public Mono<Post> editPost(String postId, String username, EditPost edit) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> deletePost(String postId, String username) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> votePost(String postId, VoteType voteType, String username) {
        return Mono.empty();
    }
}
