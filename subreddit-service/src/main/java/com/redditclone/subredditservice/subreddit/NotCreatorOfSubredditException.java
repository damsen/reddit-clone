package com.redditclone.subredditservice.subreddit;

public class NotCreatorOfSubredditException extends RuntimeException {

    public static final String NOT_CREATOR_OF_SUBREDDIT = "User %s is not the creator of subreddit with name %s";

    public NotCreatorOfSubredditException(String username, String subredditName) {
        super(String.format(NOT_CREATOR_OF_SUBREDDIT, username, subredditName));
    }
}
