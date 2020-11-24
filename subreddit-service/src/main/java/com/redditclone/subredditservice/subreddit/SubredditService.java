package com.redditclone.subredditservice.subreddit;

import com.redditclone.subredditservice.member.MemberNotFoundException;
import com.redditclone.subredditservice.member.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class SubredditService {

    private final SubredditRepository subredditRepo;
    private final MemberService memberService;

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
                        ex -> new SubredditAlreadyPresentException(create.getName(), ex))
                .flatMap(subreddit -> memberService
                        .findById(username)
                        .flatMap(member -> memberService.addSubredditToMembersJoinedSubreddits(member, subreddit.getName()))
                        .switchIfEmpty(Mono.defer(() -> memberService.createMember(username, subreddit.getName())))
                        .thenReturn(subreddit))
                .flatMap(this::updateSubredditMembersCount);
    }

    private Mono<Subreddit> updateSubredditMembersCount(Subreddit subreddit) {
        return memberService
                .countMembersBySubredditName(subreddit.getName())
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
        return memberService
                .findById(username)
                .flatMap(member -> findSubredditByName(subredditName)
                        .flatMap(subreddit -> memberService
                                .addSubredditToMembersJoinedSubreddits(member, subreddit.getName())
                                .thenReturn(subreddit)))
                .switchIfEmpty(Mono.defer(() -> findSubredditByName(subredditName)
                        .flatMap(subreddit -> memberService
                                .createMember(username, subreddit.getName())
                                .thenReturn(subreddit))))
                .flatMap(this::updateSubredditMembersCount)
                .then();
    }

    public Mono<Void> removeSubredditMember(String username, String subredditName) {
        return memberService
                .findById(username)
                .switchIfEmpty(Mono.error(new MemberNotFoundException(username)))
                .flatMap(member -> findSubredditByName(subredditName)
                        .flatMap(subreddit -> memberService
                                .removeSubredditFromMembersJoinedSubreddits(member, subreddit.getName())
                                .thenReturn(subreddit)))
                .flatMap(this::updateSubredditMembersCount)
                .then();
    }

}
