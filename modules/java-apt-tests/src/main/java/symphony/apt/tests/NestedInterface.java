package symphony.apt.tests;

import symphony.apt.annotation.InterfaceSchema;
import symphony.apt.annotation.ObjectSchema;

@InterfaceSchema
public sealed interface NestedInterface {
}


@InterfaceSchema
sealed interface Mid1 extends NestedInterface {
}

@InterfaceSchema
sealed interface Mid2 extends NestedInterface {
}

@ObjectSchema
record FooA(String a, String b, String c) implements Mid1 {
}

@ObjectSchema
record FooB(String b, String c, String d) implements Mid1, Mid2 {
}

@ObjectSchema
record FooC(String b, String d, String e) implements Mid2 {
}