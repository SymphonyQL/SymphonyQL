package symphony.example.schema;

import symphony.apt.annotation.ObjectSchema;

@ObjectSchema
public record CharacterOutput(String name, Origin origin) {
}
