package symphony.parser

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.*
import symphony.parser.*
import symphony.parser.SymphonyQLError.ParsingError
import symphony.parser.SymphonyQLInputValue.*
import symphony.parser.SymphonyQLValue.*
import symphony.parser.adt.*
import symphony.parser.adt.Definition.ExecutableDefinition.*
import symphony.parser.adt.OperationType.Mutation
import symphony.parser.adt.Selection.*
import symphony.parser.adt.Type.*

class ParserSpec extends AnyFunSpec with Matchers {

  describe("Introspection Spec") {
    it("full Introspection query") {
      val fullIntrospectionQuery =
        """
          query IntrospectionQuery {
            __schema {
              queryType { name }
              mutationType { name }
              subscriptionType { name }
              types {
                ...FullType
              }
              directives {
                name
                description
                locations
                args {
                  ...InputValue
                }
              }
            }
          }
  
          fragment FullType on __Type {
            kind
            name
            description
            fields(includeDeprecated: true) {
              name
              description
              args {
                ...InputValue
              }
              type {
                ...TypeRef
              }
              isDeprecated
              deprecationReason
            }
            inputFields {
              ...InputValue
            }
            interfaces {
              ...TypeRef
            }
            enumValues(includeDeprecated: true) {
              name
              description
              isDeprecated
              deprecationReason
            }
            possibleTypes {
              ...TypeRef
            }
          }
  
          fragment InputValue on __InputValue {
            name
            description
            type { ...TypeRef }
            defaultValue
          }
  
          fragment TypeRef on __Type {
            kind
            name
            ofType {
              kind
              name
              ofType {
                kind
                name
                ofType {
                  kind
                  name
                  ofType {
                    kind
                    name
                    ofType {
                      kind
                      name
                      ofType {
                        kind
                        name
                        ofType {
                          kind
                          name
                        }
                      }
                    }
                  }
                }
              }
            }
          }"""

      val doc = SymphonyQLParser.parseQuery(fullIntrospectionQuery)
      doc.toOption.isDefined shouldEqual true
    }
  }
  describe("Simple Mutation") {
    it("simple query with args") {
      val query = """mutation {
                    |  likeStory(storyID: 12345) {
                    |    story {
                    |      likeCount
                    |    }
                    |  }
                    |}""".stripMargin

      val doc = SymphonyQLParser.parseQuery(query)
      doc.toOption.orNull shouldEqual Document(
        List(
          OperationDefinition(
            Mutation,
            None,
            Nil,
            Nil,
            List(
              mkSimpleField(
                "likeStory",
                arguments = Map("storyID" -> IntValue(12345)),
                selectionSet = List(
                  mkSimpleField("story", selectionSet = List(mkSimpleField("likeCount")))
                )
              )
            )
          )
        ),
        SourceMapper(query)
      )
    }

  }

  describe("Simple Query") {
    it("invalid syntax") {
      val query =
        """{
          |  hero {
          |    name(
          |  }
          |}""".stripMargin
      val doc   = SymphonyQLParser.parseQuery(query)
      doc.swap.toOption.orNull shouldEqual ParsingError(
        "Invalid input '}', expected unicodeBOM, whiteSpace, lineTerminator, comment, comma, argument, ignored or ')' (line 4, column 3):\n  }\n  ^",
        Some(LocationInfo(4, 3))
      )
    }

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

      val doc = SymphonyQLParser.parseQuery(query)
      doc.toOption.orNull shouldEqual simpleQuery(
        selectionSet = List(
          mkSimpleField(
            "hero",
            selectionSet = List(
              mkSimpleField("name"),
              mkSimpleField("friends", selectionSet = List(mkSimpleField("name")))
            )
          )
        ),
        sourceMapper = SourceMapper(query)
      )
    }

    it("valid query") {
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

      val doc = SymphonyQLParser.check(query)
      doc shouldEqual None
    }

    it("simple arguments") {
      val query =
        """{
          |  human(id: "1000") {
          |    name
          |    height(unit: FOOT)
          |  }
          |}""".stripMargin

      val doc = SymphonyQLParser.parseQuery(query)
      doc.toOption.orNull shouldEqual
        simpleQuery(
          selectionSet = List(
            mkSimpleField(
              "human",
              arguments = Map("id" -> StringValue("1000")),
              selectionSet = List(
                mkSimpleField("name"),
                mkSimpleField("height", arguments = Map("unit" -> EnumValue("FOOT")))
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

      val doc = SymphonyQLParser.parseQuery(query)
      doc.toOption.orNull shouldEqual
        simpleQuery(
          selectionSet = List(
            mkSimpleField(
              "human",
              arguments = Map("id" -> StringValue("1000\\")),
              selectionSet = List(mkSimpleField("name"))
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
      val doc   = SymphonyQLParser.parseQuery(query)
      doc.toOption.orNull shouldEqual
        simpleQuery(
          selectionSet = List(
            mkSimpleField(
              "hero",
              alias = Some("empireHero"),
              arguments = Map("episode" -> EnumValue("EMPIRE")),
              selectionSet = List(mkSimpleField("name"))
            ),
            mkSimpleField(
              "hero",
              alias = Some("jediHero"),
              arguments = Map("episode" -> EnumValue("JEDI")),
              selectionSet = List(mkSimpleField("name"))
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
      val doc   = SymphonyQLParser.parseQuery(query)
//      println(doc.toOption.orNull.operationDefinitions.last.selectionSet.last.asInstanceOf[Field].arguments.map(kv => kv._1 -> kv._2.getClass))
      doc.toOption.orNull shouldEqual
        simpleQuery(
          selectionSet = List(
            mkSimpleField(
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
              selectionSet = List(mkSimpleField("name"))
            )
          ),
          sourceMapper = SourceMapper(query)
        )
    }

    it("block strings") {
      val query = "{ sendEmail(message: \"\"\"\n  Hello,\n    World!\n\n  Yours,\n    GraphQL. \"\"\") }"

      val doc = SymphonyQLParser.parseQuery(query)
      doc.toOption.orNull shouldEqual
        simpleQuery(
          selectionSet = List(
            mkSimpleField(
              "sendEmail",
              arguments = Map("message" -> StringValue("Hello,\n  World!\n\nYours,\n  GraphQL. "))
            )
          ),
          sourceMapper = SourceMapper(query)
        )
    }

    it("variables") {
      val query = """query getProfile($devicePicSize: Int = 60) {
                    |  user(id: 4) {
                    |    id
                    |    name
                    |    profilePic(size: $devicePicSize)
                    |  }
                    |}""".stripMargin
      val doc   = SymphonyQLParser.parseQuery(query)
      doc.toOption.orNull shouldEqual
        simpleQuery(
          name = Some("getProfile"),
          variableDefinitions = List(
            VariableDefinition("devicePicSize", NamedType("Int", nonNull = false), Some(IntValue(60)), Nil)
          ),
          selectionSet = List(
            mkSimpleField(
              "user",
              arguments = Map("id" -> IntValue(4)),
              selectionSet = List(
                mkSimpleField("id"),
                mkSimpleField("name"),
                mkSimpleField("profilePic", arguments = Map("size" -> VariableValue("devicePicSize")))
              )
            )
          ),
          sourceMapper = SourceMapper(query)
        )
    }

    it("directives") {
      val query = """query myQuery($someTest: Boolean) {
                    |  experimentalField @skip(if: $someTest)
                    |}""".stripMargin
      val doc   = SymphonyQLParser.parseQuery(query)
      doc.toOption.orNull shouldEqual
        simpleQuery(
          name = Some("myQuery"),
          variableDefinitions = List(VariableDefinition("someTest", NamedType("Boolean", nonNull = false), None, Nil)),
          selectionSet = List(
            mkSimpleField(
              "experimentalField",
              directives = List(Directive("skip", Map("if" -> VariableValue("someTest"))))
            )
          ),
          sourceMapper = SourceMapper(query)
        )
    }

    it("list and non-null types") {
      val query =
        """query getProfile($devicePicSize: [Int!]!) {
          |  nothing
          |}""".stripMargin
      val doc   = SymphonyQLParser.parseQuery(query)
      doc.toOption.orNull shouldEqual
        simpleQuery(
          name = Some("getProfile"),
          variableDefinitions = List(
            VariableDefinition(
              "devicePicSize",
              ListType(NamedType("Int", nonNull = true), nonNull = true),
              None,
              Nil
            )
          ),
          selectionSet = List(mkSimpleField("nothing")),
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

      val doc = SymphonyQLParser.parseQuery(query)
      doc.toOption.orNull shouldEqual simpleQueryWithFragment(
        Some("withFragments"),
        selectionSet = List(
          mkSimpleField(
            "user",
            arguments = Map("id" -> IntValue(4)),
            selectionSet = List(
              mkSimpleField(
                "friends",
                arguments = Map("first" -> IntValue(10)),
                selectionSet = List(FragmentSpread("friendFields", Nil))
              ),
              mkSimpleField(
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
            mkSimpleField("id"),
            mkSimpleField("name"),
            mkSimpleField("profilePic", arguments = Map("size" -> IntValue(50)))
          )
        ),
        sourceMapper = SourceMapper(query)
      )

    }

    it("inline fragments") {
      val query = """query inlineFragmentTyping {
                    |  profiles(handles: ["a", "b"]) {
                    |    handle
                    |    ... on User {
                    |      friends {
                    |        count
                    |      }
                    |    }
                    |    ... on Page {
                    |      likers {
                    |        count
                    |      }
                    |    }
                    |  }
                    |}""".stripMargin
      val doc   = SymphonyQLParser.parseQuery(query)
      doc.toOption.orNull shouldEqual
        simpleQuery(
          name = Some("inlineFragmentTyping"),
          selectionSet = List(
            mkSimpleField(
              "profiles",
              arguments = Map("handles" -> ListValue(List(StringValue("a"), StringValue("b")))),
              selectionSet = List(
                mkSimpleField("handle"),
                InlineFragment(
                  Some(NamedType("User", nonNull = false)),
                  Nil,
                  List(
                    mkSimpleField("friends", selectionSet = List(mkSimpleField("count")))
                  )
                ),
                InlineFragment(
                  Some(NamedType("Page", nonNull = false)),
                  Nil,
                  List(mkSimpleField("likers", selectionSet = List(mkSimpleField("count"))))
                )
              )
            )
          ),
          sourceMapper = SourceMapper(query)
        )
    }

    it("inline fragments with directives") {
      val query = """query inlineFragmentNoType($expandedInfo: Boolean) {
                    |  user(handle: "abc") {
                    |    id
                    |    name
                    |    ... @include(if: $expandedInfo) {
                    |      firstName
                    |      lastName
                    |      birthday
                    |    }
                    |  }
                    |}""".stripMargin
      val doc   = SymphonyQLParser.parseQuery(query)
      doc.toOption.orNull shouldEqual
        simpleQuery(
          name = Some("inlineFragmentNoType"),
          variableDefinitions =
            List(VariableDefinition("expandedInfo", NamedType("Boolean", nonNull = false), None, Nil)),
          selectionSet = List(
            mkSimpleField(
              "user",
              arguments = Map("handle" -> StringValue("abc")),
              selectionSet = List(
                mkSimpleField("id"),
                mkSimpleField("name"),
                InlineFragment(
                  None,
                  List(Directive("include", Map("if" -> VariableValue("expandedInfo")))),
                  List(
                    mkSimpleField("firstName"),
                    mkSimpleField("lastName"),
                    mkSimpleField("birthday")
                  )
                )
              )
            )
          ),
          sourceMapper = SourceMapper(query)
        )
    }

  }
}
