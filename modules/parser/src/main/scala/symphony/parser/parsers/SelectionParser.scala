package symphony.parser.parsers

import org.parboiled2.*
import org.parboiled2.support.hlist
import org.parboiled2.support.hlist.HNil

import symphony.parser.*
import symphony.parser.adt.*
import symphony.parser.adt.Definition.ExecutableDefinition
import symphony.parser.adt.Selection.*
import symphony.parser.adt.Type.*

abstract class SelectionParser extends ValueParser {
  def input: ParserInput

  // ========================================Selections===================================================================
  def alias: Rule1[String] = rule {
    name ~ ":" ~ ignored
  }

  def argument: Rule1[(String, SymphonyQLInputValue)] = rule {
    name ~ ":" ~ ignored ~ value ~> { (n, v) =>
      n -> v
    }
  }

  def arguments: Rule1[Map[String, SymphonyQLInputValue]] = rule {
    "(" ~!~ ignored ~ argument.*.separatedBy(ignored) ~ ignored ~ ")" ~> (_.toMap)
  }

  def directive: Rule1[Directive] = rule("@" ~ name ~ arguments ~> { (name, arguments) =>
    Directive(name, arguments)
  })

  def directives: Rule1[List[Directive]] = rule {
    directive.*.separatedBy(ignored) ~> (_.toList)
  }

  def selection: Rule1[Selection] = rule {
    field | fragmentSpread | inlineFragment
  }

  def selectionSet: Rule1[List[Selection]] = rule {
    "{" ~!~ ignored ~ selection.*.separatedBy(ignored) ~ ignored ~ "}" ~> { x => x.toList }
  }

  def namedType: Rule1[NamedType] = rule {
    name ~ !(str("null") ~ EOI) ~> (NamedType(_, nonNull = false))
  }

  def listType: Rule1[ListType] = rule {
    "[" ~ type_ ~ "]" ~> (t => ListType(t, nonNull = false))
  }

  def nonNullType: Rule1[Type] = rule {
    (namedType | listType) ~ "!" ~> {
      case t: NamedType => t.copy(nonNull = true)
      case t: ListType  => t.copy(nonNull = true)
    }
  }

  def type_ : Rule1[Type] = rule {
    nonNullType | namedType | listType
  }

  def field: Rule1[Field] = rule {
    alias.? ~ ignored ~ name ~ ignored ~ arguments.? ~ ignored ~ directives.? ~ ignored ~ selectionSet.? ~> {
      (alias, name, args, dirs, sels) =>
        Field(
          alias,
          name,
          args.getOrElse(Map()),
          dirs.getOrElse(Nil),
          sels.getOrElse(Nil)
        )
    }
  }

  def fragmentName: Rule1[String] = rule {
    name ~ !(str("on") ~ EOI)
  }

  def fragmentSpread: Rule1[FragmentSpread] = rule {
    "..." ~ fragmentName ~ ignored ~ directives.? ~> { (name, dirs) =>
      FragmentSpread(name, dirs.toList.flatten)
    }
  }

  def typeCondition: Rule1[NamedType] = rule {
    "on" ~ ignored ~!~ namedType
  }

  def inlineFragment: Rule1[InlineFragment] = rule {
    "..." ~ ignored ~ typeCondition.? ~ ignored ~ directives.? ~ ignored ~ selectionSet ~> {
      (typeCondition, dirs, sel) =>
        InlineFragment(typeCondition, dirs.toList.flatten, sel)
    }
  }
}
