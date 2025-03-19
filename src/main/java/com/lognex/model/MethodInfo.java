package com.lognex.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MethodInfo {
    private String name;
    private List<AnnotationInfo> annotations = new ArrayList<>();
}
