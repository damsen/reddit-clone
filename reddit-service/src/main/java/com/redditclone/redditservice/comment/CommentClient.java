package com.redditclone.redditservice.comment;

import com.redditclone.redditservice.VoteType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CommentClient {

    Flux<Comment> findCommentsByPostId(String postId, CommentRequest request);

    Flux<Comment> findCommentsByUsername(String username, CommentRequest request);

    Mono<Comment> createComment(String username, CreateComment create);

    Mono<Comment> editComment(String commentId, String username, EditComment edit);

    Mono<Void> deleteComment(String commentId, String username);

    Mono<Void> voteComment(String commentId, VoteType voteType, String username);
}
