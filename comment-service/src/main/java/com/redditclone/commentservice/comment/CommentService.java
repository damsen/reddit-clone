package com.redditclone.commentservice.comment;

import com.redditclone.commentservice.vote.Vote;
import com.redditclone.commentservice.vote.VoteRepository;
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
    private final VoteRepository voteRepo;

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
                .flatMap(comment -> voteRepo.save(Vote.of(comment.getCommentId(), comment.getAuthor(), VoteType.UPVOTE))
                        .then(Mono.defer(() -> updateCommentScore(comment)))
                        .thenReturn(comment));
    }

    private Mono<Comment> updateCommentScore(Comment comment) {
        return voteRepo
                .calculateCommentScoreFromVotes(comment.getCommentId())
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
                .flatMap(comment -> voteRepo
                        .findVoteByCommentIdAndUsername(comment.getCommentId(), username)
                        .flatMap(vote -> overrideVoteIfOppositeTypeOrDeleteIfSameType(vote, voteType))
                        .switchIfEmpty(Mono.defer(() -> voteRepo.insert(Vote.of(comment.getCommentId(), username, voteType))))
                        .then(Mono.defer(() -> updateCommentScore(comment))))
                .then();
    }

    private Mono<Vote> overrideVoteIfOppositeTypeOrDeleteIfSameType(Vote vote, VoteType voteType) {
        return Mono.just(vote)
                .filter(it -> it.getVoteType().equals(voteType.opposite()))
                .doOnNext(it -> it.setVoteType(voteType))
                .flatMap(voteRepo::save)
                .switchIfEmpty(Mono.defer(() -> voteRepo.delete(vote).thenReturn(vote)));
    }
}
