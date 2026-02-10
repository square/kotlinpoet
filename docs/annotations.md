Annotations
===========

Simple annotations are easy:

```kotlin
val test = FunSpec.builder("test string equality")
  .addAnnotation(Test::class)
  .addStatement("assertThat(%1S).isEqualTo(%1S)", "foo")
  .build()
```

Which generates this function with an `@Test` annotation:

```kotlin
@Test
fun `test string equality`() {
  assertThat("foo").isEqualTo("foo")
}
```

Use `AnnotationSpec.builder()` to set properties on annotations:

```kotlin
val logRecord = FunSpec.builder("recordEvent")
  .addModifiers(KModifier.ABSTRACT)
  .addAnnotation(
    AnnotationSpec.builder(Headers::class)
      .addMember("accept = %S", "application/json; charset=utf-8")
      .addMember("userAgent = %S", "Square Cash")
      .build()
  )
  .addParameter("logRecord", LogRecord::class)
  .returns(LogReceipt::class)
  .build()
```

Which generates this annotation with `accept` and `userAgent` properties:

```kotlin
@Headers(
  accept = "application/json; charset=utf-8",
  userAgent = "Square Cash"
)
abstract fun recordEvent(logRecord: LogRecord): LogReceipt
```

When you get fancy, annotation values can be annotations themselves. Use `%L` for embedded
annotations:

```kotlin
val headerList = ClassName("", "HeaderList")
val header = ClassName("", "Header")
val logRecord = FunSpec.builder("recordEvent")
  .addModifiers(KModifier.ABSTRACT)
  .addAnnotation(
    AnnotationSpec.builder(headerList)
      .addMember(
        "[\n⇥%L,\n%L⇤\n]",
        AnnotationSpec.builder(header)
          .addMember("name = %S", "Accept")
          .addMember("value = %S", "application/json; charset=utf-8")
          .build(),
        AnnotationSpec.builder(header)
          .addMember("name = %S", "User-Agent")
          .addMember("value = %S", "Square Cash")
          .build()
      )
      .build()
  )
  .addParameter("logRecord", logRecordName)
  .returns(logReceipt)
  .build()
```

Which generates this:

```kotlin
@HeaderList(
  [
    Header(name = "Accept", value = "application/json; charset=utf-8"),
    Header(name = "User-Agent", value = "Square Cash")
  ]
)
abstract fun recordEvent(logRecord: LogRecord): LogReceipt
```

KotlinPoet supports use-site targets for annotations:

```kotlin
val utils = FileSpec.builder("com.example", "Utils")
  .addAnnotation(
    AnnotationSpec.builder(JvmName::class)
      .useSiteTarget(UseSiteTarget.FILE)
      .build()
  )
  .addFunction(
    FunSpec.builder("abs")
      .receiver(Int::class)
      .returns(Int::class)
      .addStatement("return if (this < 0) -this else this")
      .build()
  )
  .build()
```

Will output this:

```kotlin
@file:JvmName

package com.example

import kotlin.Int
import kotlin.jvm.JvmName

fun Int.abs(): Int = if (this < 0) -this else this
```

## Annotating Types

KotlinPoet provides a convenient `annotated()` API for adding annotations to types:

```kotlin
// Add a single annotation
val annotatedType = String::class.asTypeName()
  .annotated(AnnotationSpec.builder(MyAnnotation::class).build())

// Add multiple annotations
val multiAnnotatedType = Int::class.asTypeName()
  .annotated(
    AnnotationSpec.builder(Suppress::class).addMember("%S", "unused").build(),
    AnnotationSpec.builder(Deprecated::class).addMember("%S", "Use something else").build()
  )

// Add annotations by class
val simpleAnnotated = String::class.asTypeName().annotated(Suppress::class, Deprecated::class)

// Chain multiple calls
val chainedAnnotations = String::class.asTypeName()
  .annotated(Suppress::class)
  .annotated(Deprecated::class)
```

This is especially useful when working with lambda types:

```kotlin
val composableType = LambdaTypeName.get(
  receiver = null,
  parameters = listOf(ParameterSpec.unnamed(ClassName("androidx.compose.ui", "Modifier"))),
  returnType = UNIT,
).annotated(AnnotationSpec.builder(ClassName("androidx.compose.runtime", "Composable")).build())
```

See the [%T for Types](t-for-types.md#annotated-types) documentation for more details.
