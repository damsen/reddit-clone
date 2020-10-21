package com.redditclone.postservice.post;

import com.redditclone.postservice.vote.Score;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Document("posts")
public class Post {

    @Id
    String postId;
    String subredditName;
    String author;
    String title;
    String body;
    Long score;
    Instant posted;
    Instant edited;
    boolean deleted;

    public static Post of(String subredditName, String author, String title, String body) {
        return new Post(
                null,
                subredditName,
                author,
                title,
                body,
                0L,
                Instant.now().truncatedTo(ChronoUnit.MILLIS),
                null,
                false
        );
    }

    public Post updateScoreWith(Score score) {
        this.score = score.getScore();
        return this;
    }

    public Post editWith(EditPost edit) {
        title = edit.getTitle();
        body = edit.getBody();
        edited = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        return this;
    }

    public Post delete() {
        deleted = true;
        return this;
    }
}
