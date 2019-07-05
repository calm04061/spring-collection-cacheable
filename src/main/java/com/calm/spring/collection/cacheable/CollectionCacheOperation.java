package com.calm.spring.collection.cacheable;

import org.springframework.cache.interceptor.CachePutOperation;

public class CollectionCacheOperation extends CachePutOperation{
	private final boolean isFindAll;

	public CollectionCacheOperation(Builder b) {
		super(b);
		this.isFindAll = b.isFindAll;
	}

	public boolean isFindAll() {
		return isFindAll;
	}

	public static class Builder extends CachePutOperation.Builder {

		private boolean isFindAll;

		public void setFindAll(boolean findAll) {
			isFindAll = findAll;
		}

		@Override
		protected StringBuilder getOperationDescription() {
			StringBuilder sb = super.getOperationDescription();
			sb.append(" | isFindAll ='");
			sb.append(this.isFindAll);
			sb.append("'");
			return sb;
		}

		@Override
		public CollectionCacheOperation build() {
			return new CollectionCacheOperation(this);
		}
	}
}
