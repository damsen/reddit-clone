package com.redditclone.commentservice.comment;

import com.redditclone.commentservice.vote.Score;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@FieldDefaults(level = AccessLevel.PRIVATE)
@Document("comments")
public class Comment {

    @Id
    String commentId;
    String postId;
    String parentId;
    String author;
    String body;
    Long score;
    Instant commented;
    Instant edited;
    boolean deleted;

    public static Comment of(String postId, String parentId, String author, String body) {
        return new Comment(
                null,
                postId,
                parentId,
                author,
                body,
                0L,
                Instant.now().truncatedTo(ChronoUnit.MILLIS),
                null,
                false
        );
    }

    public Comment updateScoreWith(Score score) {
        this.score = score.getScore();
        return this;
    }

    public Comment editWith(EditComment edit) {
        body = edit.getBody();
        edited = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        return this;
    }

    public Comment delete() {
        deleted = true;
        return this;
    }
}
