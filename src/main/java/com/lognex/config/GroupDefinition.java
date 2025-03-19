package com.lognex.config;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class GroupDefinition {
    private String name;
    private AnnotationDefinition classAnnotation;
    private List<String> superClasses;
    private AnnotationDefinition methodAnnotation;
    private List<AnnotationDefinition> methodAnnotationAnyOf;

}
