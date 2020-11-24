package com.redditclone.redditservice.comment;

import com.redditclone.redditservice.VoteType;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class DefaultCommentClient implements CommentClient {

    @Override
    public Flux<Comment> findCommentsByPostId(String postId, CommentRequest request) {
        return Flux.empty();
    }

    @Override
    public Flux<Comment> findCommentsByUsername(String username, CommentRequest request) {
        return Flux.empty();
    }

    @Override
    public Mono<Comment> createComment(String username, CreateComment create) {
        return Mono.empty();
    }

    @Override
    public Mono<Comment> editComment(String commentId, String username, EditComment edit) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> deleteComment(String commentId, String username) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> voteComment(String commentId, VoteType voteType, String username) {
        return Mono.empty();
    }
}
