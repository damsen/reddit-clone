package com.redditclone.commentservice.comment;

public class CommentNotFoundException extends RuntimeException {

    public static final String COMMENT_NOT_FOUND = "Comment not found with ID %s";

    public CommentNotFoundException(String commentId) {
        super(String.format(COMMENT_NOT_FOUND, commentId));
    }
}
