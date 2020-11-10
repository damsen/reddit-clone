package com.redditclone.postservice.post;

import com.redditclone.postservice.vote.VoteService;
import com.redditclone.postservice.vote.VoteType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepo;
    private final VoteService voteService;

    public Mono<Post> findPostById(String postId) {
        return postRepo
                .findById(postId)
                .switchIfEmpty(Mono.error(new PostNotFoundException(postId)));
    }

    public Flux<Post> findPostsBySubreddit(String subredditName, PostRequest request) {
        return postRepo.findBySubredditName(subredditName, request.toPageable());
    }

    public Flux<Post> findPostsByUsername(String username, PostRequest request) {
        return postRepo.findByAuthor(username, request.toPageable());
    }

    public Mono<Post> createPost(String username, CreatePost create) {
        return postRepo
                .insert(Post.of(create.getSubredditName(), username, create.getTitle(), create.getBody()))
                .flatMap(post -> voteService
                        .votePost(post.getPostId(), VoteType.UPVOTE, post.getAuthor())
                        .then(Mono.defer(() -> updatePostScore(post)))
                        .thenReturn(post));
    }

    private Mono<Post> updatePostScore(Post post) {
        return voteService
                .calculateScoreByPostId(post.getPostId())
                .map(post::updateScoreWith)
                .flatMap(postRepo::save);
    }

    public Mono<Post> editPost(String postId, String username, EditPost edit) {
        return findPostById(postId)
                .filter(post -> post.getAuthor().equals(username))
                .map(post -> post.editWith(edit))
                .flatMap(postRepo::save)
                .switchIfEmpty(Mono.error(new NotAuthorOfPostException(username, postId)));
    }

    public Mono<Void> deletePost(String postId, String username) {
        return findPostById(postId)
                .filter(post -> post.getAuthor().equals(username))
                .map(Post::delete)
                .flatMap(postRepo::save)
                .switchIfEmpty(Mono.error(new NotAuthorOfPostException(username, postId)))
                .then();
    }

    public Mono<Void> votePost(String postId, VoteType voteType, String username) {
        return findPostById(postId)
                .flatMap(post -> voteService
                        .votePost(post.getPostId(), voteType, username)
                        .then(Mono.defer(() -> updatePostScore(post))))
                .then();
    }

}
