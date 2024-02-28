package symphony.schema

import org.apache.pekko.stream.scaladsl
import symphony.SymphonyQLSchema
import symphony.parser.*
import symphony.parser.adt.*
import symphony.parser.adt.Definition.ExecutableDefinition.*
import symphony.parser.adt.Selection.Field
import symphony.parser.adt.introspection.*
import symphony.schema.Stage.*

object Introspector extends IntrospectionSchemaDerivation {

  private val tpe  = __introspectionSchema.tpe()
  private val root = RootType(tpe, None, None)

  def introspect(rootType: RootType): SymphonyQLSchema = {
    val types    = (rootType.types ++ root.types - "__Introspection").values.toList.sortBy(_.name.getOrElse(""))
    val resolver = __Introspection(
      __Schema(
        rootType.description,
        types,
        rootType.queryType,
        rootType.mutationType,
        rootType.subscriptionType,
        rootType.additionalDirectives
      ),
      args => types.find(_.name.contains(args.name))
    )

    SymphonyQLSchema(
      Some(
        Operation(
          tpe,
          Stage.ScalaSourceStage(scaladsl.Source.single(introspection.analyze(resolver)))
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
