package symphony.example.schema;

import symphony.apt.annotation.ArgExtractor;
import symphony.apt.annotation.InputSchema;

import java.util.Optional;

@InputSchema
@ArgExtractor
public record FilterArgs(Optional<Origin> origin, Optional<NestedArg> nestedArg) {
}

