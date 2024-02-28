package symphony.example.schema;

import symphony.apt.annotation.ArgExtractor;
import symphony.apt.annotation.EnumSchema;

@EnumSchema
@ArgExtractor
public enum Origin {
  EARTH,
  MARS,
  BELT
}
