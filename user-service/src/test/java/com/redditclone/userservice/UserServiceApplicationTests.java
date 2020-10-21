package com.redditclone.userservice;

import com.redditclone.userservice.userprofile.UserProfile;
import com.redditclone.userservice.userprofile.UserProfileAlreadyPresentException;
import com.redditclone.userservice.userprofile.UserProfileNotFoundException;
import com.redditclone.userservice.userprofile.UserProfileRepository;
import io.rsocket.metadata.WellKnownMimeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.rsocket.context.LocalRSocketServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.MediaType;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.security.oauth2.client.endpoint.OAuth2PasswordGrantRequest;
import org.springframework.security.oauth2.client.endpoint.WebClientReactivePasswordTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.rsocket.metadata.BearerTokenAuthenticationEncoder;
import org.springframework.security.rsocket.metadata.BearerTokenMetadata;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.function.Consumer;

@SpringBootTest("spring.rsocket.server.port=0")
public class UserServiceApplicationTests {

    @LocalRSocketServerPort
    private int port;

    @Autowired
    private RSocketRequester.Builder requesterBuilder;

    @Autowired
    private UserProfileRepository userProfileRepo;

    @Autowired
    private OAuth2 oAuth2;

    @BeforeEach
    public void setup() {
        userProfileRepo.deleteAll().block();
    }

    private Mono<RSocketRequester> connectTcp() {
        return requesterBuilder
                .dataMimeType(MediaType.APPLICATION_CBOR)
                .rsocketStrategies(configurer -> configurer.encoder(new BearerTokenAuthenticationEncoder()))
                .connectTcp("localhost", port);
    }

    @Test
    public void findUserProfileByUsername_whenFound_shouldReturnUserProfile() {

        userProfileRepo.save(UserProfile.of("reddit-user")).block();

        Mono<UserProfile> myUserProfile = connectTcp()
                .flatMap(req -> req
                        .route("find.user-profile.{username}", "reddit-user")
                        .retrieveMono(UserProfile.class));

        StepVerifier
                .create(myUserProfile)
                .expectNextMatches(it -> it.getUsername().equals("reddit-user"))
                .verifyComplete();
    }

    @Test
    public void findUserProfileByUsername_whenNotFound_shouldReturnError() {

        Mono<UserProfile> myUserProfile = connectTcp()
                .flatMap(req -> req
                        .route("find.user-profile.{username}", "reddit-user")
                        .retrieveMono(UserProfile.class));

        StepVerifier
                .create(myUserProfile)
                .verifyErrorMatches(ex -> ex.getMessage()
                        .equals(String.format(UserProfileNotFoundException.USER_PROFILE_NOT_FOUND, "reddit-user")));
    }

    @Test
    public void createMyUserProfile_whenNoJwt_shouldReturnError() {

        Mono<UserProfile> created = connectTcp()
                .flatMap(req -> req
                        .route("create.user-profile.me")
                        .retrieveMono(UserProfile.class));

        StepVerifier
                .create(created)
                .verifyErrorMatches(ex -> ex.getMessage().equals("Access Denied"));
    }

    @Test
    public void createMyUserProfile_shouldCreateUserProfile() {

        String token = oAuth2.getAccessTokenForUsername("reddit-user", "password").block();

        Mono<UserProfile> created = connectTcp()
                .flatMap(req -> req
                        .route("create.user-profile.me")
                        .metadata(oAuth2.addTokenToMetadata(token))
                        .retrieveMono(UserProfile.class));

        StepVerifier
                .create(created)
                .expectNextMatches(it -> it.getUsername().equals("reddit-user") &&
                                         it.getCakeDay() != null &&
                                         it.getKarma().getPostKarma() == 0L &&
                                         it.getKarma().getCommentKarma() == 0L)
                .verifyComplete();
    }

    @Test
    public void createMyUserProfile_whenUserProfileAlreadyPresent_shouldReturnError() {

        userProfileRepo.save(UserProfile.of("reddit-user")).block();

        String token = oAuth2.getAccessTokenForUsername("reddit-user", "password").block();

        Mono<UserProfile> created = connectTcp()
                .flatMap(req -> req
                        .route("create.user-profile.me")
                        .metadata(oAuth2.addTokenToMetadata(token))
                        .retrieveMono(UserProfile.class));

        StepVerifier
                .create(created)
                .verifyErrorMatches(ex -> ex.getMessage()
                        .equals(String.format(UserProfileAlreadyPresentException.USER_PROFILE_ALREADY_PRESENT, "reddit-user")));
    }

    @TestConfiguration
    public static class OAuth2 {

        @Autowired
        private ReactiveClientRegistrationRepository clients;

        private Mono<String> getAccessTokenForUsername(String username, String password) {
//        var client = new WebClientReactiveClientCredentialsTokenResponseClient();
            var client = new WebClientReactivePasswordTokenResponseClient();
            return clients.findByRegistrationId("keycloak")
//                .map(OAuth2ClientCredentialsGrantRequest::new)
                    .map(registration -> new OAuth2PasswordGrantRequest(registration, username, password))
                    .flatMap(client::getTokenResponse)
                    .map(OAuth2AccessTokenResponse::getAccessToken)
                    .map(OAuth2AccessToken::getTokenValue);
        }

        private Consumer<RSocketRequester.MetadataSpec<?>> addTokenToMetadata(String token) {
            MimeType mimeType = MimeTypeUtils.parseMimeType(WellKnownMimeType.MESSAGE_RSOCKET_AUTHENTICATION.getString());
            BearerTokenMetadata bearerTokenMetadata = new BearerTokenMetadata(token);
            return spec -> spec.metadata(bearerTokenMetadata, mimeType);
        }
    }
}
