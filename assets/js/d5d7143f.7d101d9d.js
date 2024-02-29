"use strict";(self.webpackChunkwebsite=self.webpackChunkwebsite||[]).push([[379],{3905:(e,t,n)=>{n.d(t,{Zo:()=>m,kt:()=>h});var a=n(7294);function r(e,t,n){return t in e?Object.defineProperty(e,t,{value:n,enumerable:!0,configurable:!0,writable:!0}):e[t]=n,e}function i(e,t){var n=Object.keys(e);if(Object.getOwnPropertySymbols){var a=Object.getOwnPropertySymbols(e);t&&(a=a.filter((function(t){return Object.getOwnPropertyDescriptor(e,t).enumerable}))),n.push.apply(n,a)}return n}function o(e){for(var t=1;t<arguments.length;t++){var n=null!=arguments[t]?arguments[t]:{};t%2?i(Object(n),!0).forEach((function(t){r(e,t,n[t])})):Object.getOwnPropertyDescriptors?Object.defineProperties(e,Object.getOwnPropertyDescriptors(n)):i(Object(n)).forEach((function(t){Object.defineProperty(e,t,Object.getOwnPropertyDescriptor(n,t))}))}return e}function p(e,t){if(null==e)return{};var n,a,r=function(e,t){if(null==e)return{};var n,a,r={},i=Object.keys(e);for(a=0;a<i.length;a++)n=i[a],t.indexOf(n)>=0||(r[n]=e[n]);return r}(e,t);if(Object.getOwnPropertySymbols){var i=Object.getOwnPropertySymbols(e);for(a=0;a<i.length;a++)n=i[a],t.indexOf(n)>=0||Object.prototype.propertyIsEnumerable.call(e,n)&&(r[n]=e[n])}return r}var c=a.createContext({}),l=function(e){var t=a.useContext(c),n=t;return e&&(n="function"==typeof e?e(t):o(o({},t),e)),n},m=function(e){var t=l(e.components);return a.createElement(c.Provider,{value:t},e.children)},s="mdxType",u={inlineCode:"code",wrapper:function(e){var t=e.children;return a.createElement(a.Fragment,{},t)}},g=a.forwardRef((function(e,t){var n=e.components,r=e.mdxType,i=e.originalType,c=e.parentName,m=p(e,["components","mdxType","originalType","parentName"]),s=l(n),g=r,h=s["".concat(c,".").concat(g)]||s[g]||u[g]||i;return n?a.createElement(h,o(o({ref:t},m),{},{components:n})):a.createElement(h,o({ref:t},m))}));function h(e,t){var n=arguments,r=t&&t.mdxType;if("string"==typeof e||r){var i=n.length,o=new Array(i);o[0]=g;var p={};for(var c in t)hasOwnProperty.call(t,c)&&(p[c]=t[c]);p.originalType=e,p[s]="string"==typeof e?e:r,o[1]=p;for(var l=2;l<i;l++)o[l]=n[l];return a.createElement.apply(null,o)}return a.createElement.apply(null,n)}g.displayName="MDXCreateElement"},1284:(e,t,n)=>{n.r(t),n.d(t,{assets:()=>c,contentTitle:()=>o,default:()=>u,frontMatter:()=>i,metadata:()=>p,toc:()=>l});var a=n(7462),r=(n(7294),n(3905));const i={title:"Defining the Schema (Java)",sidebar_label:"Defining the Schema (Java)",custom_edit_url:"https://github.com/SymphonyQL/SymphonyQL/edit/master/docs/schema-java.md"},o=void 0,p={unversionedId:"schema-java",id:"schema-java",title:"Defining the Schema (Java)",description:"In Java, there is no metaprogramming, we use APT (Java Annotation Processing) to generate codes.",source:"@site/../mdoc/target/mdoc/schema-java.md",sourceDirName:".",slug:"/schema-java",permalink:"/SymphonyQL/docs/schema-java",draft:!1,editUrl:"https://github.com/SymphonyQL/SymphonyQL/edit/master/docs/schema-java.md",tags:[],version:"current",frontMatter:{title:"Defining the Schema (Java)",sidebar_label:"Defining the Schema (Java)",custom_edit_url:"https://github.com/SymphonyQL/SymphonyQL/edit/master/docs/schema-java.md"},sidebar:"someSidebar",previous:{title:"Installing",permalink:"/SymphonyQL/docs/installation"},next:{title:"Schema Specification",permalink:"/SymphonyQL/docs/schema"}},c={},l=[{value:"@EnumSchema",id:"enumschema",level:2},{value:"@InputSchema",id:"inputschema",level:2},{value:"@ObjectSchema",id:"objectschema",level:2},{value:"@IgnoreSchema",id:"ignoreschema",level:2}],m={toc:l},s="wrapper";function u(e){let{components:t,...n}=e;return(0,r.kt)(s,(0,a.Z)({},m,n,{components:t,mdxType:"MDXLayout"}),(0,r.kt)("p",null,"In Java, there is no metaprogramming, we use APT (Java Annotation Processing) to generate codes."),(0,r.kt)("h2",{id:"enumschema"},"@EnumSchema"),(0,r.kt)("p",null,"Defining SymphonyQL ",(0,r.kt)("strong",{parentName:"p"},"Enum Type"),", for example:"),(0,r.kt)("pre",null,(0,r.kt)("code",{parentName:"pre",className:"language-java"},"@EnumSchema\nenum OriginEnum {\n    EARTH, MARS, BELT\n}\n")),(0,r.kt)("p",null,"The enumeration used in ",(0,r.kt)("strong",{parentName:"p"},"Input Object Type")," must be annotated with ",(0,r.kt)("inlineCode",{parentName:"p"},"@ArgExtractor"),"."),(0,r.kt)("p",null,"It is equivalent to the GraphQL Enum Type:"),(0,r.kt)("pre",null,(0,r.kt)("code",{parentName:"pre",className:"language-graphql"},"enum Origin {\n  EARTH\n  MARS\n  BELT\n}\n")),(0,r.kt)("h2",{id:"inputschema"},"@InputSchema"),(0,r.kt)("p",null,"Defining SymphonyQL ",(0,r.kt)("strong",{parentName:"p"},"Input Object Type"),", for example:"),(0,r.kt)("pre",null,(0,r.kt)("code",{parentName:"pre",className:"language-java"},"@InputSchema\n@ArgExtractor\nrecord FilterArgs(Optional<Origin> origin, Optional<NestedArg> nestedArg) {\n}\n")),(0,r.kt)("p",null,"Any custom type (including enumeration) used for ",(0,r.kt)("strong",{parentName:"p"},"Input Object Type")," needs to be annotated with ",(0,r.kt)("inlineCode",{parentName:"p"},"ArgExtractor"),"."),(0,r.kt)("p",null,"As mentioned above, ",(0,r.kt)("inlineCode",{parentName:"p"},"NestedArg")," are used in ",(0,r.kt)("strong",{parentName:"p"},"Input Object Type"),", to generate the correct ",(0,r.kt)("strong",{parentName:"p"},"Input Object Type"),",\nit is necessary to define ",(0,r.kt)("inlineCode",{parentName:"p"},"NestedArg")," and add ",(0,r.kt)("inlineCode",{parentName:"p"},"@InputSchema")," and ",(0,r.kt)("inlineCode",{parentName:"p"},"@ArgExtractor"),", for example:"),(0,r.kt)("pre",null,(0,r.kt)("code",{parentName:"pre",className:"language-java"},"@InputSchema\n@ArgExtractor\nrecord NestedArg(String id, Optional<String> name) {\n}\n")),(0,r.kt)("p",null,"It is equivalent to the GraphQL Input Type:"),(0,r.kt)("pre",null,(0,r.kt)("code",{parentName:"pre",className:"language-graphql"},"input NestedArgInput {\n    id: String!\n    name: String\n}\n")),(0,r.kt)("h2",{id:"objectschema"},"@ObjectSchema"),(0,r.kt)("p",null,"Defining SymphonyQL ",(0,r.kt)("strong",{parentName:"p"},"Object Type"),"."),(0,r.kt)("p",null,"It has one argument ",(0,r.kt)("inlineCode",{parentName:"p"},"withArgs"),", which defaults to false, for example:"),(0,r.kt)("pre",null,(0,r.kt)("code",{parentName:"pre",className:"language-java"},"@ObjectSchema\nrecord CharacterOutput(String name, Origin origin) {\n}\n")),(0,r.kt)("p",null,"It is equivalent to the GraphQL Object Type:"),(0,r.kt)("pre",null,(0,r.kt)("code",{parentName:"pre",className:"language-graphql"},"type CharacterOutput {\n  name: String!\n  origin: Origin!\n}\n")),(0,r.kt)("p",null,"When defining a ",(0,r.kt)("strong",{parentName:"p"},"Resolver")," Object, ",(0,r.kt)("inlineCode",{parentName:"p"},"withArgs")," must be ",(0,r.kt)("inlineCode",{parentName:"p"},"true"),", for example:"),(0,r.kt)("pre",null,(0,r.kt)("code",{parentName:"pre",className:"language-java"},"@ObjectSchema(withArgs = true)\nrecord Queries(Function<FilterArgs, Source<CharacterOutput, NotUsed>> characters) {\n}\n")),(0,r.kt)("p",null,"It is equivalent to the GraphQL Object Type:"),(0,r.kt)("pre",null,(0,r.kt)("code",{parentName:"pre",className:"language-graphql"},"# There is no FilterArgs, but it has all its fields: origin, nestedArg\ntype Queries {\n    characters(origin: Origin, nestedArg: NestedArgInput): [CharacterOutput!]\n}\n")),(0,r.kt)("h2",{id:"ignoreschema"},"@IgnoreSchema"),(0,r.kt)("p",null,"Ignore class from SymphonyQL's processing."))}u.isMDXComponent=!0}}]);