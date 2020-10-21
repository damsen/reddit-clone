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
    Long score;
    Instant commented;
    Instant updated;
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
                children
        );
    }
}
