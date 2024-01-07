%N for Names
============

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

Another handy feature that `%N` provides is automatically escaping names that contain illegal
identifier characters with double ticks. Suppose your code creates a `MemberName` with a Kotlin
keyword as the simple name:

```kotlin
val taco = ClassName("com.squareup.tacos", "Taco")
val packager = ClassName("com.squareup.tacos", "TacoPackager")
val file = FileSpec.builder("com.example", "Test")
  .addFunction(
    FunSpec.builder("packageTacos")
      .addParameter("tacos", LIST.parameterizedBy(taco))
      .addParameter("packager", packager)
      .addStatement("packager.%N(tacos)", packager.member("package"))
      .build()
  )
  .build()
```

`%N` will escape the name for you, ensuring that the output will pass compilation:

```kotlin
package com.example

import com.squareup.tacos.Taco
import com.squareup.tacos.TacoPackager
import kotlin.collections.List

fun packageTacos(tacos: List<Taco>, packager: TacoPackager) {
  packager.`package`(tacos)
}
```
