package com.lognex.config;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Setter
@Getter
public class AnnotationDefinition {
    private String name;
    private boolean ignoreParameters;
    private Map<String, String> parameters = new HashMap<>();
}
