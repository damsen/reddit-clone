package com.redditclone.subredditservice.member;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.LinkedHashSet;
import java.util.Set;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Document("subreddit_members")
public class Member {

    @Id
    String username;
    Set<String> joinedSubreddits;

    public static Member of(String username, String subreddit) {
        return new Member(username, new LinkedHashSet<>(Set.of(subreddit)));
    }

    public Member joinSubreddit(String subreddit) {
        joinedSubreddits.add(subreddit);
        return this;
    }

    public Member leaveSubreddit(String subreddit) {
        joinedSubreddits.remove(subreddit);
        return this;
    }

    public boolean isMemberOf(String subreddit) {
        return joinedSubreddits.contains(subreddit);
    }

    public boolean isNotMemberOf(String subreddit) {
        return !joinedSubreddits.contains(subreddit);
    }
}
