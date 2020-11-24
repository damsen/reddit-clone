package com.redditclone.redditservice.comment;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.List;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@NoArgsConstructor
public class Comment {

    String commentId;
    String postId;
    String parentId;
    String author;
    String body;
    Long score;
    Instant commented;
    Instant edited;
    boolean deleted;
    List<Comment> children;

}
