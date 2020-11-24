package com.redditclone.subredditservice.subreddit;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Controller
@RequiredArgsConstructor
public class SubredditController {

    private final SubredditService subredditService;

    @MessageMapping("find.subreddits")
    public Flux<Subreddit> findSubreddits(SubredditRequest request) {
        return subredditService.findSubreddits(request);
    }

    @MessageMapping("find.subreddit.{subredditName}")
    public Mono<Subreddit> findSubredditByName(@DestinationVariable String subredditName) {
        return subredditService.findSubredditByName(subredditName);
    }

    @MessageMapping("create.subreddit.by.{username}")
    public Mono<Subreddit> createSubreddit(@DestinationVariable String username,
                                           CreateSubreddit create) {
        return subredditService.createSubreddit(username, create);
    }

    @MessageMapping("edit.subreddit.{subredditName}.by.{username}")
    public Mono<Subreddit> editSubreddit(@DestinationVariable String subredditName,
                                         @DestinationVariable String username,
                                         EditSubreddit edit) {
        return subredditService.editSubreddit(subredditName, username, edit);
    }

    @MessageMapping("add.{username}.to.{subredditName}.members")
    public Mono<Void> addSubredditMember(@DestinationVariable String username,
                                         @DestinationVariable String subredditName) {
        return subredditService.addSubredditMember(username, subredditName);
    }

    @MessageMapping("remove.{username}.from.{subredditName}.members")
    public Mono<Void> removeSubredditMember(@DestinationVariable String username,
                                            @DestinationVariable String subredditName) {
        return subredditService.removeSubredditMember(username, subredditName);
    }
}
