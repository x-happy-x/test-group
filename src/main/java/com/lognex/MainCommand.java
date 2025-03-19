package com.lognex;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lognex.config.AnnotationDefinition;
import com.lognex.config.GroupDefinition;
import com.lognex.config.TestConfig;
import com.lognex.model.AnnotationInfo;
import com.lognex.model.ClassInfo;
import com.lognex.model.MethodInfo;
import com.lognex.model.result.EasyClass;
import com.lognex.model.result.GroupResult;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Slf4j
public final class MainCommand {
    private MainCommand() {
    }

    public static void main(String[] args) throws IOException {
        String projectPath = null;
        String parsedJsonPath = null;
        String configPath = null;
        String outputPath = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-p":
                case "--project":
                    if (i + 1 < args.length) {
                        projectPath = args[++i];
                    }
                    break;
                case "-g":
                case "--group":
                    if (i + 1 < args.length) {
                        parsedJsonPath = args[++i];
                    }
                    break;
                case "-c":
                case "--config":
                    if (i + 1 < args.length) {
                        configPath = args[++i];
                    }
                    break;
                case "-o":
                case "--output":
                    if (i + 1 < args.length) {
                        outputPath = args[++i];
                    }
                    break;
                default:
                    break;
            }
        }

        if (outputPath == null) {
            throw new RuntimeException("Не указан обязательный параметр -o (output path)");
        }


        ObjectMapper mapper = new ObjectMapper();

        // Если задан -g и -c, то запускаем режим группировки
        if (parsedJsonPath != null && configPath != null) {
            List<ClassInfo> classes;
            classes = Arrays.asList(mapper.readValue(new File(parsedJsonPath), ClassInfo[].class));
            Yaml yaml = new Yaml(new Constructor(TestConfig.class, new LoaderOptions()));
            TestConfig config;
            try (InputStream is = new FileInputStream(configPath)) {
                config = yaml.load(is);
            }
            if (config == null || config.getGroups() == null || config.getGroups().isEmpty()) {
                throw new RuntimeException("Файл конфигурации пуст или не содержит groups");
            }

            Set<ClassInfo> processedClasses = new HashSet<>();
            Map<String, Set<EasyClass>> groupResults = new LinkedHashMap<>();

            for (GroupDefinition groupDef : config.getGroups()) {
                log.info(groupDef.getName());
                Set<ClassInfo> matchingClasses = new HashSet<>(classes);
                matchingClasses.removeAll(processedClasses);

                if (groupDef.getClassAnnotation() != null) {
                    AnnotationInfo needed = toAnnotationInfo(groupDef.getClassAnnotation());
                    matchingClasses = Finder.getClassesByAnnotation(matchingClasses, needed);
                }

                if (groupDef.getSuperClasses() != null && !groupDef.getSuperClasses().isEmpty()) {
                    Set<ClassInfo> superFiltered = new HashSet<>();
                    for (String superClassName : groupDef.getSuperClasses()) {
                        superFiltered.addAll(Finder.getBySuperClass(matchingClasses, superClassName));
                    }
                    matchingClasses.retainAll(superFiltered);
                }

                Set<EasyClass> easyClasses = new HashSet<>();
                int i = 1;
                int all = matchingClasses.size();
                for (ClassInfo ci : matchingClasses) {
                    Set<MethodInfo> allMethods = new HashSet<>();
                    if (groupDef.getMethodAnnotation() != null) {
                        AnnotationInfo needed = toAnnotationInfo(groupDef.getMethodAnnotation());
                        allMethods.addAll(Finder.getMethodsByAnnotation(classes, ci, needed));
                    }
                    if (groupDef.getMethodAnnotationAnyOf() != null && !groupDef.getMethodAnnotationAnyOf().isEmpty()) {
                        for (AnnotationDefinition annDef : groupDef.getMethodAnnotationAnyOf()) {
                            AnnotationInfo needed = toAnnotationInfo(annDef);
                            allMethods.addAll(Finder.getMethodsByAnnotation(classes, ci, needed));
                        }
                    }
                    if (groupDef.getMethodAnnotation() == null &&
                            (groupDef.getMethodAnnotationAnyOf() == null || groupDef.getMethodAnnotationAnyOf().isEmpty())) {
                        allMethods.addAll(ci.getMethods());
                    }
                    if (!allMethods.isEmpty()) {
                        if (ci.isInterface() || ci.isAbstract()) {
                            continue;
                        }
                        easyClasses.add(new EasyClass(ci, allMethods));
                        processedClasses.add(ci);
                    }
                    log.info("Сгруппировано {} из {}\r", i++, all);
                    if (i > 3000) {
                        break;
                    }
                }

                groupResults.put(groupDef.getName(), easyClasses);
            }

            List<GroupResult> outputData = new ArrayList<>();
            for (Map.Entry<String, Set<EasyClass>> entry : groupResults.entrySet()) {
                GroupResult gr = new GroupResult();
                gr.setGroupName(entry.getKey());
                gr.setTests(new ArrayList<>());
                int totalTestCount = 0;
                for (EasyClass cwm : entry.getValue()) {
                    Map<String, Object> obj = new LinkedHashMap<>();
                    obj.put("class", cwm.getName());
                    obj.put("methods", cwm.getMethods().toArray());
                    int classMethodCount = cwm.getMethods().size();
                    obj.put("methodCount", classMethodCount);
                    gr.getTests().add(obj);
                    totalTestCount += classMethodCount;
                }
                gr.setTestCount(totalTestCount);
                outputData.add(gr);
            }

            mapper.writeValue(new File(outputPath), outputData);
        }
        else if (projectPath != null) {
            Parser parser = new Parser();
            List<ClassInfo> classes = parser.loadClasses(new File(projectPath));
            mapper.writeValue(new File(outputPath), classes);
        } else {
            throw new RuntimeException("Неверные аргументы. " +
                    "Для парсинга укажите: -p project/path -o parsed.json. " +
                    "Для группировки укажите: -g parsed.json -c config.yml -o output.json");
        }
    }

    private static AnnotationInfo toAnnotationInfo(AnnotationDefinition ad) {
        AnnotationInfo info = new AnnotationInfo();
        info.setName(ad.getName());
        info.setIgnoreParameters(ad.isIgnoreParameters());
        if (ad.getParameters() != null) {
            info.getParameters().putAll(ad.getParameters());
        }
        return info;
    }
}
