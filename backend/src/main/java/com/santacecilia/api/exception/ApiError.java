package com.santacecilia.api.exception;
import java.util.Map;
public record ApiError(String code,String message,Map<String,String> fieldErrors,String correlationId) {}
