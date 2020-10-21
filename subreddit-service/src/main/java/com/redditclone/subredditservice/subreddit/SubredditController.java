package com.redditclone.subredditservice.subreddit;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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

    @MessageMapping("create.subreddit")
    public Mono<Subreddit> createSubreddit(@AuthenticationPrincipal Jwt jwt,
                                           CreateSubreddit create) {
        return subredditService.createSubreddit(jwt.getClaim("preferred_username"), create);
    }

    @MessageMapping("edit.subreddit.{subredditName}")
    public Mono<Subreddit> editSubreddit(@DestinationVariable String subredditName,
                                         @AuthenticationPrincipal Jwt jwt,
                                         EditSubreddit edit) {
        return subredditService.editSubreddit(subredditName, jwt.getClaim("preferred_username"), edit);
    }

    @MessageMapping("join.subreddit.{subredditName}")
    public Mono<Void> joinSubreddit(@DestinationVariable String subredditName,
                                    @AuthenticationPrincipal Jwt jwt) {
        return subredditService.joinSubreddit(subredditName, jwt.getClaim("preferred_username"));
    }

    @MessageMapping("leave.subreddit.{subredditName}")
    public Mono<Void> leaveSubreddit(@DestinationVariable String subredditName,
                                     @AuthenticationPrincipal Jwt jwt) {
        return subredditService.leaveSubreddit(subredditName, jwt.getClaim("preferred_username"));
    }
}
