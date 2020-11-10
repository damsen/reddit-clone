package com.redditclone.commentservice.comment;

import com.redditclone.commentservice.vote.VoteService;
import com.redditclone.commentservice.vote.VoteType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepo;
    private final VoteService voteService;

    public Flux<CommentTree> findCommentsByPostId(String postId, CommentRequest request) {
        Pageable pageable = request.toPageable();
        return commentRepo
                .findByPostIdAndParentIdNull(postId, pageable)
                .flatMapSequential(parent -> findChildrenByParentId(parent.getCommentId(), pageable.getSort())
                        .collectList()
                        .map(children -> CommentTree.of(parent, children)));
    }

    private Flux<CommentTree> findChildrenByParentId(String parentId, Sort sort) {
        return commentRepo
                .findByParentId(parentId, sort)
                .flatMapSequential(childAsParent -> findChildrenByParentId(childAsParent.getCommentId(), sort)
                        .collectList()
                        .map(children -> CommentTree.of(childAsParent, children)));
    }

    public Flux<Comment> findCommentsByUsername(String username, CommentRequest request) {
        return commentRepo.findByAuthor(username, request.toPageable());
    }

    public Mono<Comment> createComment(String username, CreateComment create) {
        return commentRepo
                .insert(Comment.of(create.getPostId(), create.getParentId(), username, create.getBody()))
                .flatMap(comment -> voteService
                        .voteComment(comment.getCommentId(), VoteType.UPVOTE,  comment.getAuthor())
                        .then(Mono.defer(() -> updateCommentScore(comment)))
                        .thenReturn(comment));
    }

    private Mono<Comment> updateCommentScore(Comment comment) {
        return voteService
                .calculateScoreByCommentId(comment.getCommentId())
                .map(comment::updateScoreWith)
                .flatMap(commentRepo::save);
    }

    public Mono<Comment> editComment(String commentId, String username, EditComment edit) {
        return findCommentById(commentId)
                .filter(it -> it.getAuthor().equals(username))
                .map(it -> it.editWith(edit))
                .flatMap(commentRepo::save)
                .switchIfEmpty(Mono.error(new NotAuthorOfCommentException(username, commentId)));
    }

    private Mono<Comment> findCommentById(String commentId) {
        return commentRepo
                .findById(commentId)
                .switchIfEmpty(Mono.error(new CommentNotFoundException(commentId)));
    }

    public Mono<Void> deleteComment(String commentId, String username) {
        return findCommentById(commentId)
                .filter(it -> it.getAuthor().equals(username))
                .map(Comment::delete)
                .flatMap(commentRepo::save)
                .switchIfEmpty(Mono.error(new NotAuthorOfCommentException(username, commentId)))
                .then();
    }

    public Mono<Void> voteComment(String commentId, VoteType voteType, String username) {
        return findCommentById(commentId)
                .flatMap(comment -> voteService
                        .voteComment(comment.getCommentId(), voteType, username)
                        .then(Mono.defer(() -> updateCommentScore(comment))))
                .then();
    }

}
