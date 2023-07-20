package org.springframework.cache.extension;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.concurrent.ConcurrentHashMap;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;

@Slf4j
public class CacheManagerRepositoryImpl implements CacheManagerRepository {

  private ConcurrentHashMap<String, CacheManager> cacheManagersMap = new ConcurrentHashMap<>();

  @Override
  public void register(@NonNull final CacheManager cacheManager, @NonNull  final String beanName) {
    log.info("register cache manager: {}", beanName);
    cacheManagersMap.putIfAbsent(beanName, cacheManager);
  }

  @Override
  public Collection<CacheManager> findAllByCacheName(@NonNull final String cacheName) {
    Collection<CacheManager> list = new LinkedHashSet<>();

    for (CacheManager cacheManager: cacheManagersMap.values()) {
      if (cacheManager.getCache(cacheName) != null) {
        list.add(cacheManager);
      }
    }

    log.debug("findAllByCacheName: {}, count : {}", cacheName, list.size());
    return list;
  }

  @Override
  public CacheManager findByName(@NonNull final String name) {
    return cacheManagersMap.get(name);
  }

}
