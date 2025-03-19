package com.lognex.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnnotationInfo {
    String name;
    @EqualsAndHashCode.Exclude
    boolean ignoreParameters = false;
    Map<String, String> parameters = new HashMap<>();
}
