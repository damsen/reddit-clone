package com.redditclone.subredditservice;

import com.redditclone.subredditservice.member.*;
import com.redditclone.subredditservice.subreddit.*;
import io.rsocket.metadata.WellKnownMimeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.rsocket.context.LocalRSocketServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.MediaType;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest;
import org.springframework.security.oauth2.client.endpoint.WebClientReactiveClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.rsocket.metadata.BearerTokenAuthenticationEncoder;
import org.springframework.security.rsocket.metadata.BearerTokenMetadata;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class SubredditServiceApplicationTests {

    @LocalRSocketServerPort
    private int port;

    @Autowired
    private RSocketRequester.Builder requesterBuilder;

    @Autowired
    private SubredditRepository subredditRepo;

    @Autowired
    private MemberRepository memberRepo;

    @Autowired
    private OAuth2 oAuth2;

    @BeforeEach
    public void setup() {
        subredditRepo.deleteAll().block();
        memberRepo.deleteAll().block();
    }

    private RSocketRequester tcp() {
        return requesterBuilder
                .dataMimeType(MediaType.APPLICATION_CBOR)
                .rsocketStrategies(configurer -> configurer.encoder(new BearerTokenAuthenticationEncoder()))
                .tcp("localhost", port);
    }

    @Test
    public void findSubreddits_whenNoJwt_shouldReturnError() {

        Flux<Subreddit> subreddits = tcp()
                .route("find.subreddits")
                .data(SubredditRequest.builder().build())
                .retrieveFlux(Subreddit.class);

        StepVerifier
                .create(subreddits)
                .verifyErrorMatches(ex -> ex.getMessage().equals("Access Denied"));
    }

    @Test
    public void findSubreddits_shouldReturnPagedResults() {

        subredditRepo.save(Subreddit.of("test1", "title", "description", "author", Set.of())).block();
        subredditRepo.save(Subreddit.of("test2", "title", "description", "author", Set.of())).block();
        subredditRepo.save(Subreddit.of("test3", "title", "description", "author", Set.of())).block();

        String token = oAuth2.getAccessTokenForUsername().block();

        Flux<Subreddit> subreddits = tcp()
                .route("find.subreddits")
                .metadata(oAuth2.addTokenToMetadata(token))
                .data(SubredditRequest.builder().page(1).size(2).build())
                .retrieveFlux(Subreddit.class);

        StepVerifier
                .create(subreddits)
                .expectNextMatches(it -> it.getName().equals("test3"))
                .verifyComplete();
    }

    @Test
    public void findSubreddits_whenSortByNew_shouldReturnResultsSortedByCreatedDesc() {

        Subreddit newest = Subreddit.of("test1", "title", "description", "author", Set.of());
        newest.setCreated(LocalDate.of(2020, Month.OCTOBER, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
        subredditRepo.save(newest).block();

        Subreddit oldest = Subreddit.of("test2", "title", "description", "author", Set.of());
        oldest.setCreated(LocalDate.of(2019, Month.DECEMBER, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
        subredditRepo.save(oldest).block();

        Subreddit middle = Subreddit.of("test3", "title", "description", "author", Set.of());
        middle.setCreated(LocalDate.of(2020, Month.MARCH, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
        subredditRepo.save(middle).block();

        String token = oAuth2.getAccessTokenForUsername().block();

        Flux<Subreddit> subreddits = tcp()
                .route("find.subreddits")
                .metadata(oAuth2.addTokenToMetadata(token))
                .data(SubredditRequest.builder().sort(SubredditRequest.SortBy.NEW).build())
                .retrieveFlux(Subreddit.class);

        StepVerifier
                .create(subreddits)
                .expectNextMatches(it -> it.getName().equals("test1"))
                .expectNextMatches(it -> it.getName().equals("test3"))
                .expectNextMatches(it -> it.getName().equals("test2"))
                .verifyComplete();
    }

    @Test
    public void findSubreddits_whenSortByOld_shouldReturnResultsSortedByCreatedAsc() {

        Subreddit newest = Subreddit.of("test1", "title", "description", "author", Set.of());
        newest.setCreated(LocalDate.of(2020, Month.OCTOBER, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
        subredditRepo.save(newest).block();

        Subreddit oldest = Subreddit.of("test2", "title", "description", "author", Set.of());
        oldest.setCreated(LocalDate.of(2019, Month.DECEMBER, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
        subredditRepo.save(oldest).block();

        Subreddit middle = Subreddit.of("test3", "title", "description", "author", Set.of());
        middle.setCreated(LocalDate.of(2020, Month.MARCH, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
        subredditRepo.save(middle).block();

        String token = oAuth2.getAccessTokenForUsername().block();

        Flux<Subreddit> subreddits = tcp()
                .route("find.subreddits")
                .metadata(oAuth2.addTokenToMetadata(token))
                .data(SubredditRequest.builder().sort(SubredditRequest.SortBy.OLD).build())
                .retrieveFlux(Subreddit.class);

        StepVerifier
                .create(subreddits)
                .expectNextMatches(it -> it.getName().equals("test2"))
                .expectNextMatches(it -> it.getName().equals("test3"))
                .expectNextMatches(it -> it.getName().equals("test1"))
                .verifyComplete();
    }

    @Test
    public void findSubreddits_whenSortByTop_shouldReturnResultsSortedByMembersDesc() {

        Subreddit highest = Subreddit.of("test1", "title", "description", "author", Set.of());
        highest.setMembers(1200L);
        subredditRepo.save(highest).block();

        Subreddit lowest = Subreddit.of("test2", "title", "description", "author", Set.of());
        lowest.setMembers(1L);
        subredditRepo.save(lowest).block();

        Subreddit middle = Subreddit.of("test3", "title", "description", "author", Set.of());
        middle.setMembers(190L);
        subredditRepo.save(middle).block();

        String token = oAuth2.getAccessTokenForUsername().block();

        Flux<Subreddit> subreddits = tcp()
                .route("find.subreddits")
                .metadata(oAuth2.addTokenToMetadata(token))
                .data(SubredditRequest.builder().sort(SubredditRequest.SortBy.TOP).build())
                .retrieveFlux(Subreddit.class);

        StepVerifier
                .create(subreddits)
                .expectNextMatches(it -> it.getName().equals("test1"))
                .expectNextMatches(it -> it.getName().equals("test3"))
                .expectNextMatches(it -> it.getName().equals("test2"))
                .verifyComplete();
    }

    @Test
    public void findSubredditByName_whenNoJwt_shouldReturnError() {

        Mono<Subreddit> subreddit = tcp()
                .route("find.subreddit.{subredditName}", "test")
                .retrieveMono(Subreddit.class);

        StepVerifier
                .create(subreddit)
                .verifyErrorMatches(ex -> ex.getMessage().equals("Access Denied"));
    }

    @Test
    public void findSubredditByName_whenSubredditFound_shouldReturnThatSubreddit() {

        subredditRepo
                .save(Subreddit.of("test", "title", "description", "author", Set.of()))
                .block();

        String token = oAuth2.getAccessTokenForUsername().block();

        Mono<Subreddit> subreddit = tcp()
                .route("find.subreddit.{subredditName}", "test")
                .metadata(oAuth2.addTokenToMetadata(token))
                .retrieveMono(Subreddit.class);

        StepVerifier
                .create(subreddit)
                .expectNextMatches(it -> it.getName().equals("test"))
                .verifyComplete();
    }

    @Test
    public void findSubredditByName_whenSubredditNotFound_shouldReturnError() {

        String token = oAuth2.getAccessTokenForUsername().block();

        Mono<Subreddit> subreddit = tcp()
                .route("find.subreddit.{subredditName}", "test")
                .metadata(oAuth2.addTokenToMetadata(token))
                .retrieveMono(Subreddit.class);

        StepVerifier
                .create(subreddit)
                .expectErrorMatches(ex -> ex.getMessage().equals(String.format(SubredditNotFoundException.SUBREDDIT_NOT_FOUND, "test")))
                .verify();
    }

    @Test
    public void createSubreddit_whenNoJwt_shouldReturnError() {

        Mono<Subreddit> subreddit = tcp()
                .route("create.subreddit.by.{username}", "reddit-user")
                .data(new CreateSubreddit("test", "title", "description", Set.of("topic")))
                .retrieveMono(Subreddit.class);

        StepVerifier
                .create(subreddit)
                .verifyErrorMatches(ex -> ex.getMessage().equals("Access Denied"));
    }

    @Test
    public void createSubreddit_shouldCreateSubreddit() {

        String token = oAuth2.getAccessTokenForUsername().block();

        Mono<Subreddit> subreddit = tcp()
                .route("create.subreddit.by.{username}", "reddit-user")
                .metadata(oAuth2.addTokenToMetadata(token))
                .data(new CreateSubreddit("test", "title", "description", Set.of("topic")))
                .retrieveMono(Subreddit.class);

        StepVerifier
                .create(subreddit)
                .expectNextMatches(it -> it.getName().equals("test") &&
                                         it.getTitle().equals("title") &&
                                         it.getDescription().equals("description") &&
                                         it.getCreator().equals("reddit-user") &&
                                         it.getTopics().contains("topic"))
                .verifyComplete();
    }

    @Test
    public void createSubreddit_whenSubredditWithSameNameAlreadyExists_shouldReturnError() {

        String token = oAuth2.getAccessTokenForUsername().block();

        subredditRepo
                .save(Subreddit.of("test", "title", "description", "author", Set.of()))
                .block();

        Mono<Subreddit> subreddit = tcp()
                .route("create.subreddit.by.{username}", "reddit-user")
                .metadata(oAuth2.addTokenToMetadata(token))
                .data(new CreateSubreddit("test", "new title", "new description", Set.of("new topic")))
                .retrieveMono(Subreddit.class);

        StepVerifier
                .create(subreddit)
                .verifyErrorMatches(ex -> ex.getMessage()
                        .equals(String.format(SubredditAlreadyPresentException.SUBREDDIT_ALREADY_PRESENT, "test")));
    }

    @Test
    public void createSubreddit_whenCreatorIsNotAMember_shouldSaveSubredditCreatorAsMember() {

        String token = oAuth2.getAccessTokenForUsername().block();

        Mono<Member> author = tcp()
                .route("create.subreddit.by.{username}", "reddit-user")
                .metadata(oAuth2.addTokenToMetadata(token))
                .data(new CreateSubreddit("test", "title", "description", Set.of("topic")))
                .retrieveMono(Subreddit.class)
                .flatMap(subreddit -> memberRepo.findById("reddit-user"));

        StepVerifier
                .create(author)
                .expectNextMatches(it ->
                        it.getUsername().equals("reddit-user") &&
                        it.getJoinedSubreddits().contains("test"))
                .verifyComplete();
    }

    @Test
    public void createSubreddit_whenCreatorIsAlreadyAMemberOfOtherSubreddits_shouldAddSubredditToCreatorsJoinedSubreddits() {

        memberRepo
                .save(Member.of("reddit-user", "test1"))
                .block();

        String token = oAuth2.getAccessTokenForUsername().block();

        Mono<Member> member = tcp()
                .route("create.subreddit.by.{username}", "reddit-user")
                .metadata(oAuth2.addTokenToMetadata(token))
                .data(new CreateSubreddit("test2", "title", "description", Set.of("topic")))
                .retrieveMono(Subreddit.class)
                .flatMap(subreddit -> memberRepo.findById("reddit-user"));

        StepVerifier
                .create(member)
                .expectNextMatches(it -> it.getJoinedSubreddits().containsAll(Set.of("test1", "test2")))
                .verifyComplete();
    }

    @Test
    public void createSubreddit_shouldSetSubredditMembersCountToOne() {

        String token = oAuth2.getAccessTokenForUsername().block();

        Mono<Subreddit> subreddit = tcp()
                .route("create.subreddit.by.{username}", "reddit-user")
                .metadata(oAuth2.addTokenToMetadata(token))
                .data(new CreateSubreddit("test", "title", "description", Set.of("topic")))
                .retrieveMono(Subreddit.class);

        StepVerifier
                .create(subreddit)
                .expectNextMatches(it -> it.getMembers() == 1L)
                .verifyComplete();
    }

    @Test
    public void editSubreddit_whenNoJwt_shouldReturnError() {

        Mono<Subreddit> edited = tcp()
                .route("edit.subreddit.{subredditName}.by.{username}", "test", "reddit-user")
                .data(new EditSubreddit("new title", "new description", Set.of("topic", "another topic")))
                .retrieveMono(Subreddit.class);

        StepVerifier
                .create(edited)
                .verifyErrorMatches(ex -> ex.getMessage().equals("Access Denied"));
    }

    @Test
    public void editSubreddit_whenUserIsCreator_shouldEditSubreddit() {

        subredditRepo.save(Subreddit.of("test", "title", "description", "reddit-user", Set.of("topic")))
                .block();

        String token = oAuth2.getAccessTokenForUsername().block();

        Mono<Subreddit> edited = tcp()
                .route("edit.subreddit.{subredditName}.by.{username}", "test", "reddit-user")
                .metadata(oAuth2.addTokenToMetadata(token))
                .data(new EditSubreddit("new title", "new description", Set.of("topic", "another topic")))
                .retrieveMono(Subreddit.class);

        StepVerifier
                .create(edited)
                .expectNextMatches(it -> it.getTitle().equals("new title") &&
                                         it.getDescription().equals("new description") &&
                                         it.getCreator().equals("reddit-user") &&
                                         it.getTopics().containsAll(Set.of("topic", "another topic")))
                .verifyComplete();
    }

    @Test
    public void editSubreddit_whenSubredditNotFound_shouldReturnError() {

        String token = oAuth2.getAccessTokenForUsername().block();

        Mono<Subreddit> edited = tcp()
                .route("edit.subreddit.{subredditName}.by.{username}", "test", "reddit-user")
                .metadata(oAuth2.addTokenToMetadata(token))
                .data(new EditSubreddit("new title", "new description", Set.of("topic", "another topic")))
                .retrieveMono(Subreddit.class);

        StepVerifier
                .create(edited)
                .expectErrorMatches(ex -> ex.getMessage().equals(String.format(SubredditNotFoundException.SUBREDDIT_NOT_FOUND, "test")))
                .verify();
    }

    @Test
    public void editSubreddit_whenUserIsNotTheCreator_shouldReturnError() {

        subredditRepo.save(Subreddit.of("test", "title", "description", "another-reddit-user", Set.of("topic")))
                .block();

        String token = oAuth2.getAccessTokenForUsername().block();

        Mono<Subreddit> edited = tcp()
                .route("edit.subreddit.{subredditName}.by.{username}", "test", "reddit-user")
                .metadata(oAuth2.addTokenToMetadata(token))
                .data(new EditSubreddit("new title", "new description", Set.of("topic", "another topic")))
                .retrieveMono(Subreddit.class);

        StepVerifier
                .create(edited)
                .expectErrorMatches(ex -> ex.getMessage()
                        .equals(String.format(NotCreatorOfSubredditException.NOT_CREATOR_OF_SUBREDDIT, "reddit-user", "test")))
                .verify();
    }

    @Test
    public void addSubredditMember_whenNoJwt_shouldReturnError() {

        Mono<Void> joined = tcp()
                .route("add.{username}.to.{subredditName}.members", "reddit-user", "test")
                .retrieveMono(Void.class);

        StepVerifier
                .create(joined)
                .verifyErrorMatches(ex -> ex.getMessage().equals("Access Denied"));
    }

    @Test
    public void addSubredditMember_whenUserIsAlreadyAMemberOfOtherSubreddits_shouldJoinThatSubreddit() {

        memberRepo
                .save(Member.of("reddit-user", "test1"))
                .block();

        subredditRepo
                .save(Subreddit.of("test2", "title", "description", "another-reddit-user", Set.of("topic")))
                .block();

        String token = oAuth2.getAccessTokenForUsername().block();

        Mono<Member> joined = tcp()
                .route("add.{username}.to.{subredditName}.members", "reddit-user", "test2")
                .metadata(oAuth2.addTokenToMetadata(token))
                .retrieveMono(Void.class)
                .then(Mono.defer(() -> memberRepo.findById("reddit-user")));

        StepVerifier
                .create(joined)
                .assertNext(it -> {
                    assertThat(it.getUsername()).isEqualTo("reddit-user");
                    assertThat(it.getJoinedSubreddits()).containsOnlyOnce("test1");
                    assertThat(it.getJoinedSubreddits()).containsOnlyOnce("test2");
                })
                .verifyComplete();
    }

    @Test
    public void addSubredditMember_whenUserIsAlreadyAMemberOfOtherSubreddits_shouldIncreaseSubredditMembersCountByOne() {

        memberRepo.save(Member.of("another-reddit-user", "test2")).block();

        Subreddit subreddit = memberRepo
                .save(Member.of("reddit-user", "test1"))
                .then(Mono.just(Subreddit.of("test2", "title", "description", "another-reddit-user", Set.of("topic"))))
                .doOnNext(it -> it.setMembers(1L))
                .flatMap(subredditRepo::save)
                .block();

        Long previousMembersCount = subreddit.getMembers();
        Long expectedMembersCount = previousMembersCount + 1;

        String token = oAuth2.getAccessTokenForUsername().block();

        Mono<Subreddit> joined = tcp()
                .route("add.{username}.to.{subredditName}.members", "reddit-user", "test2")
                .metadata(oAuth2.addTokenToMetadata(token))
                .retrieveMono(Void.class)
                .then(Mono.defer(() -> subredditRepo.findById("test2")));

        StepVerifier
                .create(joined)
                .expectNextMatches(it -> it.getMembers() == expectedMembersCount)
                .verifyComplete();
    }

    @Test
    public void addSubredditMember_whenUserIsAlreadyAMemberOfThatSubreddit_shouldReturnError() {

        memberRepo
                .save(Member.of("reddit-user", "test"))
                .block();

        subredditRepo
                .save(Subreddit.of("test", "title", "description", "another-reddit-user", Set.of("topic")))
                .block();

        String token = oAuth2.getAccessTokenForUsername().block();

        Mono<Void> joined = tcp()
                .route("add.{username}.to.{subredditName}.members", "reddit-user", "test")
                .metadata(oAuth2.addTokenToMetadata(token))
                .retrieveMono(Void.class);

        StepVerifier
                .create(joined)
                .expectErrorMatches(ex -> ex.getMessage().equals(
                        String.format(AlreadyAMemberOfSubredditException.ALREADY_A_MEMBER_OF_SUBREDDIT, "reddit-user", "test")))
                .verify();
    }

    @Test
    public void addSubredditMember_whenUserIsNotAMemberOfAnySubreddits_shouldCreateMemberAndaddSubredditMember() {

        subredditRepo
                .save(Subreddit.of("test", "title", "description", "another-reddit-user", Set.of("topic")))
                .block();

        String token = oAuth2.getAccessTokenForUsername().block();

        Mono<Member> joined = tcp()
                .route("add.{username}.to.{subredditName}.members", "reddit-user", "test")
                .metadata(oAuth2.addTokenToMetadata(token))
                .retrieveMono(Void.class)
                .then(Mono.defer(() -> memberRepo.findById("reddit-user")));

        StepVerifier
                .create(joined)
                .assertNext(it -> {
                    assertThat(it.getUsername()).isEqualTo("reddit-user");
                    assertThat(it.getJoinedSubreddits()).containsOnlyOnce("test");
                })
                .verifyComplete();
    }

    @Test
    public void addSubredditMember_whenUserIsNotAMemberOfAnySubreddits_shouldIncreaseSubredditMembersCountByOne() {

        memberRepo.save(Member.of("another-reddit-user", "test")).block();

        Subreddit sub = Subreddit.of("test", "title", "description", "another-reddit-user", Set.of("topic"));
        sub.setMembers(1L);

        Subreddit subreddit = subredditRepo.save(sub).block();

        Long previousMembersCount = subreddit.getMembers();
        Long expectedMembersCount = previousMembersCount + 1;

        String token = oAuth2.getAccessTokenForUsername().block();

        Mono<Subreddit> joined = tcp()
                .route("add.{username}.to.{subredditName}.members", "reddit-user", "test")
                .metadata(oAuth2.addTokenToMetadata(token))
                .retrieveMono(Void.class)
                .then(Mono.defer(() -> subredditRepo.findById(subreddit.getName())));

        StepVerifier
                .create(joined)
                .expectNextMatches(it -> it.getMembers() == expectedMembersCount)
                .verifyComplete();
    }

    @Test
    public void addSubredditMember_whenSubredditNotFound_shouldReturnError() {

        String token = oAuth2.getAccessTokenForUsername().block();

        Mono<Void> joined = tcp()
                .route("add.{username}.to.{subredditName}.members", "reddit-user", "test")
                .metadata(oAuth2.addTokenToMetadata(token))
                .retrieveMono(Void.class);

        StepVerifier
                .create(joined)
                .expectErrorMatches(ex -> ex.getMessage().equals(String.format(SubredditNotFoundException.SUBREDDIT_NOT_FOUND, "test")))
                .verify();
    }

    @Test
    public void removeSubredditMember_whenNoJwt_shouldReturnError() {

        Mono<Void> left = tcp()
                .route("remove.{username}.from.{subredditName}.members", "reddit-user", "test")
                .retrieveMono(Void.class);

        StepVerifier
                .create(left)
                .verifyErrorMatches(ex -> ex.getMessage().equals("Access Denied"));
    }

    @Test
    public void removeSubredditMember_whenUserIsMemberOfSubreddit_shouldLeaveThatSubreddit() {

        memberRepo
                .save(Member.of("reddit-user", "test"))
                .block();

        subredditRepo
                .save(Subreddit.of("test", "title", "description", "another-reddit-user", Set.of("topic")))
                .block();

        String token = oAuth2.getAccessTokenForUsername().block();

        Mono<Member> left = tcp()
                .route("remove.{username}.from.{subredditName}.members", "reddit-user", "test")
                .metadata(oAuth2.addTokenToMetadata(token))
                .retrieveMono(Void.class)
                .then(Mono.defer(() -> memberRepo.findById("reddit-user")));

        StepVerifier
                .create(left)
                .assertNext(it -> {
                    assertThat(it.getUsername()).isEqualTo("reddit-user");
                    assertThat(it.getJoinedSubreddits()).doesNotContain("test");
                })
                .verifyComplete();
    }

    @Test
    public void removeSubredditMember_whenUserIsMemberOfSubreddit_shouldDecreaseSubredditMembersCountByOne() {

        Subreddit subreddit = memberRepo
                .save(Member.of("reddit-user", "test"))
                .then(Mono.just(Subreddit.of("test", "title", "description", "another-reddit-user", Set.of("topic"))))
                .doOnNext(it -> it.setMembers(1L))
                .flatMap(subredditRepo::save)
                .block();

        Long previousMembersCount = subreddit.getMembers();
        Long expectedMembersCount = previousMembersCount - 1;

        String token = oAuth2.getAccessTokenForUsername().block();

        Mono<Subreddit> left = tcp()
                .route("remove.{username}.from.{subredditName}.members", "reddit-user", "test")
                .metadata(oAuth2.addTokenToMetadata(token))
                .retrieveMono(Void.class)
                .then(Mono.defer(() -> subredditRepo.findById("test")));

        StepVerifier
                .create(left)
                .expectNextMatches(it -> it.getMembers() == expectedMembersCount)
                .verifyComplete();
    }

    @Test
    public void removeSubredditMember_whenUserIsNotAMemberOfThatSubreddit_shouldReturnError() {

        memberRepo
                .save(Member.of("reddit-user", "test1"))
                .block();

        subredditRepo
                .save(Subreddit.of("test2", "title", "description", "another-reddit-user", Set.of()))
                .block();

        String token = oAuth2.getAccessTokenForUsername().block();

        Mono<Void> left = tcp()
                .route("remove.{username}.from.{subredditName}.members", "reddit-user", "test2")
                .metadata(oAuth2.addTokenToMetadata(token))
                .retrieveMono(Void.class);

        StepVerifier
                .create(left)
                .expectErrorMatches(ex -> ex.getMessage()
                        .equals(String.format(NotMemberOfSubredditException.NOT_A_MEMBER_OF_SUBREDDIT, "reddit-user", "test2")))
                .verify();
    }

    @Test
    public void removeSubredditMember_whenUserIsNotAMember_shouldReturnError() {

        String token = oAuth2.getAccessTokenForUsername().block();

        Mono<Void> left = tcp()
                .route("remove.{username}.from.{subredditName}.members", "reddit-user", "test")
                .metadata(oAuth2.addTokenToMetadata(token))
                .retrieveMono(Void.class);

        StepVerifier
                .create(left)
                .expectErrorMatches(ex -> ex.getMessage()
                        .equals(String.format(MemberNotFoundException.MEMBER_NOT_FOUND, "reddit-user")))
                .verify();
    }

    @Test
    public void removeSubredditMember_whenSubredditNotFound_shouldReturnError() {

        memberRepo
                .save(Member.of("reddit-user", "test"))
                .block();

        String token = oAuth2.getAccessTokenForUsername().block();

        Mono<Void> left = tcp()
                .route("remove.{username}.from.{subredditName}.members", "reddit-user", "test")
                .metadata(oAuth2.addTokenToMetadata(token))
                .retrieveMono(Void.class);

        StepVerifier
                .create(left)
                .expectErrorMatches(ex -> ex.getMessage()
                        .equals(String.format(SubredditNotFoundException.SUBREDDIT_NOT_FOUND, "test")))
                .verify();
    }

    @TestConfiguration
    public static class OAuth2 {

        @Autowired
        private ReactiveClientRegistrationRepository clients;

        private Mono<String> getAccessTokenForUsername() {
            var client = new WebClientReactiveClientCredentialsTokenResponseClient();
            return clients.findByRegistrationId("keycloak")
                    .map(OAuth2ClientCredentialsGrantRequest::new)
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
