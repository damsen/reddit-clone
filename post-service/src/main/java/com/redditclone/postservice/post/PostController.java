package com.redditclone.postservice.post;

import com.redditclone.postservice.vote.VoteType;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Controller
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @MessageMapping("find.posts.subreddit.{subredditName}")
    public Flux<Post> findPostsBySubreddit(@DestinationVariable String subredditName,
                                           PostRequest request) {
        return postService.findPostsBySubreddit(subredditName, request);
    }

    @MessageMapping("find.posts.user.{username}")
    public Flux<Post> findPostsByUsername(@DestinationVariable String username,
                                          PostRequest request) {
        return postService.findPostsByUsername(username, request);
    }

    @MessageMapping("find.post.{postId}")
    public Mono<Post> findPostById(@DestinationVariable String postId) {
        return postService.findPostById(postId);
    }

    @MessageMapping("create.post")
    public Mono<Post> createPost(@AuthenticationPrincipal Jwt jwt,
                                 CreatePost create) {
        return postService.createPost(jwt.getClaim("preferred_username"), create);
    }

    @MessageMapping("edit.post.{postId}")
    public Mono<Post> editPost(@DestinationVariable String postId,
                               @AuthenticationPrincipal Jwt jwt,
                               EditPost edit) {
        return postService.editPost(postId, jwt.getClaim("preferred_username"), edit);
    }

    @MessageMapping("delete.post.{postId}")
    public Mono<Void> deletePost(@DestinationVariable String postId,
                                 @AuthenticationPrincipal Jwt jwt) {
        return postService.deletePost(postId, jwt.getClaim("preferred_username"));
    }

    @MessageMapping("vote.post.{postId}.{voteType}")
    public Mono<Void> votePost(@DestinationVariable String postId,
                               @DestinationVariable VoteType voteType,
                               @AuthenticationPrincipal Jwt jwt) {
        return postService.votePost(postId, voteType, jwt.getClaim("preferred_username"));
    }
}
