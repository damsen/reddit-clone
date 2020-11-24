package com.redditclone.redditservice.subreddit;

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
public class SubredditRequest implements CacheableRequest {

    Integer page = 0;
    Integer size = 10;
    SortBy sort = SortBy.NEW;

}
