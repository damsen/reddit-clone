package com.redditclone.postservice.vote;

import lombok.Getter;

@Getter
public enum VoteType {

    UPVOTE {
        @Override
        public VoteType opposite() {
            return DOWNVOTE;
        }
    },
    DOWNVOTE {
        @Override
        public VoteType opposite() {
            return UPVOTE;
        }
    };

    public abstract VoteType opposite();
}
