package com.redditclone.subredditservice.subreddit;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.Set;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Document("subreddits")
public class Subreddit {

    @Id
    String name;
    String title;
    String description;
    String creator;
    Instant created;
    Instant edited;
    Set<String> topics;
    long members;

    public static Subreddit of(String name, String title, String description, String author, Set<String> topics) {
        return new Subreddit(
                name,
                title,
                description,
                author,
                Instant.now().truncatedTo(ChronoUnit.MILLIS),
                null,
                new LinkedHashSet<>(topics),
                0L
        );
    }

    public static Subreddit from(String creator, CreateSubreddit create) {
        return Subreddit.of(
                create.getName(),
                create.getTitle(),
                create.getDescription(),
                creator,
                create.getTopics()
        );
    }

    public Subreddit updateMembersCountWith(Long members) {
        this.members = members;
        return this;
    }

    public Subreddit editWith(EditSubreddit edit) {
        title = edit.getTitle();
        description = edit.getDescription();
        topics.addAll(edit.getTopics());
        edited = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        return this;
    }
}
