"use strict";(self.webpackChunkwebsite=self.webpackChunkwebsite||[]).push([[141],{3905:(t,e,n)=>{n.d(e,{Zo:()=>m,kt:()=>c});var a=n(7294);function r(t,e,n){return e in t?Object.defineProperty(t,e,{value:n,enumerable:!0,configurable:!0,writable:!0}):t[e]=n,t}function l(t,e){var n=Object.keys(t);if(Object.getOwnPropertySymbols){var a=Object.getOwnPropertySymbols(t);e&&(a=a.filter((function(e){return Object.getOwnPropertyDescriptor(t,e).enumerable}))),n.push.apply(n,a)}return n}function i(t){for(var e=1;e<arguments.length;e++){var n=null!=arguments[e]?arguments[e]:{};e%2?l(Object(n),!0).forEach((function(e){r(t,e,n[e])})):Object.getOwnPropertyDescriptors?Object.defineProperties(t,Object.getOwnPropertyDescriptors(n)):l(Object(n)).forEach((function(e){Object.defineProperty(t,e,Object.getOwnPropertyDescriptor(n,e))}))}return t}function o(t,e){if(null==t)return{};var n,a,r=function(t,e){if(null==t)return{};var n,a,r={},l=Object.keys(t);for(a=0;a<l.length;a++)n=l[a],e.indexOf(n)>=0||(r[n]=t[n]);return r}(t,e);if(Object.getOwnPropertySymbols){var l=Object.getOwnPropertySymbols(t);for(a=0;a<l.length;a++)n=l[a],e.indexOf(n)>=0||Object.prototype.propertyIsEnumerable.call(t,n)&&(r[n]=t[n])}return r}var d=a.createContext({}),p=function(t){var e=a.useContext(d),n=e;return t&&(n="function"==typeof t?t(e):i(i({},e),t)),n},m=function(t){var e=p(t.components);return a.createElement(d.Provider,{value:e},t.children)},k="mdxType",u={inlineCode:"code",wrapper:function(t){var e=t.children;return a.createElement(a.Fragment,{},e)}},N=a.forwardRef((function(t,e){var n=t.components,r=t.mdxType,l=t.originalType,d=t.parentName,m=o(t,["components","mdxType","originalType","parentName"]),k=p(n),N=r,c=k["".concat(d,".").concat(N)]||k[N]||u[N]||l;return n?a.createElement(c,i(i({ref:e},m),{},{components:n})):a.createElement(c,i({ref:e},m))}));function c(t,e){var n=arguments,r=e&&e.mdxType;if("string"==typeof t||r){var l=n.length,i=new Array(l);i[0]=N;var o={};for(var d in e)hasOwnProperty.call(e,d)&&(o[d]=e[d]);o.originalType=t,o[k]="string"==typeof t?t:r,i[1]=o;for(var p=2;p<l;p++)i[p]=n[p];return a.createElement.apply(null,i)}return a.createElement.apply(null,n)}N.displayName="MDXCreateElement"},2777:(t,e,n)=>{n.r(e),n.d(e,{assets:()=>d,contentTitle:()=>i,default:()=>u,frontMatter:()=>l,metadata:()=>o,toc:()=>p});var a=n(7462),r=(n(7294),n(3905));const l={},i="Schema Specification",o={unversionedId:"schema",id:"schema",title:"Schema Specification",description:"A SymphonyQL schema will be derived automatically at compile-time from the types present in your resolver.",source:"@site/../mdoc/target/mdoc/schema.md",sourceDirName:".",slug:"/schema",permalink:"/SymphonyQL/docs/schema",draft:!1,tags:[],version:"current",frontMatter:{},sidebar:"tutorialSidebar",previous:{title:"Java Schema Annotation",permalink:"/SymphonyQL/docs/schema-apt"}},d={},p=[],m={toc:p},k="wrapper";function u(t){let{components:e,...n}=t;return(0,r.kt)(k,(0,a.Z)({},m,n,{components:e,mdxType:"MDXLayout"}),(0,r.kt)("h1",{id:"schema-specification"},"Schema Specification"),(0,r.kt)("p",null,"A SymphonyQL schema will be derived automatically at compile-time from the types present in your resolver."),(0,r.kt)("p",null,"The following table shows how to convert common Scala/Java types to SymphonyQL types."),(0,r.kt)("table",null,(0,r.kt)("thead",{parentName:"table"},(0,r.kt)("tr",{parentName:"thead"},(0,r.kt)("th",{parentName:"tr",align:null},"Scala Type (Java Type)"),(0,r.kt)("th",{parentName:"tr",align:null},"SymphonyQL Type"))),(0,r.kt)("tbody",{parentName:"table"},(0,r.kt)("tr",{parentName:"tbody"},(0,r.kt)("td",{parentName:"tr",align:null},(0,r.kt)("inlineCode",{parentName:"td"},"Boolean")," (",(0,r.kt)("inlineCode",{parentName:"td"},"boolean"),")"),(0,r.kt)("td",{parentName:"tr",align:null},"Boolean")),(0,r.kt)("tr",{parentName:"tbody"},(0,r.kt)("td",{parentName:"tr",align:null},(0,r.kt)("inlineCode",{parentName:"td"},"Int")," (",(0,r.kt)("inlineCode",{parentName:"td"},"int"),")"),(0,r.kt)("td",{parentName:"tr",align:null},"Int")),(0,r.kt)("tr",{parentName:"tbody"},(0,r.kt)("td",{parentName:"tr",align:null},(0,r.kt)("inlineCode",{parentName:"td"},"Short")," (",(0,r.kt)("inlineCode",{parentName:"td"},"short"),")"),(0,r.kt)("td",{parentName:"tr",align:null},"Int")),(0,r.kt)("tr",{parentName:"tbody"},(0,r.kt)("td",{parentName:"tr",align:null},(0,r.kt)("inlineCode",{parentName:"td"},"Float")," (",(0,r.kt)("inlineCode",{parentName:"td"},"float"),")"),(0,r.kt)("td",{parentName:"tr",align:null},"Float")),(0,r.kt)("tr",{parentName:"tbody"},(0,r.kt)("td",{parentName:"tr",align:null},(0,r.kt)("inlineCode",{parentName:"td"},"Double")," (",(0,r.kt)("inlineCode",{parentName:"td"},"double"),")"),(0,r.kt)("td",{parentName:"tr",align:null},"Float")),(0,r.kt)("tr",{parentName:"tbody"},(0,r.kt)("td",{parentName:"tr",align:null},(0,r.kt)("inlineCode",{parentName:"td"},"String")," (",(0,r.kt)("inlineCode",{parentName:"td"},"String"),")"),(0,r.kt)("td",{parentName:"tr",align:null},"String")),(0,r.kt)("tr",{parentName:"tbody"},(0,r.kt)("td",{parentName:"tr",align:null},(0,r.kt)("inlineCode",{parentName:"td"},"BigDecimal")," (",(0,r.kt)("inlineCode",{parentName:"td"},"BigDecimal"),")"),(0,r.kt)("td",{parentName:"tr",align:null},"BigDecimal (custom scalar)")),(0,r.kt)("tr",{parentName:"tbody"},(0,r.kt)("td",{parentName:"tr",align:null},(0,r.kt)("inlineCode",{parentName:"td"},"Unit")," (",(0,r.kt)("inlineCode",{parentName:"td"},"void"),")"),(0,r.kt)("td",{parentName:"tr",align:null},"Unit (custom scalar)")),(0,r.kt)("tr",{parentName:"tbody"},(0,r.kt)("td",{parentName:"tr",align:null},(0,r.kt)("inlineCode",{parentName:"td"},"Long")," (",(0,r.kt)("inlineCode",{parentName:"td"},"long"),")"),(0,r.kt)("td",{parentName:"tr",align:null},"Long (custom scalar)")),(0,r.kt)("tr",{parentName:"tbody"},(0,r.kt)("td",{parentName:"tr",align:null},(0,r.kt)("inlineCode",{parentName:"td"},"BigInt")," (",(0,r.kt)("inlineCode",{parentName:"td"},"BigInteger"),")"),(0,r.kt)("td",{parentName:"tr",align:null},"BigInt (custom scalar)")),(0,r.kt)("tr",{parentName:"tbody"},(0,r.kt)("td",{parentName:"tr",align:null},"Case Class (Record Class)"),(0,r.kt)("td",{parentName:"tr",align:null},"Object")),(0,r.kt)("tr",{parentName:"tbody"},(0,r.kt)("td",{parentName:"tr",align:null},"Enum Class (Enum Class)"),(0,r.kt)("td",{parentName:"tr",align:null},"Enum")),(0,r.kt)("tr",{parentName:"tbody"},(0,r.kt)("td",{parentName:"tr",align:null},(0,r.kt)("inlineCode",{parentName:"td"},"Option[A]")," (",(0,r.kt)("inlineCode",{parentName:"td"},"Optional[A]"),")"),(0,r.kt)("td",{parentName:"tr",align:null},"Nullable A")),(0,r.kt)("tr",{parentName:"tbody"},(0,r.kt)("td",{parentName:"tr",align:null},(0,r.kt)("inlineCode",{parentName:"td"},"List[A]")," (",(0,r.kt)("inlineCode",{parentName:"td"},"List[A]"),")"),(0,r.kt)("td",{parentName:"tr",align:null},"List of A")),(0,r.kt)("tr",{parentName:"tbody"},(0,r.kt)("td",{parentName:"tr",align:null},(0,r.kt)("inlineCode",{parentName:"td"},"Set[A]")," (",(0,r.kt)("inlineCode",{parentName:"td"},"Set[A]"),")"),(0,r.kt)("td",{parentName:"tr",align:null},"List of A")),(0,r.kt)("tr",{parentName:"tbody"},(0,r.kt)("td",{parentName:"tr",align:null},(0,r.kt)("inlineCode",{parentName:"td"},"Seq[A]")," (not have)"),(0,r.kt)("td",{parentName:"tr",align:null},"List of A")),(0,r.kt)("tr",{parentName:"tbody"},(0,r.kt)("td",{parentName:"tr",align:null},(0,r.kt)("inlineCode",{parentName:"td"},"Vector[A]")," (",(0,r.kt)("inlineCode",{parentName:"td"},"Vector[A]"),")"),(0,r.kt)("td",{parentName:"tr",align:null},"List of A")),(0,r.kt)("tr",{parentName:"tbody"},(0,r.kt)("td",{parentName:"tr",align:null},(0,r.kt)("inlineCode",{parentName:"td"},"A => B")," (",(0,r.kt)("inlineCode",{parentName:"td"},"Function[A, B]"),")"),(0,r.kt)("td",{parentName:"tr",align:null},"A and B")),(0,r.kt)("tr",{parentName:"tbody"},(0,r.kt)("td",{parentName:"tr",align:null},(0,r.kt)("inlineCode",{parentName:"td"},"() => A")," (",(0,r.kt)("inlineCode",{parentName:"td"},"Supplier[A]"),")"),(0,r.kt)("td",{parentName:"tr",align:null},"A")),(0,r.kt)("tr",{parentName:"tbody"},(0,r.kt)("td",{parentName:"tr",align:null},(0,r.kt)("inlineCode",{parentName:"td"},"Future[A]")," (",(0,r.kt)("inlineCode",{parentName:"td"},"CompletionStage[A]"),")"),(0,r.kt)("td",{parentName:"tr",align:null},"Nullable A")),(0,r.kt)("tr",{parentName:"tbody"},(0,r.kt)("td",{parentName:"tr",align:null},(0,r.kt)("inlineCode",{parentName:"td"},"Tuple2[A, B]")," (not have)"),(0,r.kt)("td",{parentName:"tr",align:null},"Object with 2 fields ",(0,r.kt)("inlineCode",{parentName:"td"},"_1")," and ",(0,r.kt)("inlineCode",{parentName:"td"},"_2"))),(0,r.kt)("tr",{parentName:"tbody"},(0,r.kt)("td",{parentName:"tr",align:null},(0,r.kt)("inlineCode",{parentName:"td"},"Either[A, B]")," (not have)"),(0,r.kt)("td",{parentName:"tr",align:null},"Object with 2 nullable fields ",(0,r.kt)("inlineCode",{parentName:"td"},"left")," and ",(0,r.kt)("inlineCode",{parentName:"td"},"right"))),(0,r.kt)("tr",{parentName:"tbody"},(0,r.kt)("td",{parentName:"tr",align:null},(0,r.kt)("inlineCode",{parentName:"td"},"Map[A, B]")," (",(0,r.kt)("inlineCode",{parentName:"td"},"Map[A, B]"),")"),(0,r.kt)("td",{parentName:"tr",align:null},"List of Object with 2 fields ",(0,r.kt)("inlineCode",{parentName:"td"},"key")," and ",(0,r.kt)("inlineCode",{parentName:"td"},"value"))),(0,r.kt)("tr",{parentName:"tbody"},(0,r.kt)("td",{parentName:"tr",align:null},"pekko-streams ",(0,r.kt)("inlineCode",{parentName:"td"},"scaladsl.Source[A, NotUsed]"),(0,r.kt)("br",null),"(",(0,r.kt)("inlineCode",{parentName:"td"},"javadsl.Source[A, NotUsed]"),")"),(0,r.kt)("td",{parentName:"tr",align:null},"A (subscription) or List of A (query, mutation)")))))}u.isMDXComponent=!0}}]);