package com.redditclone.userservice.userprofile;

public class UserProfileNotFoundException extends RuntimeException {

    public static final String USER_PROFILE_NOT_FOUND = "User profile not found for username %s";

    public UserProfileNotFoundException(String username) {
        super(String.format(USER_PROFILE_NOT_FOUND, username));
    }
}
