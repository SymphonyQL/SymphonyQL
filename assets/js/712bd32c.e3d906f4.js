"use strict";(self.webpackChunkwebsite=self.webpackChunkwebsite||[]).push([[899],{3905:(n,e,t)=>{t.d(e,{Zo:()=>u,kt:()=>y});var a=t(7294);function o(n,e,t){return e in n?Object.defineProperty(n,e,{value:t,enumerable:!0,configurable:!0,writable:!0}):n[e]=t,n}function r(n,e){var t=Object.keys(n);if(Object.getOwnPropertySymbols){var a=Object.getOwnPropertySymbols(n);e&&(a=a.filter((function(e){return Object.getOwnPropertyDescriptor(n,e).enumerable}))),t.push.apply(t,a)}return t}function i(n){for(var e=1;e<arguments.length;e++){var t=null!=arguments[e]?arguments[e]:{};e%2?r(Object(t),!0).forEach((function(e){o(n,e,t[e])})):Object.getOwnPropertyDescriptors?Object.defineProperties(n,Object.getOwnPropertyDescriptors(t)):r(Object(t)).forEach((function(e){Object.defineProperty(n,e,Object.getOwnPropertyDescriptor(t,e))}))}return n}function l(n,e){if(null==n)return{};var t,a,o=function(n,e){if(null==n)return{};var t,a,o={},r=Object.keys(n);for(a=0;a<r.length;a++)t=r[a],e.indexOf(t)>=0||(o[t]=n[t]);return o}(n,e);if(Object.getOwnPropertySymbols){var r=Object.getOwnPropertySymbols(n);for(a=0;a<r.length;a++)t=r[a],e.indexOf(t)>=0||Object.prototype.propertyIsEnumerable.call(n,t)&&(o[t]=n[t])}return o}var s=a.createContext({}),p=function(n){var e=a.useContext(s),t=e;return n&&(t="function"==typeof n?n(e):i(i({},e),n)),t},u=function(n){var e=p(n.components);return a.createElement(s.Provider,{value:e},n.children)},c="mdxType",d={inlineCode:"code",wrapper:function(n){var e=n.children;return a.createElement(a.Fragment,{},e)}},m=a.forwardRef((function(n,e){var t=n.components,o=n.mdxType,r=n.originalType,s=n.parentName,u=l(n,["components","mdxType","originalType","parentName"]),c=p(t),m=o,y=c["".concat(s,".").concat(m)]||c[m]||d[m]||r;return t?a.createElement(y,i(i({ref:e},u),{},{components:t})):a.createElement(y,i({ref:e},u))}));function y(n,e){var t=arguments,o=e&&e.mdxType;if("string"==typeof n||o){var r=t.length,i=new Array(r);i[0]=m;var l={};for(var s in e)hasOwnProperty.call(e,s)&&(l[s]=e[s]);l.originalType=n,l[c]="string"==typeof n?n:o,i[1]=l;for(var p=2;p<r;p++)i[p]=t[p];return a.createElement.apply(null,i)}return a.createElement.apply(null,t)}m.displayName="MDXCreateElement"},7194:(n,e,t)=>{t.r(e),t.d(e,{assets:()=>s,contentTitle:()=>i,default:()=>d,frontMatter:()=>r,metadata:()=>l,toc:()=>p});var a=t(7462),o=(t(7294),t(3905));const r={title:"Installing SymphonyQL",sidebar_label:"Installing",custom_edit_url:"https://github.com/SymphonyQL/SymphonyQL/edit/master/docs/installation.md"},i=void 0,l={unversionedId:"installation",id:"installation",title:"Installing SymphonyQL",description:"SymphonyQL currently only supports Scala3 and Java21, but can be extended to other versions.",source:"@site/../mdoc/target/mdoc/installation.md",sourceDirName:".",slug:"/installation",permalink:"/SymphonyQL/docs/installation",draft:!1,editUrl:"https://github.com/SymphonyQL/SymphonyQL/edit/master/docs/installation.md",tags:[],version:"current",frontMatter:{title:"Installing SymphonyQL",sidebar_label:"Installing",custom_edit_url:"https://github.com/SymphonyQL/SymphonyQL/edit/master/docs/installation.md"},sidebar:"someSidebar",previous:{title:"Quick Start - Scala",permalink:"/SymphonyQL/docs/quickstart-scala"},next:{title:"Defining the Schema - Java",permalink:"/SymphonyQL/docs/schema-java"}},s={},p=[{value:"Installation using SBT",id:"installation-using-sbt",level:2},{value:"Installation using Maven",id:"installation-using-maven",level:2}],u={toc:p},c="wrapper";function d(n){let{components:e,...t}=n;return(0,o.kt)(c,(0,a.Z)({},u,t,{components:e,mdxType:"MDXLayout"}),(0,o.kt)("p",null,"SymphonyQL currently only supports ",(0,o.kt)("strong",{parentName:"p"},"Scala3")," and ",(0,o.kt)("strong",{parentName:"p"},"Java21"),", but can be extended to other versions."),(0,o.kt)("p",null,(0,o.kt)("inlineCode",{parentName:"p"},"io.github.jxnu-liguobin")," is a temporary location used for artifact publishing."),(0,o.kt)("h2",{id:"installation-using-sbt"},"Installation using SBT"),(0,o.kt)("p",null,"If you are building with sbt, add the following to your ",(0,o.kt)("inlineCode",{parentName:"p"},"project/plugins.sbt"),":"),(0,o.kt)("pre",null,(0,o.kt)("code",{parentName:"pre",className:"language-scala"},'libraryDependencies ++= Seq(\n  "io.github.jxnu-liguobin" %% "symphony-core" % "<version>",\n  // a default http-server provided by pekko-http\n  "io.github.jxnu-liguobin" %% "symphony-server" % "<version>"\n)\n')),(0,o.kt)("p",null,"If you want to develop SymphonyQL application using Java, you also need to add ",(0,o.kt)("inlineCode",{parentName:"p"},"symphony-java-apt")," and some settings."),(0,o.kt)("p",null,"Here is a complete configuration:"),(0,o.kt)("pre",null,(0,o.kt)("code",{parentName:"pre",className:"language-scala"},'Compile / unmanagedSourceDirectories += (Compile / crossTarget).value / "src_managed"\nlibraryDependencies ++= Seq(\n  "io.github.jxnu-liguobin" %% "symphony-core" % "<version>",\n  "io.github.jxnu-liguobin" %% "symphony-server" % "<version>",\n  "io.github.jxnu-liguobin" %% "symphony-java-apt" % "<version>",\n  "javax.annotation" % "javax.annotation-api" % "<version>"\n)\nCompile / javacOptions ++= Seq(\n  "-processor",\n  "symphony.apt.SymphonyQLProcessor",\n  "-s",\n  ((Compile / crossTarget).value / "src_managed").getAbsolutePath\n)\n')),(0,o.kt)("p",null,"APT and this setting are unique to Java and are not required in Scala."),(0,o.kt)("h2",{id:"installation-using-maven"},"Installation using Maven"),(0,o.kt)("p",null,"If you are building with maven, add the following to your ",(0,o.kt)("inlineCode",{parentName:"p"},"pom.xml"),":"),(0,o.kt)("pre",null,(0,o.kt)("code",{parentName:"pre",className:"language-xml"},"<properties>\n    <symphonyql.version>version</symphonyql.version>\n    <javax.annotation.api.version>1.3.2</javax.annotation.api.version>\n    <maven.compiler.plugin.version>3.7.0</maven.compiler.plugin.version>\n</properties>\n\n<dependencies>\n    <dependency>\n        <groupId>io.github.jxnu-liguobin</groupId>\n        <artifactId>symphony-core_3</artifactId>\n        <version>${symphonyql.version}</version>\n    </dependency>\n    <dependency>\n        <groupId>io.github.jxnu-liguobin</groupId>\n        <artifactId>symphony-server_3</artifactId>\n        <version>${symphonyql.version}</version>\n    </dependency>\n    <dependency>\n        <groupId>javax.annotation</groupId>\n        <artifactId>javax.annotation-api</artifactId>\n        <version>${javax.annotation.api.version}</version>\n    </dependency>\n</dependencies>\n\n<build>\n    <plugins>\n        <plugin>\n            <groupId>org.apache.maven.plugins</groupId>\n            <artifactId>maven-compiler-plugin</artifactId>\n            <version>${maven.compiler.plugin.version}</version>\n            <configuration>\n                <forceJavacCompilerUse>true</forceJavacCompilerUse>\n                <annotationProcessorPaths>\n                    <annotationProcessorPath>\n                        <groupId>io.github.jxnu-liguobin</groupId>\n                        <artifactId>symphony-java-apt</artifactId>\n                        <version>${symphonyql.version}</version>\n                    </annotationProcessorPath>\n                </annotationProcessorPaths>\n            </configuration>\n        </plugin>\n    </plugins>\n</build>\n")))}d.isMDXComponent=!0}}]);