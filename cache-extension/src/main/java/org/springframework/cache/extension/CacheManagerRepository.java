package org.springframework.cache.extension;

import java.util.Collection;
import org.springframework.cache.CacheManager;

public interface CacheManagerRepository {

  void register(CacheManager bean, String beanName);

  Collection<CacheManager> findAllByCacheName(String cacheName);

  CacheManager findByName(String name);

}
