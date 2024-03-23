package symphony.schema

import org.apache.pekko.stream.scaladsl
import symphony.schema.RootSchema
import symphony.parser.*
import symphony.parser.adt.*
import symphony.parser.adt.Definition.ExecutableDefinition.*
import symphony.parser.adt.Selection.Field
import symphony.parser.adt.introspection.*
import symphony.schema.Stage.*

object Introspector extends IntrospectionSchemaDerivation {

  private val directives = List(
    __Directive(
      "skip",
      Some(
        "The @skip directive may be provided for fields, fragment spreads, and inline fragments, and allows for conditional exclusion during execution as described by the if argument."
      ),
      Set(__DirectiveLocation.FIELD, __DirectiveLocation.FRAGMENT_SPREAD, __DirectiveLocation.INLINE_FRAGMENT),
      _ => List(__InputValue("if", None, () => Types.boolean.nonNull, None)),
      isRepeatable = false
    ),
    __Directive(
      "include",
      Some(
        "The @include directive may be provided for fields, fragment spreads, and inline fragments, and allows for conditional inclusion during execution as described by the if argument."
      ),
      Set(__DirectiveLocation.FIELD, __DirectiveLocation.FRAGMENT_SPREAD, __DirectiveLocation.INLINE_FRAGMENT),
      _ => List(__InputValue("if", None, () => Types.boolean.nonNull, None)),
      isRepeatable = false
    ),
    __Directive(
      "specifiedBy",
      Some(
        "The @specifiedBy directive is used within the type system definition language to provide a URL for specifying the behavior of custom scalar types. The URL should point to a human-readable specification of the data format, serialization, and coercion rules. It must not appear on built-in scalar types."
      ),
      Set(__DirectiveLocation.SCALAR),
      _ => List(__InputValue("url", None, () => Types.string.nonNull, None)),
      isRepeatable = false
    )
  )

  private val tpe  = __introspectionSchema.tpe()
  private val root = RootType(tpe, None, None)

  def introspect(rootType: RootType): RootSchema = {
    val types    = (rootType.types ++ root.types - "__Introspection").values.toList.sortBy(_.name.getOrElse(""))
    val resolver = __Introspection(
      __Schema(
        rootType.description,
        types,
        rootType.queryType,
        rootType.mutationType,
        rootType.subscriptionType,
        directives ++ rootType.additionalDirectives
      ),
      args => types.find(_.name.contains(args.name))
    )

    RootSchema(
      Some(
        Operation(
          tpe,
          introspection.analyze(resolver)
        )
      ),
      None,
      None
    )
  }

  def isIntrospection(document: Document): Boolean =
    document.definitions.forall {
      case OperationDefinition(_, _, _, _, selectionSet) =>
        selectionSet.nonEmpty && selectionSet.forall {
          case Field(_, name, _, _, _) => name == "__schema" || name == "__type"
          case _                       => false
        }
      case _                                             => true
    }
}
