Interfaces
==========

KotlinPoet has no trouble with interfaces. Note that interface methods must always be `ABSTRACT`.
The modifier is necessary when defining the interface:

```kotlin
val helloWorld = TypeSpec.interfaceBuilder("HelloWorld")
  .addProperty("buzz", String::class)
  .addFunction(
    FunSpec.builder("beep")
      .addModifiers(KModifier.ABSTRACT)
      .build()
  )
  .build()
```

But these modifiers are omitted when the code is generated. These are the default so we don't need
to include them for `kotlinc`'s benefit!

```kotlin
interface HelloWorld {
  val buzz: String

  fun beep()
}
```

Kotlin 1.4 adds support for functional interfaces via `fun interface` syntax. To create this in
KotlinPoet, use `TypeSpec.funInterfaceBuilder()`.

```kotlin
val helloWorld = TypeSpec.funInterfaceBuilder("HelloWorld")
  .addFunction(
    FunSpec.builder("beep")
      .addModifiers(KModifier.ABSTRACT)
      .build()
  )
  .build()

// Generates...
fun interface HelloWorld {
  fun beep()
}
```
