package com.redditclone.redditservice.post;

import com.redditclone.redditservice.VoteType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/posts")
public class PostController {

    private final PostClient postClient;

    @GetMapping
    public Flux<Post> findPostsBySubreddit(PostRequest request) {
        // FIXME
        return postClient.findPostsBySubreddit("", request);
    }

    @GetMapping
    public Flux<Post> findPostsByUsername(PostRequest request) {
        // FIXME
        return postClient.findPostsByUsername("", request);
    }

    @GetMapping("/{postId}")
    public Mono<Post> findPostById(@PathVariable String postId) {
        return postClient.findPostById(postId);
    }

    @PostMapping
    public Mono<Post> createPost(@AuthenticationPrincipal Jwt jwt,
                                 @RequestBody CreatePost create) {
        return postClient.createPost(jwt.getClaimAsString("preferred_username"), create);
    }

    @PutMapping("/{postId}")
    public Mono<Post> editPost(@PathVariable String postId,
                               @AuthenticationPrincipal Jwt jwt,
                               @RequestBody EditPost edit) {
        return postClient.editPost(postId, jwt.getClaimAsString("preferred_username"), edit);
    }

    @DeleteMapping("/{postId}")
    public Mono<Void> deletePost(@PathVariable String postId,
                                 @AuthenticationPrincipal Jwt jwt) {
        return postClient.deletePost(postId, jwt.getClaimAsString("preferred_username"));
    }

    @PutMapping("/{postId}/vote/{voteType}")
    public Mono<Void> votePost(@PathVariable String postId,
                               @PathVariable VoteType voteType,
                               @AuthenticationPrincipal Jwt jwt) {
        return postClient.votePost(postId, voteType, jwt.getClaimAsString("preferred_username"));
    }

}
