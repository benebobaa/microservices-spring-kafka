package com.beneboba.product_service.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ObjectConverter {

    private final ObjectMapper objectMapper;

    public String convertObjectToString(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException("Error converting object to string", e);
        }
    }

    public <T> T convertStringToObject(String string, Class<T> clazz) {
        try {
            return objectMapper.readValue(string, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Error converting string to object", e);
        }
    }
}
