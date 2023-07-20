package org.springframework.cache.extension;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.support.NoOpCache;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.CollectionUtils;

/**
 * Resolves which caches should be used based on invocation context.
 * The sprint-boot cache interceptor will provide the call context: type, method, argumentTypes.
 * DynamicCacheResolver will try to match any cache item definition with that context.
 */
@Slf4j
@RequiredArgsConstructor
public class DynamicCacheResolver implements CacheResolver, ApplicationContextAware {

  private final CacheItemRepository cacheItemRepository;

  private final CacheManagerRepository cacheManagerRepository;

  private ApplicationContext applicationContext;

  @Override
  public Collection<? extends Cache> resolveCaches(CacheOperationInvocationContext<?> context) {
    CacheItemDefinition cacheItemDefinition = cacheItemRepository.findByContext(
        context.getTarget(), context.getMethod(), context.getArgs());

    List<Cache> cacheList = new ArrayList<>();

    if (cacheItemDefinition == null) {
      log.warn("No cache item definition found in class {}, method {}.",
          context.getTarget().getClass(), context.getMethod().getName());

      findCachesInCacheableAnnotation(context.getMethod(), cacheList);

      return cacheList;
    }

    List<NoOpCache> noOpCaches = getNoOpCachesWhenNoConditionMatch(context, cacheItemDefinition);
    if (noOpCaches != null) {
      return noOpCaches;
    }

    Set<String> cacheNames = cacheItemDefinition.getCacheNames();
    String cacheManagerName = cacheItemDefinition.getCacheManager();

    findCachesInCacheManager(cacheManagerName, cacheNames, cacheList);

    if (cacheList.isEmpty()) {
      log.debug("No cache manager defined for this method.");
      findCachesInCacheList(cacheNames, cacheList);
    }

    log.debug("Cache list size: {}", cacheList.size());
    return cacheList;
  }

  private List<NoOpCache> getNoOpCachesWhenNoConditionMatch(
      final CacheOperationInvocationContext<?> context, final CacheItemDefinition cacheItemDefinition) {
    String cacheableConditionEvaluatorBeanName = cacheItemDefinition.getCacheableConditionEvaluator();
    if (cacheableConditionEvaluatorBeanName != null) {
      CacheableConditionEvaluator cacheableConditionEvaluator = applicationContext.getBean(
          cacheableConditionEvaluatorBeanName, CacheableConditionEvaluator.class);

      boolean matchCondition = cacheableConditionEvaluator.evaluate(
          context.getTarget(), context.getMethod(), context.getArgs());

      if (!matchCondition) {
        log.warn("Condition no matching cacheable method: {}-{}",
            context.getTarget().getClass(), context.getMethod());

        return cacheItemDefinition.getCacheNames().stream()
            .map(NoOpCache::new)
            .collect(Collectors.toList());
      }
    }
    return null;
  }

  private void findCachesInCacheableAnnotation(final Method method, final List<Cache> cacheList) {
    log.debug("Fallback: find cache names from Cacheable annotation...");

    Optional.ofNullable(method.getAnnotation(Cacheable.class))
        .map(Cacheable::cacheNames)
        .map(cacheNames -> new LinkedHashSet(Arrays.asList(cacheNames)))
        .ifPresent(cacheNames -> findCachesInCacheList(cacheNames, cacheList));
  }

  private void findCachesInCacheManager(
      final String cacheManagerName, final Set<String> cacheNames, final List<Cache> result) {
    if (cacheManagerName != null) {
      log.debug("Find cache names in cache manager: {}", cacheManagerName);
      CacheManager cacheManager = cacheManagerRepository.findByName(cacheManagerName);
      if (cacheManager != null) {
        for (String cacheName : cacheNames) {
          Optional.ofNullable(cacheManager.getCache(cacheName))
              .ifPresent(result::add);
        }
      }
    }
  }

  private void findCachesInCacheList(final Set<String> cacheNames, final List<Cache> result) {
    log.debug("Cache names: {}", String.join(",", cacheNames));
    for (String cacheName : cacheNames) {
      Collection<CacheManager> cacheManagers = cacheManagerRepository.findAllByCacheName(cacheName);
      if (!CollectionUtils.isEmpty(cacheManagers)) {
        for (CacheManager cacheManager : cacheManagers) {
          Optional.ofNullable(cacheManager.getCache(cacheName))
              .ifPresent(result::add);
        }
      }
    }
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    log.info("Application context injected.");
    this.applicationContext = applicationContext;
  }
}
