package com.redditclone.subredditservice.member;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepo;

    public Mono<Member> findById(String username) {
        return memberRepo.findById(username);
    }

    public Mono<Member> createMember(String username, String subredditName) {
        return memberRepo.insert(Member.of(username, subredditName));
    }

    public Mono<Member> addSubredditToMembersJoinedSubreddits(Member member, String subredditName) {
        return Mono.just(subredditName)
                .filter(member::isNotMemberOf)
                .switchIfEmpty(Mono.error(new AlreadyAMemberOfSubredditException(member.getUsername(), subredditName)))
                .map(member::joinSubreddit)
                .flatMap(memberRepo::save);
    }

    public Mono<Member> removeSubredditFromMembersJoinedSubreddits(Member member, String subredditName) {
        return Mono.just(subredditName)
                .filter(member::isMemberOf)
                .switchIfEmpty(Mono.error(new NotMemberOfSubredditException(member.getUsername(), subredditName)))
                .map(member::leaveSubreddit)
                .flatMap(memberRepo::save);
    }

    public Mono<Long> countMembersBySubredditName(String subredditName) {
        return memberRepo.countByJoinedSubredditsContains(subredditName);
    }
}
