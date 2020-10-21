package com.redditclone.userservice.userprofile;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository userProfileRepo;

    public Mono<UserProfile> findPublicUserProfileByUsername(String username) {
        return userProfileRepo
                .findByUsername(username)
                .switchIfEmpty(Mono.error(new UserProfileNotFoundException(username)));
    }

    public Mono<UserProfile> createMyUserProfile(String username) {
        return userProfileRepo
                .insert(UserProfile.of(username))
                .onErrorMap(
                        ex -> ex instanceof DuplicateKeyException,
                        ex -> new UserProfileAlreadyPresentException(username, ex)
                );
    }
}
