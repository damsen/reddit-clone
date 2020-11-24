package com.redditclone.redditservice.comment;

import com.redditclone.redditservice.VoteType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/comments")
public class CommentController {

    private final CommentClient commentClient;

    @GetMapping
    public Flux<Comment> findCommentsByPostId(CommentRequest request) {
        // FIXME
        return commentClient.findCommentsByPostId("", request);
    }

    @GetMapping
    public Flux<Comment> findCommentsByUsername(CommentRequest request) {
        // FIXME
        return commentClient.findCommentsByUsername("", request);
    }

    @PostMapping
    public Mono<Comment> createComment(@AuthenticationPrincipal Jwt jwt,
                                       @RequestBody CreateComment create) {
        return commentClient.createComment(jwt.getClaimAsString("preferred_username"), create);
    }

    @PutMapping("/{commentId}")
    public Mono<Comment> editComment(@PathVariable String commentId,
                                     @AuthenticationPrincipal Jwt jwt,
                                     @RequestBody EditComment edit) {
        return commentClient.editComment(commentId, jwt.getClaimAsString("preferred_username"), edit);
    }

    @DeleteMapping("/{commentId}")
    public Mono<Void> deleteComment(@PathVariable String commentId,
                                    @AuthenticationPrincipal Jwt jwt) {
        return commentClient.deleteComment(commentId, jwt.getClaimAsString("preferred_username"));
    }

    @PutMapping("/{commentId}/vote/{voteType}")
    public Mono<Void> voteComment(@PathVariable String commentId,
                                  @PathVariable VoteType voteType,
                                  @AuthenticationPrincipal Jwt jwt) {
        return commentClient.voteComment(commentId, voteType, jwt.getClaimAsString("preferred_username"));
    }
}
