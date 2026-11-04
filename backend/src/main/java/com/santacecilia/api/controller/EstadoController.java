package com.santacecilia.api.controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import com.santacecilia.api.dto.ApiResponse;
import java.util.Map;
@RestController @RequestMapping("/api/v1/health")
public class EstadoController {
    private final JdbcTemplate jdbc;
    public EstadoController(JdbcTemplate jdbc){this.jdbc=jdbc;}
    @GetMapping public ApiResponse<Map<String,String>> estado(){jdbc.queryForObject("SELECT 1",Integer.class);return new ApiResponse<>(Map.of("estado","DISPONIBLE"));}
}
