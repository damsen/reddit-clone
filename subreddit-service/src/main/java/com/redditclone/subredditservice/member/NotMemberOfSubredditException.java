package com.redditclone.subredditservice.member;

public class NotMemberOfSubredditException extends RuntimeException {

    public static final String NOT_A_MEMBER_OF_SUBREDDIT = "User %s is not a member of subreddit with name %s";

    public NotMemberOfSubredditException(String username, String subreddit) {
        super(String.format(NOT_A_MEMBER_OF_SUBREDDIT, username, subreddit));
    }
}
