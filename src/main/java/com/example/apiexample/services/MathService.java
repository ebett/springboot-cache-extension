package com.example.apiexample.services;

import java.util.Arrays;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class MathService {

  @Cacheable
  public Integer sum(Integer a, Integer b) {
    return a + b;
  }

  @Cacheable
  public Integer multiply(Integer a, Integer b) {
    return a * b;
  }

  @Cacheable
  public Integer substract(Integer a, Integer b) {
    return a - b;
  }

  @Cacheable
  public List<Integer> getList(Integer a) {
    return Arrays.asList(2,3,4,5);
  }
}
