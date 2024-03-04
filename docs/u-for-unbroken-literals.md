%U for Unbroken Literals
===============

Although the string specifier (`%S`) and literal specifier (`%L`) work in the vast majority of
cases, sometimes escaping and line breaks are unacceptable in the output. In these cases, `%U` is
the right specifier. `%U` has identical behavior to `%L` with the exception that it will never
insert line breaks.

This format specifier is useful where unexpected line breaks may break the correctness of the
output. A specific example is the case where a byte array serialized as a pre-escaped String. Using
any of the other specifiers in cases like this may unintentionally modify the content and make the
emitted code incorrect.

With `%U`:

```kotlin
private fun findLongInString(): FunSpec {
  val longPreQuotedString = "\"very long string very long string very long string very long string very long string very long string very long string\""
  return FunSpec.builder("findLongInString")
    .returns(Boolean::class)
    .addStatement("val theString = %U", longPreQuotedString)
    .addStatement("return theString.contains(\"long\".toRegex())")
    .build()
}
```

Produces (note lack of line break on long string):

```kotlin
public fun findLongInString(): Boolean {
  val theString = "very long string very long string very long string very long string very long string very long string very long string"
  return theString.contains("long".toRegex())
}
```

Whereas using `%L` would have produced (note line break):

```kotlin
public fun findLongInString(): Boolean {
  val theString = "very long string very long string very long string very long string very
      long string very long string very long string"
  return theString.contains("long".toRegex())
}
```

Which wouldn't compile.
