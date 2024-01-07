Anonymous Inner Classes
=======================

In the enum code, we used `TypeSpec.anonymousClassBuilder()`. Anonymous inner classes can also be
used in code blocks. They are values that can be referenced with `%L`:

```kotlin
val comparator = TypeSpec.anonymousClassBuilder()
  .addSuperinterface(Comparator::class.parameterizedBy(String::class))
  .addFunction(
    FunSpec.builder("compare")
      .addModifiers(KModifier.OVERRIDE)
      .addParameter("a", String::class)
      .addParameter("b", String::class)
      .returns(Int::class)
      .addStatement("return %N.length - %N.length", "a", "b")
      .build()
  )
  .build()

val helloWorld = TypeSpec.classBuilder("HelloWorld")
  .addFunction(
    FunSpec.builder("sortByLength")
      .addParameter("strings", List::class.parameterizedBy(String::class))
      .addStatement("%N.sortedWith(%L)", "strings", comparator)
      .build()
  )
  .build()
```

This generates a method that contains a class that contains a method:

```kotlin
class HelloWorld {
  fun sortByLength(strings: List<String>) {
    strings.sortedWith(object : Comparator<String> {
      override fun compare(a: String, b: String): Int = a.length - b.length
    })
  }
}
```

One particularly tricky part of defining anonymous inner classes is the arguments to the superclass
constructor. To pass them use `TypeSpec.Builder`'s `addSuperclassConstructorParameter()` method.
