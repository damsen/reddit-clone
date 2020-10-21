package com.redditclone.postservice.vote;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Document("post_votes")
public class Vote {

    @Id
    String voteId;
    String postId;
    String username;
    VoteType voteType;

    public static Vote of(String postId, String username, VoteType voteType) {
        return new Vote(null, postId, username, voteType);
    }

}
