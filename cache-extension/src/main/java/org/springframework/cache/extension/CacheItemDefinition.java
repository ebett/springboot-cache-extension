package org.springframework.cache.extension;

import java.util.Arrays;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

/**
 * A cache item definition contains the definition of a @Cacheable method.
 */
@Getter
@Builder
public class CacheItemDefinition {

  /**
   * The Cacheable type.
   */
  @NonNull
  private final Class<?> type;

  /**
   * The Cacheable method.
   */
  @NonNull
  private final String method;

  /**
   * The list of argument types.
   */
  private final Class<?>[] argumentTypes;

  /**
   * The set of cache names.
   */
  @NonNull
  private final Set<String> cacheNames;

  /**
   * The CacheManager bean name.
   */
  private final String cacheManager;

  /**
   * The KeyGenerator bean name.
   */
  private String keyGenerator;

  /**
   * The Cacheable condition evaluator bean name.
   */
  private String cacheableConditionEvaluator;

  @Override
  public String toString() {
    return "CacheItemDefinition{" +
        "type=" + type +
        ", method='" + method + '\'' +
        ", parameterTypes=" + Arrays.toString(argumentTypes) +
        ", cacheNames=" + String.join(",", cacheNames) +
        ", cacheManager='" + cacheManager + '\'' +
        ", keyGenerator='" + keyGenerator + '\'' +
        ", cacheableConditionEvaluator='" + cacheableConditionEvaluator + '\'' +
        '}';
  }
}
