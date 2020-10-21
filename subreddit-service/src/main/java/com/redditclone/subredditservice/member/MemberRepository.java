package com.redditclone.subredditservice.member;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface MemberRepository extends ReactiveMongoRepository<Member, String> {

    Mono<Long> countByJoinedSubredditsContains(String subredditName);
}
