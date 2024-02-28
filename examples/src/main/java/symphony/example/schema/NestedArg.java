package symphony.example.schema;

import symphony.apt.annotation.ArgExtractor;
import symphony.apt.annotation.InputSchema;

import java.util.Optional;

@InputSchema
@ArgExtractor
public record NestedArg(String id, Optional<String> name) {
}