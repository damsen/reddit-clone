package com.redditclone.commentservice.comment;

public class NotAuthorOfCommentException extends RuntimeException {

    public static final String NOT_AUTHOR_OF_COMMENT = "User %s is not the author of comment with ID %s";

    public NotAuthorOfCommentException(String username, String commentId) {
        super(String.format(NOT_AUTHOR_OF_COMMENT, username, commentId));
    }
}
