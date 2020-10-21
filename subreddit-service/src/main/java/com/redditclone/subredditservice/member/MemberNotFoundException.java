package com.redditclone.subredditservice.member;

public class MemberNotFoundException extends RuntimeException {

    public static final String MEMBER_NOT_FOUND = "Member not found with username %s";

    public MemberNotFoundException(String username) {
        super(String.format(MEMBER_NOT_FOUND, username));
    }
}
