package org.springframework.cache.extension;

import java.lang.reflect.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.context.ApplicationContext;

/**
 * Finds the KeyGenerator defined in the CacheItemDefinition found by context.
 */
@Slf4j
@RequiredArgsConstructor
public class DynamicKeyGenerator implements KeyGenerator {

  private final CacheItemRepository cacheItemRepository;

  private final ApplicationContext applicationContext;

  @Override
  public final Object generate(Object target, Method method, Object... params) {
    //Finds Cache Item definition by context
    CacheItemDefinition cacheItemDefinition = cacheItemRepository.findByContext(target, method, params);

    if (cacheItemDefinition == null) {
      //Use default if none was provided.
      return new SimpleKeyGenerator().generate(target, method, params);
    }

    //Extracts cache key generator bean name.
    final String keyGenerator = cacheItemDefinition.getKeyGenerator();

    if (keyGenerator == null) {
      //Use default if none was provided.
      return new SimpleKeyGenerator().generate(target, method, params);
    }

    //Finds KeyGenerator bean.
    final KeyGenerator keyGeneratorBean = applicationContext.getBean(keyGenerator, KeyGenerator.class);

    //Generate key
    return keyGeneratorBean.generate(target, method, params);
  }

}
