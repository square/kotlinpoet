kotlin-reflect
==============

To generate source code from any [`KType`][k-type], including information that's not accessible to
the builtin reflection APIs, KotlinPoet depends on [kotlin-reflect][kotlin-reflect]. `kotlin-reflect`
can read the metadata of your classes and access this extra information. KotlinPoet can for an
example, read the type parameters and their [variance][variance] from a generic `KType` and
generate appropriate source code.

`kotlin-reflect` is a relatively big dependency though and in some cases it is desirable to remove
it from the final executable to save some space and/or simplify the proguard/R8 setup (for example
for a Gradle plugin that generates Kotlin code). It is possible to do so and still use most of the
KotlinPoet APIs:

```kotlin
dependencies {
  implementation("com.squareup:kotlinpoet:<version>") {
    exclude(module = "kotlin-reflect")
  }
}
```

The main APIs that require `kotlin-reflect` are [`KType.asTypeName()`][as-type-name] and
[`typeNameOf<T>()`][type-name-of]. If you're calling one of these without `kotlin-reflect` in the
classpath and the type is generic or has annotations you will get a crash.

You can replace it with code that passes type parameters or annotations explicitly and doesn't
need `kotlin-reflect`. For example:

```kotlin
// Replace
// kotlin-reflect needed
val typeName = typeNameOf<List<Int?>>()

// With
// kotlin-reflect not needed
val typeName =
  List::class.asClassName().parameterizedBy(Int::class.asClassName().copy(nullable = true))
```

 [k-type]: https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.reflect/-k-type/
 [kotlin-reflect]: https://kotlinlang.org/docs/reflection.html#jvm-dependency
 [variance]: https://kotlinlang.org/docs/generics.html#variance
 [as-type-name]: https://square.github.io/kotlinpoet/1.x/kotlinpoet/kotlinpoet/com.squareup.kotlinpoet/as-type-name.html
 [type-name-of]: https://square.github.io/kotlinpoet/1.x/kotlinpoet/kotlinpoet/com.squareup.kotlinpoet/type-name-of.html
