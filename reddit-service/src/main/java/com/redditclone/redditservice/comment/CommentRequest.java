package com.redditclone.redditservice.comment;

import com.redditclone.redditservice.CacheableRequest;
import com.redditclone.redditservice.SortBy;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@NoArgsConstructor
public class CommentRequest implements CacheableRequest {

    Integer page;
    Integer size;
    SortBy sort;

}
