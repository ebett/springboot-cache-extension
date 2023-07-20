package org.springframework.cache.extension;

import java.lang.reflect.Method;

/**
 * CacheItemRepository contains Cacheable method definitions.
 */
public interface CacheItemRepository extends Iterable<CacheItemDefinition>{

  void register(CacheItemDefinition cacheItemDefinition);

  CacheItemDefinition findByContext(Object target, Method method, Object... args);

  static CacheItemRepository getDefault() {
    return new CacheItemRepositoryImpl();
  }
}
