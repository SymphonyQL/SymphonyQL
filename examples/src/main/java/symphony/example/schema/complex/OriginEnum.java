package symphony.example.schema.complex;

import symphony.annotations.java.GQLDeprecated;
import symphony.annotations.java.GQLDescription;
import symphony.annotations.java.GQLName;
import symphony.apt.annotation.ArgExtractor;
import symphony.apt.annotation.EnumSchema;

@EnumSchema
@ArgExtractor
@GQLName("GQLOriginEnum")
enum OriginEnum {
  @GQLDeprecated(reason = "deprecated")
  @GQLDescription("EARTH")
  EARTH,
  MARS,
  BELT
}
