package com.redditclone.postservice.vote;

import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.aggregation.ArithmeticOperators.Subtract;
import org.springframework.data.mongodb.core.aggregation.ComparisonOperators.Eq;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators.Cond;
import reactor.core.publisher.Mono;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;
import static org.springframework.data.mongodb.core.query.Criteria.where;

@RequiredArgsConstructor
public class CustomVoteRepositoryImpl implements CustomVoteRepository {

    private final ReactiveMongoOperations mongoOps;

    @Override
    public Mono<Score> calculateScoreByPostId(String postId) {
        return mongoOps.aggregate(
                newAggregation(
                        match(where("postId").is(postId)),
                        group()
                                .sum(countVotesOfType(VoteType.UPVOTE)).as("upvotes")
                                .sum(countVotesOfType(VoteType.DOWNVOTE)).as("downvotes"),
                        project()
                                .and(Subtract.valueOf("upvotes").subtract("downvotes"))
                                .as("score")
                ),
                "post_votes",
                Score.class)
                .next()
                .defaultIfEmpty(Score.withScoreZero());
    }

    private Cond countVotesOfType(VoteType voteType) {
        return Cond
                .when(Eq.valueOf("voteType").equalToValue(voteType))
                .then(1)
                .otherwise(0);
    }
}
