package com.example.apiexample.cache;

import com.example.apiexample.services.MathService;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import javax.cache.Caching;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheEventListenerConfigurationBuilder;
import org.ehcache.config.builders.ConfigurationBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.event.EventType;
import org.ehcache.jsr107.EhcacheCachingProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.CacheManager;
import org.springframework.cache.extension.CacheItemDefinition;
import org.springframework.cache.extension.CacheItemRepository;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.cache.support.CompositeCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.util.StringUtils;

@Configuration
public class CacheConfig {

  public static final String MATH_CACHE = "mathCache";
  public static final String LIST_CACHE = "listCache";

  @Value("${spring.redis.host}")
  private String redisHost;

  @Value("${spring.redis.port}")
  private int redisPort;

  @Value("${spring.redis.password}")
  private String redisPassword;

  @Bean
  public KeyGenerator mathCacheKeyGenerator() {
    return (target, method, params) ->
        "MathCache::" + method.getName()
            + "[" + StringUtils.arrayToCommaDelimitedString(params) + "]";
  }

  @Bean
  public KeyGenerator listCacheKeyGenerator() {
    return (target, method, params) ->
        "ListCache::" + method.getName()
            + "[" + StringUtils.arrayToCommaDelimitedString(params) + "]";
  }

  @Bean
  public CacheItemRepository cacheItemRepository() {
    CacheItemRepository cacheItemRepository = CacheItemRepository.getDefault();

    CacheItemDefinition cid = CacheItemDefinition.builder()
        .type(MathService.class)
        .method("sum")
        .argumentTypes(new Class[]{Integer.class, Integer.class})
        .cacheManager("memoryCacheManager")
        .cacheNames(Collections.singleton(MATH_CACHE))
        .keyGenerator("mathCacheKeyGenerator")
        .build();

    cacheItemRepository.register(cid);

    CacheItemDefinition cid2 = CacheItemDefinition.builder()
        .type(MathService.class)
        .method("multiply")
        .argumentTypes(new Class[]{Integer.class, Integer.class})
        .cacheManager("redisCacheManager")
        .cacheNames(Collections.singleton(MATH_CACHE))
        .keyGenerator("mathCacheKeyGenerator")
        .build();

    cacheItemRepository.register(cid2);

    CacheItemDefinition cid3 = CacheItemDefinition.builder()
        .type(MathService.class)
        .method("substract")
        .argumentTypes(new Class[]{Integer.class, Integer.class})
        .cacheManager("compositeCacheManager")
        .cacheNames(Collections.singleton(MATH_CACHE))
        .keyGenerator("mathCacheKeyGenerator")
        .build();

    cacheItemRepository.register(cid3);

    CacheItemDefinition cid4 = CacheItemDefinition.builder()
        .type(MathService.class)
        .method("getList")
        .argumentTypes(new Class[]{Integer.class})
        .cacheManager("memoryCacheManager")
        .cacheNames(Collections.singleton(LIST_CACHE))
        .keyGenerator("listCacheKeyGenerator")
        .build();

    cacheItemRepository.register(cid4);

    return cacheItemRepository;
  }

  @Bean
  public CompositeCacheManager compositeCacheManager() {
    return new CompositeCacheManager(memoryCacheManager(), redisCacheManager());
  }

  @Bean
  public RedisCacheConfiguration cacheConfiguration() {
    return RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMinutes(60))
        .disableCachingNullValues()
        .serializeValuesWith(
            SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
  }

  @Bean
  public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
    return (builder) -> builder
        .withCacheConfiguration(MATH_CACHE,
            RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(5)));
  }

  @Primary
  @Bean
  public CacheManager redisCacheManager() {
    RedisCacheConfiguration cacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
        .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
            RedisSerializer.json()));

    RedisCacheManager.RedisCacheManagerBuilder builder = RedisCacheManager.builder(
            redisConnectionFactory())
        .cacheDefaults(cacheConfiguration);

    return builder.build();
  }

  @Bean
  public RedisConnectionFactory redisConnectionFactory() {
    RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
    redisStandaloneConfiguration.setHostName(redisHost);
    redisStandaloneConfiguration.setPort(redisPort);

    return new LettuceConnectionFactory(redisStandaloneConfiguration);
  }

  @Bean
  public CacheManager memoryCacheManager() {
    return new JCacheCacheManager(ehCacheManager());
  }

  @Bean
  public javax.cache.CacheManager ehCacheManager() {
    CacheEventListenerConfigurationBuilder eventListener = CacheEventListenerConfigurationBuilder
        .newEventListenerConfiguration(CustomCacheEventLogger.class, EventType.CREATED,
            EventType.REMOVED)
        .unordered()
        .asynchronous();

    CacheConfiguration<String, Integer> cacheConfig = CacheConfigurationBuilder
        .newCacheConfigurationBuilder(String.class, Integer.class, ResourcePoolsBuilder.heap(10))
        .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofSeconds(20)))
        .withService(eventListener)
        .build();

    CacheConfiguration<String, List> listConfig = CacheConfigurationBuilder
        .newCacheConfigurationBuilder(String.class, List.class, ResourcePoolsBuilder.heap(10))
        .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofSeconds(20)))
        .withService(eventListener)
        .build();

    org.ehcache.config.Configuration configuration = ConfigurationBuilder.newConfigurationBuilder()
        .withCache(MATH_CACHE, cacheConfig)
        .withCache(LIST_CACHE, listConfig)
        .build();

    EhcacheCachingProvider provider = (EhcacheCachingProvider) Caching.getCachingProvider(
        "org.ehcache.jsr107.EhcacheCachingProvider");

    javax.cache.CacheManager cacheManager = provider.getCacheManager(
        provider.getDefaultURI(), configuration);

    cacheManager.enableStatistics(MATH_CACHE, true);
    cacheManager.enableStatistics(LIST_CACHE, true);
    return cacheManager;
  }

}