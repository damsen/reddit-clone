package com.redditclone.postservice.post;

public class NotAuthorOfPostException extends RuntimeException {

    public static final String NOT_AUTHOR_OF_POST = "User %s is not the author of post with ID %s";

    public NotAuthorOfPostException(String username, String postId) {
        super(String.format(NOT_AUTHOR_OF_POST, username, postId));
    }
}
