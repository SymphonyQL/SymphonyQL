package symphony.apt.model;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import java.util.function.Function;

public class WrappedContext {

  public String fieldName;
  public TypeName fieldTypeName;
  public ClassName className;
  public Function<String, String> suffix;

  public WrappedContext(
      String fieldName,
      TypeName fieldTypeName,
      ClassName className,
      Function<String, String> suffix) {
    this.fieldName = fieldName;
    this.fieldTypeName = fieldTypeName;
    this.className = className;
    this.suffix = suffix;
  }
}
