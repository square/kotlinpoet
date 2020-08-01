KotlinPoet-metadata-specs
=========================

`KotlinPoet-metadata-specs` is an API for converting `kotlinpoet-metadata` types to KotlinPoet 
source representations of their APIs. This includes full type resolution, signatures, 
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
 represented sources can be incomplete without this information available. Reflective and javax
`Elements` implementations are available under the `kotlinpoet-classinspector-*` artifacts.

Generated sources are solely _stub_ implementations, meaning implementation details of elements
like functions, property getters, and delegated properties are simply stubbed with `TODO()` 
placeholders.

### Known limitations

- Only `KotlinClassMetadata.Class` supported for now. No support for `FileFacade`, `SyntheticClass`, `MultiFileClassFacade`, or `MultiFileClassPart`
- `@file:` annotations are not supported yet.
- `@JvmOverloads` annotations are only supported with `kotlinpoet-classinspector-elements` and not reflection.
- Non-const literal values are only supported with `kotlinpoet-classinspector-reflective` and not reflection.
- ClassInspector data sourced from `synthetic` constructs are only supported with 
`kotlinpoet-classinspector-reflective` and not elements. This is because the javax Elements API does not model
synthetic constructs. This can yield some missing information, like static companion object properties 
or `property:` site target annotations.
- Annotations annotated with `AnnotationRetention.SOURCE` are not parsable in reflection nor javax elements.
