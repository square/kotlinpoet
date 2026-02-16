%T for Types
============

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
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR

val hoverboard = ClassName("com.mattel", "Hoverboard")
val list = ClassName("kotlin.collections", "List")
val arrayList = ClassName("kotlin.collections", "ArrayList")
val listOfHoverboards = list.parameterizedBy(hoverboard)
val arrayListOfHoverboards = arrayList.parameterizedBy(hoverboard)

val thing = ClassName("com.misc", "Thing")
val array = ClassName("kotlin", "Array")
val producerArrayOfThings = array.parameterizedBy(WildcardTypeName.producerOf(thing))

val beyond = FunSpec.builder("beyond")
  .returns(listOfHoverboards)
  .addStatement("val result = %T()", arrayListOfHoverboards)
  .addStatement("result += %T()", hoverboard)
  .addStatement("result += %T()", hoverboard)
  .addStatement("result += %T()", hoverboard)
  .addStatement("return result")
  .build()

val printThings = FunSpec.builder("printThings")
  .addParameter("things", producerArrayOfThings)
  .addStatement("println(things)")
  .build()

val printKClass = FunSpec.builder("printKClass")
  .addParameter("kClass", KClass::class.asClassName().parameterizedBy(STAR))
  .addStatement("println(kClass)")
  .build()
```

The `STAR` is represented as `*` in KotlinPoet. You can find more in the [KDoc][kdoc].

KotlinPoet will decompose each type and import its components where possible.

```kotlin
package com.example.helloworld

import com.mattel.Hoverboard
import com.misc.Thing
import kotlin.Array
import kotlin.collections.ArrayList
import kotlin.collections.List
import kotlin.reflect.KClass

class HelloWorld {
  fun beyond(): List<Hoverboard> {
    val result = ArrayList<Hoverboard>()
    result += Hoverboard()
    result += Hoverboard()
    result += Hoverboard()
    return result
  }

  fun printThings(things: Array<out Thing>) {
    println(things)
  }

  fun printKClass(kClass: KClass<*>) {
    println(kClass)
  }
}
```

## Nullable Types

KotlinPoet supports nullable types. To convert a `TypeName` into its nullable counterpart, use the
`copy()` method with `nullable` parameter set to `true`:

```kotlin
val java = PropertySpec.builder("java", String::class.asTypeName().copy(nullable = true))
  .mutable()
  .addModifiers(KModifier.PRIVATE)
  .initializer("null")
  .build()

val helloWorld = TypeSpec.classBuilder("HelloWorld")
  .addProperty(java)
  .addProperty("kotlin", String::class, KModifier.PRIVATE)
  .build()
```

generates:

```kotlin
class HelloWorld {
  private var java: String? = null

  private val kotlin: String
}
```

## Annotated Types

KotlinPoet supports annotated types. To add an annotation to a type, use the `annotated()` API:

```kotlin
val annotatedType = String::class.asTypeName()
  .annotated(AnnotationSpec.builder(MyAnnotation::class).build())
```

For comprehensive examples of annotating types, see [Annotations](annotations.md#annotating-types).

 [kdoc]: https://square.github.io/kotlinpoet/1.x/kotlinpoet/kotlinpoet/com.squareup.kotlinpoet/
