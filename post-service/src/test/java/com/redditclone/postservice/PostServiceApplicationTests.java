package com.redditclone.postservice;

import com.redditclone.postservice.post.*;
import com.redditclone.postservice.vote.Vote;
import com.redditclone.postservice.vote.VoteRepository;
import com.redditclone.postservice.vote.VoteType;
import io.rsocket.metadata.WellKnownMimeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.function.Consumer;

@SpringBootTest("spring.rsocket.server.port=0")
public class PostServiceApplicationTests {

    @LocalRSocketServerPort
    private int port;

    @Autowired
    private RSocketRequester.Builder requesterBuilder;

    @Autowired
    private PostRepository postRepo;

    @Autowired
    private VoteRepository voteRepo;

    @Autowired
    private OAuth2 oAuth2;

    @BeforeEach
    public void setup() {
        postRepo.deleteAll().block();
        voteRepo.deleteAll().block();
    }

    private Mono<RSocketRequester> connectTcp() {
        return requesterBuilder
                .dataMimeType(MediaType.APPLICATION_CBOR)
                .rsocketStrategies(configurer -> configurer.encoder(new BearerTokenAuthenticationEncoder()))
                .connectTcp("localhost", port);
    }

    @Test
    public void findPostById_whenFound_shouldReturnPost() {

        Post post = postRepo.save(Post.of("test", "author", "title", "body")).block();

        Mono<Post> postById = connectTcp()
                .flatMap(req -> req
                        .route("find.post.{postId}", post.getPostId())
                        .retrieveMono(Post.class));

        StepVerifier
                .create(postById)
                .expectNextMatches(it -> it.getSubredditName().equals("test") &&
                                         it.getAuthor().equals("author") &&
                                         it.getTitle().equals("title") &&
                                         it.getBody().equals("body"))
                .verifyComplete();
    }

    @Test
    public void findPostById_whenNotFound_shouldReturnError() {

        Mono<Post> post = connectTcp()
                .flatMap(req -> req
                        .route("find.post.{postId}", "test")
                        .retrieveMono(Post.class));

        StepVerifier
                .create(post)
                .expectErrorMatches(ex -> ex.getMessage().equals(String.format(PostNotFoundException.POST_NOT_FOUND, "test")))
                .verify();
    }

    @Test
    public void findPostsBySubredditName_shouldReturnPagedResults() {

        postRepo.save(Post.of("test1", "author1", "title1", "body1")).block();
        postRepo.save(Post.of("test1", "author2", "title2", "body2")).block();

        Flux<Post> postsBySubredditName = connectTcp()
                .flatMapMany(req -> req
                        .route("find.posts.subreddit.{subredditName}", "test1")
                        .data(PostRequest.builder().page(1).size(1).build())
                        .retrieveFlux(Post.class));

        StepVerifier
                .create(postsBySubredditName)
                .expectNextMatches(it -> it.getSubredditName().equals("test1") &&
                                         it.getAuthor().equals("author2") &&
                                         it.getTitle().equals("title2") &&
                                         it.getBody().equals("body2"))
                .verifyComplete();
    }

    @Test
    public void findPostsBySubredditName_whenSortByNew_shouldReturnResultsSortedByPostedDesc() {

        Post newest = Post.of("test1", "author1", "title1", "body1");
        newest.setPosted(LocalDate.of(2020, Month.OCTOBER, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
        postRepo.save(newest).block();

        Post oldest = Post.of("test1", "author1", "title2", "body2");
        oldest.setPosted(LocalDate.of(2019, Month.DECEMBER, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
        postRepo.save(oldest).block();

        Post middle = Post.of("test1", "author2", "title3", "body3");
        middle.setPosted(LocalDate.of(2020, Month.MARCH, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
        postRepo.save(middle).block();

        Flux<Post> postsBySubredditName = connectTcp()
                .flatMapMany(req -> req
                        .route("find.posts.subreddit.{subredditName}", "test1")
                        .data(PostRequest.builder().sort(PostRequest.SortBy.NEW).build())
                        .retrieveFlux(Post.class));

        StepVerifier
                .create(postsBySubredditName)
                .expectNextMatches(it -> it.getPostId().equals(newest.getPostId()))
                .expectNextMatches(it -> it.getPostId().equals(middle.getPostId()))
                .expectNextMatches(it -> it.getPostId().equals(oldest.getPostId()))
                .verifyComplete();
    }

    @Test
    public void findPostsBySubredditName_whenSortByOld_shouldReturnResultsSortedByPostedAsc() {

        Post newest = Post.of("test1", "author1", "title1", "body1");
        newest.setPosted(LocalDate.of(2020, Month.OCTOBER, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
        postRepo.save(newest).block();

        Post oldest = Post.of("test1", "author1", "title2", "body2");
        oldest.setPosted(LocalDate.of(2019, Month.DECEMBER, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
        postRepo.save(oldest).block();

        Post middle = Post.of("test1", "author2", "title3", "body3");
        middle.setPosted(LocalDate.of(2020, Month.MARCH, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
        postRepo.save(middle).block();

        Flux<Post> postsBySubredditName = connectTcp()
                .flatMapMany(req -> req
                        .route("find.posts.subreddit.{subredditName}", "test1")
                        .data(PostRequest.builder().sort(PostRequest.SortBy.OLD).build())
                        .retrieveFlux(Post.class));

        StepVerifier
                .create(postsBySubredditName)
                .expectNextMatches(it -> it.getPostId().equals(oldest.getPostId()))
                .expectNextMatches(it -> it.getPostId().equals(middle.getPostId()))
                .expectNextMatches(it -> it.getPostId().equals(newest.getPostId()))
                .verifyComplete();
    }

    @Test
    public void findPostsBySubredditName_whenSortByTop_shouldReturnResultsSortedByScoreDesc() {

        Post highest = Post.of("test1", "author1", "title1", "body1");
        highest.setScore(200L);
        postRepo.save(highest).block();

        Post lowest = Post.of("test1", "author1", "title2", "body2");
        lowest.setScore(-150L);
        postRepo.save(lowest).block();

        Post middle = Post.of("test1", "author2", "title3", "body3");
        middle.setScore(20L);
        postRepo.save(middle).block();

        Flux<Post> postsBySubredditName = connectTcp()
                .flatMapMany(req -> req
                        .route("find.posts.subreddit.{subredditName}", "test1")
                        .data(PostRequest.builder().sort(PostRequest.SortBy.TOP).build())
                        .retrieveFlux(Post.class));

        StepVerifier
                .create(postsBySubredditName)
                .expectNextMatches(it -> it.getScore() == 200L)
                .expectNextMatches(it -> it.getScore() == 20L)
                .expectNextMatches(it -> it.getScore() == -150L)
                .verifyComplete();
    }

    @Test
    public void findPostsByUsername_shouldReturnPagedResults() {

        postRepo.save(Post.of("test1", "author1", "title1", "body1")).block();
        postRepo.save(Post.of("test2", "author1", "title2", "body2")).block();

        Flux<Post> postsBySubredditName = connectTcp()
                .flatMapMany(req -> req
                        .route("find.posts.user.{username}", "author1")
                        .data(PostRequest.builder().page(1).size(1).build())
                        .retrieveFlux(Post.class));

        StepVerifier
                .create(postsBySubredditName)
                .expectNextMatches(it -> it.getSubredditName().equals("test2") &&
                                         it.getAuthor().equals("author1") &&
                                         it.getTitle().equals("title2") &&
                                         it.getBody().equals("body2"))
                .verifyComplete();
    }

    @Test
    public void findPostsByUsername_whenSortByNew_shouldReturnResultsSortedByPostedDesc() {

        Post newest = Post.of("test1", "author1", "title1", "body1");
        newest.setPosted(LocalDate.of(2020, Month.OCTOBER, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
        postRepo.save(newest).block();

        Post oldest = Post.of("test2", "author1", "title2", "body2");
        oldest.setPosted(LocalDate.of(2019, Month.DECEMBER, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
        postRepo.save(oldest).block();

        Post middle = Post.of("test3", "author1", "title3", "body3");
        middle.setPosted(LocalDate.of(2020, Month.MARCH, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
        postRepo.save(middle).block();

        Flux<Post> postsBySubredditName = connectTcp()
                .flatMapMany(req -> req
                        .route("find.posts.user.{username}", "author1")
                        .data(PostRequest.builder().sort(PostRequest.SortBy.NEW).build())
                        .retrieveFlux(Post.class));

        StepVerifier
                .create(postsBySubredditName)
                .expectNextMatches(it -> it.getPostId().equals(newest.getPostId()))
                .expectNextMatches(it -> it.getPostId().equals(middle.getPostId()))
                .expectNextMatches(it -> it.getPostId().equals(oldest.getPostId()))
                .verifyComplete();
    }

    @Test
    public void findPostsByUsername_whenSortByOld_shouldReturnResultsSortedByPostedAsc() {

        Post newest = Post.of("test1", "author1", "title1", "body1");
        newest.setPosted(LocalDate.of(2020, Month.OCTOBER, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
        postRepo.save(newest).block();

        Post oldest = Post.of("test2", "author1", "title2", "body2");
        oldest.setPosted(LocalDate.of(2019, Month.DECEMBER, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
        postRepo.save(oldest).block();

        Post middle = Post.of("test3", "author1", "title3", "body3");
        middle.setPosted(LocalDate.of(2020, Month.MARCH, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
        postRepo.save(middle).block();

        Flux<Post> postsBySubredditName = connectTcp()
                .flatMapMany(req -> req
                        .route("find.posts.user.{username}", "author1")
                        .data(PostRequest.builder().sort(PostRequest.SortBy.OLD).build())
                        .retrieveFlux(Post.class));

        StepVerifier
                .create(postsBySubredditName)
                .expectNextMatches(it -> it.getPostId().equals(oldest.getPostId()))
                .expectNextMatches(it -> it.getPostId().equals(middle.getPostId()))
                .expectNextMatches(it -> it.getPostId().equals(newest.getPostId()))
                .verifyComplete();
    }

    @Test
    public void findPostsByUsername_whenSortByTop_shouldReturnResultsSortedByScoreDesc() {

        Post highest = Post.of("test1", "author1", "title1", "body1");
        highest.setScore(200L);
        postRepo.save(highest).block();

        Post lowest = Post.of("test2", "author1", "title2", "body2");
        lowest.setScore(-150L);
        postRepo.save(lowest).block();

        Post middle = Post.of("test3", "author1", "title3", "body3");
        middle.setScore(20L);
        postRepo.save(middle).block();

        Flux<Post> postsBySubredditName = connectTcp()
                .flatMapMany(req -> req
                        .route("find.posts.user.{username}", "author1")
                        .data(PostRequest.builder().sort(PostRequest.SortBy.TOP).build())
                        .retrieveFlux(Post.class));

        StepVerifier
                .create(postsBySubredditName)
                .expectNextMatches(it -> it.getScore() == 200L)
                .expectNextMatches(it -> it.getScore() == 20L)
                .expectNextMatches(it -> it.getScore() == -150L)
                .verifyComplete();
    }

    @Test
    public void createPost_whenNoJwt_shouldReturnError() {

        Mono<Post> post = connectTcp()
                .flatMap(req -> req
                        .route("create.post")
                        .data(new CreatePost("test", "title", "body"))
                        .retrieveMono(Post.class));

        StepVerifier
                .create(post)
                .verifyErrorMatches(ex -> ex.getMessage().equals("Access Denied"));
    }

    @Test
    public void createPost_shouldCreatePost() {

        String token = oAuth2.getAccessTokenForUsername("reddit-user", "password").block();

        Mono<Post> post = connectTcp()
                .flatMap(req -> req
                        .route("create.post")
                        .metadata(oAuth2.addTokenToMetadata(token))
                        .data(new CreatePost("test", "title", "body"))
                        .retrieveMono(Post.class));

        StepVerifier
                .create(post)
                .expectNextMatches(it -> it.getSubredditName().equals("test") &&
                                         it.getAuthor().equals("reddit-user") &&
                                         it.getTitle().equals("title") &&
                                         it.getBody().equals("body"))
                .verifyComplete();
    }

    @Test
    public void createPost_shouldAutomaticallyCreateTheUpvoteForTheAuthor() {

        String token = oAuth2.getAccessTokenForUsername("reddit-user", "password").block();

        Mono<Vote> vote = connectTcp()
                .flatMap(req -> req
                        .route("create.post")
                        .metadata(oAuth2.addTokenToMetadata(token))
                        .data(new CreatePost("test", "title", "body"))
                        .retrieveMono(Post.class))
                .flatMap(post -> voteRepo.findVoteByPostIdAndUsername(post.getPostId(), post.getAuthor()));

        StepVerifier
                .create(vote)
                .expectNextMatches(it -> it.getVoteType().equals(VoteType.UPVOTE) &&
                                         it.getUsername().equals("reddit-user"))
                .verifyComplete();
    }

    @Test
    public void createPost_shouldAutomaticallyUpvoteTheScoreOfThePost() {

        String token = oAuth2.getAccessTokenForUsername("reddit-user", "password").block();

        Mono<Post> created = connectTcp()
                .flatMap(req -> req
                        .route("create.post")
                        .metadata(oAuth2.addTokenToMetadata(token))
                        .data(new CreatePost("test", "title", "body"))
                        .retrieveMono(Post.class))
                .flatMap(post -> postRepo.findById(post.getPostId()));

        StepVerifier
                .create(created)
                .expectNextMatches(it -> it.getScore().equals(1L))
                .verifyComplete();
    }

    @Test
    public void editPost_whenNoJwt_shouldReturnError() {

        Mono<Post> edited = connectTcp()
                .flatMap(req -> req
                        .route("edit.post.{postId}", "test")
                        .data(new EditPost("new title", "new body"))
                        .retrieveMono(Post.class));

        StepVerifier
                .create(edited)
                .verifyErrorMatches(ex -> ex.getMessage().equals("Access Denied"));
    }

    @Test
    public void editPost_whenUserIsAuthor_shouldEditPost() {

        String token = oAuth2.getAccessTokenForUsername("reddit-user", "password").block();

        Post post = postRepo
                .save(Post.of("subreddit", "reddit-user", "title", "body"))
                .block();

        Mono<Post> edited = connectTcp()
                .flatMap(req -> req
                        .route("edit.post.{postId}", post.getPostId())
                        .metadata(oAuth2.addTokenToMetadata(token))
                        .data(new EditPost("new title", "new body"))
                        .retrieveMono(Post.class));

        StepVerifier
                .create(edited)
                .expectNextMatches(it -> it.getAuthor().equals("reddit-user") &&
                                         it.getTitle().equals("new title") &&
                                         it.getBody().equals("new body"))
                .verifyComplete();
    }

    @Test
    public void editPost_whenPostNotFound_shouldReturnError() {

        String token = oAuth2.getAccessTokenForUsername("reddit-user", "password").block();

        Mono<Post> edited = connectTcp()
                .flatMap(req -> req
                        .route("edit.post.{postId}", "test")
                        .metadata(oAuth2.addTokenToMetadata(token))
                        .data(new EditPost("new title", "new body"))
                        .retrieveMono(Post.class));

        StepVerifier
                .create(edited)
                .expectErrorMatches(ex -> ex.getMessage().equals(String.format(PostNotFoundException.POST_NOT_FOUND, "test")))
                .verify();
    }

    @Test
    public void editPost_whenUserIsNotAuthor_shouldReturnError() {

        Post post = postRepo
                .save(Post.of("subreddit", "another-reddit-user", "title", "body"))
                .block();

        String token = oAuth2.getAccessTokenForUsername("reddit-user", "password").block();

        Mono<Post> edited = connectTcp()
                .flatMap(req -> req
                        .route("edit.post.{postId}", post.getPostId())
                        .metadata(oAuth2.addTokenToMetadata(token))
                        .data(new EditPost("new title", "new body"))
                        .retrieveMono(Post.class));

        StepVerifier
                .create(edited)
                .expectErrorMatches(ex -> ex.getMessage().equals(String.format(NotAuthorOfPostException.NOT_AUTHOR_OF_POST, "reddit-user", post.getPostId())))
                .verify();
    }

    @Test
    public void deletePost_whenNoJwt_shouldReturnError() {

        Mono<Void> deleted = connectTcp()
                .flatMap(req -> req
                        .route("delete.post.{postId}", "test")
                        .retrieveMono(Void.class));
        StepVerifier
                .create(deleted)
                .verifyErrorMatches(ex -> ex.getMessage().equals("Access Denied"));
    }

    @Test
    public void deletePost_whenUserIsAuthor_shouldDeletePost() {

        Post post = postRepo
                .save(Post.of("subreddit", "reddit-user", "title", "body"))
                .block();

        String token = oAuth2.getAccessTokenForUsername("reddit-user", "password").block();

        Mono<Post> deleted = connectTcp()
                .flatMap(req -> req
                        .route("delete.post.{postId}", post.getPostId())
                        .metadata(oAuth2.addTokenToMetadata(token))
                        .retrieveMono(Void.class))
                .then(Mono.defer(() -> postRepo.findById(post.getPostId())));

        StepVerifier
                .create(deleted)
                .expectNextMatches(Post::isDeleted)
                .verifyComplete();
    }

    @Test
    public void deletePost_whenPostNotFound_shouldReturnError() {

        String token = oAuth2.getAccessTokenForUsername("reddit-user", "password").block();

        Mono<Void> deleted = connectTcp()
                .flatMap(req -> req
                        .route("delete.post.{postId}", "test")
                        .metadata(oAuth2.addTokenToMetadata(token))
                        .retrieveMono(Void.class));

        StepVerifier
                .create(deleted)
                .expectErrorMatches(ex -> ex.getMessage().equals(String.format(PostNotFoundException.POST_NOT_FOUND, "test")))
                .verify();
    }

    @Test
    public void deletePost_whenUserIsNotAuthor_shouldReturnError() {

        Post post = postRepo
                .save(Post.of("subreddit", "another-reddit-user", "title", "body"))
                .block();

        String token = oAuth2.getAccessTokenForUsername("reddit-user", "password").block();

        Mono<Void> deleted = connectTcp()
                .flatMap(req -> req
                        .route("delete.post.{postId}", post.getPostId())
                        .metadata(oAuth2.addTokenToMetadata(token))
                        .retrieveMono(Void.class));

        StepVerifier
                .create(deleted)
                .expectErrorMatches(ex -> ex.getMessage()
                        .equals(String.format(NotAuthorOfPostException.NOT_AUTHOR_OF_POST, "reddit-user", post.getPostId())))
                .verify();
    }

    @Test
    public void votePost_whenNoJwt_shouldReturnError() {

        Mono<Void> voted = connectTcp()
                .flatMap(req -> req
                        .route("vote.post.{postId}.{voteType}", "test", VoteType.UPVOTE)
                        .retrieveMono(Void.class));
        StepVerifier
                .create(voted)
                .verifyErrorMatches(ex -> ex.getMessage().equals("Access Denied"));
    }

    @ParameterizedTest
    @EnumSource(VoteType.class)
    public void votePost_whenVoteNotFound_shouldSaveVote(VoteType voteType) {

        Post post = postRepo
                .save(Post.of("subreddit", "another-reddit-user", "title", "body"))
                .block();

        String token = oAuth2.getAccessTokenForUsername("reddit-user", "password").block();

        Mono<Vote> voted = connectTcp()
                .flatMap(req -> req
                        .route("vote.post.{postId}.{voteType}", post.getPostId(), voteType)
                        .metadata(oAuth2.addTokenToMetadata(token))
                        .retrieveMono(Void.class))
                .then(Mono.defer(() -> voteRepo.findVoteByPostIdAndUsername(post.getPostId(), "reddit-user")));

        StepVerifier
                .create(voted)
                .expectNextMatches(it -> it.getPostId().equals(post.getPostId()) &&
                                         it.getUsername().equals("reddit-user") &&
                                         it.getVoteType().equals(voteType))
                .verifyComplete();
    }

    @ParameterizedTest
    @EnumSource(VoteType.class)
    public void votePost_whenVoteNotFound_shouldChangeScoreBasedOnTheSubmittedVote(VoteType voteType) {

        Post post = postRepo
                .save(Post.of("subreddit", "another-reddit-user", "title", "body"))
                .block();

        Long previousScore = post.getScore();
        Long expectedScore = voteType == VoteType.UPVOTE ? (previousScore + 1) : (previousScore - 1);

        String token = oAuth2.getAccessTokenForUsername("reddit-user", "password").block();

        Mono<Post> voted = connectTcp()
                .flatMap(req -> req
                        .route("vote.post.{postId}.{voteType}", post.getPostId(), voteType)
                        .metadata(oAuth2.addTokenToMetadata(token))
                        .retrieveMono(Void.class))
                .then(Mono.defer(() -> postRepo.findById(post.getPostId())));

        StepVerifier
                .create(voted)
                .expectNextMatches(it -> it.getScore().equals(expectedScore))
                .verifyComplete();
    }

    @ParameterizedTest
    @EnumSource(VoteType.class)
    public void votePost_whenPostNotFound_returnError(VoteType voteType) {

        String token = oAuth2.getAccessTokenForUsername("reddit-user", "password").block();

        Mono<Void> voted = connectTcp()
                .flatMap(req -> req
                        .route("vote.post.{postId}.{voteType}", "test", voteType)
                        .metadata(oAuth2.addTokenToMetadata(token))
                        .retrieveMono(Void.class));

        StepVerifier
                .create(voted)
                .expectErrorMatches(ex -> ex.getMessage().equals(String.format(PostNotFoundException.POST_NOT_FOUND, "test")))
                .verify();
    }

    @ParameterizedTest
    @EnumSource(VoteType.class)
    public void votePost_whenVoteFoundAndHasDifferentType_shouldOverrideVote(VoteType voteType) {

        Post post = postRepo
                .save(Post.of("subreddit", "another-reddit-user", "title", "body"))
                .block();

        Vote vote = voteRepo
                .save(Vote.of(post.getPostId(), "reddit-user", voteType))
                .block();

        String token = oAuth2.getAccessTokenForUsername("reddit-user", "password").block();

        Mono<Vote> overridden = connectTcp()
                .flatMap(req -> req
                        .route("vote.post.{postId}.{voteType}", post.getPostId(), voteType.opposite())
                        .metadata(oAuth2.addTokenToMetadata(token))
                        .retrieveMono(Void.class))
                .then(Mono.defer(() -> voteRepo.findVoteByPostIdAndUsername(post.getPostId(), "reddit-user")));

        StepVerifier
                .create(overridden)
                .expectNextMatches(it -> it.getPostId().equals(post.getPostId()) &&
                                         it.getUsername().equals("reddit-user") &&
                                         it.getVoteType().equals(voteType.opposite()))
                .verifyComplete();
    }

    @ParameterizedTest
    @EnumSource(VoteType.class)
    public void votePost_whenVoteFoundAndHasDifferentType_shouldOverrideScoreBasedOnSubmittedVote(VoteType voteType) {

        Post post = postRepo
                .save(Post.of("subreddit", "another-reddit-user", "title", "body"))
                .block();

        Long previousScore = post.getScore();
        Long updatedScore = voteType == VoteType.UPVOTE ? (previousScore + 1) : (previousScore - 1);
        Long expectedScore = voteType.opposite() == VoteType.UPVOTE ? (previousScore + 1) : (previousScore - 1);

        String token = oAuth2.getAccessTokenForUsername("reddit-user", "password").block();

        Post voted = voteRepo
                .save(Vote.of(post.getPostId(), "reddit-user", voteType))
                .then(Mono.just(post))
                .doOnNext(it -> it.setScore(updatedScore))
                .flatMap(postRepo::save)
                .block();

        Mono<Post> overridden = connectTcp()
                .flatMap(req -> req
                        .route("vote.post.{postId}.{voteType}", post.getPostId(), voteType.opposite())
                        .metadata(oAuth2.addTokenToMetadata(token))
                        .retrieveMono(Void.class))
                .then(Mono.defer(() -> postRepo.findById(post.getPostId())));

        StepVerifier
                .create(overridden)
                .expectNextMatches(it -> it.getScore().equals(expectedScore))
                .verifyComplete();
    }

    @ParameterizedTest
    @EnumSource(VoteType.class)
    public void votePost_whenVoteFoundAndHasSameType_shouldDeleteVote(VoteType voteType) {

        Post post = postRepo
                .save(Post.of("subreddit", "another-reddit-user", "title", "body"))
                .block();

        Vote vote = voteRepo
                .save(Vote.of(post.getPostId(), "reddit-user", voteType))
                .block();

        String token = oAuth2.getAccessTokenForUsername("reddit-user", "password").block();

        Mono<Vote> voted = connectTcp()
                .flatMap(req -> req
                        .route("vote.post.{postId}.{voteType}", post.getPostId(), voteType)
                        .metadata(oAuth2.addTokenToMetadata(token))
                        .retrieveMono(Void.class))
                .then(Mono.defer(() -> voteRepo.findVoteByPostIdAndUsername(post.getPostId(), "reddit-user")));

        StepVerifier
                .create(voted)
                .verifyComplete();
    }

    @ParameterizedTest
    @EnumSource(VoteType.class)
    public void votePost_whenVoteFoundAndHasSameType_shouldRestorePreviousScore(VoteType voteType) {

        Post post = postRepo
                .save(Post.of("subreddit", "another-reddit-user", "title", "body"))
                .block();

        Long previousScore = post.getScore();

        String token = oAuth2.getAccessTokenForUsername("reddit-user", "password").block();

        voteRepo
                .save(Vote.of(post.getPostId(), "reddit-user", voteType))
                .then(Mono.just(post))
                .doOnNext(it -> it.setScore(voteType.equals(VoteType.UPVOTE) ? 1L : -1L))
                .flatMap(postRepo::save)
                .block();

        Mono<Post> restored = connectTcp()
                .flatMap(req -> req
                        .route("vote.post.{postId}.{voteType}", post.getPostId(), voteType)
                        .metadata(oAuth2.addTokenToMetadata(token))
                        .retrieveMono(Void.class))
                .then(Mono.defer(() -> postRepo.findById(post.getPostId())));

        StepVerifier
                .create(restored)
                .expectNextMatches(it -> it.getScore().equals(previousScore))
                .verifyComplete();
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
