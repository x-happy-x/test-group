package com.lognex.model.result;

import com.lognex.model.ClassInfo;
import com.lognex.model.MethodInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EasyClass {
    private ClassInfo classInfo;
    private String name;
    private Set<String> methods;

    public EasyClass(ClassInfo classInfo, Collection<MethodInfo> methods) {
        this.setName(classInfo.getName());
        this.setMethods(methods.stream()
                .map(m -> m.getName().substring(m.getName().lastIndexOf(".") + 1))
                .collect(Collectors.toSet()));
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        EasyClass that = (EasyClass) object;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
