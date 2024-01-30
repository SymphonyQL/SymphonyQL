package symphony.parser

import org.scalactic.Explicitly.*
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.*

import symphony.parser.*
import symphony.parser.InputValue.*
import symphony.parser.Value.*
import symphony.parser.adt.*
import symphony.parser.adt.Definition.ExecutableDefinition.*
import symphony.parser.adt.Definition.ExecutableDefinition.OperationDefinition
import symphony.parser.adt.OperationType.Query
import symphony.parser.adt.Selection.*
import symphony.parser.adt.Selection.Field
import symphony.parser.adt.Type.*

class ParserSpec extends AnyFunSpec with Matchers {

  describe("A Simple ParserSpec") {
    it("simple query with fields") {
      val query =
        """{
          |  hero {
          |    name
          |    # Queries can have comments!
          |    friends {
          |      name
          |    }
          |  }
          |}""".stripMargin

      val doc = Parser.parseQuery(query)
      doc.toOption.orNull shouldEqual simpleQuery(
        selectionSet = List(
          simpleField(
            "hero",
            selectionSet = List(
              simpleField("name"),
              simpleField("friends", selectionSet = List(simpleField("name")))
            )
          )
        ),
        sourceMapper = SourceMapper(query)
      )
    }

    it("simple arguments") {
      val query =
        """{
          |  human(id: "1000") {
          |    name
          |    height(unit: FOOT)
          |  }
          |}""".stripMargin

      val doc = Parser.parseQuery(query)
      doc.toOption.orNull shouldEqual
        simpleQuery(
          selectionSet = List(
            simpleField(
              "human",
              arguments = Map("id" -> StringValue("1000")),
              selectionSet = List(
                simpleField("name"),
                simpleField("height", arguments = Map("unit" -> EnumValue("FOOT")))
              ),
              directives = List.empty
            )
          ),
          sourceMapper = SourceMapper(query)
        )
    }

    it("arguments with a backslash") {
      val query =
        """{
          |  human(id: "1000\\") {
          |    name
          |  }
          |}""".stripMargin

      val doc = Parser.parseQuery(query)
      doc.toOption.orNull shouldEqual
        simpleQuery(
          selectionSet = List(
            simpleField(
              "human",
              arguments = Map("id" -> StringValue("1000\\")),
              selectionSet = List(simpleField("name"))
            )
          ),
          sourceMapper = SourceMapper(query)
        )
    }

    it("aliases") {
      val query =
        """{
          |  empireHero: hero(episode: EMPIRE) {
          |    name
          |  }
          |  jediHero: hero(episode: JEDI) {
          |    name
          |  }
          |}""".stripMargin
      val doc = Parser.parseQuery(query)
      doc.toOption.orNull shouldEqual
        simpleQuery(
          selectionSet = List(
            simpleField(
              "hero",
              alias = Some("empireHero"),
              arguments = Map("episode" -> EnumValue("EMPIRE")),
              selectionSet = List(simpleField("name"))
            ),
            simpleField(
              "hero",
              alias = Some("jediHero"),
              arguments = Map("episode" -> EnumValue("JEDI")),
              selectionSet = List(simpleField("name"))
            )
          ),
          sourceMapper = SourceMapper(query)
        )
    }

    it("input values") {
      val query = """{
                    |  human(id: "1000", int: 3, float: 3.14, bool: true, nope: null, enum: YES, list: [1,2,3], obj: {
                    |   name: "name"
                    |   }
                    |  ) {
                    |    name
                    |  }
                    |}""".stripMargin
      val doc = Parser.parseQuery(query)
//      println(doc.toOption.orNull.operationDefinitions.last.selectionSet.last.asInstanceOf[Field].arguments.map(kv => kv._1 -> kv._2.getClass))
      doc.toOption.orNull shouldEqual
        simpleQuery(
          selectionSet = List(
            simpleField(
              "human",
              arguments = Map(
                "id"    -> StringValue("1000"),
                "int"   -> IntValue(3),
                "float" -> FloatValue("3.14"),
                "bool"  -> BooleanValue(true),
                "nope"  -> NullValue,
                "enum"  -> EnumValue("YES"),
                "list"  -> ListValue(List(IntValue(1), IntValue(2), IntValue(3))),
                "obj"   -> ObjectValue(Map("name" -> StringValue("name")))
              ),
              selectionSet = List(simpleField("name"))
            )
          ),
          sourceMapper = SourceMapper(query)
        )
    }

    it("block strings") {
      val query = "{ sendEmail(message: \"\"\"\n  Hello,\n    World!\n\n  Yours,\n    GraphQL. \"\"\") }"

      val doc = Parser.parseQuery(query)
      doc.toOption.orNull shouldEqual
        simpleQuery(
          selectionSet = List(
            simpleField(
              "sendEmail",
              arguments = Map("message" -> StringValue("Hello,\n  World!\n\nYours,\n  GraphQL. "))
            )
          ),
          sourceMapper = SourceMapper(query)
        )
    }

  }

  describe("Complex ParserSpec") {

    it("simple query withFragments") {
      val query =
        """query withFragments {
          |  user(id: 4) {
          |    friends(first: 10) {
          |      ...friendFields
          |    }
          |    mutualFriends(first: 10) {
          |      ...friendFields
          |    }
          |  }
          |}
          |
          |fragment friendFields on User {
          |  id
          |  name
          |  profilePic(size: 50)
          |}""".stripMargin

      val doc = Parser.parseQuery(query)
      doc.toOption.orNull shouldEqual simpleQueryWithFragment(
        Some("withFragments"),
        selectionSet = List(
          simpleField(
            "user",
            arguments = Map("id" -> IntValue(4)),
            selectionSet = List(
              simpleField(
                "friends",
                arguments = Map("first" -> IntValue(10)),
                selectionSet = List(FragmentSpread("friendFields", Nil))
              ),
              simpleField(
                "mutualFriends",
                arguments = Map("first" -> IntValue(10)),
                selectionSet = List(FragmentSpread("friendFields", Nil))
              )
            )
          )
        ),
        fragment = FragmentDefinition(
          "friendFields",
          NamedType("User", nonNull = false),
          Nil,
          List(
            simpleField("id"),
            simpleField("name"),
            simpleField("profilePic", arguments = Map("size" -> IntValue(50)))
          )
        ),
        sourceMapper = SourceMapper(query)
      )

    }
  }
}
