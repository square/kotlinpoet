Callable References
===================

[Callable references][callable-references] to constructors, functions, and properties may be emitted
via:

- `ClassName.constructorReference()` for constructors
- `MemberName.reference()` for functions and properties

For example,

```kotlin
val helloClass = ClassName("com.example.hello", "Hello")
val worldFunction: MemberName = helloClass.member("world")
val byeProperty: MemberName = helloClass.nestedClass("World").member("bye")

val factoriesFun = FunSpec.builder("factories")
  .addStatement("val hello = %L", helloClass.constructorReference())
  .addStatement("val world = %L", worldFunction.reference())
  .addStatement("val bye = %L", byeProperty.reference())
  .build()

FileSpec.builder("com.example", "HelloWorld")
  .addFunction(factoriesFun)
  .build()
```

would generate:

```kotlin
package com.example

import com.example.hello.Hello

fun factories() {
  val hello = ::Hello
  val world = Hello::world
  val bye = Hello.World::bye
}
```

Top-level classes and members with conflicting names may require aliased imports, as with
[member names](m-for-members.md).

 [callable-references]: https://kotlinlang.org/docs/reference/reflection.html#callable-references
