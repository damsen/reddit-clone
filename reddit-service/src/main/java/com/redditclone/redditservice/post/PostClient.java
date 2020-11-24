package com.redditclone.redditservice.post;

import com.redditclone.redditservice.VoteType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PostClient {

    Flux<Post> findPostsBySubreddit(String subredditName, PostRequest request);

    Flux<Post> findPostsByUsername(String username, PostRequest request);

    Mono<Post> findPostById(String postId);

    Mono<Post> createPost(String username, CreatePost create);

    Mono<Post> editPost(String postId, String username, EditPost edit);

    Mono<Void> deletePost(String postId, String username);

    Mono<Void> votePost(String postId, VoteType voteType, String username);
}
