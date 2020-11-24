package com.redditclone.subredditservice.subreddit;

import com.redditclone.subredditservice.member.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class SubredditService {

    private final SubredditRepository subredditRepo;
    private final MemberRepository memberRepo;

    public Flux<Subreddit> findSubreddits(SubredditRequest request) {
        return subredditRepo.findAllBy(request.toPageable());
    }

    public Mono<Subreddit> findSubredditByName(String subredditName) {
        return subredditRepo
                .findById(subredditName)
                .switchIfEmpty(Mono.error(new SubredditNotFoundException(subredditName)));
    }

    public Mono<Subreddit> createSubreddit(String username, CreateSubreddit create) {
        return subredditRepo
                .insert(Subreddit.from(username, create))
                .onErrorMap(
                        ex -> ex instanceof DuplicateKeyException,
                        ex -> new SubredditAlreadyPresentException(create.getName(), ex)
                )
                .flatMap(subreddit -> memberRepo
                        .findById(username)
                        .doOnNext(member -> member.joinSubreddit(create.getName()))
                        .switchIfEmpty(Mono.just(Member.of(username, create.getName())))
                        .flatMap(memberRepo::save)
                        .thenReturn(subreddit))
                .flatMap(this::updateSubredditMembersCount);
    }

    private Mono<Subreddit> updateSubredditMembersCount(Subreddit subreddit) {
        return memberRepo
                .countByJoinedSubredditsContains(subreddit.getName())
                .map(subreddit::updateMembersCountWith)
                .flatMap(subredditRepo::save);
    }

    public Mono<Subreddit> editSubreddit(String subredditName, String username, EditSubreddit edit) {
        return findSubredditByName(subredditName)
                .filter(subreddit -> subreddit.getCreator().equals(username))
                .map(subreddit -> subreddit.editWith(edit))
                .flatMap(subredditRepo::save)
                .switchIfEmpty(Mono.error(new NotCreatorOfSubredditException(username, subredditName)));
    }

    public Mono<Void> addSubredditMember(String username, String subredditName) {
        return memberRepo
                .findById(username)
                .flatMap(member -> findSubredditByName(subredditName)
                        .flatMap(subreddit -> Mono.just(subreddit)
                                .map(Subreddit::getName)
                                .filter(member::isNotMemberOf)
                                .switchIfEmpty(Mono.error(new AlreadyAMemberOfSubredditException(username, subredditName)))
                                .map(member::joinSubreddit)
                                .flatMap(memberRepo::save)
                                .then(Mono.defer(() -> updateSubredditMembersCount(subreddit)))))
                .switchIfEmpty(Mono.defer(() -> findSubredditByName(subredditName)
                        .flatMap(subreddit -> Mono.just(subreddit)
                                .map(Subreddit::getName)
                                .map(sub -> Member.of(username, sub))
                                .flatMap(memberRepo::insert)
                                .then(Mono.defer(() -> updateSubredditMembersCount(subreddit))))))
                .then();
    }

    public Mono<Void> removeSubredditMember(String username, String subredditName) {
        return memberRepo
                .findById(username)
                .switchIfEmpty(Mono.error(new MemberNotFoundException(username)))
                .filter(member -> member.isMemberOf(subredditName))
                .switchIfEmpty(Mono.error(new NotMemberOfSubredditException(username, subredditName)))
                .flatMap(member -> findSubredditByName(subredditName)
                        .flatMap(subreddit -> Mono.just(subreddit)
                                .map(Subreddit::getName)
                                .map(member::leaveSubreddit)
                                .flatMap(memberRepo::save)
                                .then(Mono.defer(() -> updateSubredditMembersCount(subreddit)))))
                .then();
    }

}
