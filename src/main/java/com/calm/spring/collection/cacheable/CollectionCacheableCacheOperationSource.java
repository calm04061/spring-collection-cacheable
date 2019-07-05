package com.calm.spring.collection.cacheable;

import org.springframework.cache.annotation.AnnotationCacheOperationSource;

public class CollectionCacheableCacheOperationSource extends AnnotationCacheOperationSource {
    public CollectionCacheableCacheOperationSource() {
        super(new CollectionCacheableCacheAnnotationParser());
    }
}
