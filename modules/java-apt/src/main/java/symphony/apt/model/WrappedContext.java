package symphony.apt.model;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import java.util.function.Function;

public class WrappedContext {
  public TypeName typeName;
  public ClassName usedClassName;
  public Function<String, String> addSuffix;
  public ClassName extractorClassName;

  public WrappedContext(
      TypeName fieldTypeName,
      ClassName className,
      Function<String, String> suffix,
      ClassName extractorClassName) {
    this.typeName = fieldTypeName;
    this.usedClassName = className;
    this.addSuffix = suffix;
    this.extractorClassName = extractorClassName;
  }
}
