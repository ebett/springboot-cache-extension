# SpringBoot-cache-extension
SpringBoot cache extension for centralized configuration

# Features
- Support for centralized caching configuration.
- Support for multiple cache managers beans.
- Support for multiple cache key generators
- Support for Cacheable conditional evaluator instances.

### Client cache configuration
1. Define one or more cache managers.
```java
@Configuration
public class MyCacheConfig {
  @Bean
  public CacheManager memoryCacheManager() { ... }

  @Bean
  public CacheManager redisCacheManager() { ... }
}
 ```

2. Define KeyGenerator bean instances for different scenarios.
```java
@Configuration
public class MyCacheConfig {
  ...
  
  @Bean
  public KeyGenerator sumKeyGenerator() { ... }
  
  @Bean
  public KeyGenerator multiplyGenerator() { ... }
  
}
 ```

3. Create and register cache items
```java
@Configuration
public class MyCacheConfig {
  ...
  
  @Bean
  public CacheItemRepository cacheItemRepository() {
      CacheItemRepository repository =  CacheItemRepository.getDefault();
      
     CacheItemDefinition sumCacheItem = CacheItemDefinition.builder()
        .type(MathService.class)
        .method("sum")
        .argumentTypes(new Class[] {Integer.class, Integer.class})
        .cacheNames(Collections.singleton("mathCache"))
        .cacheManager("memoryCacheManager")
        .keyGenerator("sumKeyGenerator")
        .build();
    repository.register(sumCacheItem);

    CacheItemDefinition multiplyCacheItem = CacheItemDefinition.builder()
        .type(MathService.class)
        .method("multiply")
        .argumentTypes(new Class[] {Integer.class, Integer.class})
        .cacheNames(Collections.singleton("mathCache"))
        .cacheManager("memoryCacheManager")
        .keyGenerator("multiplyGenerator")
        .build();
      
      repository.register(multiplyCacheItem);
      ...
      return repository;
  }
}
 ```
### Define cacheable methods
Spring-boot cache provides @Cacheable annotation. **We can use it to only in beans classes and public methods.**
There is no need to configure any annotation attribute, all configuration is provided when registering cache items.

Example:
```java
@Component
public class MyService {
    
    @Cacheable
    public Integer getValue(String data) { ... }
}
 ```

# Add conditional caching evaluators
We can define beans that will skip caching in some conditions, for example thread context variables
or some argument values.
Examples:

```java
@Bean
public CacheableConditionEvaluator mdcSkipCacheableConditionEvaluator() {
  return (target, method, args) -> MDC.get("skipCacheable") != null;
}

@Bean
public CacheableConditionEvaluator nullCacheableConditionEvaluator() {
  return (target, method, args) -> args[0] != null;
}

@Bean
public CacheItemRepository cacheItemRepository() {
    CacheItemRepository repository =  CacheItemRepository.getDefault();

    CacheItemDefinition sumCacheItem = CacheItemDefinition.builder()
        .type(MathService.class)
        .method("sum")
        .argumentTypes(new Class[] {Integer.class, Integer.class})
        .cacheNames(Collections.singleton("mathCache"))
        .cacheManager("memoryCacheManager") //bean name
        .keyGenerator("sumKeyGenerator") //bean name
        .cacheableConditionEvaluator("nullCacheableConditionEvaluator") //bean name
        .build();
    repository.register(sumCacheItem);
    ...
    return repository;
}
```
 
# Disable cache extension configuration
cache.extension-enabled=false

# Donate
https://www.paypal.com/donate/?hosted_button_id=V2T3W6GKYTAYG