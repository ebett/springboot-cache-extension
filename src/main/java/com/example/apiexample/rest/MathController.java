package com.example.apiexample.rest;

import com.example.apiexample.services.MathService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MathController {
  @Autowired
  private MathService mathService;

  @GetMapping("/sum")
  private ResponseEntity<Integer> sum(@RequestParam(defaultValue = "0") Integer a, @RequestParam(defaultValue = "0") Integer b) {
    return ResponseEntity.ok(mathService.sum(a, b));
  }

  @GetMapping("/multiply")
  private ResponseEntity<Integer> multiply(@RequestParam(defaultValue = "0") Integer a, @RequestParam(defaultValue = "0") Integer b) {
    return ResponseEntity.ok(mathService.multiply(a, b));
  }

  @GetMapping("/substract")
  private ResponseEntity<Integer> substract(@RequestParam(defaultValue = "0") Integer a, @RequestParam(defaultValue = "0") Integer b) {
    return ResponseEntity.ok(mathService.substract(a, b));
  }

  @GetMapping("/list")
  private ResponseEntity<List<Integer>> list(@RequestParam(defaultValue = "0") Integer a) {
    return ResponseEntity.ok(mathService.getList(a));
  }
}
