package symphony.apt.model;

import java.util.ArrayList;
import java.util.List;

public class TypeInfo {
    private final String name;
    private final int depth;
    private List<TypeInfo> parameterizedTypes;

    public TypeInfo(String name, int depth) {
        this.name = name;
        this.depth = depth;
        this.parameterizedTypes = new ArrayList<>();
    }

    public void setParameterizedTypes(List<TypeInfo> parameterizedTypes) {
        this.parameterizedTypes = parameterizedTypes;
    }

    public void addParameterizedType(TypeInfo type) {
        parameterizedTypes.add(type);
    }

    public String getName() {
        return name;
    }

    public int getDepth() {
        return depth;
    }

    public List<TypeInfo> getParameterizedTypes() {
        return parameterizedTypes;
    }

    public static int calculateMaxDepth(TypeInfo type) {
        int maxDepth = type.getDepth();
        for (TypeInfo paramType : type.getParameterizedTypes()) {
            int childDepth = calculateMaxDepth(paramType);
            if (childDepth > maxDepth) {
                maxDepth = childDepth;
            }
        }
        return maxDepth;
    }

    @Override
    public String toString() {
        return "TypeInfo{" + "name='" + name + '\'' + ", depth=" + depth + ", parameterizedTypes=" + parameterizedTypes + '}';
    }
}
