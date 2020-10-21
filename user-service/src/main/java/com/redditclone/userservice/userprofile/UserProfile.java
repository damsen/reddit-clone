package com.redditclone.userservice.userprofile;

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
@AllArgsConstructor
@Document("user_profiles")
public class UserProfile {

    @Id
    String username;
    Instant cakeDay;
    Karma karma;

    public static UserProfile of(String username) {
        return new UserProfile(
                username,
                Instant.now().truncatedTo(ChronoUnit.MILLIS),
                Karma.withZeroKarma()
        );
    }
}
