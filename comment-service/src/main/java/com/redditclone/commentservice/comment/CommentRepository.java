package com.redditclone.commentservice.comment;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface CommentRepository extends ReactiveMongoRepository<Comment, String> {

    Flux<Comment> findByPostIdAndParentIdNull(String postId, Pageable pageable);

    Flux<Comment> findByParentId(String parentId, Sort sort);

    Flux<Comment> findByAuthor(String author, Pageable pageable);
}
