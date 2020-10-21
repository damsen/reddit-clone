package com.redditclone.userservice.userprofile;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

@Controller
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @MessageMapping("find.user-profile.{username}")
    public Mono<UserProfile> findPublicUserProfileByUsername(@DestinationVariable String username) {
        return userProfileService.findPublicUserProfileByUsername(username);
    }

    @MessageMapping("create.user-profile.me")
    public Mono<UserProfile> createMyUserProfile(@AuthenticationPrincipal Jwt jwt) {
        return userProfileService.createMyUserProfile(jwt.getClaimAsString("preferred_username"));
    }
}
