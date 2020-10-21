package com.redditclone.subredditservice.subreddit;

public class SubredditAlreadyPresentException extends RuntimeException {

    public static final String SUBREDDIT_ALREADY_PRESENT = "Subreddit already present with name %s";

    public SubredditAlreadyPresentException(String name, Throwable cause) {
        super(String.format(SUBREDDIT_ALREADY_PRESENT, name), cause);
    }
}
