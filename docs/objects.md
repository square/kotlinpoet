Objects
=======

KotlinPoet supports objects:

```kotlin
val helloWorld = TypeSpec.objectBuilder("HelloWorld")
  .addProperty(
    PropertySpec.builder("buzz", String::class)
      .initializer("%S", "buzz")
      .build()
  )
  .addFunction(
    FunSpec.builder("beep")
      .addStatement("println(%S)", "Beep!")
      .build()
  )
  .build()
```

Similarly, you can create companion objects and add them to classes using `addType()`:

```kotlin
val companion = TypeSpec.companionObjectBuilder()
  .addProperty(
    PropertySpec.builder("buzz", String::class)
      .initializer("%S", "buzz")
      .build()
  )
  .addFunction(
    FunSpec.builder("beep")
      .addStatement("println(%S)", "Beep!")
      .build()
  )
  .build()

val helloWorld = TypeSpec.classBuilder("HelloWorld")
  .addType(companion)
  .build()
```

You can provide an optional name for a companion object.
