package com.calm.spring.collection.cacheable.test;

import java.util.Map;

public interface MyDbRepository {
    MyValue findById(MyId id);

    Map<MyId, MyValue> findAll();
}
