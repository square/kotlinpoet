Constructors
============

`FunSpec` is a slight misnomer; it can also be used for constructors:

```kotlin
val flux = FunSpec.constructorBuilder()
  .addParameter("greeting", String::class)
  .addStatement("this.%N = %N", "greeting", "greeting")
  .build()

val helloWorld = TypeSpec.classBuilder("HelloWorld")
  .addProperty("greeting", String::class, KModifier.PRIVATE)
  .addFunction(flux)
  .build()
```

Which generates this:

```kotlin
class HelloWorld {
  private val greeting: String

  constructor(greeting: String) {
    this.greeting = greeting
  }
}
```

For the most part, constructors work just like methods. When emitting code, KotlinPoet will place
constructors before methods in the output file.

Often times you'll need to generate the primary constructor for a class:

```kotlin
val helloWorld = TypeSpec.classBuilder("HelloWorld")
  .primaryConstructor(flux)
  .addProperty("greeting", String::class, KModifier.PRIVATE)
  .build()
```

This code, however, generates the following:

```kotlin
class HelloWorld(greeting: String) {
  private val greeting: String

  init {
    this.greeting = greeting
  }
}
```

By default, KotlinPoet won't merge primary constructor parameters and properties, even if they share
the same name. To achieve the effect, you have to tell KotlinPoet that the property is initialized
via the constructor parameter:

```kotlin
val flux = FunSpec.constructorBuilder()
  .addParameter("greeting", String::class)
  .build()

val helloWorld = TypeSpec.classBuilder("HelloWorld")
  .primaryConstructor(flux)
  .addProperty(
    PropertySpec.builder("greeting", String::class)
      .initializer("greeting")
      .addModifiers(KModifier.PRIVATE)
      .build()
  )
  .build()
```

Now we're getting the following output:

```kotlin
class HelloWorld(private val greeting: String)
```

Notice that KotlinPoet omits `{}` for classes with empty bodies.
