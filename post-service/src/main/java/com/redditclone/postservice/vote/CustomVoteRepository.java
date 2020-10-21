package com.redditclone.postservice.vote;

import reactor.core.publisher.Mono;

public interface CustomVoteRepository {

    Mono<Score> calculatePostScoreFromVotes(String postId);
}
