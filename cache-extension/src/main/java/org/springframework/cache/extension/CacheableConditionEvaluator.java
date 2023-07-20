package org.springframework.cache.extension;

import java.lang.reflect.Method;

/**
 * CacheableConditionEvaluator bean instances can be associated with CacheItemDefinition's in order
 * to conditionally skip caching. The decision may be taken using current argument values or thread
 * context variables.
 */
public interface CacheableConditionEvaluator {

  /**
   * Evaluates if cacheable method should be cached or not based on the invocation context.
   * @param target the target object.
   * @param method the target method.
   * @param args the method arguments.
   * @return true if cacheable method should be cached, false otherwise.
   */
  boolean evaluate(Object target, Method method, Object... args);
}
