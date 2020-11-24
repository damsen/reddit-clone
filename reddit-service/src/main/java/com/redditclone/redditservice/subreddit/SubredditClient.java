package com.redditclone.redditservice.subreddit;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface SubredditClient {

    Flux<Subreddit> findSubreddits(SubredditRequest request);

    Mono<Subreddit> findSubredditByName(String subredditName);

    Mono<Subreddit> createSubreddit(String username, CreateSubreddit create);

    Mono<Subreddit> editSubreddit(String subredditName, String username, EditSubreddit edit);

    Mono<Void> addSubredditMember(String subredditName, String username);

    Mono<Void> removeSubredditMember(String subredditName, String username);
}
