package org.springframework.cache.extension;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

@Slf4j
public class CacheItemRepositoryImpl implements
    CacheItemRepository, ApplicationContextAware, InitializingBean {

  private final static Map<Class<?>, Class<?>> typesMap = new HashMap<>();
  static {
    typesMap.put(boolean.class, Boolean.class);
    typesMap.put(byte.class, Byte.class);
    typesMap.put(short.class, Short.class);
    typesMap.put(char.class, Character.class);
    typesMap.put(int.class, Integer.class);
    typesMap.put(long.class, Long.class);
    typesMap.put(float.class, Float.class);
    typesMap.put(double.class, Double.class);
  }

  private ApplicationContext applicationContext;

  private ConcurrentHashMap<CacheItemDefinitionKey, CacheItemDefinition> map = new ConcurrentHashMap<>();

  @Override
  public void register(@NonNull final CacheItemDefinition cacheItemDefinition) {
    log.info("Register cache item definition: {}", cacheItemDefinition);

    map.putIfAbsent(CacheItemDefinitionKey.of(cacheItemDefinition), cacheItemDefinition);
  }

  @Override
  public CacheItemDefinition findByContext(
      @NonNull final Object target, @NonNull final Method method, final Object... args) {
    Class[] argTypes = null;

    if (args != null) {
      argTypes = Arrays.stream(args)
          .map(Object::getClass)
          .toArray(Class[]::new);
    }

    final CacheItemDefinitionKey key = new CacheItemDefinitionKey(
        target.getClass(), method.getName(), argTypes);

    CacheItemDefinition cacheItemDefinition = map.get(key);

    if (cacheItemDefinition != null) {
      log.debug("Found exact match: {}", cacheItemDefinition);
      return cacheItemDefinition;
    }

    log.debug("No cache item definition found, looks for same signature with primitive types.");
    // Looks for same signature but primitive types
    for (CacheItemDefinition def : map.values()) {
      final Class<?>[] argumentTypes = def.getArgumentTypes();

      final boolean sameSignature = def.getType().equals(target.getClass())
          && def.getMethod().equals(method.getName())
          && argumentTypes != null && argTypes != null
          && argumentTypes.length == argTypes.length;

      if (sameSignature) {
        if (!matchPrimitiveTypes(argTypes, argumentTypes)) {
          return null;
        }
        log.debug("Found compatible match: {}", def);
        return def;
      }
    }

    return null;
  }

  private boolean matchPrimitiveTypes(final Class[] argTypes, final Class<?>[] argumentTypes) {
    for(int i = 0; i < argTypes.length; i++) {
      log.debug("comparing {}, {}", argTypes[i], argumentTypes[i]);
      if (argTypes[i].isPrimitive() && !typesMap.get(argTypes[i]).equals(argumentTypes[i])) {
        return false;
      }
      if (argumentTypes[i].isPrimitive() && !typesMap.get(argumentTypes[i]).equals(argTypes[i])) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    log.info("Application context injected.");
    this.applicationContext = applicationContext;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    map.values().forEach(this::validateCacheItemDefinition);
  }

  private void validateCacheItemDefinition(final CacheItemDefinition cacheItemDefinition) {
    log.info("validate cache item definition: {}", cacheItemDefinition);
    if (cacheItemDefinition.getCacheManager() != null) {
      applicationContext.getBean(cacheItemDefinition.getCacheManager(), CacheManager.class);
    }

    if (cacheItemDefinition.getKeyGenerator() != null) {
      applicationContext.getBean(cacheItemDefinition.getKeyGenerator(), KeyGenerator.class);
    }

    if (cacheItemDefinition.getCacheableConditionEvaluator() != null) {
      applicationContext.getBean(cacheItemDefinition.getCacheableConditionEvaluator(), CacheableConditionEvaluator.class);
    }
  }

  @Override
  public Iterator<CacheItemDefinition> iterator() {
    return new ArrayList<>(map.values()).iterator();
  }

  @RequiredArgsConstructor
  private static class CacheItemDefinitionKey {

    private final Class<?> type;

    private final String method;

    private final Class[] argumentTypes;

    public static CacheItemDefinitionKey of(CacheItemDefinition cacheItemDefinition) {
      return new CacheItemDefinitionKey(
          cacheItemDefinition.getType(), cacheItemDefinition.getMethod(),
          cacheItemDefinition.getArgumentTypes());
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      CacheItemDefinitionKey that = (CacheItemDefinitionKey) o;
      return type.equals(that.type) && method.equals(that.method) && Arrays.equals(
          argumentTypes, that.argumentTypes);
    }

    @Override
    public int hashCode() {
      int result = Objects.hash(type, method);
      result = 31 * result + Arrays.hashCode(argumentTypes);
      return result;
    }
  }
}
