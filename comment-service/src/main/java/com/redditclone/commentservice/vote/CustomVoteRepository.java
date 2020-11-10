package com.redditclone.commentservice.vote;

import reactor.core.publisher.Mono;

public interface CustomVoteRepository {

    Mono<Score> calculateScoreByCommentId(String commentId);
}
