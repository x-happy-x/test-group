package com.lognex;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.lognex.model.AnnotationInfo;
import com.lognex.model.ClassInfo;
import com.lognex.model.MethodInfo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Parser {
    private static final Logger log = Logger.getLogger("Parser.class");

    public static List<Path> loadTestFiles(File root) {
        try (Stream<Path> stream = Files.walk(root.toPath())) {
            return stream
//                    .filter(p -> p.toString().contains(File.separator + "test" + File.separator + "java"+File.separator))
                    .filter(p -> p.toString().endsWith(".java"))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    public List<ClassInfo> loadClasses(File root) {
        List<Path> testFiles = loadTestFiles(root);
        List<ClassInfo> classInfos = new ArrayList<>();
        int totalFiles = testFiles.size();
        for (int i = 0; i < totalFiles; i++) {
            Path file = testFiles.get(i);
            classInfos.addAll(loadClassesFromFile(file.toFile()));
            System.out.print("Обработан файл " + (i + 1) + " из " + totalFiles+"\r");
        }
        return classInfos;
    }

    private List<ClassInfo> loadClassesFromFile(File file) {
        List<ClassInfo> classInfos = new ArrayList<>();
        try {
            CompilationUnit cu = StaticJavaParser.parse(file);
            String packageName = cu.getPackageDeclaration().map(pd -> pd.getName().asString()).orElse("");

            Map<String, String> annotationImports = new HashMap<>();
            List<String> wildcardImports = new ArrayList<>();
            cu.getImports().forEach(imp -> {
                String importName = imp.getNameAsString();
                if (imp.isAsterisk()) {
                    wildcardImports.add(importName + ".");
                } else {
                    String simpleName = importName.substring(importName.lastIndexOf('.') + 1);
                    annotationImports.put(simpleName, importName);
                }
            });

            cu.accept(new VoidVisitorAdapter<Void>() {
                @Override
                public void visit(ClassOrInterfaceDeclaration cid, Void arg) {
                    String className = cid.getNameAsString();
                    String fullClassName = packageName.isEmpty() ? className : packageName + "." + className;
                    ClassInfo classInfo = new ClassInfo();

                    classInfo.setName(fullClassName);
                    classInfo.setInterface(cid.isInterface());
                    classInfo.setAbstract(cid.isAbstract());

                    fillAnnotations(
                            annotationName -> resolveAnnotationName(annotationName, file, packageName, annotationImports, wildcardImports),
                            cid.getAnnotations(),
                            classInfo.getAnnotations()
                    );

                    cid.getExtendedTypes().forEach(et ->
                            classInfo.getSuperClasses().add(resolveClassName(et.getNameAsString(), file, packageName, annotationImports, wildcardImports))
                    );

                    cid.getImplementedTypes().forEach(it ->
                            classInfo.getSuperClasses().add(resolveClassName(it.getNameAsString(), file, packageName, annotationImports, wildcardImports))
                    );

                    cid.getMethods().forEach(method -> {
                        MethodInfo methodInfo = new MethodInfo();
                        methodInfo.setName(fullClassName + "." + method.getNameAsString());

                        fillAnnotations(
                                annotationName -> resolveAnnotationName(annotationName, file, packageName, annotationImports, wildcardImports),
                                method.getAnnotations(),
                                methodInfo.getAnnotations()
                        );

                        classInfo.getMethods().add(methodInfo);
                    });

                    classInfos.add(classInfo);
                    super.visit(cid, arg);
                }
            }, null);
        } catch (Exception e) {
            log.warning("Ошибка при разборе файла: " + file.getAbsolutePath() + "\n" + e.getMessage());
        }
        return classInfos;
    }

    private String resolveAnnotationName(String annotationName, File file, String packageName,
                                         Map<String, String> annotationImports, List<String> wildcardImports) {
        try {
            Class.forName("java.lang." + annotationName);
            return "java.lang." + annotationName;
        } catch (ClassNotFoundException e) {
            if (new File(file.getParentFile().getAbsolutePath() + "/" + annotationName + ".java").exists()) {
                return annotationImports.getOrDefault(annotationName, packageName + "." + annotationName);
            }
            return annotationImports.getOrDefault(annotationName,
                    wildcardImports.stream()
                            .map(wildcard -> wildcard + annotationName)
                            .findFirst()
                            .orElse(annotationName));
        }
    }

    private String resolveClassName(String simpleName, File file, String packageName,
                                    Map<String, String> annotationImports, List<String> wildcardImports) {
        if (new File(file.getParentFile().getAbsolutePath() + "/" + simpleName + ".java").exists()) {
            return annotationImports.getOrDefault(simpleName, packageName + "." + simpleName);
        }
        return annotationImports.getOrDefault(simpleName,
                wildcardImports.stream()
                        .map(wildcard -> wildcard + simpleName)
                        .findFirst()
                        .orElse(packageName.isEmpty() ? simpleName : packageName + "." + simpleName));
    }

    private static void fillAnnotations(Function<String, String> getFullAnnotationName,
                                        NodeList<AnnotationExpr> annotations,
                                        List<AnnotationInfo> annotationsList) {
        annotations.forEach(a -> {
            AnnotationInfo annotationInfo = new AnnotationInfo();
            annotationInfo.setName(getFullAnnotationName.apply(a.getName().asString()));
            if (a instanceof SingleMemberAnnotationExpr annotationExpr) {
                annotationInfo.getParameters().put("value", annotationExpr.getMemberValue().toString());
            } else if (a instanceof NormalAnnotationExpr annotationExpr) {
                annotationExpr.getPairs().forEach(pair ->
                        annotationInfo.getParameters().put(pair.getNameAsString(), pair.getValue().toString()));
            }
            annotationsList.add(annotationInfo);
        });
    }
}
