package com.vynedam.stockai.controller; import java.time.Instant; import java.util.*; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1") public class HealthController {@GetMapping("/health") Map<String,Object>health(){return Map.of("status","ok","service","stockai-backend","timestamp",Instant.now().toString());}}
