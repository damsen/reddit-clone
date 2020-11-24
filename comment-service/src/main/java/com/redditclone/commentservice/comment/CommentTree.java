package com.redditclone.commentservice.comment;

import lombok.Value;

import java.time.Instant;
import java.util.List;

@Value
public class CommentTree {

    String commentId;
    String postId;
    String parentId;
    String author;
    String body;
    long score;
    Instant commented;
    Instant edited;
    boolean deleted;
    List<CommentTree> children;

    public static CommentTree of(Comment parent, List<CommentTree> children) {
        return new CommentTree(
                parent.getCommentId(),
                parent.getPostId(),
                parent.getParentId(),
                parent.getAuthor(),
                parent.getBody(),
                parent.getScore(),
                parent.getCommented(),
                parent.getEdited(),
                parent.isDeleted(),
                children
        );
    }
}
