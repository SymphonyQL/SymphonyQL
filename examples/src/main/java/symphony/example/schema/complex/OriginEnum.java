package symphony.example.schema.complex;

import symphony.apt.annotation.ArgExtractor;
import symphony.apt.annotation.EnumSchema;

@EnumSchema
@ArgExtractor
enum OriginEnum {
  EARTH,
  MARS,
  BELT
}
