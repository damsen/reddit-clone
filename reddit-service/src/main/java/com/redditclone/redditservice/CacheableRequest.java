package com.redditclone.redditservice;

public interface CacheableRequest {

    Integer getPage();

    Integer getSize();

    SortBy getSort();

    default String toCacheKey() {
        return String.format("%d_%d_%s", getPage(), getSize(), getSort());
    }
}
