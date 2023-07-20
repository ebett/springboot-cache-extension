package org.springframework.cache.extension;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@ConditionalOnProperty(prefix = "cache", name = "extension-enabled", matchIfMissing = true)
@Configuration
@EnableCaching
@ComponentScan("org.springframework.cache.extension")
@Slf4j
public class CacheConfig extends CachingConfigurerSupport implements BeanPostProcessor {

  @Autowired
  private ApplicationContext applicationContext;

  @ConditionalOnMissingBean(CacheItemRepository.class)
  @Bean
  public CacheItemRepository cacheItemRepository() {
    return new CacheItemRepositoryImpl();
  }

  @Bean
  public CacheManagerRepository cacheManagerRepository() {
    return new CacheManagerRepositoryImpl();
  }

  @ConditionalOnMissingBean(
      name = "keyGenerator",
      ignored = {SimpleKeyGenerator.class, DynamicKeyGenerator.class})
  @Bean
  @Override
  public KeyGenerator keyGenerator() {
    return new DynamicKeyGenerator(cacheItemRepository(), applicationContext);
  }

  @Bean
  @Override
  public CacheResolver cacheResolver() {
    return new DynamicCacheResolver(cacheItemRepository(), cacheManagerRepository());
  }

  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
    if (bean instanceof ApplicationContextAware
        && bean.getClass().getName().startsWith("org.springframework.cache.extension")) {
      ((ApplicationContextAware) bean).setApplicationContext(applicationContext);
    }
    return bean;
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    // Registers all instances of CacheManager found in app context.
    if (bean instanceof CacheManager) {
      cacheManagerRepository().register((CacheManager) bean, beanName);
    }

    return bean;
  }
}
