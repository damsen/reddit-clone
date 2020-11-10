package com.redditclone.commentservice.vote;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class VoteService {

    private final VoteRepository voteRepo;

    public Mono<Vote> voteComment(String commentId, VoteType voteType, String username) {
        return voteRepo
                .findVoteByCommentIdAndUsername(commentId, username)
                .flatMap(vote -> overrideVoteIfOppositeTypeOrDeleteIfSameType(vote, voteType))
                .switchIfEmpty(Mono.defer(() -> voteRepo.insert(Vote.of(commentId, username, voteType))));
    }

    public Mono<Score> calculateScoreByCommentId(String commentId) {
        return voteRepo.calculateScoreByCommentId(commentId);
    }

    private Mono<Vote> overrideVoteIfOppositeTypeOrDeleteIfSameType(Vote vote, VoteType voteType) {
        return Mono.just(vote)
                .filter(it -> it.getVoteType().equals(voteType.opposite()))
                .doOnNext(it -> it.setVoteType(voteType))
                .flatMap(voteRepo::save)
                .switchIfEmpty(Mono.defer(() -> voteRepo.delete(vote).thenReturn(vote)));
    }
}
