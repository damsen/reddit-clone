package com.redditclone.userservice.userprofile;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
public class Karma {

    long postKarma;
    long commentKarma;

    public static Karma withZeroKarma() {
        return new Karma(0L, 0L);
    }
}
