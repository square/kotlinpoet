Enums
=====

Use `enumBuilder` to create the enum type, and `addEnumConstant()` for each value:

```kotlin
val helloWorld = TypeSpec.enumBuilder("Roshambo")
  .addEnumConstant("ROCK")
  .addEnumConstant("SCISSORS")
  .addEnumConstant("PAPER")
  .build()
```

To generate this:

```kotlin
enum class Roshambo {
  ROCK,

  SCISSORS,

  PAPER
}
```

Fancy enums are supported, where the enum values override methods or call a superclass constructor.
Here's a comprehensive example:

```kotlin
val helloWorld = TypeSpec.enumBuilder("Roshambo")
  .primaryConstructor(
    FunSpec.constructorBuilder()
      .addParameter("handsign", String::class)
      .build()
  )
  .addEnumConstant(
    "ROCK", TypeSpec.anonymousClassBuilder()
      .addSuperclassConstructorParameter("%S", "fist")
      .addFunction(
        FunSpec.builder("toString")
          .addModifiers(KModifier.OVERRIDE)
          .addStatement("return %S", "avalanche!")
          .returns(String::class)
          .build()
      )
      .build()
  )
  .addEnumConstant(
    "SCISSORS", TypeSpec.anonymousClassBuilder()
      .addSuperclassConstructorParameter("%S", "peace")
      .build()
  )
  .addEnumConstant(
    "PAPER", TypeSpec.anonymousClassBuilder()
      .addSuperclassConstructorParameter("%S", "flat")
      .build()
  )
  .addProperty(
    PropertySpec.builder("handsign", String::class, KModifier.PRIVATE)
      .initializer("handsign")
      .build()
  )
  .build()
```

Which generates this:

```kotlin
enum class Roshambo(private val handsign: String) {
  ROCK("fist") {
    override fun toString(): String = "avalanche!"
  },

  SCISSORS("peace"),

  PAPER("flat");
}
```
