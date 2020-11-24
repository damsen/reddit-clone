package com.redditclone.commentservice;

import com.redditclone.commentservice.comment.*;
import com.redditclone.commentservice.vote.Vote;
import com.redditclone.commentservice.vote.VoteRepository;
import com.redditclone.commentservice.vote.VoteType;
import io.rsocket.metadata.WellKnownMimeType;
import org.bson.types.ObjectId;
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

@SpringBootTest
public class CommentServiceApplicationTests {

    @LocalRSocketServerPort
    private int port;

    @Autowired
    private RSocketRequester.Builder requesterBuilder;

    @Autowired
    private CommentRepository commentRepo;

    @Autowired
    private VoteRepository voteRepo;

    @Autowired
    private OAuth2 oAuth2;

    @BeforeEach
    public void setup() {
        commentRepo.deleteAll().block();
        voteRepo.deleteAll().block();
    }

    private RSocketRequester tcp() {
        return requesterBuilder
                .dataMimeType(MediaType.APPLICATION_CBOR)
                .rsocketStrategies(configurer -> configurer.encoder(new BearerTokenAuthenticationEncoder()))
                .tcp("localhost", port);
    }

    @Test
    public void findCommentsByPostId_shouldReturnCommentTree() {

        Comment root = commentRepo.save(Comment.of("post1", null, "user1", "body1")).block();
        Comment child1 = commentRepo.save(Comment.of("post1", root.getCommentId(), "user2", "body2")).block();
        Comment child2 = commentRepo.save(Comment.of("post1", root.getCommentId(), "user3", "body3")).block();
        Comment child3 = commentRepo.save(Comment.of("post1", child1.getCommentId(), "user4", "body4")).block();
        Comment child4 = commentRepo.save(Comment.of("post1", child2.getCommentId(), "user5", "body5")).block();
        Comment root2 = commentRepo.save(Comment.of("post1", null, "user6", "body6")).block();
        Comment root3 = commentRepo.save(Comment.of("post2", null, "user7", "body7")).block();

        Flux<CommentTree> comments = tcp().route("find.comments.post.{postId}", "post1")
                        .data(CommentRequest.builder().build())
                        .retrieveFlux(CommentTree.class);

        StepVerifier
                .create(comments)
                .expectNextMatches(it -> it.getPostId().equals("post1") &&
                                         it.getParentId() == null &&
                                         it.getAuthor().equals("user1") &&
                                         it.getBody().equals("body1") &&
                                         it.getChildren().size() == 2 &&
                                         it.getChildren().get(0).getChildren().size() == 1 &&
                                         it.getChildren().get(1).getChildren().size() == 1)
                .expectNextMatches(it -> it.getPostId().equals("post1") &&
                                         it.getParentId() == null &&
                                         it.getAuthor().equals("user6") &&
                                         it.getBody().equals("body6") &&
                                         it.getChildren().isEmpty())
                .verifyComplete();
    }

    @Test
    public void findCommentsByPostId_shouldReturnPagedResults() {

        commentRepo.save(Comment.of("post2", null, "user2", "body2")).block();
        commentRepo.save(Comment.of("post2", null, "user3", "body3")).block();

        Flux<CommentTree> comments = tcp().route("find.comments.post.{postId}", "post2")
                        .data(CommentRequest.builder().page(1).size(1).build())
                        .retrieveFlux(CommentTree.class);

        StepVerifier
                .create(comments)
                .expectNextMatches(it -> it.getPostId().equals("post2") &&
                                         it.getAuthor().equals("user3") &&
                                         it.getBody().equals("body3"))
                .verifyComplete();
    }

    @Test
    public void findCommentsByPostId_whenSortByNew_shouldReturnResultsSortedByCommentedDesc() {

        Comment oldest = Comment.of("post2", null, "user2", "body2");
        oldest.setCommented(LocalDate.of(2019, Month.DECEMBER, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
        commentRepo.save(oldest).block();

        Comment newest = Comment.of("post2", null, "user3", "body3");
        newest.setCommented(LocalDate.of(2020, Month.APRIL, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
        commentRepo.save(newest).block();

        Comment middle = Comment.of("post2", null, "user4", "body4");
        middle.setCommented(LocalDate.of(2020, Month.MARCH, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
        commentRepo.save(middle).block();

        Comment oldestChild = Comment.of("post2", newest.getCommentId(), "user5", "body5");
        oldestChild.setCommented(LocalDate.of(2020, Month.MAY, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
        commentRepo.save(oldestChild).block();

        Comment newestChild = Comment.of("post2", newest.getCommentId(), "user6", "body6");
        newestChild.setCommented(LocalDate.of(2020, Month.OCTOBER, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
        commentRepo.save(newestChild).block();

        Comment middleChild = Comment.of("post2", newest.getCommentId(), "user7", "body7");
        middleChild.setCommented(LocalDate.of(2020, Month.AUGUST, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
        commentRepo.save(middleChild).block();

        Flux<CommentTree> comments = tcp().route("find.comments.post.{postId}", "post2")
                        .data(CommentRequest.builder().sort(CommentRequest.SortBy.NEW).build())
                        .retrieveFlux(CommentTree.class);

        StepVerifier
                .create(comments)
                .expectNextMatches(it -> it.getCommentId().equals(newest.getCommentId()) &&
                                         it.getChildren().get(0).getCommentId().equals(newestChild.getCommentId()) &&
                                         it.getChildren().get(1).getCommentId().equals(middleChild.getCommentId()) &&
                                         it.getChildren().get(2).getCommentId().equals(oldestChild.getCommentId()))
                .expectNextMatches(it -> it.getCommentId().equals(middle.getCommentId()))
                .expectNextMatches(it -> it.getCommentId().equals(oldest.getCommentId()))
                .verifyComplete();
    }

    @Test
    public void findCommentsByPostId_whenSortByOld_shouldReturnResultsSortedByCommentedAsc() {

        Comment oldest = Comment.of("post2", null, "user2", "body2");
        oldest.setCommented(LocalDate.of(2019, Month.DECEMBER, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
        commentRepo.save(oldest).block();

        Comment newest = Comment.of("post2", null, "user3", "body3");
        newest.setCommented(LocalDate.of(2020, Month.OCTOBER, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
        commentRepo.save(newest).block();

        Comment middle = Comment.of("post2", null, "user4", "body4");
        middle.setCommented(LocalDate.of(2020, Month.MARCH, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
        commentRepo.save(middle).block();

        Comment oldestChild = Comment.of("post2", newest.getCommentId(), "user5", "body5");
        oldestChild.setCommented(LocalDate.of(2020, Month.MAY, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
        commentRepo.save(oldestChild).block();

        Comment newestChild = Comment.of("post2", newest.getCommentId(), "user6", "body6");
        newestChild.setCommented(LocalDate.of(2020, Month.OCTOBER, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
        commentRepo.save(newestChild).block();

        Comment middleChild = Comment.of("post2", newest.getCommentId(), "user7", "body7");
        middleChild.setCommented(LocalDate.of(2020, Month.AUGUST, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
        commentRepo.save(middleChild).block();

        Flux<CommentTree> comments = tcp().route("find.comments.post.{postId}", "post2")
                        .data(CommentRequest.builder().sort(CommentRequest.SortBy.OLD).build())
                        .retrieveFlux(CommentTree.class);

        StepVerifier
                .create(comments)
                .expectNextMatches(it -> it.getCommentId().equals(oldest.getCommentId()))
                .expectNextMatches(it -> it.getCommentId().equals(middle.getCommentId()))
                .expectNextMatches(it -> it.getCommentId().equals(newest.getCommentId()) &&
                                         it.getChildren().get(0).getCommentId().equals(oldestChild.getCommentId()) &&
                                         it.getChildren().get(1).getCommentId().equals(middleChild.getCommentId()) &&
                                         it.getChildren().get(2).getCommentId().equals(newestChild.getCommentId()))
                .verifyComplete();
    }

    @Test
    public void findCommentsByPostId_whenSortByTop_shouldReturnResultsSortedByScoreDesc() {

        Comment lowest = Comment.of("post2", null, "user2", "body2");
        lowest.setScore(-160L);
        commentRepo.save(lowest).block();

        Comment highest = Comment.of("post2", null, "user3", "body3");
        highest.setScore(245L);
        commentRepo.save(highest).block();

        Comment middle = Comment.of("post2", null, "user4", "body4");
        middle.setScore(32L);
        commentRepo.save(middle).block();

        Comment lowestChild = Comment.of("post2", lowest.getCommentId(), "user5", "body5");
        lowestChild.setScore(-10L);
        commentRepo.save(lowestChild).block();

        Comment highestChild = Comment.of("post2", lowest.getCommentId(), "user6", "body6");
        highestChild.setScore(88L);
        commentRepo.save(highestChild).block();

        Comment middleChild = Comment.of("post2", lowest.getCommentId(), "user7", "body7");
        middleChild.setScore(20L);
        commentRepo.save(middleChild).block();

        Flux<CommentTree> comments = tcp().route("find.comments.post.{postId}", "post2")
                        .data(CommentRequest.builder().sort(CommentRequest.SortBy.TOP).build())
                        .retrieveFlux(CommentTree.class);

        StepVerifier
                .create(comments)
                .expectNextMatches(it -> it.getCommentId().equals(highest.getCommentId()))
                .expectNextMatches(it -> it.getCommentId().equals(middle.getCommentId()))
                .expectNextMatches(it -> it.getCommentId().equals(lowest.getCommentId()) &&
                                         it.getChildren().get(0).getCommentId().equals(highestChild.getCommentId()) &&
                                         it.getChildren().get(1).getCommentId().equals(middleChild.getCommentId()) &&
                                         it.getChildren().get(2).getCommentId().equals(lowestChild.getCommentId()))
                .verifyComplete();
    }

    @Test
    public void findCommentsByUsername_shouldReturnPagedResults() {

        commentRepo.save(Comment.of("post1", null, "user1", "body1")).block();
        Comment c2 = commentRepo.save(Comment.of("post2", null, "user2", "body2")).block();
        commentRepo.save(Comment.of("post2", c2.getCommentId(), "user1", "body3")).block();

        Flux<Comment> comments = tcp().route("find.comments.user.{username}", "user1")
                        .data(CommentRequest.builder().page(1).size(1).build())
                        .retrieveFlux(Comment.class);

        StepVerifier
                .create(comments)
                .expectNextMatches(it -> it.getPostId().equals("post2") &&
                                         it.getParentId().equals(c2.getCommentId()) &&
                                         it.getAuthor().equals("user1") &&
                                         it.getBody().equals("body3"))
                .verifyComplete();
    }

    @Test
    public void findCommentsByUsername_whenSortByNew_shouldReturnResultsSortedByCommentedDesc() {

        Comment oldest = Comment.of("post1", null, "user1", "body1");
        oldest.setCommented(LocalDate.of(2019, Month.DECEMBER, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
        commentRepo.save(oldest).block();

        Comment newest = Comment.of("post2", null, "user1", "body2");
        newest.setCommented(LocalDate.of(2020, Month.APRIL, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
        commentRepo.save(newest).block();

        Comment middle = Comment.of("post3", null, "user1", "body3");
        middle.setCommented(LocalDate.of(2020, Month.MARCH, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
        commentRepo.save(middle).block();

        Flux<Comment> comments = tcp().route("find.comments.user.{username}", "user1")
                        .data(CommentRequest.builder().sort(CommentRequest.SortBy.NEW).build())
                        .retrieveFlux(Comment.class);

        StepVerifier
                .create(comments)
                .expectNextMatches(it -> it.getCommentId().equals(newest.getCommentId()))
                .expectNextMatches(it -> it.getCommentId().equals(middle.getCommentId()))
                .expectNextMatches(it -> it.getCommentId().equals(oldest.getCommentId()))
                .verifyComplete();
    }

    @Test
    public void findCommentsByUsername_whenSortByOld_shouldReturnResultsSortedByCommentedAsc() {

        Comment oldest = Comment.of("post1", null, "user1", "body1");
        oldest.setCommented(LocalDate.of(2019, Month.DECEMBER, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
        commentRepo.save(oldest).block();

        Comment newest = Comment.of("post2", null, "user1", "body2");
        newest.setCommented(LocalDate.of(2020, Month.APRIL, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
        commentRepo.save(newest).block();

        Comment middle = Comment.of("post3", null, "user1", "body3");
        middle.setCommented(LocalDate.of(2020, Month.MARCH, 1).atStartOfDay().toInstant(ZoneOffset.UTC));
        commentRepo.save(middle).block();

        Flux<Comment> comments = tcp().route("find.comments.user.{username}", "user1")
                        .data(CommentRequest.builder().sort(CommentRequest.SortBy.OLD).build())
                        .retrieveFlux(Comment.class);

        StepVerifier
                .create(comments)
                .expectNextMatches(it -> it.getCommentId().equals(oldest.getCommentId()))
                .expectNextMatches(it -> it.getCommentId().equals(middle.getCommentId()))
                .expectNextMatches(it -> it.getCommentId().equals(newest.getCommentId()))
                .verifyComplete();
    }

    @Test
    public void findCommentsByUsername_whenSortByTop_shouldReturnResultsSortedByScoreDesc() {

        Comment lowest = Comment.of("post1", null, "user1", "body1");
        lowest.setScore(-160L);
        commentRepo.save(lowest).block();

        Comment highest = Comment.of("post2", null, "user1", "body2");
        highest.setScore(245L);
        commentRepo.save(highest).block();

        Comment middle = Comment.of("post3", null, "user1", "body3");
        middle.setScore(32L);
        commentRepo.save(middle).block();

        Flux<Comment> comments = tcp().route("find.comments.user.{username}", "user1")
                        .data(CommentRequest.builder().sort(CommentRequest.SortBy.TOP).build())
                        .retrieveFlux(Comment.class);

        StepVerifier
                .create(comments)
                .expectNextMatches(it -> it.getCommentId().equals(highest.getCommentId()))
                .expectNextMatches(it -> it.getCommentId().equals(middle.getCommentId()))
                .expectNextMatches(it -> it.getCommentId().equals(lowest.getCommentId()))
                .verifyComplete();
    }

    @Test
    public void createComment_whenNoJwt_shouldReturnError() {

        Mono<Comment> created = tcp().route("create.comment")
                        .data(new CreateComment("post", null, "body"))
                        .retrieveMono(Comment.class);

        StepVerifier
                .create(created)
                .verifyErrorMatches(ex -> ex.getMessage().equals("Access Denied"));
    }

    @Test
    public void createComment_withoutParentId_shouldCreateCommentWithoutParentId() {

        String token = oAuth2.getAccessTokenForUsername("reddit-user", "password").block();

        Mono<Comment> created = tcp().route("create.comment")
                        .metadata(oAuth2.addTokenToMetadata(token))
                        .data(new CreateComment("post", null, "body"))
                        .retrieveMono(Comment.class);

        StepVerifier
                .create(created)
                .expectNextMatches(it -> it.getPostId().equals("post") &&
                                         it.getParentId() == null &&
                                         it.getAuthor().equals("reddit-user") &&
                                         it.getBody().equals("body"))
                .verifyComplete();
    }

    @Test
    public void createComment_withParentId_shouldCreateCommentWithParentId() {

        String parentId = new ObjectId().toString();

        String token = oAuth2.getAccessTokenForUsername("reddit-user", "password").block();

        Mono<Comment> created = tcp().route("create.comment")
                        .metadata(oAuth2.addTokenToMetadata(token))
                        .data(new CreateComment("post", parentId, "body"))
                        .retrieveMono(Comment.class);

        StepVerifier
                .create(created)
                .expectNextMatches(it -> it.getPostId().equals("post") &&
                                         it.getAuthor().equals("reddit-user") &&
                                         it.getParentId().equals(parentId) &&
                                         it.getBody().equals("body"))
                .verifyComplete();
    }

    @Test
    public void createComment_shouldAutomaticallyCreateTheUpvoteForTheAuthor() {

        String token = oAuth2.getAccessTokenForUsername("reddit-user", "password").block();

        Mono<Vote> vote = tcp().route("create.comment")
                        .metadata(oAuth2.addTokenToMetadata(token))
                        .data(new CreateComment("post", null, "body"))
                        .retrieveMono(Comment.class)
                .flatMap(comment -> voteRepo.findVoteByCommentIdAndUsername(comment.getCommentId(), comment.getAuthor()));

        StepVerifier
                .create(vote)
                .expectNextMatches(it -> it.getVoteType().equals(VoteType.UPVOTE) &&
                                         it.getUsername().equals("reddit-user"))
                .verifyComplete();
    }

    @Test
    public void createComment_shouldAutomaticallyUpvoteTheScoreOfTheComment() {

        String token = oAuth2.getAccessTokenForUsername("reddit-user", "password").block();

        Mono<Comment> created = tcp().route("create.comment")
                        .metadata(oAuth2.addTokenToMetadata(token))
                        .data(new CreateComment("post", null, "body"))
                        .retrieveMono(Comment.class)
                .flatMap(comment -> commentRepo.findById(comment.getCommentId()));

        StepVerifier
                .create(created)
                .expectNextMatches(it -> it.getScore() == 1L)
                .verifyComplete();
    }

    @Test
    public void editComment_whenNoJwt_shouldReturnError() {

        Mono<Comment> edited = tcp().route("edit.comment.{commentId}", "test")
                        .data(new EditComment("new body"))
                        .retrieveMono(Comment.class);

        StepVerifier
                .create(edited)
                .verifyErrorMatches(ex -> ex.getMessage().equals("Access Denied"));
    }

    @Test
    public void editComment_whenUserIsAuthor_shouldEditComment() {

        Comment comment = commentRepo
                .save(Comment.of("post", null, "reddit-user", "body"))
                .block();

        String token = oAuth2.getAccessTokenForUsername("reddit-user", "password").block();

        Mono<Comment> edited = tcp().route("edit.comment.{commentId}", comment.getCommentId())
                        .metadata(oAuth2.addTokenToMetadata(token))
                        .data(new EditComment("new body"))
                        .retrieveMono(Comment.class);

        StepVerifier
                .create(edited)
                .expectNextMatches(it -> it.getAuthor().equals("reddit-user") &&
                                         it.getBody().equals("new body") &&
                                         it.getEdited() != null)
                .verifyComplete();
    }

    @Test
    public void editComment_whenCommentNotFound_shouldReturnError() {

        String token = oAuth2.getAccessTokenForUsername("reddit-user", "password").block();

        Mono<Comment> edited = tcp().route("edit.comment.{commentId}", "test")
                        .metadata(oAuth2.addTokenToMetadata(token))
                        .data(new EditComment("new body"))
                        .retrieveMono(Comment.class);

        StepVerifier
                .create(edited)
                .expectErrorMatches(ex -> ex.getMessage().equals(String.format(CommentNotFoundException.COMMENT_NOT_FOUND, "test")))
                .verify();
    }

    @Test
    public void editComment_whenUserIsNotAuthor_shouldReturnError() {

        Comment comment = commentRepo
                .save(Comment.of("post", null, "another-reddit-user", "body"))
                .block();

        String token = oAuth2.getAccessTokenForUsername("reddit-user", "password").block();

        Mono<Comment> edited = tcp().route("edit.comment.{commentId}", comment.getCommentId())
                        .metadata(oAuth2.addTokenToMetadata(token))
                        .data(new EditComment("new body"))
                        .retrieveMono(Comment.class);

        StepVerifier
                .create(edited)
                .expectErrorMatches(ex -> ex.getMessage().equals(String.format(NotAuthorOfCommentException.NOT_AUTHOR_OF_COMMENT, "reddit-user", comment.getCommentId())))
                .verify();
    }

    @Test
    public void deleteComment_whenNoJwt_shouldReturnError() {

        Mono<Void> deleted = tcp().route("delete.comment.{commentId}", "test")
                        .retrieveMono(Void.class);

        StepVerifier
                .create(deleted)
                .verifyErrorMatches(ex -> ex.getMessage().equals("Access Denied"));
    }

    @Test
    public void deleteComment_whenUserIsAuthor_shouldDeleteComment() {

        Comment comment = commentRepo
                .save(Comment.of("post", null, "reddit-user", "body"))
                .block();

        String token = oAuth2.getAccessTokenForUsername("reddit-user", "password").block();

        Mono<Comment> deleted = tcp().route("delete.comment.{commentId}", comment.getCommentId())
                        .metadata(oAuth2.addTokenToMetadata(token))
                        .retrieveMono(Void.class)
                .then(Mono.defer(() -> commentRepo.findById(comment.getCommentId())));

        StepVerifier
                .create(deleted)
                .expectNextMatches(Comment::isDeleted)
                .verifyComplete();
    }

    @Test
    public void deleteComment_whenCommentNotFound_shouldReturnError() {

        String token = oAuth2.getAccessTokenForUsername("reddit-user", "password").block();

        Mono<Void> deleted = tcp().route("delete.comment.{commentId}", "test")
                        .metadata(oAuth2.addTokenToMetadata(token))
                        .retrieveMono(Void.class);

        StepVerifier
                .create(deleted)
                .expectErrorMatches(ex -> ex.getMessage().equals(String.format(CommentNotFoundException.COMMENT_NOT_FOUND, "test")))
                .verify();
    }

    @Test
    public void deleteComment_whenUserIsNotAuthor_shouldReturnError() {

        Comment comment = commentRepo
                .save(Comment.of("post", null, "another-reddit-user", "body"))
                .block();

        String token = oAuth2.getAccessTokenForUsername("reddit-user", "password").block();

        Mono<Void> deleted = tcp().route("delete.comment.{commentId}", comment.getCommentId())
                        .metadata(oAuth2.addTokenToMetadata(token))
                        .retrieveMono(Void.class);

        StepVerifier
                .create(deleted)
                .expectErrorMatches(ex -> ex.getMessage()
                        .equals(String.format(NotAuthorOfCommentException.NOT_AUTHOR_OF_COMMENT, "reddit-user", comment.getCommentId())))
                .verify();
    }

    @Test
    public void voteComment_whenNoJwt_shouldReturnError() {

        Mono<Void> voted = tcp().route("vote.comment.{commentId}.{voteType}", "test", VoteType.UPVOTE)
                        .retrieveMono(Void.class);

        StepVerifier
                .create(voted)
                .verifyErrorMatches(ex -> ex.getMessage().equals("Access Denied"));
    }

    @ParameterizedTest
    @EnumSource(VoteType.class)
    public void voteComment_whenVoteNotFound_shouldSaveVote(VoteType voteType) {

        Comment comment = commentRepo
                .save(Comment.of("post", null, "another-reddit-user", "body"))
                .block();

        String token = oAuth2.getAccessTokenForUsername("reddit-user", "password").block();

        Mono<Vote> voted = tcp().route("vote.comment.{commentId}.{voteType}", comment.getCommentId(), voteType)
                        .metadata(oAuth2.addTokenToMetadata(token))
                        .retrieveMono(Void.class)
                .then(Mono.defer(() -> voteRepo.findVoteByCommentIdAndUsername(comment.getCommentId(), "reddit-user")));

        StepVerifier
                .create(voted)
                .expectNextMatches(it -> it.getVoteType().equals(voteType) &&
                                         it.getCommentId().equals(comment.getCommentId()) &&
                                         it.getUsername().equals("reddit-user"))
                .verifyComplete();
    }

    @ParameterizedTest
    @EnumSource(VoteType.class)
    public void voteComment_whenVoteNotFound_shouldChangeScoreBasedOnTheSubmittedVote(VoteType voteType) {

        Comment comment = commentRepo
                .save(Comment.of("post", null, "another-reddit-user", "body"))
                .block();

        Long previousScore = comment.getScore();
        Long expectedScore = voteType == VoteType.UPVOTE ? (previousScore + 1) : (previousScore - 1);

        String token = oAuth2.getAccessTokenForUsername("reddit-user", "password").block();

        Mono<Comment> voted = tcp().route("vote.comment.{commentId}.{voteType}", comment.getCommentId(), voteType)
                        .metadata(oAuth2.addTokenToMetadata(token))
                        .retrieveMono(Void.class)
                .then(Mono.defer(() -> commentRepo.findById(comment.getCommentId())));

        StepVerifier
                .create(voted)
                .expectNextMatches(it -> it.getScore() == expectedScore)
                .verifyComplete();
    }

    @ParameterizedTest
    @EnumSource(VoteType.class)
    public void voteComment_whenCommentNotFound_returnError(VoteType voteType) {

        String token = oAuth2.getAccessTokenForUsername("reddit-user", "password").block();

        Mono<Void> voted = tcp().route("vote.comment.{commentId}.{voteType}", "test", voteType)
                        .metadata(oAuth2.addTokenToMetadata(token))
                        .retrieveMono(Void.class);

        StepVerifier
                .create(voted)
                .expectErrorMatches(ex -> ex.getMessage().equals(String.format(CommentNotFoundException.COMMENT_NOT_FOUND, "test")))
                .verify();
    }

    @ParameterizedTest
    @EnumSource(VoteType.class)
    public void voteComment_whenVoteFoundAndHasDifferentType_shouldOverrideVote(VoteType voteType) {

        Comment comment = commentRepo
                .save(Comment.of("post", null, "another-reddit-user", "body"))
                .block();

        Vote vote = voteRepo
                .save(Vote.of(comment.getCommentId(), "reddit-user", voteType))
                .block();

        String token = oAuth2.getAccessTokenForUsername("reddit-user", "password").block();

        Mono<Vote> overridden = tcp().route("vote.comment.{commentId}.{voteType}", comment.getCommentId(), voteType.opposite())
                        .metadata(oAuth2.addTokenToMetadata(token))
                        .retrieveMono(Void.class)
                .then(Mono.defer(() -> voteRepo.findVoteByCommentIdAndUsername(comment.getCommentId(), "reddit-user")));

        StepVerifier
                .create(overridden)
                .expectNextMatches(it -> it.getVoteType().equals(voteType.opposite()) &&
                                         it.getCommentId().equals(comment.getCommentId()) &&
                                         it.getUsername().equals("reddit-user"))
                .verifyComplete();
    }

    @ParameterizedTest
    @EnumSource(VoteType.class)
    public void voteComment_whenVoteFoundAndHasDifferentType_shouldOverrideScoreBasedOnSubmittedVote(VoteType voteType) {

        Comment comment = commentRepo
                .save(Comment.of("post", null, "another-reddit-user", "body"))
                .block();

        Long previousScore = comment.getScore();
        Long updatedScore = voteType == VoteType.UPVOTE ? (previousScore + 1) : (previousScore - 1);
        Long expectedScore = voteType.opposite() == VoteType.UPVOTE ? (previousScore + 1) : (previousScore - 1);

        Comment voted = voteRepo
                .save(Vote.of(comment.getCommentId(), "reddit-user", voteType))
                .then(Mono.just(comment))
                .doOnNext(it -> it.setScore(updatedScore))
                .flatMap(commentRepo::save)
                .block();

        String token = oAuth2.getAccessTokenForUsername("reddit-user", "password").block();

        Mono<Comment> overridden = tcp().route("vote.comment.{commentId}.{voteType}", comment.getCommentId(), voteType.opposite())
                        .metadata(oAuth2.addTokenToMetadata(token))
                        .retrieveMono(Void.class)
                .then(Mono.defer(() -> commentRepo.findById(comment.getCommentId())));

        StepVerifier
                .create(overridden)
                .expectNextMatches(it -> it.getScore() == expectedScore)
                .verifyComplete();
    }

    @ParameterizedTest
    @EnumSource(VoteType.class)
    public void voteComment_whenVoteFoundAndHasSameType_shouldDeleteVote(VoteType voteType) {

        Comment comment = commentRepo
                .save(Comment.of("post", null, "another-reddit-user", "body"))
                .block();

        Vote vote = voteRepo
                .save(Vote.of(comment.getCommentId(), "reddit-user", voteType))
                .block();

        String token = oAuth2.getAccessTokenForUsername("reddit-user", "password").block();

        Mono<Vote> voted = tcp().route("vote.comment.{commentId}.{voteType}", comment.getCommentId(), voteType)
                        .metadata(oAuth2.addTokenToMetadata(token))
                        .retrieveMono(Void.class)
                .then(Mono.defer(() -> voteRepo.findVoteByCommentIdAndUsername(comment.getCommentId(), "reddit-user")));

        StepVerifier
                .create(voted)
                .verifyComplete();
    }

    @ParameterizedTest
    @EnumSource(VoteType.class)
    public void voteComment_whenVoteFoundAndHasSameType_shouldRestorePreviousScore(VoteType voteType) {

        Comment comment = commentRepo
                .save(Comment.of("post", null, "another-reddit-user", "body"))
                .block();

        Long previousScore = comment.getScore();

        voteRepo
                .save(Vote.of(comment.getCommentId(), "reddit-user", voteType))
                .then(Mono.just(comment))
                .doOnNext(it -> it.setScore(voteType.equals(VoteType.UPVOTE) ? 1L : -1L))
                .flatMap(commentRepo::save)
                .block();

        String token = oAuth2.getAccessTokenForUsername("reddit-user", "password").block();

        Mono<Comment> restored = tcp().route("vote.comment.{commentId}.{voteType}", comment.getCommentId(), voteType)
                        .metadata(oAuth2.addTokenToMetadata(token))
                        .retrieveMono(Void.class)
                .then(Mono.defer(() -> commentRepo.findById(comment.getCommentId())));

        StepVerifier
                .create(restored)
                .expectNextMatches(it -> it.getScore() == previousScore)
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
