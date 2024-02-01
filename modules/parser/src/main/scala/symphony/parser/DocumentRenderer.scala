package symphony
package parser

import scala.annotation.switch

import adt.*
import adt.Definition.ExecutableDefinition.*
import adt.Definition.TypeSystemDefinition.*
import introspection.*
import symphony.parser.adt.Definition.TypeSystemDefinition.DirectiveLocation.*
import symphony.parser.adt.Definition.TypeSystemDefinition.TypeDefinition.*
import symphony.parser.adt.Type.{ innerType, NamedType }

object DocumentRenderer extends SymphonyQLRenderer[Document] {

  def renderType(t: __Type): String = {
    val builder = new StringBuilder
    __typeNameRenderer.unsafeRender(t, None, builder)
    builder.toString()
  }

  private implicit val typeOrdering: Ordering[TypeDefinition] = Ordering.by {
    case TypeDefinition.ScalarTypeDefinition(_, name, _)          => (0, name)
    case TypeDefinition.UnionTypeDefinition(_, name, _, _)        => (1, name)
    case TypeDefinition.EnumTypeDefinition(_, name, _, _)         => (2, name)
    case TypeDefinition.InputObjectTypeDefinition(_, name, _, _)  => (3, name)
    case TypeDefinition.InterfaceTypeDefinition(_, name, _, _, _) => (4, name)
    case TypeDefinition.ObjectTypeDefinition(_, name, _, _, _)    => (5, name)
  }

  private implicit val directiveLocationOrdering: Ordering[DirectiveLocation] = Ordering.by {
    case ExecutableDirectiveLocation.QUERY                  => 0
    case ExecutableDirectiveLocation.MUTATION               => 1
    case ExecutableDirectiveLocation.SUBSCRIPTION           => 2
    case ExecutableDirectiveLocation.FIELD                  => 3
    case ExecutableDirectiveLocation.FRAGMENT_DEFINITION    => 4
    case ExecutableDirectiveLocation.FRAGMENT_SPREAD        => 5
    case ExecutableDirectiveLocation.INLINE_FRAGMENT        => 6
    case TypeSystemDirectiveLocation.SCHEMA                 => 7
    case TypeSystemDirectiveLocation.SCALAR                 => 8
    case TypeSystemDirectiveLocation.OBJECT                 => 9
    case TypeSystemDirectiveLocation.FIELD_DEFINITION       => 10
    case TypeSystemDirectiveLocation.ARGUMENT_DEFINITION    => 11
    case TypeSystemDirectiveLocation.INTERFACE              => 12
    case TypeSystemDirectiveLocation.UNION                  => 13
    case TypeSystemDirectiveLocation.ENUM                   => 14
    case TypeSystemDirectiveLocation.ENUM_VALUE             => 15
    case TypeSystemDirectiveLocation.INPUT_OBJECT           => 16
    case TypeSystemDirectiveLocation.INPUT_FIELD_DEFINITION => 17
    case TypeSystemDirectiveLocation.VARIABLE_DEFINITION    => 18

  }

  override def unsafeRender(value: Document, indent: Option[Int], write: StringBuilder): Unit = {
    val sizeEstimate = value.sourceMapper.size.getOrElse {
      val numDefs = value.definitions.length
      numDefs * 16
    }
    write.ensureCapacity(sizeEstimate)
    documentRenderer.unsafeRender(value, indent, write)
  }

  lazy val directiveDefinitionsRenderer: SymphonyQLRenderer[List[DirectiveDefinition]] =
    directiveDefinitionRenderer.list(SymphonyQLRenderer.newlineOrSpace)

  lazy val typesRenderer: SymphonyQLRenderer[List[__Type]] =
    typeDefinitionsRenderer.contramap(_.flatMap(_.toTypeDefinition))

  lazy val directivesRenderer: SymphonyQLRenderer[List[Directive]] =
    directiveRenderer.list(SymphonyQLRenderer.spaceOrEmpty, omitFirst = false).contramap(_.sortBy(_.name))

  lazy val descriptionRenderer: SymphonyQLRenderer[Option[String]] =
    new SymphonyQLRenderer[Option[String]] {
      private val tripleQuote = "\"\"\""

      override def unsafeRender(description: Option[String], indent: Option[Int], writer: StringBuilder): Unit =
        description.foreach {
          case value if value.contains('\n') =>
            def valueEscaped(): Unit = unsafeFastEscapeQuote(value, writer)

            writer append tripleQuote
            // check if it ends in quote but it is already escaped
            if (value.endsWith("\\\"")) {
              newlineOrEmpty(indent, writer)
              valueEscaped()
              newlineOrEmpty(indent, writer)
            } else if (value.last == '"') {
              newlineOrEmpty(indent, writer)
              valueEscaped()
              newlineOrSpace(indent, writer)
              // check if it ends in quote. We need to break the sequence of 4 or more '"'
            } else {
              // ok. No quotes at the end of value
              newlineOrEmpty(indent, writer)
              valueEscaped()
              newlineOrEmpty(indent, writer)
            }
            writer append tripleQuote
            newlineOrSpace(indent, writer)
          case value                         =>
            pad(indent, writer)
            writer append '"'
            SymphonyQLRenderer.escapedString.unsafeRender(value, indent, writer)
            writer append '"'
            newlineOrSpace(indent, writer)
        }
    }

  private lazy val documentRenderer: SymphonyQLRenderer[Document] = SymphonyQLRenderer.combine(
    (directiveDefinitionsRenderer ++
      (SymphonyQLRenderer.newlineOrSpace ++ SymphonyQLRenderer.newlineOrEmpty)
        .when[List[DirectiveDefinition]](_.nonEmpty))
      .contramap(_.directiveDefinitions),
    schemaRenderer.optional.contramap(_.schemaDefinition),
    operationDefinitionRenderer
      .list(SymphonyQLRenderer.newlineOrSpace ++ SymphonyQLRenderer.newlineOrEmpty)
      .contramap(_.operationDefinitions),
    typeDefinitionsRenderer.contramap(_.typeDefinitions),
    fragmentRenderer.list.contramap(_.fragmentDefinitions)
  )

  private lazy val __typeNameRenderer: SymphonyQLRenderer[__Type] =
    (value: __Type, indent: Option[Int], write: StringBuilder) => {
      def loop(typ: Option[__Type]): Unit = typ match {
        case Some(t) =>
          t.kind match {
            case __TypeKind.NON_NULL =>
              loop(t.ofType)
              write append '!'
            case __TypeKind.LIST     =>
              write append '['
              loop(t.ofType)
              write append ']'
            case _                   =>
              write append t.name.getOrElse("null")
          }
        case None    =>
          write append "null"
      }

      loop(Some(value))
    }

  private lazy val directiveDefinitionRenderer: SymphonyQLRenderer[DirectiveDefinition] =
    new SymphonyQLRenderer[DirectiveDefinition] {

      private val inputRenderer =
        inputValueDefinitionRenderer.list(SymphonyQLRenderer.comma ++ SymphonyQLRenderer.spaceOrEmpty)

      private val locationsRenderer: SymphonyQLRenderer[Set[DirectiveLocation]] =
        locationRenderer
          .list(SymphonyQLRenderer.spaceOrEmpty ++ SymphonyQLRenderer.char('|') ++ SymphonyQLRenderer.spaceOrEmpty)
          .contramap(_.toList.sorted)

      override def unsafeRender(value: DirectiveDefinition, indent: Option[Int], writer: StringBuilder): Unit =
        value match {
          case DirectiveDefinition(description, name, args, isRepeatable, locations) =>
            descriptionRenderer.unsafeRender(description, indent, writer)
            writer append "directive @"
            writer append name
            if (args.nonEmpty) {
              writer append '('
              inputRenderer.unsafeRender(args, indent, writer)
              writer append ')'
            }
            if (isRepeatable) writer append " repeatable"
            writer append " on "
            locationsRenderer.unsafeRender(locations, indent, writer)
        }

      lazy val locationRenderer: SymphonyQLRenderer[DirectiveLocation] =
        (location: DirectiveLocation, indent: Option[Int], writer: StringBuilder) =>
          location match {
            case ExecutableDirectiveLocation.QUERY                  => writer append "QUERY"
            case ExecutableDirectiveLocation.MUTATION               => writer append "MUTATION"
            case ExecutableDirectiveLocation.SUBSCRIPTION           => writer append "SUBSCRIPTION"
            case ExecutableDirectiveLocation.FIELD                  => writer append "FIELD"
            case ExecutableDirectiveLocation.FRAGMENT_DEFINITION    => writer append "FRAGMENT_DEFINITION"
            case ExecutableDirectiveLocation.FRAGMENT_SPREAD        => writer append "FRAGMENT_SPREAD"
            case ExecutableDirectiveLocation.INLINE_FRAGMENT        => writer append "INLINE_FRAGMENT"
            case TypeSystemDirectiveLocation.SCHEMA                 => writer append "SCHEMA"
            case TypeSystemDirectiveLocation.SCALAR                 => writer append "SCALAR"
            case TypeSystemDirectiveLocation.OBJECT                 => writer append "OBJECT"
            case TypeSystemDirectiveLocation.FIELD_DEFINITION       => writer append "FIELD_DEFINITION"
            case TypeSystemDirectiveLocation.ARGUMENT_DEFINITION    => writer append "ARGUMENT_DEFINITION"
            case TypeSystemDirectiveLocation.INTERFACE              => writer append "INTERFACE"
            case TypeSystemDirectiveLocation.UNION                  => writer append "UNION"
            case TypeSystemDirectiveLocation.ENUM                   => writer append "ENUM"
            case TypeSystemDirectiveLocation.ENUM_VALUE             => writer append "ENUM_VALUE"
            case TypeSystemDirectiveLocation.INPUT_OBJECT           => writer append "INPUT_OBJECT"
            case TypeSystemDirectiveLocation.INPUT_FIELD_DEFINITION => writer append "INPUT_FIELD_DEFINITION"
            case TypeSystemDirectiveLocation.VARIABLE_DEFINITION    => writer append "VARIABLE_DEFINITION"
          }

    }

  private lazy val operationDefinitionRenderer: SymphonyQLRenderer[OperationDefinition] =
    (definition: OperationDefinition, indent: Option[Int], writer: StringBuilder) =>
      definition match {
        case OperationDefinition(operationType, name, variableDefinitions, directives, selectionSet) =>
          operationTypeRenderer.unsafeRender(operationType, indent, writer)
          name.foreach { n =>
            writer append ' '
            writer append n
          }
          variableDefinitionsRenderer.unsafeRender(variableDefinitions, indent, writer)
          directivesRenderer.unsafeRender(directives, indent, writer)
          selectionsRenderer.unsafeRender(selectionSet, indent, writer)
      }

  private lazy val operationTypeRenderer: SymphonyQLRenderer[OperationType] =
    (operationType: OperationType, indent: Option[Int], writer: StringBuilder) =>
      operationType match {
        case OperationType.Query        => writer append "query"
        case OperationType.Mutation     => writer append "mutation"
        case OperationType.Subscription => writer append "subscription"
      }

  private lazy val variableDefinitionsRenderer: SymphonyQLRenderer[List[VariableDefinition]] =
    new SymphonyQLRenderer[List[VariableDefinition]] {
      private val inner = variableDefinition.list(SymphonyQLRenderer.comma ++ SymphonyQLRenderer.spaceOrEmpty)

      override def unsafeRender(value: List[VariableDefinition], indent: Option[Int], writer: StringBuilder): Unit =
        if (value.nonEmpty) {
          writer append '('
          inner.unsafeRender(value, indent, writer)
          writer append ')'
        }
    }

  private lazy val variableDefinition: SymphonyQLRenderer[VariableDefinition] =
    (definition: VariableDefinition, indent: Option[Int], writer: StringBuilder) => {
      writer append '$'
      writer append definition.name
      writer append ':'
      spaceOrEmpty(indent, writer)
      typeRenderer.unsafeRender(definition.variableType, indent, writer)
      defaultValueRenderer.unsafeRender(definition.defaultValue, indent, writer)
    }

  private lazy val selectionsRenderer: SymphonyQLRenderer[List[Selection]] = new SymphonyQLRenderer[List[Selection]] {
    private val inner = selectionRenderer.list(SymphonyQLRenderer.newlineOrSpace)

    override def unsafeRender(selections: List[Selection], indent: Option[Int], writer: StringBuilder): Unit =
      if (selections.nonEmpty) {
        spaceOrEmpty(indent, writer)
        writer append '{'
        newlineOrEmpty(indent, writer)
        inner.unsafeRender(selections, increment(indent), writer)
        newlineOrEmpty(indent, writer)
        pad(indent, writer)
        writer append '}'
      }
  }

  private lazy val selectionRenderer: SymphonyQLRenderer[Selection] =
    (selection: Selection, indent: Option[Int], builder: StringBuilder) => {
      pad(indent, builder)
      selection match {
        case Selection.Field(alias, name, arguments, directives, selectionSet) =>
          alias.foreach { a =>
            builder append a
            builder append ':'
            spaceOrEmpty(indent, builder)
          }
          builder append name
          inputArgumentsRenderer.unsafeRender(arguments, indent, builder)
          directivesRenderer.unsafeRender(directives, indent, builder)
          selectionsRenderer.unsafeRender(selectionSet, indent, builder)
        case Selection.FragmentSpread(name, directives)                        =>
          builder append "..."
          builder append name
          directivesRenderer.unsafeRender(directives, indent, builder)
        case Selection.InlineFragment(typeCondition, dirs, selectionSet)       =>
          builder append "..."
          typeCondition.foreach { t =>
            spaceOrEmpty(indent, builder)
            builder append "on "
            builder append t.name
          }
          directivesRenderer.unsafeRender(dirs, indent, builder)
          if (selectionSet.nonEmpty) {
            selectionsRenderer.unsafeRender(selectionSet, indent, builder)
          }
      }
    }

  private lazy val inputArgumentsRenderer: SymphonyQLRenderer[Map[String, SymphonyQLInputValue]] =
    new SymphonyQLRenderer[Map[String, SymphonyQLInputValue]] {

      private val inner =
        SymphonyQLRenderer.map(
          SymphonyQLRenderer.string,
          ValueRenderer.inputValueRenderer,
          SymphonyQLRenderer.comma ++ SymphonyQLRenderer.spaceOrEmpty,
          SymphonyQLRenderer.char(':') ++ SymphonyQLRenderer.spaceOrEmpty
        )

      override def unsafeRender(
        arguments: Map[String, SymphonyQLInputValue],
        indent: Option[Int],
        writer: StringBuilder
      ): Unit =
        if (arguments.nonEmpty) {
          writer append '('
          inner.unsafeRender(arguments, indent, writer)
          writer append ')'
        }
    }

  private lazy val schemaRenderer: SymphonyQLRenderer[SchemaDefinition] =
    (definition: SchemaDefinition, indent: Option[Int], writer: StringBuilder) =>
      definition match {
        case SchemaDefinition(directives, query, mutation, subscription, description) =>
          val hasTypes    = query.nonEmpty || mutation.nonEmpty || subscription.nonEmpty
          val isExtension = directives.nonEmpty && !hasTypes
          var first       = true

          def renderOp(name: String, op: Option[String]): Unit =
            op.foreach { o =>
              if (first) {
                first = false
                newlineOrEmpty(indent, writer)
              } else newlineOrComma(indent, writer)
              pad(increment(indent), writer)
              writer append name
              writer append ':'
              spaceOrEmpty(indent, writer)
              writer append o
            }

          descriptionRenderer.unsafeRender(description, indent, writer)
          if (isExtension) writer append "extend "
          if (isExtension || hasTypes) {
            writer append "schema"
            directivesRenderer.unsafeRender(directives, indent, writer)
            if (hasTypes) {
              spaceOrEmpty(indent, writer)
              writer append '{'
              renderOp("query", query)
              renderOp("mutation", mutation)
              renderOp("subscription", subscription)
              newlineOrEmpty(indent, writer)
              writer append '}'
            }
          }
      }

  private lazy val typeDefinitionsRenderer: SymphonyQLRenderer[List[TypeDefinition]] =
    typeDefinitionRenderer.list.contramap(_.sorted)

  private lazy val typeDefinitionRenderer: SymphonyQLRenderer[TypeDefinition] =
    (definition: TypeDefinition, indent: Option[Int], writer: StringBuilder) =>
      definition match {
        case typ: TypeDefinition.ObjectTypeDefinition      =>
          objectTypeDefinitionRenderer.unsafeRender(typ, indent, writer)
        case typ: TypeDefinition.InterfaceTypeDefinition   =>
          interfaceTypeDefinitionRenderer.unsafeRender(typ, indent, writer)
        case typ: TypeDefinition.InputObjectTypeDefinition =>
          inputObjectTypeDefinition.unsafeRender(typ, indent, writer)
        case typ: TypeDefinition.EnumTypeDefinition        =>
          enumRenderer.unsafeRender(typ, indent, writer)
        case typ: TypeDefinition.UnionTypeDefinition       =>
          unionRenderer.unsafeRender(typ, indent, writer)
        case typ: TypeDefinition.ScalarTypeDefinition      =>
          scalarRenderer.unsafeRender(typ, indent, writer)
      }

  private lazy val fragmentRenderer: SymphonyQLRenderer[FragmentDefinition] =
    (value: FragmentDefinition, indent: Option[Int], writer: StringBuilder) =>
      value match {
        case FragmentDefinition(name, typeCondition, directives, selectionSet) =>
          newlineOrSpace(indent, writer)
          newlineOrEmpty(indent, writer)
          writer append "fragment "
          writer append name
          writer append " on "
          writer append typeCondition.name
          directivesRenderer.unsafeRender(directives, indent, writer)
          selectionsRenderer.unsafeRender(selectionSet, indent, writer)
      }

  private lazy val unionRenderer: SymphonyQLRenderer[UnionTypeDefinition] =
    new SymphonyQLRenderer[UnionTypeDefinition] {

      private val memberRenderer =
        SymphonyQLRenderer.string.list(
          SymphonyQLRenderer.spaceOrEmpty ++ SymphonyQLRenderer.char('|') ++ SymphonyQLRenderer.spaceOrEmpty
        )

      override def unsafeRender(value: UnionTypeDefinition, indent: Option[Int], writer: StringBuilder): Unit =
        value match {
          case UnionTypeDefinition(description, name, directives, members) =>
            newlineOrSpace(indent, writer)
            newlineOrEmpty(indent, writer)
            descriptionRenderer.unsafeRender(description, indent, writer)
            writer append "union "
            writer append name
            directivesRenderer.unsafeRender(directives, indent, writer)
            spaceOrEmpty(indent, writer)
            writer append '='
            spaceOrEmpty(indent, writer)
            memberRenderer.unsafeRender(members, indent, writer)
        }
    }

  private lazy val scalarRenderer: SymphonyQLRenderer[ScalarTypeDefinition] =
    (value: ScalarTypeDefinition, indent: Option[Int], write: StringBuilder) =>
      value match {
        case ScalarTypeDefinition(description, name, directives) =>
          if (!isBuiltinScalar(name)) {
            newlineOrSpace(indent, write)
            descriptionRenderer.unsafeRender(description, indent, write)
            write append "scalar "
            write append name
            directivesRenderer.unsafeRender(directives, indent, write)
          }
      }

  private lazy val enumRenderer: SymphonyQLRenderer[EnumTypeDefinition] = new SymphonyQLRenderer[EnumTypeDefinition] {
    private val memberRenderer = enumValueDefinitionRenderer.list(SymphonyQLRenderer.newlineOrComma)

    override def unsafeRender(value: EnumTypeDefinition, indent: Option[Int], writer: StringBuilder): Unit =
      value match {
        case EnumTypeDefinition(description, name, directives, values) =>
          newlineOrSpace(indent, writer)
          newlineOrEmpty(indent, writer)
          descriptionRenderer.unsafeRender(description, indent, writer)
          writer append "enum "
          writer append name
          directivesRenderer.unsafeRender(directives, indent, writer)
          spaceOrEmpty(indent, writer)
          writer append '{'
          newlineOrEmpty(indent, writer)
          memberRenderer.unsafeRender(values, increment(indent), writer)
          newlineOrEmpty(indent, writer)
          writer append '}'
      }
  }

  private lazy val enumValueDefinitionRenderer: SymphonyQLRenderer[EnumValueDefinition] =
    (value: EnumValueDefinition, indent: Option[Int], writer: StringBuilder) =>
      value match {
        case EnumValueDefinition(description, name, directives) =>
          descriptionRenderer.unsafeRender(description, indent, writer)
          pad(indent, writer)
          writer append name
          directivesRenderer.unsafeRender(directives, indent, writer)
      }

  private lazy val inputObjectTypeDefinition: SymphonyQLRenderer[InputObjectTypeDefinition] =
    new SymphonyQLRenderer[InputObjectTypeDefinition] {
      private val fieldsRenderer = inputValueDefinitionRenderer.list(SymphonyQLRenderer.newlineOrSpace)

      override def unsafeRender(value: InputObjectTypeDefinition, indent: Option[Int], writer: StringBuilder): Unit =
        value match {
          case InputObjectTypeDefinition(description, name, directives, fields) =>
            newlineOrSpace(indent, writer)
            newlineOrEmpty(indent, writer)
            descriptionRenderer.unsafeRender(description, indent, writer)
            writer append "input "
            writer append name
            directivesRenderer.unsafeRender(directives, indent, writer)
            spaceOrEmpty(indent, writer)
            writer append '{'
            newlineOrEmpty(indent, writer)
            fieldsRenderer.unsafeRender(fields, increment(indent), writer)
            newlineOrEmpty(indent, writer)
            writer append '}'
        }
    }

  private lazy val inputValueDefinitionRenderer: SymphonyQLRenderer[InputValueDefinition] =
    (definition: InputValueDefinition, indent: Option[Int], builder: StringBuilder) =>
      definition match {
        case InputValueDefinition(description, name, valueType, defaultValue, directives) =>
          descriptionRenderer.unsafeRender(description, indent, builder)
          pad(indent, builder)
          builder append name
          builder append ':'
          spaceOrEmpty(indent, builder)
          typeRenderer.unsafeRender(valueType, indent, builder)
          defaultValueRenderer.unsafeRender(defaultValue, indent, builder)
          directivesRenderer.unsafeRender(directives, indent, builder)
      }

  private lazy val objectTypeDefinitionRenderer: SymphonyQLRenderer[ObjectTypeDefinition] =
    (value: ObjectTypeDefinition, indent: Option[Int], writer: StringBuilder) =>
      unsafeRenderObjectLike(
        "type",
        value.description,
        value.name,
        value.implements,
        value.directives,
        value.fields,
        indent,
        writer
      )

  private lazy val interfaceTypeDefinitionRenderer: SymphonyQLRenderer[InterfaceTypeDefinition] =
    (value: InterfaceTypeDefinition, indent: Option[Int], writer: StringBuilder) =>
      unsafeRenderObjectLike(
        "interface",
        value.description,
        value.name,
        value.implements,
        value.directives,
        value.fields,
        indent,
        writer
      )

  private def unsafeRenderObjectLike(
    variant: String,
    description: Option[String],
    name: String,
    implements: List[NamedType],
    directives: List[Directive],
    fields: List[FieldDefinition],
    indent: Option[Int],
    writer: StringBuilder
  ): Unit = {
    newlineOrEmpty(indent, writer)
    newlineOrEmpty(indent, writer)
    descriptionRenderer.unsafeRender(description, indent, writer)
    writer append variant
    writer append ' '
    writer append name
    implements match {
      case Nil          =>
      case head :: tail =>
        writer append " implements "
        writer append innerType(head)
        tail.foreach { impl =>
          writer append " & "
          writer append innerType(impl)
        }
    }
    directivesRenderer.unsafeRender(directives, indent, writer)
    if (fields.nonEmpty) {
      spaceOrEmpty(indent, writer)
      writer append '{'
      newlineOrEmpty(indent, writer)
      fieldDefinitionsRenderer.unsafeRender(fields, increment(indent), writer)
      newlineOrEmpty(indent, writer)
      writer append '}'
    }
  }

  private lazy val directiveRenderer: SymphonyQLRenderer[Directive] =
    (d: Directive, indent: Option[Int], writer: StringBuilder) => {
      writer append '@'
      writer append d.name
      inputArgumentsRenderer.unsafeRender(d.arguments, indent, writer)
    }

  private lazy val fieldDefinitionsRenderer: SymphonyQLRenderer[List[FieldDefinition]] =
    fieldDefinitionRenderer.list(SymphonyQLRenderer.newlineOrSpace)

  private lazy val fieldDefinitionRenderer: SymphonyQLRenderer[FieldDefinition] =
    (definition: FieldDefinition, indent: Option[Int], writer: StringBuilder) =>
      definition match {
        case FieldDefinition(description, name, arguments, tpe, directives) =>
          descriptionRenderer.unsafeRender(description, indent, writer)
          pad(indent, writer)
          writer append name
          inlineInputValueDefinitionsRenderer.unsafeRender(arguments, indent, writer)
          writer append ':'
          spaceOrEmpty(indent, writer)
          typeRenderer.unsafeRender(tpe, indent, writer)
          directivesRenderer.unsafeRender(directives, indent, writer)
      }

  private lazy val typeRenderer: SymphonyQLRenderer[Type] = (value: Type, indent: Option[Int], write: StringBuilder) =>
    {
      def loop(t: Type): Unit = t match {
        case Type.NamedType(name, nonNull)  =>
          write append name
          if (nonNull) write append '!'
        case Type.ListType(ofType, nonNull) =>
          write append '['
          loop(ofType)
          write append ']'
          if (nonNull) write append '!'
      }

      loop(value)
    }

  private lazy val inlineInputValueDefinitionsRenderer: SymphonyQLRenderer[List[InputValueDefinition]] =
    (SymphonyQLRenderer.char('(') ++
      inlineInputValueDefinitionRenderer.list(SymphonyQLRenderer.comma ++ SymphonyQLRenderer.spaceOrEmpty) ++
      SymphonyQLRenderer.char(')')).when(_.nonEmpty)

  private lazy val inlineInputValueDefinitionRenderer: SymphonyQLRenderer[InputValueDefinition] =
    (definition: InputValueDefinition, indent: Option[Int], writer: StringBuilder) =>
      definition match {
        case InputValueDefinition(description, name, tpe, defaultValue, directives) =>
          descriptionRenderer.unsafeRender(description, None, writer)
          writer append name
          writer append ':'
          spaceOrEmpty(indent, writer)
          typeRenderer.unsafeRender(tpe, indent, writer)
          defaultValueRenderer.unsafeRender(defaultValue, indent, writer)
          directivesRenderer.unsafeRender(directives, indent, writer)
      }

  private lazy val defaultValueRenderer: SymphonyQLRenderer[Option[SymphonyQLInputValue]] =
    (value: Option[SymphonyQLInputValue], indent: Option[Int], writer: StringBuilder) =>
      value.foreach { value =>
        spaceOrEmpty(indent, writer)
        writer append '='
        spaceOrEmpty(indent, writer)
        ValueRenderer.inputValueRenderer.unsafeRender(value, indent, writer)
      }

  private def pad(indentation: Option[Int], writer: StringBuilder): Unit = {
    var i = indentation.getOrElse(0)
    while (i > 0) {
      writer append "  "
      i -= 1
    }
  }

  def isBuiltinScalar(name: String): Boolean =
    name == "Int" || name == "Float" || name == "String" || name == "Boolean" || name == "ID"

  private def spaceOrEmpty(indentation: Option[Int], writer: StringBuilder): Unit =
    if (indentation.isDefined) writer append ' '

  private def newlineOrEmpty(indent: Option[Int], writer: StringBuilder): Unit =
    if (indent.isDefined) writer append '\n'

  private def newlineOrSpace(indent: Option[Int], writer: StringBuilder): Unit =
    if (indent.isDefined) writer append '\n' else writer append ' '

  private def newlineOrComma(indentation: Option[Int], writer: StringBuilder): Unit =
    if (indentation.isDefined) writer append '\n' else writer append ','

  private def increment(indentation: Option[Int]): Option[Int] = indentation.map(_ + 1)

  private def unsafeFastEscapeQuote(value: String, builder: StringBuilder): Unit = {
    var i                 = 0
    var quotes            = 0
    def padQuotes(): Unit =
      while (quotes > 0) {
        builder.append('"')
        quotes -= 1
      }
    while (i < value.length) {
      (value.charAt(i): @switch) match {
        case '"' =>
          quotes += 1
          if (quotes == 3) {
            builder.append("\\")
            padQuotes()
          }
        case c   =>
          if (quotes > 0) {
            padQuotes()
          }
          builder.append(c)
      }
      i += 1
    }
    if (quotes > 0) {
      padQuotes()
    }
  }

}
