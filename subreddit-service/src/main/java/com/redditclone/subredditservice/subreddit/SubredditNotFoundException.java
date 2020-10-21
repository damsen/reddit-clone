package com.redditclone.subredditservice.subreddit;

public class SubredditNotFoundException extends RuntimeException {

    public static final String SUBREDDIT_NOT_FOUND = "Subreddit not found with name %s";

    public SubredditNotFoundException(String name) {
        super(String.format(SUBREDDIT_NOT_FOUND, name));
    }
}
