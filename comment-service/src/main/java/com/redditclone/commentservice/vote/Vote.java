package com.redditclone.commentservice.vote;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Document("comment_votes")
public class Vote {

    @Id
    String voteId;
    String commentId;
    String username;
    VoteType voteType;

    public static Vote of(String commentId, String username, VoteType voteType) {
        return new Vote(null, commentId, username, voteType);
    }

}
