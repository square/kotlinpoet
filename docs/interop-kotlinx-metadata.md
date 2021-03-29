KotlinPoet-metadata
===================

`interop:kotlinx-metadata` is an API for working with Kotlin `@Metadata` annotations. Its API 
sits atop [kotlinx-metadata](https://github.com/JetBrains/kotlin/tree/master/libraries/kotlinx-metadata/jvm) and
can be used to read Kotlin language semantics off of `Class` or `TypeElement` `@Metadata` annotations.

### Example

```kotlin
data class Taco(val seasoning: String, val soft: Boolean) {
  fun prepare() {
    
  }
}

val kmClass = Taco::class.toKmClass()

// Now you can access misc information about Taco from a Kotlin lens
println(kmClass.name)
kmClass.properties.forEach { println(it.name) }
kmClass.functions.forEach { println(it.name) }
```

### Flags

There are a number of boolean flags available to types as well under `Flags.kt`. These read the 
underlying kotlinx-metadata `Flags` property.

Using the Taco example above, we can glean certain information:

```kotlin
println("Is class? ${kmClass.isClass}")
println("Is data class? ${kmClass.isData}")
```

## Interop with KotlinPoet

This artifact offers interop APIs to create KotlinPoet source representations 
(i.e. `TypeSpec`/`FileSpec`) of their APIs.  This includes full type resolution, signatures,
enclosed elements, and general stub source representations of the underlying API.

### Example

```kotlin
data class Taco(val seasoning: String, val soft: Boolean) {
  fun prepare() {
  }
}

val typeSpec = Taco::class.toTypeSpec()

// Or FileSpec
val fileSpec = Taco::class.toFileSpec()
```

### Source representation

The generated representations are a _best effort_ representation of the underlying source code.
This means that synthetic elements will be excluded from generation. Kotlin-specific language
features like lambdas or delegation will be coerced to their idiomatic source form.

To aid with this, `toTypeSpec()` and `toFileSpec()` accept optional `ClassInspector` instances
to assist in parsing/understanding the underlying JVM code. This is important for things like
annotations, companion objects, certain JVM modifiers, overrides, and more. While it is optional,
represented sources can be incomplete without this information available. There are reflective and 
javax `Elements` implementations available.

Generated sources are solely _stub_ implementations, meaning implementation details of elements
like functions, property getters, and delegated properties are simply stubbed with `TODO()`
placeholders.

### Known limitations

- Only `KotlinClassMetadata.Class` supported for now. No support for `FileFacade`, `SyntheticClass`, `MultiFileClassFacade`, or `MultiFileClassPart`
- `@file:` annotations are not supported yet.
- `@JvmOverloads` annotations are only supported with `ElementsClassInspector` and not reflection.
- Non-const literal values are only supported with `ReflectiveClassInspector` and not elements.
- ClassInspector data sourced from `synthetic` constructs are only supported with
  `ReflectiveClassInspector` and not elements. This is because the javax Elements API does not model
  synthetic constructs. This can yield some missing information, like static companion object properties
  or `property:` site target annotations.
- Annotations annotated with `AnnotationRetention.SOURCE` are not parsable in reflection nor javax elements.

