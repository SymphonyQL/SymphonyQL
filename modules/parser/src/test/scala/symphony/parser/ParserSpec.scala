package symphony.parser

import org.scalatest.funspec.AnyFunSpec

import symphony.parser.adt.*
import symphony.parser.adt.Selection.Field
import symphony.parser.value.*

class ParserSpec extends AnyFunSpec {

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
      assertResult(
        simpleQuery(
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
      )(doc.toOption.orNull)
    }
  }
}
