KSP Extensions for KotlinPoet
==============

`interop:ksp` is an interop API for converting
[Kotlin Symbol Processing][ksp] (KSP) types to KotlinPoet types and
writing to KSP `CodeGenerator`.

```kotlin
dependencies {
  implementation("com.squareup:kotlinpoet-ksp:<version>")
}
```

### Examples

Examples are based on reading the following property as a `KSProperty`:

```kotlin
class Taco {
  internal inline val seasoning: String get() = "spicy"
}
```

**Convert a `KSType` to a `TypeName`**

```kotlin
// returns a `ClassName` of value `kotlin.String`
seasoningKsProperty.type.toTypeName()
```

**Convert a `Modifier` to a `KModifier`**

```kotlin
// returns `[KModifier.INLINE]`
seasoningKsProperty.modifiers.mapNotNull { it.toKModifier() }
```

**Convert a `Visibility` to a `KModifier`**

```kotlin
// returns `KModifier.INTERNAL`
seasoningKsProperty.getVisibility().toKModifier()
```

**Write to `CodeGenerator`**

To write a `FileSpec` to a KSP `CodeGenerator`, simply call the `FileSpec.writeTo(CodeGenerator, ...)`
extension function.

```kotlin
fileSpec.writeTo(codeGenerator)
```

### Type Parameters

Type parameters can be declared on classes, functions, and typealiases. These parameters are then
available to all of its enclosed elements. In order for these elements to resolve these in KSP, you
must be able to reference these type parameters by their _index_.

In `kotlinpoet-ksp` this is orchestrated by the `TypeParameterResolver` API, which can be passed
into most `toTypeName()` (or similar) functions to give them access to enclosing type parameters
that they may reference.

The canonical way to create an instance of this is to call `toTypeParameterResolver()` on a
`List<KSTypeParameter>`.

Consider the following class and function

```kotlin
abstract class Taco<T> {
  abstract val seasoning: T
}
```

To properly resolve the type of `seasoning`, we need to pass the class `TypeParameterResolver` to
`toTypeName()` so that it can properly resolve it.

```kotlin
val classTypeParams = ksClassDeclaration.typeParameters.toTypeParameterResolver()
// returns `T`
val seasoningType = seasoningKsProperty.type.toTypeName(classTypeParams)
```

`TypeParameterResolver` is also composable to allow for multi-level nesting. `toTypeParameterResolver()`
has an optional `parent` parameter to provide a parent instance.

Consider our previous example again, but this time with a function that defines its own type parameters.

```kotlin
class Taco<T> {
  fun <E> getShellOfType(param1: E, param2: T) {

  }
}
```

To resolve its parameters, we need to create a `TypeParameterResolver` from the function's
`typeParameters` and _compose_ it with the enclosing class's type parameters as a `parent`.

```kotlin
val classTypeParams = ksClassDeclaration.typeParameters.toTypeParameterResolver()
val functionTypeParams = ksFunction.typeParameters.toTypeParameterResolver(parent = classTypeParams)
// returns `[E, T]`
val seasoningType = ksFunction.parameterTypes.map { it.toTypeName(functionTypeParams) }
```

### Incremental Processing

KSP supports [incremental processing][incremental] as
long as symbol processors properly indicate originating files in generated new files and whether or
not they are `aggregating`. `kotlinpoet-ksp` supports this via `OriginatingKSFiles`, which is a simple
API that sits atop KotlinPoet's `Taggable` API. To use this, simply add relevant originating files to
any `TypeSpec`, `TypeAliasSpec`, `PropertySpec`, or `FunSpec` builders.

```kotlin
val functionBuilder = FunSpec.builder("sayHello")
  .addOriginatingKSFile(sourceKsFile)
  .build()
```

Like KotlinPoet's _originating elements_ support for javac annotation processors, calling the
`FileSpec.writeTo(CodeGenerator, ...)` function will automatically collect and de-dupe these originating
`KSFile` references and automatically assemble them in the underlying `Dependencies` for KSP's reference.

Optionally you can define your own collection of files and pass them to the `writeTo` function, but usually
you don't need to do this manually.

Lastly - `FileSpec.writeTo(CodeGenerator, ...)` also requires you to specify if your processor is
_aggregating_ or not via required parameter by the same name.

### TypeAlias Handling

For `typealias` types, KSP interop will store a `TypeAliasTag` in the `TypeName`'s tags with a reference to the abbreviated type. This can be useful for APIs that want to resolve all un-aliased types.

 [ksp]: https://github.com/google/ksp
 [incremental]: https://github.com/google/ksp/blob/main/docs/incremental.md
