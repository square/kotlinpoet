KotlinPoet
==========

`KotlinPoet` is a Kotlin and Java API for generating `.kt` source files.

Source file generation can be useful when doing things such as annotation processing or interacting
with metadata files (e.g., database schemas, protocol formats). By generating code, you eliminate
the need to write boilerplate while also keeping a single source of truth for the metadata.


### Example

Here's a `HelloWorld` file:

```kotlin
class Greeter(val name: String) {
  fun greet() {
    println("Hello, $name")
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
    .addType(TypeSpec.classBuilder("Greeter")
        .primaryConstructor(FunSpec.constructorBuilder()
            .addParameter("name", String::class)
            .build())
        .addProperty(PropertySpec.builder("name", String::class)
            .initializer("name")
            .build())
        .addFunction(FunSpec.builder("greet")
            .addStatement("println(%S)", "Hello, \$name")
            .build())
        .build())
    .addFunction(FunSpec.builder("main")
        .addParameter("args", String::class, VARARG)
        .addStatement("%T(args[0]).greet()", greeterClass)
        .build())
    .build()

file.writeTo(System.out)
```

The [KDoc][kdoc] catalogs the complete KotlinPoet API, which is inspired by [JavaPoet][javapoet].

### Code & Control Flow

Most of KotlinPoet's API uses immutable Kotlin objects. There's also builders, method chaining
and varargs to make the API friendly. KotlinPoet offers models for Kotlin files (`FileSpec`),
classes, interfaces & objects (`TypeSpec`), type aliases (`TypeAliasSpec`),
properties (`PropertySpec`), functions & constructors (`FunSpec`), parameters (`ParameterSpec`) and
annotations (`AnnotationSpec`).

But the _body_ of methods and constructors is not modeled. There's no expression class, no
statement class or syntax tree nodes. Instead, KotlinPoet uses strings for code blocks, and you can 
take advantage of Kotlin's multiline strings to make this look nice:

```kotlin
val main = FunSpec.builder("main")
    .addCode("""
        |var total = 0
        |for (i in 0 until 10) {
        |    total += i
        |}
        |""".trimMargin())
    .build()
```

Which generates this:

```kotlin
fun main() {
    var total = 0
    for (i in 0 until 10) {
        total += i
    }
}
```

There are additional APIs to assist with newlines, braces and indentation:

```kotlin
val main = FunSpec.builder("main")
    .addStatement("var total = 0")
    .beginControlFlow("for (i in 0 until 10)")
    .addStatement("total += i")
    .endControlFlow()
    .build()
```

This example is lame because the generated code is constant! Suppose instead of just adding 0 to 10,
we want to make the operation and range configurable. Here's a method that generates a method:

```kotlin
private fun computeRange(name: String, from: Int, to: Int, op: String): FunSpec {
  return FunSpec.builder(name)
      .returns(Int::class)
      .addStatement("var result = 1")
      .beginControlFlow("for (i in $from until $to)")
      .addStatement("result = result $op i")
      .endControlFlow()
      .addStatement("return result")
      .build()
}
```

And here's what we get when we call `computeRange("multiply10to20", 10, 20, "*")`:

```kotlin
fun multiply10to20(): kotlin.Int {
    var result = 1
    for (i in 10 until 20) {
        result = result * i
    }
    return result
}
```

Methods generating methods! And since KotlinPoet generates source instead of bytecode, you can
read through it to make sure it's right.

Download
--------

Download [the latest .jar][dl] or depend via Maven:

```xml
<dependency>
  <groupId>com.squareup</groupId>
  <artifactId>kotlinpoet</artifactId>
  <version>0.7.0</version>
</dependency>
```

or Gradle:

```groovy
compile 'com.squareup:kotlinpoet:0.7.0'
```

Snapshots of the development version are available in [Sonatype's `snapshots` repository][snap].


License
-------

    Copyright 2017 Square, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.


 [dl]: https://search.maven.org/remote_content?g=com.squareup&a=kotlinpoet&v=LATEST
 [snap]: https://oss.sonatype.org/content/repositories/snapshots/com/squareup/kotlinpoet/
 [kdoc]: https://square.github.io/kotlinpoet/0.x/kotlinpoet/com.squareup.kotlinpoet/
 [javapoet]: https://github.com/square/javapoet/
