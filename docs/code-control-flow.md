Code & Control Flow
===================

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
    |for (i in 0..<10) {
    |    total += i
    |}
    |""".trimMargin())
  .build()
```

Which generates this:

```kotlin
fun main() {
  var total = 0
  for (i in 0..<10) {
    total += i
  }
}
```

There are additional APIs to assist with newlines, braces and indentation:

```kotlin
val main = FunSpec.builder("main")
  .addStatement("var total = 0")
  .beginControlFlow("for (i in 0..<10)")
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
    .beginControlFlow("for (i in $from..<$to)")
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
  for (i in 10..<20) {
    result = result * i
  }
  return result
}
```

Methods generating methods! And since KotlinPoet generates source instead of bytecode, you can
read through it to make sure it's right.
