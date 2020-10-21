package com.redditclone.commentservice.comment;

import com.redditclone.commentservice.vote.VoteType;
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
public class CommentController {

    private final CommentService commentService;

    @MessageMapping("find.comments.post.{postId}")
    public Flux<CommentTree> findCommentsByPostId(@DestinationVariable String postId,
                                                  CommentRequest request) {
        return commentService.findCommentsByPostId(postId, request);
    }

    @MessageMapping("find.comments.user.{username}")
    public Flux<Comment> findCommentsByUsername(@DestinationVariable String username,
                                                CommentRequest request) {
        return commentService.findCommentsByUsername(username, request);
    }

    @MessageMapping("create.comment")
    public Mono<Comment> createComment(@AuthenticationPrincipal Jwt jwt,
                                       CreateComment create) {
        return commentService.createComment(jwt.getClaim("preferred_username"), create);
    }

    @MessageMapping("edit.comment.{commentId}")
    public Mono<Comment> editComment(@DestinationVariable String commentId,
                                     @AuthenticationPrincipal Jwt jwt,
                                     EditComment edit) {
        return commentService.editComment(commentId, jwt.getClaim("preferred_username"), edit);
    }

    @MessageMapping("delete.comment.{commentId}")
    public Mono<Void> deleteComment(@DestinationVariable String commentId,
                                    @AuthenticationPrincipal Jwt jwt) {
        return commentService.deleteComment(commentId, jwt.getClaim("preferred_username"));
    }

    @MessageMapping("vote.comment.{commentId}.{voteType}")
    public Mono<Void> voteComment(@DestinationVariable String commentId,
                                  @DestinationVariable VoteType voteType,
                                  @AuthenticationPrincipal Jwt jwt) {
        return commentService.voteComment(commentId, voteType, jwt.getClaim("preferred_username"));
    }
}
