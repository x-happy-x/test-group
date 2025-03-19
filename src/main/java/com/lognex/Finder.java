package com.lognex;

import com.lognex.model.AnnotationInfo;
import com.lognex.model.ClassInfo;
import com.lognex.model.MethodInfo;

import java.util.*;
import java.util.stream.Collectors;

public final class Finder {

    private Finder() {
    }

    static Set<ClassInfo> getBySuperClass(Set<ClassInfo> classes, String className) {
        Set<ClassInfo> result = classes.stream()
                .filter(ci -> ci.getSuperClasses().contains(className))
                .collect(Collectors.toSet());

        Queue<ClassInfo> queue = new LinkedList<>(result);
        Set<ClassInfo> visited = new HashSet<>(result);

        while (!queue.isEmpty()) {
            ClassInfo current = queue.poll();
            for (ClassInfo ci : classes) {
                if (!visited.contains(ci) && ci.getSuperClasses().contains(current.getName())) {
                    visited.add(ci);
                    result.add(ci);
                    queue.add(ci);
                }
            }
        }

        return result;
    }

    private static Set<ClassInfo> getAllSuperClasses(List<ClassInfo> classes, ClassInfo classInfo) {
        Map<String, ClassInfo> classMap = classes.stream()
                .collect(Collectors.toMap(ClassInfo::getName, ci -> ci, (a, b) -> a));

        Set<ClassInfo> result = new HashSet<>();
        collectAllSuperClasses(classMap, classInfo, result, new HashSet<>());
        return result;
    }

    private static void collectAllSuperClasses(Map<String, ClassInfo> classMap,
                                               ClassInfo current,
                                               Set<ClassInfo> result,
                                               Set<String> visited) {
        if (visited.contains(current.getName())) {
            return;
        }
        visited.add(current.getName());

        for (String superClassName : current.getSuperClasses()) {
            ClassInfo superInfo = classMap.get(superClassName);
            if (superInfo != null) {
                result.add(superInfo);
                collectAllSuperClasses(classMap, superInfo, result, visited);
            }
        }
    }

    private static boolean containsAnnotation(List<AnnotationInfo> annotations, AnnotationInfo find) {
        if (find.isIgnoreParameters()) {
            return annotations.stream()
                    .anyMatch(annotationInfo -> annotationInfo.getName().equals(find.getName()));
        }
        return annotations.contains(find);
    }

    public static Set<MethodInfo> getMethodsByAnnotation(List<ClassInfo> classes,
                                                         ClassInfo cls,
                                                         AnnotationInfo annotation) {
        Set<ClassInfo> allClasses = new HashSet<>(getAllSuperClasses(classes, cls));
        allClasses.add(cls);

        Set<MethodInfo> methods = new HashSet<>();
        for (ClassInfo ci : allClasses) {
            methods.addAll(
                    ci.getMethods().stream()
                            .filter(methodInfo -> containsAnnotation(methodInfo.getAnnotations(), annotation))
                            .collect(Collectors.toSet())
            );
        }

        return methods;
    }

    public static Set<ClassInfo> getClassesByAnnotation(Set<ClassInfo> classes, AnnotationInfo annotation) {
        Set<ClassInfo> annotatedClasses = new HashSet<>();
        for (ClassInfo ci : classes) {
            if (containsAnnotation(ci.getAnnotations(), annotation)) {
                annotatedClasses.add(ci);
                annotatedClasses.addAll(getBySuperClass(classes, ci.getName()));
            }
        }
        return annotatedClasses;
    }

}
