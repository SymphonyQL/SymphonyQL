"use strict";(self.webpackChunkwebsite=self.webpackChunkwebsite||[]).push([[624],{3905:(e,n,t)=>{t.d(n,{Zo:()=>p,kt:()=>y});var r=t(7294);function a(e,n,t){return n in e?Object.defineProperty(e,n,{value:t,enumerable:!0,configurable:!0,writable:!0}):e[n]=t,e}function i(e,n){var t=Object.keys(e);if(Object.getOwnPropertySymbols){var r=Object.getOwnPropertySymbols(e);n&&(r=r.filter((function(n){return Object.getOwnPropertyDescriptor(e,n).enumerable}))),t.push.apply(t,r)}return t}function o(e){for(var n=1;n<arguments.length;n++){var t=null!=arguments[n]?arguments[n]:{};n%2?i(Object(t),!0).forEach((function(n){a(e,n,t[n])})):Object.getOwnPropertyDescriptors?Object.defineProperties(e,Object.getOwnPropertyDescriptors(t)):i(Object(t)).forEach((function(n){Object.defineProperty(e,n,Object.getOwnPropertyDescriptor(t,n))}))}return e}function c(e,n){if(null==e)return{};var t,r,a=function(e,n){if(null==e)return{};var t,r,a={},i=Object.keys(e);for(r=0;r<i.length;r++)t=i[r],n.indexOf(t)>=0||(a[t]=e[t]);return a}(e,n);if(Object.getOwnPropertySymbols){var i=Object.getOwnPropertySymbols(e);for(r=0;r<i.length;r++)t=i[r],n.indexOf(t)>=0||Object.prototype.propertyIsEnumerable.call(e,t)&&(a[t]=e[t])}return a}var l=r.createContext({}),s=function(e){var n=r.useContext(l),t=n;return e&&(t="function"==typeof e?e(n):o(o({},n),e)),t},p=function(e){var n=s(e.components);return r.createElement(l.Provider,{value:n},e.children)},u="mdxType",m={inlineCode:"code",wrapper:function(e){var n=e.children;return r.createElement(r.Fragment,{},n)}},g=r.forwardRef((function(e,n){var t=e.components,a=e.mdxType,i=e.originalType,l=e.parentName,p=c(e,["components","mdxType","originalType","parentName"]),u=s(t),g=a,y=u["".concat(l,".").concat(g)]||u[g]||m[g]||i;return t?r.createElement(y,o(o({ref:n},p),{},{components:t})):r.createElement(y,o({ref:n},p))}));function y(e,n){var t=arguments,a=n&&n.mdxType;if("string"==typeof e||a){var i=t.length,o=new Array(i);o[0]=g;var c={};for(var l in n)hasOwnProperty.call(n,l)&&(c[l]=n[l]);c.originalType=e,c[u]="string"==typeof e?e:a,o[1]=c;for(var s=2;s<i;s++)o[s]=t[s];return r.createElement.apply(null,o)}return r.createElement.apply(null,t)}g.displayName="MDXCreateElement"},4788:(e,n,t)=>{t.r(n),t.d(n,{assets:()=>l,contentTitle:()=>o,default:()=>m,frontMatter:()=>i,metadata:()=>c,toc:()=>s});var r=t(7462),a=(t(7294),t(3905));const i={},o="Quick Start",c={unversionedId:"quickstart",id:"quickstart",title:"Quick Start",description:"Assuming we want to develop an application for the GraphQL Schema below:",source:"@site/../mdoc/target/mdoc/quickstart.md",sourceDirName:".",slug:"/quickstart",permalink:"/SymphonyQL/docs/quickstart",draft:!1,tags:[],version:"current",frontMatter:{},sidebar:"tutorialSidebar",next:{title:"Java Schema Annotation",permalink:"/SymphonyQL/docs/schema-apt"}},l={},s=[{value:"Java21 Example",id:"java21-example",level:2},{value:"Scala3 Example",id:"scala3-example",level:2}],p={toc:s},u="wrapper";function m(e){let{components:n,...t}=e;return(0,a.kt)(u,(0,r.Z)({},p,t,{components:n,mdxType:"MDXLayout"}),(0,a.kt)("h1",{id:"quick-start"},"Quick Start"),(0,a.kt)("p",null,"Assuming we want to develop an application for the GraphQL Schema below:"),(0,a.kt)("pre",null,(0,a.kt)("code",{parentName:"pre",className:"language-graphql"},"schema {\n  query: Queries\n}\n\nenum Origin {\n  EARTH\n  MARS\n  BELT\n}\n\ninput NestedArgInput {\n  id: String!\n  name: String\n}\n\ntype CharacterOutput {\n  name: String!\n  origin: Origin!\n}\n\ntype Queries {\n  characters(origin: Origin, nestedArg: NestedArgInput): [CharacterOutput!]\n}\n")),(0,a.kt)("h2",{id:"java21-example"},"Java21 Example"),(0,a.kt)("p",null,"SymphonyQL uses APT (Annotation Processing Tool) to automatically generate schema during compilation.\nTherefore, you only need to write ",(0,a.kt)("strong",{parentName:"p"},"record class")," to define the schema:"),(0,a.kt)("pre",null,(0,a.kt)("code",{parentName:"pre",className:"language-java"},"@ObjectSchema(withArgs = true)\nrecord Queries(Function<FilterArgs, Source<CharacterOutput, NotUsed>> characters) {\n}\n\n@ObjectSchema\nrecord CharacterOutput(String name, Origin origin) {\n}\n\n@InputSchema\n@ArgExtractor\nrecord FilterArgs(Optional<Origin> origin, Optional<NestedArg> nestedArg) {\n}\n\n@InputSchema\n@ArgExtractor\nrecord NestedArg(String id, Optional<String> name) {\n}\n\n@EnumSchema\n@ArgExtractor\nenum Origin {\n  EARTH,\n  MARS,\n  BELT\n}\n")),(0,a.kt)("p",null,"No need to write anything else, let's start developing the application now:"),(0,a.kt)("pre",null,(0,a.kt)("code",{parentName:"pre",className:"language-java"},'public static void main(String[] args) {\n    var graphql = SymphonyQL\n            .newSymphonyQL()\n            .addQuery(\n                    new Queries(\n                            args1 -> Source.single(new CharacterOutput("hello-" + args1.origin().map(Enum::toString).get(), args1.origin().get()))\n                    ),\n                    QueriesSchema.schema\n            )\n            .build();\n\n    var characters = """\n              {\n              characters(origin: "BELT") {\n                name\n                origin\n              }\n            }""";\n\n    final var actorSystem = ActorSystem.create("symphonyActorSystem");\n\n    var getRes = graphql.run(\n            SymphonyQLRequest.newRequest().query(Optional.of(characters)).build(),\n            actorSystem\n    );\n}\n')),(0,a.kt)("p",null,(0,a.kt)("inlineCode",{parentName:"p"},"QueriesSchema.schema")," is a static method automatically generated by APT."),(0,a.kt)("h2",{id:"scala3-example"},"Scala3 Example"),(0,a.kt)("p",null,"Similarly, in Scala, you only need to use ",(0,a.kt)("strong",{parentName:"p"},"case class")," to define the schema."),(0,a.kt)("pre",null,(0,a.kt)("code",{parentName:"pre",className:"language-scala"},"enum Origin {\n  case EARTH, MARS, BELT\n}\n\ncase class Character(name: String, origin: Origin)\ncase class FilterArgs(origin: Option[Origin])\ncase class NestedArg(id: String, name: Optional[String])\ncase class Queries(characters: FilterArgs => Source[Character, NotUsed])\n")),(0,a.kt)("p",null,"SymphonyQL automatically generates schema during compilation:"),(0,a.kt)("pre",null,(0,a.kt)("code",{parentName:"pre",className:"language-scala"},'def main(args: Array[String]): Unit = {\n    val graphql: SymphonyQL = SymphonyQL\n    .newSymphonyQL()\n    .addQuery(\n      Queries(args =>\n        Source.single(\n          Character("hello-" + args.origin.map(_.toString).getOrElse(""), args.origin.getOrElse(Origin.BELT))\n        )\n      ),\n      Schema.derived[Queries]\n    )\n    .build()\n    \n    val characters =\n    """{\n      |  characters(origin: "MARS") {\n      |    name\n      |    origin\n      |  }\n      |}""".stripMargin\n      \n    implicit val actorSystem: ActorSystem                   = ActorSystem("symphonyActorSystem")\n    val getRes: Future[SymphonyQLResponse[SymphonyQLError]] = graphql.runWith(SymphonyQLRequest(Some(characters)))\n}\n')),(0,a.kt)("p",null,(0,a.kt)("inlineCode",{parentName:"p"},"Schema.derived[Queries]")," is an inline call by metaprogramming."))}m.isMDXComponent=!0}}]);