package com.redditclone.subredditservice.member;

public class AlreadyAMemberOfSubredditException extends RuntimeException {

    public static final String ALREADY_A_MEMBER_OF_SUBREDDIT = "User %s is already a member of subreddit with name %s";

    public AlreadyAMemberOfSubredditException(String username, String subreddit) {
        super(String.format(ALREADY_A_MEMBER_OF_SUBREDDIT, username, subreddit));
    }
}
