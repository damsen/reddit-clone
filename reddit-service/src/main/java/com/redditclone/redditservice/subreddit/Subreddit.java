package com.redditclone.redditservice.subreddit;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.Set;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@NoArgsConstructor
public class Subreddit {

    String name;
    String title;
    String description;
    String creator;
    Instant created;
    Instant edited;
    Set<String> topics;
    Long members;
}
