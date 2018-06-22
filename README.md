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

### %S for Strings

When emitting code that includes string literals, we can use **`%S`** to emit a **string**, complete
with wrapping quotation marks and escaping. Here's a program that emits 3 methods, each of which
returns its own name:

```kotlin
fun main(args: Array<String>) {
  val helloWorld = TypeSpec.classBuilder("HelloWorld")
      .addFunction(whatsMyNameYo("slimShady"))
      .addFunction(whatsMyNameYo("eminem"))
      .addFunction(whatsMyNameYo("marshallMathers"))
      .build()
  
  val kotlinFile = FileSpec.builder("com.example.helloworld", "HelloWorld")
      .addType(helloWorld)
      .build()
  
  kotlinFile.writeTo(System.out)
}

private fun whatsMyNameYo(name: String): FunSpec {
  return FunSpec.builder(name)
      .returns(String::class)
      .addStatement("return %S", name)
      .build()
}
```

In this case, using `%S` gives us quotation marks:

```kotlin
class HelloWorld {
    fun slimShady(): String = "slimShady"

    fun eminem(): String = "eminem"

    fun marshallMathers(): String = "marshallMathers"
}
```

### %T for Types

KotlinPoet has rich built-in support for types, including automatic generation of `import`
statements. Just use **`%T`** to reference **types**:

```kotlin
val today = FunSpec.builder("today")
    .returns(Date::class)
    .addStatement("return %T()", Date::class)
    .build()

val helloWorld = TypeSpec.classBuilder("HelloWorld")
    .addFunction(today)
    .build()

val kotlinFile = FileSpec.builder("com.example.helloworld", "HelloWorld")
    .addType(helloWorld)
    .build()

kotlinFile.writeTo(System.out)
```

That generates the following `.kt` file, complete with the necessary `import`:

```kotlin
package com.example.helloworld

import java.util.Date

class HelloWorld {
    fun today(): Date = Date()
}
```

We passed `Date::class` to reference a class that just-so-happens to be available when we're
generating code. This doesn't need to be the case. Here's a similar example, but this one
references a class that doesn't exist (yet):

```kotlin
val hoverboard = ClassName("com.mattel", "Hoverboard")

val tomorrow = FunSpec.builder("tomorrow")
    .returns(hoverboard)
    .addStatement("return %T()", hoverboard)
    .build()
```

And that not-yet-existent class is imported as well:

```kotlin
package com.example.helloworld

import com.mattel.Hoverboard

class HelloWorld {
    fun tomorrow(): Hoverboard = Hoverboard()
}
```

The `ClassName` type is very important, and you'll need it frequently when you're using KotlinPoet.
It can identify any _declared_ class. Declared types are just the beginning of Kotlin's rich type
system: we also have arrays, parameterized types, wildcard types, lambda types and type variables. 
KotlinPoet has classes for building each of these:

```kotlin
val hoverboard = ClassName("com.mattel", "Hoverboard")
val list = ClassName("kotlin.collections", "List")
val arrayList = ClassName("kotlin.collections", "ArrayList")
val listOfHoverboards = list.parameterizedBy(hoverboard)
val arrayListOfHoverboards = arrayList.parameterizedBy(hoverboard)

val beyond = FunSpec.builder("beyond")
    .returns(listOfHoverboards)
    .addStatement("val result = %T()", arrayListOfHoverboards)
    .addStatement("result += %T()", hoverboard)
    .addStatement("result += %T()", hoverboard)
    .addStatement("result += %T()", hoverboard)
    .addStatement("return result")
    .build()
```

KotlinPoet will decompose each type and import its components where possible.

```kotlin
package com.example.helloworld

import com.mattel.Hoverboard
import kotlin.collections.ArrayList
import kotlin.collections.List

class HelloWorld {
    fun beyond(): List<Hoverboard> {
        val result = ArrayList<Hoverboard>()
        result += Hoverboard()
        result += Hoverboard()
        result += Hoverboard()
        return result
    }
}
```

### %N for Names

Generated code is often self-referential. Use **`%N`** to refer to another generated declaration by
its name. Here's a method that calls another:

```kotlin
fun byteToHex(b: Int): String {
  val result = CharArray(2)
  result[0] = hexDigit((b ushr 4) and 0xf)
  result[1] = hexDigit(b and 0xf)
  return String(result)
}

fun hexDigit(i: Int): Char {
  return (if (i < 10) i + '0'.toInt() else i - 10 + 'a'.toInt()).toChar()
}
```

When generating the code above, we pass the `hexDigit()` method as an argument to the `byteToHex()`
method using `%N`:

```kotlin
val hexDigit = FunSpec.builder("hexDigit")
    .addParameter("i", Int::class)
    .returns(Char::class)
    .addStatement("return (if (i < 10) i + '0'.toInt() else i - 10 + 'a'.toInt()).toChar()")
    .build()

val byteToHex = FunSpec.builder("byteToHex")
    .addParameter("b", Int::class)
    .returns(String::class)
    .addStatement("val result = CharArray(2)")
    .addStatement("result[0] = %N((b ushr 4) and 0xf)", hexDigit)
    .addStatement("result[1] = %N(b and 0xf)", hexDigit)
    .addStatement("return String(result)")
    .build()
```

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
