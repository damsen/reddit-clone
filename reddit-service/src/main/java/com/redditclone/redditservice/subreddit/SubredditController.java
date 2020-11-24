package com.redditclone.redditservice.subreddit;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/subreddits")
public class SubredditController {

    private final SubredditClient subredditClient;

    @GetMapping
    public Flux<Subreddit> findSubreddits(SubredditRequest request) {
        return subredditClient.findSubreddits(request);
    }

    @GetMapping("/{subredditName}")
    public Mono<Subreddit> findSubredditByName(@PathVariable String subredditName) {
        return subredditClient.findSubredditByName(subredditName);
    }

    @PostMapping
    public Mono<Subreddit> createSubreddit(@AuthenticationPrincipal Jwt jwt,
                                           @RequestBody CreateSubreddit create) {
        return subredditClient.createSubreddit(jwt.getClaimAsString("preferred_username"), create);
    }

    @PutMapping("/{subredditName}")
    public Mono<Subreddit> editSubreddit(@PathVariable String subredditName,
                                         @AuthenticationPrincipal Jwt jwt,
                                         @RequestBody EditSubreddit edit) {
        return subredditClient.editSubreddit(subredditName, jwt.getClaimAsString("preferred_username"), edit);
    }

    @PutMapping("/{subredditName}/join")
    public Mono<Void> joinSubreddit(@PathVariable String subredditName,
                                    @AuthenticationPrincipal Jwt jwt) {
        return subredditClient.addSubredditMember(subredditName, jwt.getClaimAsString("preferred_username"));
    }

    @PutMapping("/{subredditName}/leave")
    public Mono<Void> leaveSubreddit(@PathVariable String subredditName,
                                     @AuthenticationPrincipal Jwt jwt) {
        return subredditClient.removeSubredditMember(subredditName, jwt.getClaimAsString("preferred_username"));
    }
}
