package symphony.apt;

import java.util.List;
import symphony.apt.function.AddSuffix;

public final class Constant {

  private Constant() {
    throw new UnsupportedOperationException();
  }

  public static final String NOT_USED_CLASS = "org.apache.pekko.NotUsed";
  public static final String JAVA_FUNCTION_CLASS = "java.util.function.Function";
  public static final String JAVA_SUPPLIER_CLASS = "java.util.function.Supplier";
  public static final String JAVA_SOURCE_CLASS = "org.apache.pekko.stream.javadsl.Source";
  public static final String JAVA_OPTIONAL_CLASS = "java.util.Optional";
  public static final String JAVA_MAP_CLASS = "java.util.Map";

  public static final AddSuffix SCHEMA_SUFFIX = new AddSuffix("Schema");
  public static final AddSuffix INPUT_SCHEMA_SUFFIX = new AddSuffix("InputSchema");
  public static final AddSuffix EXTRACTOR_SUFFIX_FUNCTION = new AddSuffix("Extractor");
  public static final AddSuffix OPTIONAL_SUFFIX_FUNCTION = new AddSuffix("Optional");
  public static final AddSuffix STRING_SUFFIX_FUNCTION = new AddSuffix("StringValue");
  public static final AddSuffix EITHER_SUFFIX_FUNCTION = new AddSuffix("Either");

  public static final String SCHEMA_METHOD_NAME = "schema";
  public static final String EXTRACTOR_METHOD_NAME = "extractor";
  public static final String CREATE_OBJECT_FUNCTION = "function";

  public static final String CREATE_ENUM_ERROR_MSG = "Cannot build enum %s from input";
  public static final String CREATE_ERROR_MSG = "Cannot build %s from input";
  public static final String NOT_FOUND_ERROR_MSG = "%s was not found in input";

  public static final List<String> mapList = List.of(JAVA_MAP_CLASS);

  public static final List<String> functionList = List.of(Constant.JAVA_FUNCTION_CLASS);

  public static final List<String> supplierList = List.of(Constant.JAVA_SUPPLIER_CLASS);

  public static final List<String> collectionList =
      List.of(
          "java.util.Optional",
          "java.util.List",
          "java.util.Vector",
          "java.util.Set",
          "java.util.concurrent.CompletionStage");

  public static final List<String> scalarList =
      List.of("java.lang.String", "java.math.BigInteger", "java.math.BigDecimal");

  public static final List<String> primitiveTypes =
      List.of(
          "java.lang.Boolean",
          "java.lang.String",
          "java.lang.Integer",
          "java.lang.Long",
          "java.lang.Double",
          "java.lang.Float",
          "java.lang.Short",
          "java.math.BigInteger",
          "java.math.BigDecimal",
          "java.lang.Void",
          "boolean",
          "int",
          "long",
          "float",
          "double",
          "short",
          "void");
}
