package com.hrms.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrms.dto.ApiResponse;
import com.hrms.dto.ResponseStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice(basePackages = "com.hrms.controller")
public class ApiResponseAdvice implements ResponseBodyAdvice<Object> {

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // Do not wrap if response is already an ApiResponse or if it's already an ApiResponse type
        return !returnType.getParameterType().equals(ApiResponse.class);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        
        // Handle String return types specifically to prevent ClassCastException in StringHttpMessageConverter
        if (body instanceof String) {
            try {
                ApiResponse<String> apiResponse = new ApiResponse<>(ResponseStatus.SUCCESS, (String) body, "Success");
                return objectMapper.writeValueAsString(apiResponse);
            } catch (JsonProcessingException e) {
                return "{\"status\":\"EXCEPTION\",\"response\":null,\"message\":\"JSON serialization error\"}";
            }
        }

        if (body instanceof ApiResponse) {
            return body;
        }

        return new ApiResponse<>(ResponseStatus.SUCCESS, body, "Success");
    }
}
