package com.redditclone.commentservice.comment;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Optional;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CommentRequest {

    Integer page;
    Integer size;
    SortBy sort;

    @Getter
    public enum SortBy {
        NEW(Sort.by("commented").descending()),
        OLD(Sort.by("commented").ascending()),
        TOP(Sort.by("score").descending());

        Sort sort;

        SortBy(Sort sort) {
            this.sort = sort;
        }
    }

    public Pageable toPageable() {
        return PageRequest.of(
                Optional.ofNullable(page).orElse(0),
                Optional.ofNullable(size).orElse(10),
                Optional.ofNullable(sort).map(SortBy::getSort).orElse(Sort.unsorted())
        );
    }
}
