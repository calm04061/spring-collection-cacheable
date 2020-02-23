/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.calm.spring.collection.cacheable;

import com.calm.spring.collection.cacheable.annotation.CollectionCacheEvict;
import com.calm.spring.collection.cacheable.annotation.CollectionCachePut;
import com.calm.spring.collection.cacheable.annotation.CollectionCacheable;
import org.springframework.cache.annotation.CacheAnnotationParser;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Caching;
import org.springframework.cache.interceptor.CacheOperation;
import org.springframework.cache.interceptor.CachePutOperation;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Strategy implementation for parsing Spring's {@link Caching}, {@link CollectionCacheable},
 * {@link CollectionCacheEvict}, and {@link CollectionCachePut} annotations.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 3.1
 */
@SuppressWarnings("serial")
public class CollectionCacheableCacheAnnotationParser implements CacheAnnotationParser, Serializable {
	private static final Set<Class<? extends Annotation>> CACHE_OPERATION_ANNOTATIONS = new LinkedHashSet<>(8);

	static {
		CACHE_OPERATION_ANNOTATIONS.add(CollectionCacheable.class);
		CACHE_OPERATION_ANNOTATIONS.add(CollectionCachePut.class);
		CACHE_OPERATION_ANNOTATIONS.add(CollectionCacheEvict.class);
//        CACHE_OPERATION_ANNOTATIONS.add(Caching.class);
	}

	@Override
	public Collection<CacheOperation> parseCacheAnnotations(Class<?> type) {
		// @CollectionCacheable only makes sense on methods
		return Collections.emptyList();
	}

	@Override
	public Collection<CacheOperation> parseCacheAnnotations(Method method) {
		DefaultCacheConfig defaultConfig = new DefaultCacheConfig(method.getDeclaringClass());
		return parseCacheAnnotations(defaultConfig, method);
	}

	private Collection<CacheOperation> parseCacheAnnotations(DefaultCacheConfig cachingConfig, Method method) {
		Collection<CacheOperation> ops = parseCacheAnnotations(cachingConfig, method, false);
		if (ops.size() > 1) {
			// More than one operation found -> local declarations override interface-declared ones...
			return parseCacheAnnotations(cachingConfig, method, true);
		}
		return ops;
	}

	private Collection<CacheOperation> parseCacheAnnotations(
			DefaultCacheConfig cachingConfig, Method method, boolean localOnly) {

		Collection<? extends Annotation> anns = (localOnly ?
				AnnotatedElementUtils.getAllMergedAnnotations(method, CACHE_OPERATION_ANNOTATIONS) :
				AnnotatedElementUtils.findAllMergedAnnotations(method, CACHE_OPERATION_ANNOTATIONS));
		if (anns.isEmpty()) {
			return Collections.emptyList();
		}

		final Collection<CacheOperation> ops = new ArrayList<>(1);
		anns.stream().filter(ann -> ann instanceof CollectionCacheable).forEach(
				ann -> ops.add(parseCollectionCacheableAnnotation(method, cachingConfig, (CollectionCacheable) ann)));
		anns.stream().filter(ann -> ann instanceof CollectionCachePut).forEach(
				ann -> ops.add(parseCollectionCachePutAnnotation(method, cachingConfig, (CollectionCachePut) ann)));
		anns.stream().filter(ann -> ann instanceof CollectionCacheEvict).forEach(
				ann -> ops.add(parseCollectionCacheEvictAnnotation(method, cachingConfig, (CollectionCacheEvict) ann)));
		return ops;
	}

	private CollectionCacheOperation parseCollectionCacheableAnnotation(
			Method method, DefaultCacheConfig defaultConfig, CollectionCacheable collectionCacheable) {

		boolean isFindAll = checkFindAll(method);
		validateMethodSignature(isFindAll, method);

		CollectionCacheOperation.Builder builder = new CollectionCacheOperation.Builder();

		builder.setName(method.toString());
		builder.setCacheNames(collectionCacheable.cacheNames());
		builder.setCondition(collectionCacheable.condition());
		builder.setKey(collectionCacheable.key());
		builder.setKeyGenerator(collectionCacheable.keyGenerator());
		builder.setCacheManager(collectionCacheable.cacheManager());
		builder.setCacheResolver(collectionCacheable.cacheResolver());
		builder.setUnless(collectionCacheable.unless());
		builder.setFindAll(isFindAll);

		defaultConfig.applyDefault(builder);
		CollectionCacheOperation op = builder.build();
		validateCollectionCacheOperation(method, op);

		return op;
	}

	private CollectionCacheOperation parseCollectionCachePutAnnotation(
			Method method, DefaultCacheConfig defaultConfig, CollectionCachePut collectionCachePut) {

		boolean isFindAll = checkFindAll(method);
		validatePutMethodSignature(isFindAll, method);

		CollectionCacheOperation.Builder builder = new CollectionCacheOperation.Builder();

		builder.setName(method.toString());
		builder.setCacheNames(collectionCachePut.cacheNames());
		builder.setCondition(collectionCachePut.condition());
		builder.setKey(collectionCachePut.key());
		builder.setKeyGenerator(collectionCachePut.keyGenerator());
		builder.setCacheManager(collectionCachePut.cacheManager());
		builder.setCacheResolver(collectionCachePut.cacheResolver());
		builder.setUnless(collectionCachePut.unless());
		builder.setFindAll(isFindAll);

		defaultConfig.applyDefault(builder);
		CollectionCacheOperation op = builder.build();
		validateCollectionCacheOperation(method, op);

		return op;
	}

	private CollectionCacheEvictOperation parseCollectionCacheEvictAnnotation(
			Method method, DefaultCacheConfig defaultConfig, CollectionCacheEvict collectionCacheable) {

		CollectionCacheEvictOperation.Builder builder = new CollectionCacheEvictOperation.Builder();

		builder.setName(method.toString());
		builder.setCacheNames(collectionCacheable.cacheNames());
		builder.setCondition(collectionCacheable.condition());
		builder.setKey(collectionCacheable.key());
		builder.setKeyGenerator(collectionCacheable.keyGenerator());
		builder.setCacheManager(collectionCacheable.cacheManager());
		builder.setCacheResolver(collectionCacheable.cacheResolver());

		defaultConfig.applyDefault(builder);
		CollectionCacheEvictOperation op = builder.build();
		validateCollectionCacheEvictOperation(method, op);

		return op;
	}

	private void validatePutMethodSignature(boolean isFindAll, Method method) {
		if (!method.getReturnType().isAssignableFrom(List.class)) {
			throw new IllegalStateException("Invalid CollectionCachePut annotation configuration on '" +
					method.toString() + "'. Method return type is not assignable from Collection.");
		}
	}

	private boolean checkFindAll(Method method) {
		return method.getParameterTypes().length == 0;
	}

	private void validateMethodSignature(boolean isFindAll, Method method) {
		if (!method.getReturnType().isAssignableFrom(Map.class)) {
			throw new IllegalStateException("Invalid CollectionCacheable annotation configuration on '" +
					method.toString() + "'. Method return type is not assignable from Map.");
		}
		if (isFindAll) {
			return;
		}
		Class<?>[] parameterTypes = method.getParameterTypes();
		if (parameterTypes.length != 1 || !parameterTypes[0].equals(Collection.class)) {
			throw new IllegalStateException("Invalid CollectionCacheable annotation configuration on '" +
					method.toString() + "'. Did not find zero or one Collection argument.");
		}
		Type[] genericParameterTypes = method.getGenericParameterTypes();
		if (genericParameterTypes.length != 1 || !(genericParameterTypes[0] instanceof ParameterizedType)) {
			// assume method is not generic
			return;
		}
		if (!(method.getGenericReturnType() instanceof ParameterizedType)) {
			// assume method is not generic
			return;
		}
		ParameterizedType parameterizedCollection = (ParameterizedType) genericParameterTypes[0];
		if (parameterizedCollection.getActualTypeArguments().length != 1) {
			throw new IllegalStateException("Invalid CollectionCacheable annotation configuration on '" +
					method.toString() + "'. Parameterized collection does not have exactly one type argument.");
		}
		ParameterizedType parameterizedMap = (ParameterizedType) method.getGenericReturnType();
		if (parameterizedMap.getActualTypeArguments().length != 2) {
			throw new IllegalStateException("Invalid CollectionCacheable annotation configuration on '" +
					method.toString() + "'. Parameterized map does not have exactly two type arguments.");
		}
		if (!parameterizedMap.getActualTypeArguments()[0].equals(parameterizedCollection.getActualTypeArguments()[0])) {
			throw new IllegalStateException("Invalid CollectionCacheable annotation configuration on '" +
					method.toString() + "'. The Map key type should be equal to the collection type.");
		}
	}

	private void validateCollectionCacheOperation(AnnotatedElement ae, CollectionCacheOperation operation) {
		if (StringUtils.hasText(operation.getCacheManager()) && StringUtils.hasText(operation.getCacheResolver())) {
			throw new IllegalStateException("Invalid cache annotation configuration on '" +
					ae.toString() + "'. Both 'cacheManager' and 'cacheResolver' attributes have been set. " +
					"These attributes are mutually exclusive: the cache manager is used to configure a" +
					"default cache resolver if none is set. If a cache resolver is set, the cache manager" +
					"won't be used.");
		}
		if (operation.isFindAll() && StringUtils.hasText(operation.getCondition())) {
			throw new IllegalStateException("Invalid cache annotation configuration on '" +
					ae.toString() + "'. Cannot use 'condition' on 'findAll'-like methods.");
		}
	}
	private void validateCollectionCacheEvictOperation(AnnotatedElement ae, CollectionCacheEvictOperation operation) {
		if (StringUtils.hasText(operation.getCacheManager()) && StringUtils.hasText(operation.getCacheResolver())) {
			throw new IllegalStateException("Invalid cache annotation configuration on '" +
					ae.toString() + "'. Both 'cacheManager' and 'cacheResolver' attributes have been set. " +
					"These attributes are mutually exclusive: the cache manager is used to configure a" +
					"default cache resolver if none is set. If a cache resolver is set, the cache manager" +
					"won't be used.");
		}
	}


	@Override
	public boolean equals(Object other) {
		return (this == other || other instanceof CollectionCacheableCacheAnnotationParser);
	}

	@Override
	public int hashCode() {
		return CollectionCacheableCacheAnnotationParser.class.hashCode();
	}


	/**
	 * Provides default settings for a given set of cache operations.
	 */
	private static class DefaultCacheConfig {

		private final Class<?> target;

		@Nullable
		private String[] cacheNames;

		@Nullable
		private String keyGenerator;

		@Nullable
		private String cacheManager;

		@Nullable
		private String cacheResolver;

		private boolean initialized = false;

		public DefaultCacheConfig(Class<?> target) {
			this.target = target;
		}

		/**
		 * Apply the defaults to the specified {@link CacheOperation.Builder}.
		 *
		 * @param builder the operation builder to update
		 */
		public void applyDefault(CacheOperation.Builder builder) {
			if (!this.initialized) {
				CacheConfig annotation = AnnotatedElementUtils.findMergedAnnotation(this.target, CacheConfig.class);
				if (annotation != null) {
					this.cacheNames = annotation.cacheNames();
					this.keyGenerator = annotation.keyGenerator();
					this.cacheManager = annotation.cacheManager();
					this.cacheResolver = annotation.cacheResolver();
				}
				this.initialized = true;
			}

			if (builder.getCacheNames().isEmpty() && this.cacheNames != null) {
				builder.setCacheNames(this.cacheNames);
			}
			if (!StringUtils.hasText(builder.getKey()) && !StringUtils.hasText(builder.getKeyGenerator()) &&
					StringUtils.hasText(this.keyGenerator)) {
				builder.setKeyGenerator(this.keyGenerator);
			}

			if (StringUtils.hasText(builder.getCacheManager()) || StringUtils.hasText(builder.getCacheResolver())) {
				// One of these is set so we should not inherit anything
			} else if (StringUtils.hasText(this.cacheResolver)) {
				builder.setCacheResolver(this.cacheResolver);
			} else if (StringUtils.hasText(this.cacheManager)) {
				builder.setCacheManager(this.cacheManager);
			}
		}
	}

}
