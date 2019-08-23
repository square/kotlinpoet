KotlinPoet-metadata
===================

`KotlinPoet-metadata` is an API for working with Kotlin `@Metadata` annotations. Its API sits atop
 [kotlinx-metadata](https://github.com/JetBrains/kotlin/tree/master/libraries/kotlinx-metadata/jvm), 
 offering immutable versions of its types + JVM metadata information. This can be used to read 
 Kotlin language semantics off of class files.

### Example

```kotlin
data class Taco(val seasoning: String, val soft: Boolean) {
  fun prepare() {
    
  }
}

val kmClass = Taco::class.toImmutableKmClass()

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

### Interop with kotlinx-metadata

To convert a kotlinx-metadata type to its analogous immutable type, simply call `toImmutable()`

```kotlin
val immutableKmClass: ImmutableKmClass = kmClass.toImmutable()
```

To convert an immutable type back to its analogous kotlinx-metadata type, call `toMutable()`.

```kotlin
val mutableKmClass: KmClass = kmClass.toMutable()
```

### Interop with KotlinPoet

There is a separate `kotlinpoet-metadata-specs` artifact that offers interop APIs to create 
`TypeSpec`/`FileSpec` representations of classes using this artifact for intermediary parsing.
