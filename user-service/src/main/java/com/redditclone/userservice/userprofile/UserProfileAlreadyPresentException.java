package com.redditclone.userservice.userprofile;

public class UserProfileAlreadyPresentException extends RuntimeException {

    public static final String USER_PROFILE_ALREADY_PRESENT = "User profile already present with username %s";

    public UserProfileAlreadyPresentException(String username, Throwable cause) {
        super(String.format(USER_PROFILE_ALREADY_PRESENT, username), cause);
    }
}
