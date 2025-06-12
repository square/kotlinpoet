KotlinPoet
==========

`KotlinPoet` is a Kotlin and Java API for generating `.kt` source files.

Source file generation can be useful when doing things such as annotation processing or interacting
with metadata files (e.g., database schemas, protocol formats). By generating code, you eliminate
the need to write boilerplate while also keeping a single source of truth for the metadata.

## Example

Here's a `HelloWorld` file:

```kotlin
class Greeter(val name: String) {
  fun greet() {
    println("""Hello, $name""")
  }
}

fun main(vararg args: String) {
  Greeter(args[0]).greet()
}
```

And this is the code to generate it with KotlinPoet:

```kotlin
val greeterClass = ClassName("", "Greeter")
val file = FileSpec.builder("", "HelloWorld")
  .addType(
    TypeSpec.classBuilder("Greeter")
      .primaryConstructor(
        FunSpec.constructorBuilder()
          .addParameter("name", String::class)
          .build()
      )
      .addProperty(
        PropertySpec.builder("name", String::class)
          .initializer("name")
          .build()
      )
      .addFunction(
        FunSpec.builder("greet")
          .addStatement("println(%P)", "Hello, \$name")
          .build()
      )
      .build()
  )
  .addFunction(
    FunSpec.builder("main")
      .addParameter("args", String::class, VARARG)
      .addStatement("%T(args[0]).greet()", greeterClass)
      .build()
  )
  .build()

file.writeTo(System.out)
```

The [KDoc][kdoc] catalogs the complete KotlinPoet API, which is inspired by [JavaPoet][javapoet].

**Note:** In order to maximize portability, KotlinPoet generates code with explicit visibility
modifiers. This ensures compatibility with both standard Kotlin projects as well as projects
using [explicit API mode][explicit-api]. Examples in the documentation omit those modifiers for
brevity.

Download
--------

![Maven Central][version-shield]

Download [the latest .jar][dl] or depend via Maven:

```xml
<dependency>
  <groupId>com.squareup</groupId>
  <artifactId>kotlinpoet-jvm</artifactId>
  <version>[version]</version>
</dependency>
```

or Gradle:

```groovy
implementation("com.squareup:kotlinpoet:[version]")
```

Snapshots of the development version are available in [the Central Portal Snapshots repository][snap].

License
-------

    Copyright 2017 Square, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       https://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

 [kdoc]: https://square.github.io/kotlinpoet/1.x/kotlinpoet/kotlinpoet/com.squareup.kotlinpoet/
 [javapoet]: https://github.com/square/javapoet/
 [explicit-api]: https://kotlinlang.org/docs/whatsnew14.html#explicit-api-mode-for-library-authors
 [version-shield]: https://img.shields.io/maven-central/v/com.squareup/kotlinpoet
 [dl]: https://search.maven.org/remote_content?g=com.squareup&a=kotlinpoet-jvm&v=LATEST
 [snap]: https://central.sonatype.com/repository/maven-snapshots/
