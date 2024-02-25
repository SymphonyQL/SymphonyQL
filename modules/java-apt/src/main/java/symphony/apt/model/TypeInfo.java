package symphony.apt.model;

import java.util.ArrayList;
import java.util.List;

public class TypeInfo {
  private final String name;
  private List<TypeInfo> parameterizedTypes;

  public TypeInfo(String name) {
    this.name = name;
    this.parameterizedTypes = new ArrayList<>();
  }

  public void setParameterizedTypes(List<TypeInfo> parameterizedTypes) {
    this.parameterizedTypes = parameterizedTypes;
  }

  public String getName() {
    return name;
  }

  public List<TypeInfo> getParameterizedTypes() {
    return parameterizedTypes;
  }

  @Override
  public String toString() {
    return "TypeInfo{"
        + "name='"
        + name
        + '\''
        + ", parameterizedTypes="
        + parameterizedTypes
        + '}';
  }
}
