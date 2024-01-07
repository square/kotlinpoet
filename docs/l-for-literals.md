%L for Literals
===============

Although Kotlin's string templates usually work well in cases when you want to include literals into
generated code, KotlinPoet offers additional syntax inspired-by but incompatible-with
[`String.format()`][formatter]. It accepts **`%L`** to emit a **literal** value in the output. This
works just like `Formatter`'s `%s`:

```kotlin
private fun computeRange(name: String, from: Int, to: Int, op: String): FunSpec {
  return FunSpec.builder(name)
    .returns(Int::class)
    .addStatement("var result = 0")
    .beginControlFlow("for (i in %L..<%L)", from, to)
    .addStatement("result = result %L i", op)
    .endControlFlow()
    .addStatement("return result")
    .build()
}
```

Literals are emitted directly to the output code with no escaping. Arguments for literals may be
strings, primitives, and a few KotlinPoet types described below.

 [formatter]: https://developer.android.com/reference/java/util/Formatter.html
