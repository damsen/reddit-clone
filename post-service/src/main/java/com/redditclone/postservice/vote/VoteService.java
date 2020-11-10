package com.redditclone.postservice.vote;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class VoteService {

    private final VoteRepository voteRepo;

    public Mono<Vote> votePost(String postId, VoteType voteType, String username) {
        return voteRepo
                .findVoteByPostIdAndUsername(postId, username)
                .flatMap(vote -> overrideVoteIfOppositeTypeOrDeleteIfSameType(vote, voteType))
                .switchIfEmpty(Mono.defer(() -> voteRepo.insert(Vote.of(postId, username, voteType))));
    }

    public Mono<Score> calculateScoreByPostId(String postId) {
        return voteRepo.calculateScoreByPostId(postId);
    }

    private Mono<Vote> overrideVoteIfOppositeTypeOrDeleteIfSameType(Vote vote, VoteType voteType) {
        return Mono.just(vote)
                .filter(it -> it.getVoteType().equals(voteType.opposite()))
                .doOnNext(it -> it.setVoteType(voteType))
                .flatMap(voteRepo::save)
                .switchIfEmpty(Mono.defer(() -> voteRepo.delete(vote).thenReturn(vote)));
    }
}
