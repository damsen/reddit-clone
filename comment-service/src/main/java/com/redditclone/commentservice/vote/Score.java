package com.redditclone.commentservice.vote;

import lombok.Value;

@Value
public class Score {

    long score;

    public static Score withScoreZero() {
        return new Score(0L);
    }
}
