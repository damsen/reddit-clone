package com.redditclone.redditservice.post;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@NoArgsConstructor
public class Post {

    String postId;
    String subredditName;
    String author;
    String title;
    String body;
    Long score;
    Instant posted;
    Instant edited;
    boolean deleted;
}
