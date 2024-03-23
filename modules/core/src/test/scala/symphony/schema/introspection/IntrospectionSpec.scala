package symphony.schema.introspection

import org.apache.pekko.NotUsed
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.scaladsl.Source
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.*
import symphony.*
import symphony.parser.*
import symphony.schema.*

import scala.concurrent.*
import scala.concurrent.duration.*

class IntrospectionSpec extends AnyFunSpec with Matchers {

  // test classes
  final case class UserQueryInput(id: String)
  final case class UserOutput(id: String, username: String)
  final case class SourceQueryResolver(
    getUsers: UserQueryInput => Source[UserOutput, NotUsed]
  )

  implicit val actorSystem: ActorSystem = ActorSystem("symphonyActorSystem")
  val resolver                          = SourceQueryResolver(args => Source.single(UserOutput("id", "symphony")))
  val graphql: SymphonyQL               = SymphonyQL
    .newSymphonyQL()
    .rootResolver(SymphonyQLResolver(resolver -> Schema.derived[SourceQueryResolver]))
    .build()

  describe("Introspection Spec") {
    it("full introspection query") {
      val fullIntrospectionQuery = """query IntrospectionQuery {
                                     |  __schema {
                                     |    queryType { name }
                                     |    mutationType { name }
                                     |    subscriptionType { name }
                                     |    types {
                                     |      ...FullType
                                     |    }
                                     |    directives {
                                     |      name
                                     |      description
                                     |      locations
                                     |      args {
                                     |        ...InputValue
                                     |      }
                                     |    }
                                     |  }
                                     |}
                                     |
                                     |fragment FullType on __Type {
                                     |  kind
                                     |  name
                                     |  description
                                     |  fields(includeDeprecated: true) {
                                     |    name
                                     |    description
                                     |    args {
                                     |      ...InputValue
                                     |    }
                                     |    type {
                                     |      ...TypeRef
                                     |    }
                                     |    isDeprecated
                                     |    deprecationReason
                                     |  }
                                     |  inputFields {
                                     |    ...InputValue
                                     |  }
                                     |  interfaces {
                                     |    ...TypeRef
                                     |  }
                                     |  enumValues(includeDeprecated: true) {
                                     |    name
                                     |    description
                                     |    isDeprecated
                                     |    deprecationReason
                                     |  }
                                     |  possibleTypes {
                                     |    ...TypeRef
                                     |  }
                                     |}
                                     |
                                     |fragment InputValue on __InputValue {
                                     |  name
                                     |  description
                                     |  type { ...TypeRef }
                                     |  defaultValue
                                     |}
                                     |
                                     |fragment TypeRef on __Type {
                                     |  kind
                                     |  name
                                     |  ofType {
                                     |    kind
                                     |    name
                                     |    ofType {
                                     |      kind
                                     |      name
                                     |      ofType {
                                     |        kind
                                     |        name
                                     |        ofType {
                                     |          kind
                                     |          name
                                     |          ofType {
                                     |            kind
                                     |            name
                                     |            ofType {
                                     |              kind
                                     |              name
                                     |              ofType {
                                     |                kind
                                     |                name
                                     |              }
                                     |            }
                                     |          }
                                     |        }
                                     |      }
                                     |    }
                                     |  }
                                     |}""".stripMargin
      val res                    =
        Await.result(graphql.runWith(SymphonyQLRequest.newRequest().query(fullIntrospectionQuery).build()), 10.seconds)
      res.data.toString shouldEqual
        """{"__schema":{"queryType":{"name":"SourceQueryResolver"},"mutationType":null,"subscriptionType":null,"types":[{"kind":"SCALAR","name":"Boolean","description":null,"fields":null,"inputFields":null,"interfaces":null,"enumValues":null,"possibleTypes":null},{"kind":"OBJECT","name":"SourceQueryResolver","description":null,"fields":[{"name":"getUsers","description":null,"args":[{"name":"id","description":null,"type":{"kind":"NON_NULL","name":null,"ofType":{"kind":"SCALAR","name":"String","ofType":null}},"defaultValue":null}],"type":{"kind":"LIST","name":null,"ofType":{"kind":"NON_NULL","name":null,"ofType":{"kind":"OBJECT","name":"UserOutput","ofType":null}}},"isDeprecated":false,"deprecationReason":null}],"inputFields":null,"interfaces":[],"enumValues":null,"possibleTypes":null},{"kind":"SCALAR","name":"String","description":null,"fields":null,"inputFields":null,"interfaces":null,"enumValues":null,"possibleTypes":null},{"kind":"OBJECT","name":"UserOutput","description":null,"fields":[{"name":"id","description":null,"args":[],"type":{"kind":"NON_NULL","name":null,"ofType":{"kind":"SCALAR","name":"String","ofType":null}},"isDeprecated":false,"deprecationReason":null},{"name":"username","description":null,"args":[],"type":{"kind":"NON_NULL","name":null,"ofType":{"kind":"SCALAR","name":"String","ofType":null}},"isDeprecated":false,"deprecationReason":null}],"inputFields":null,"interfaces":[],"enumValues":null,"possibleTypes":null},{"kind":"OBJECT","name":"__Directive","description":null,"fields":[{"name":"name","description":null,"args":[],"type":{"kind":"NON_NULL","name":null,"ofType":{"kind":"SCALAR","name":"String","ofType":null}},"isDeprecated":false,"deprecationReason":null},{"name":"description","description":null,"args":[],"type":{"kind":"SCALAR","name":"String","ofType":null},"isDeprecated":false,"deprecationReason":null},{"name":"locations","description":null,"args":[],"type":{"kind":"NON_NULL","name":null,"ofType":{"kind":"LIST","name":null,"ofType":{"kind":"NON_NULL","name":null,"ofType":{"kind":"ENUM","name":"__DirectiveLocation","ofType":null}}}},"isDeprecated":false,"deprecationReason":null},{"name":"args","description":null,"args":[{"name":"includeDeprecated","description":null,"type":{"kind":"SCALAR","name":"Boolean","ofType":null},"defaultValue":null}],"type":{"kind":"NON_NULL","name":null,"ofType":{"kind":"LIST","name":null,"ofType":{"kind":"NON_NULL","name":null,"ofType":{"kind":"OBJECT","name":"__InputValue","ofType":null}}}},"isDeprecated":false,"deprecationReason":null},{"name":"isRepeatable","description":null,"args":[],"type":{"kind":"NON_NULL","name":null,"ofType":{"kind":"SCALAR","name":"Boolean","ofType":null}},"isDeprecated":false,"deprecationReason":null}],"inputFields":null,"interfaces":[],"enumValues":null,"possibleTypes":null},{"kind":"ENUM","name":"__DirectiveLocation","description":null,"fields":null,"inputFields":null,"interfaces":null,"enumValues":[{"name":"ARGUMENT_DEFINITION","description":null,"isDeprecated":false,"deprecationReason":null},{"name":"ENUM","description":null,"isDeprecated":false,"deprecationReason":null},{"name":"ENUM_VALUE","description":null,"isDeprecated":false,"deprecationReason":null},{"name":"FIELD","description":null,"isDeprecated":false,"deprecationReason":null},{"name":"FIELD_DEFINITION","description":null,"isDeprecated":false,"deprecationReason":null},{"name":"FRAGMENT_DEFINITION","description":null,"isDeprecated":false,"deprecationReason":null},{"name":"FRAGMENT_SPREAD","description":null,"isDeprecated":false,"deprecationReason":null},{"name":"INLINE_FRAGMENT","description":null,"isDeprecated":false,"deprecationReason":null},{"name":"INPUT_FIELD_DEFINITION","description":null,"isDeprecated":false,"deprecationReason":null},{"name":"INPUT_OBJECT","description":null,"isDeprecated":false,"deprecationReason":null},{"name":"INTERFACE","description":null,"isDeprecated":false,"deprecationReason":null},{"name":"MUTATION","description":null,"isDeprecated":false,"deprecationReason":null},{"name":"OBJECT","description":null,"isDeprecated":false,"deprecationReason":null},{"name":"QUERY","description":null,"isDeprecated":false,"deprecationReason":null},{"name":"SCALAR","description":null,"isDeprecated":false,"deprecationReason":null},{"name":"SCHEMA","description":null,"isDeprecated":false,"deprecationReason":null},{"name":"SUBSCRIPTION","description":null,"isDeprecated":false,"deprecationReason":null},{"name":"UNION","description":null,"isDeprecated":false,"deprecationReason":null},{"name":"VARIABLE_DEFINITION","description":null,"isDeprecated":false,"deprecationReason":null}],"possibleTypes":null},{"kind":"OBJECT","name":"__EnumValue","description":null,"fields":[{"name":"name","description":null,"args":[],"type":{"kind":"NON_NULL","name":null,"ofType":{"kind":"SCALAR","name":"String","ofType":null}},"isDeprecated":false,"deprecationReason":null},{"name":"description","description":null,"args":[],"type":{"kind":"SCALAR","name":"String","ofType":null},"isDeprecated":false,"deprecationReason":null},{"name":"isDeprecated","description":null,"args":[],"type":{"kind":"NON_NULL","name":null,"ofType":{"kind":"SCALAR","name":"Boolean","ofType":null}},"isDeprecated":false,"deprecationReason":null},{"name":"deprecationReason","description":null,"args":[],"type":{"kind":"SCALAR","name":"String","ofType":null},"isDeprecated":false,"deprecationReason":null}],"inputFields":null,"interfaces":[],"enumValues":null,"possibleTypes":null},{"kind":"OBJECT","name":"__Field","description":null,"fields":[{"name":"name","description":null,"args":[],"type":{"kind":"NON_NULL","name":null,"ofType":{"kind":"SCALAR","name":"String","ofType":null}},"isDeprecated":false,"deprecationReason":null},{"name":"description","description":null,"args":[],"type":{"kind":"SCALAR","name":"String","ofType":null},"isDeprecated":false,"deprecationReason":null},{"name":"args","description":null,"args":[{"name":"includeDeprecated","description":null,"type":{"kind":"SCALAR","name":"Boolean","ofType":null},"defaultValue":null}],"type":{"kind":"NON_NULL","name":null,"ofType":{"kind":"LIST","name":null,"ofType":{"kind":"NON_NULL","name":null,"ofType":{"kind":"OBJECT","name":"__InputValue","ofType":null}}}},"isDeprecated":false,"deprecationReason":null},{"name":"type","description":null,"args":[],"type":{"kind":"NON_NULL","name":null,"ofType":{"kind":"OBJECT","name":"__Type","ofType":null}},"isDeprecated":false,"deprecationReason":null},{"name":"isDeprecated","description":null,"args":[],"type":{"kind":"NON_NULL","name":null,"ofType":{"kind":"SCALAR","name":"Boolean","ofType":null}},"isDeprecated":false,"deprecationReason":null},{"name":"deprecationReason","description":null,"args":[],"type":{"kind":"SCALAR","name":"String","ofType":null},"isDeprecated":false,"deprecationReason":null}],"inputFields":null,"interfaces":[],"enumValues":null,"possibleTypes":null},{"kind":"OBJECT","name":"__InputValue","description":null,"fields":[{"name":"name","description":null,"args":[],"type":{"kind":"NON_NULL","name":null,"ofType":{"kind":"SCALAR","name":"String","ofType":null}},"isDeprecated":false,"deprecationReason":null},{"name":"description","description":null,"args":[],"type":{"kind":"SCALAR","name":"String","ofType":null},"isDeprecated":false,"deprecationReason":null},{"name":"type","description":null,"args":[],"type":{"kind":"NON_NULL","name":null,"ofType":{"kind":"OBJECT","name":"__Type","ofType":null}},"isDeprecated":false,"deprecationReason":null},{"name":"defaultValue","description":null,"args":[],"type":{"kind":"SCALAR","name":"String","ofType":null},"isDeprecated":false,"deprecationReason":null},{"name":"isDeprecated","description":null,"args":[],"type":{"kind":"NON_NULL","name":null,"ofType":{"kind":"SCALAR","name":"Boolean","ofType":null}},"isDeprecated":false,"deprecationReason":null},{"name":"deprecationReason","description":null,"args":[],"type":{"kind":"SCALAR","name":"String","ofType":null},"isDeprecated":false,"deprecationReason":null}],"inputFields":null,"interfaces":[],"enumValues":null,"possibleTypes":null},{"kind":"OBJECT","name":"__Schema","description":null,"fields":[{"name":"description","description":null,"args":[],"type":{"kind":"SCALAR","name":"String","ofType":null},"isDeprecated":false,"deprecationReason":null},{"name":"types","description":null,"args":[],"type":{"kind":"NON_NULL","name":null,"ofType":{"kind":"LIST","name":null,"ofType":{"kind":"NON_NULL","name":null,"ofType":{"kind":"OBJECT","name":"__Type","ofType":null}}}},"isDeprecated":false,"deprecationReason":null},{"name":"queryType","description":null,"args":[],"type":{"kind":"NON_NULL","name":null,"ofType":{"kind":"OBJECT","name":"__Type","ofType":null}},"isDeprecated":false,"deprecationReason":null},{"name":"mutationType","description":null,"args":[],"type":{"kind":"OBJECT","name":"__Type","ofType":null},"isDeprecated":false,"deprecationReason":null},{"name":"subscriptionType","description":null,"args":[],"type":{"kind":"OBJECT","name":"__Type","ofType":null},"isDeprecated":false,"deprecationReason":null},{"name":"directives","description":null,"args":[],"type":{"kind":"NON_NULL","name":null,"ofType":{"kind":"LIST","name":null,"ofType":{"kind":"NON_NULL","name":null,"ofType":{"kind":"OBJECT","name":"__Directive","ofType":null}}}},"isDeprecated":false,"deprecationReason":null}],"inputFields":null,"interfaces":[],"enumValues":null,"possibleTypes":null},{"kind":"OBJECT","name":"__Type","description":null,"fields":[{"name":"kind","description":null,"args":[],"type":{"kind":"NON_NULL","name":null,"ofType":{"kind":"ENUM","name":"__TypeKind","ofType":null}},"isDeprecated":false,"deprecationReason":null},{"name":"name","description":null,"args":[],"type":{"kind":"SCALAR","name":"String","ofType":null},"isDeprecated":false,"deprecationReason":null},{"name":"description","description":null,"args":[],"type":{"kind":"SCALAR","name":"String","ofType":null},"isDeprecated":false,"deprecationReason":null},{"name":"fields","description":null,"args":[{"name":"includeDeprecated","description":null,"type":{"kind":"SCALAR","name":"Boolean","ofType":null},"defaultValue":null}],"type":{"kind":"LIST","name":null,"ofType":{"kind":"NON_NULL","name":null,"ofType":{"kind":"OBJECT","name":"__Field","ofType":null}}},"isDeprecated":false,"deprecationReason":null},{"name":"interfaces","description":null,"args":[],"type":{"kind":"LIST","name":null,"ofType":{"kind":"NON_NULL","name":null,"ofType":{"kind":"OBJECT","name":"__Type","ofType":null}}},"isDeprecated":false,"deprecationReason":null},{"name":"possibleTypes","description":null,"args":[],"type":{"kind":"LIST","name":null,"ofType":{"kind":"NON_NULL","name":null,"ofType":{"kind":"OBJECT","name":"__Type","ofType":null}}},"isDeprecated":false,"deprecationReason":null},{"name":"enumValues","description":null,"args":[{"name":"includeDeprecated","description":null,"type":{"kind":"SCALAR","name":"Boolean","ofType":null},"defaultValue":null}],"type":{"kind":"LIST","name":null,"ofType":{"kind":"NON_NULL","name":null,"ofType":{"kind":"OBJECT","name":"__EnumValue","ofType":null}}},"isDeprecated":false,"deprecationReason":null},{"name":"inputFields","description":null,"args":[{"name":"includeDeprecated","description":null,"type":{"kind":"SCALAR","name":"Boolean","ofType":null},"defaultValue":null}],"type":{"kind":"LIST","name":null,"ofType":{"kind":"NON_NULL","name":null,"ofType":{"kind":"OBJECT","name":"__InputValue","ofType":null}}},"isDeprecated":false,"deprecationReason":null},{"name":"ofType","description":null,"args":[],"type":{"kind":"OBJECT","name":"__Type","ofType":null},"isDeprecated":false,"deprecationReason":null},{"name":"specifiedBy","description":null,"args":[],"type":{"kind":"SCALAR","name":"String","ofType":null},"isDeprecated":false,"deprecationReason":null}],"inputFields":null,"interfaces":[],"enumValues":null,"possibleTypes":null},{"kind":"ENUM","name":"__TypeKind","description":null,"fields":null,"inputFields":null,"interfaces":null,"enumValues":[{"name":"ENUM","description":null,"isDeprecated":false,"deprecationReason":null},{"name":"INPUT_OBJECT","description":null,"isDeprecated":false,"deprecationReason":null},{"name":"INTERFACE","description":null,"isDeprecated":false,"deprecationReason":null},{"name":"LIST","description":null,"isDeprecated":false,"deprecationReason":null},{"name":"NON_NULL","description":null,"isDeprecated":false,"deprecationReason":null},{"name":"OBJECT","description":null,"isDeprecated":false,"deprecationReason":null},{"name":"SCALAR","description":null,"isDeprecated":false,"deprecationReason":null},{"name":"UNION","description":null,"isDeprecated":false,"deprecationReason":null}],"possibleTypes":null}],"directives":[{"name":"skip","description":"The @skip directive may be provided for fields, fragment spreads, and inline fragments, and allows for conditional exclusion during execution as described by the if argument.","locations":["FIELD","FRAGMENT_SPREAD","INLINE_FRAGMENT"],"args":[{"name":"if","description":null,"type":{"kind":"NON_NULL","name":null,"ofType":{"kind":"SCALAR","name":"Boolean","ofType":null}},"defaultValue":null}]},{"name":"include","description":"The @include directive may be provided for fields, fragment spreads, and inline fragments, and allows for conditional inclusion during execution as described by the if argument.","locations":["FIELD","FRAGMENT_SPREAD","INLINE_FRAGMENT"],"args":[{"name":"if","description":null,"type":{"kind":"NON_NULL","name":null,"ofType":{"kind":"SCALAR","name":"Boolean","ofType":null}},"defaultValue":null}]},{"name":"specifiedBy","description":"The @specifiedBy directive is used within the type system definition language to provide a URL for specifying the behavior of custom scalar types. The URL should point to a human-readable specification of the data format, serialization, and coercion rules. It must not appear on built-in scalar types.","locations":["SCALAR"],"args":[{"name":"url","description":null,"type":{"kind":"NON_NULL","name":null,"ofType":{"kind":"SCALAR","name":"String","ofType":null}},"defaultValue":null}]}]}}""".stripMargin
    }

    it("introspect type") {
      val introspectTypeQuery = """
              query {
                __type(name: "UserOutput") {
                  name
                }
              }
            """
      val res                 =
        Await.result(graphql.runWith(SymphonyQLRequest.newRequest().query(introspectTypeQuery).build()), 10.seconds)
      res.data.toString shouldEqual """{"__type":{"name":"UserOutput"}}"""
    }

    it("introspect non-existent type") {
      val introspectTypeQuery = """
              query {
                __type(name: "__NonExistent") {
                  name
                }
              }
            """
      val res                 =
        Await.result(graphql.runWith(SymphonyQLRequest.newRequest().query(introspectTypeQuery).build()), 10.seconds)
      res.data.toString shouldEqual """{"__type":null}"""
    }
  }
}
