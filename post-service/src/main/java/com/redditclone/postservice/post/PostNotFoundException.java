package com.redditclone.postservice.post;

public class PostNotFoundException extends RuntimeException {

    public static final String POST_NOT_FOUND = "Post not found with ID %s";

    public PostNotFoundException(String id) {
        super(String.format(POST_NOT_FOUND, id));
    }
}
