JavaPoet Extensions for KotlinPoet
==================================

`interop:javapoet` is an interop API for converting [JavaPoet](https://github.com/square/javapoet)
types to KotlinPoet types. This is particularly useful for projects that support code gen in
multiple languages and want to easily be able to jump between.

Note that this API is currently in preview and subject to API changes. Usage of them requires opting
in to the `@KotlinPoetJavaPoetPreview` annotation.

### Examples

**Typealiases for common conflicting type names**

```kotlin
// Points to com.squareup.kotlinpoet.TypeName
KTypeName
// Points to com.squareup.javapoet.TypeName
JTypeName
```

**Convert between a `JTypeName` and `KTypeName`**

Most usages of these can run through the `toKTypeName()` and `toJTypeName()` extensions.

```kotlin
val jType = JTypeName.get("com.example", "Taco")

// Returns a KotlinPoet `ClassName` of value `com.example.Taco`
val kType = jType.toKTypeName()

// Returns a JavaPoet `ClassName` of value `com.example.Taco`
val jType2 = kType.toJTypeName()
```

### Intrinsics

Kotlin supports a number of intrinsic types that live in the `kotlin` package, such as primitives,
`List`, `String`, `IntArray`, etc. Where possible, interop will best-effort attempt to convert to
the idiomatic Kotlin type when converting from the Java type.

### Lossy Conversions

Kotlin has more expressive types in some regards. These cannot be simply expressed in JavaPoet and
are subject to lossy conversions.

Examples include:

- Nullability
  - Nullable types in Kotlin will appear as normal types in JavaPoet.
- Collection mutability
  - Immutable Kotlin collections will convert to their standard (mutable) Java analogs.
  - Java collections will convert to _immutable_ Kotlin analogs, erring on the side of safety in generated public APIs
- Unsigned types
