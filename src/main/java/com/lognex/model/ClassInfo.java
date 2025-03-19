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
public class ClassInfo {
    private String name;
    private List<AnnotationInfo> annotations = new ArrayList<>();
    private List<String> superClasses = new ArrayList<>();
    private List<MethodInfo> methods = new ArrayList<>();

    private boolean isInterface;
    private boolean isAbstract;

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        ClassInfo classInfo = (ClassInfo) object;
        return isInterface == classInfo.isInterface && isAbstract == classInfo.isAbstract && Objects.equals(name, classInfo.name) && Objects.equals(annotations, classInfo.annotations) && Objects.equals(superClasses, classInfo.superClasses) && Objects.equals(methods, classInfo.methods);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, annotations, superClasses, methods, isInterface, isAbstract);
    }
}
