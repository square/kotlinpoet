%M for Members
==============

Similar to types, KotlinPoet has a special placeholder for **members** (functions and properties),
which comes handy when your code needs to access top-level members and members declared inside
objects. Use **`%M`** to reference members, pass an instance of `MemberName` as the argument for the
placeholder, and KotlinPoet will handle imports automatically:

```kotlin
val createTaco = MemberName("com.squareup.tacos", "createTaco")
val isVegan = MemberName("com.squareup.tacos", "isVegan")
val file = FileSpec.builder("com.squareup.example", "TacoTest")
  .addFunction(
    FunSpec.builder("main")
      .addStatement("val taco = %M()", createTaco)
      .addStatement("println(taco.%M)", isVegan)
      .build()
  )
  .build()
println(file)
```

The code above generates the following file:

```kotlin
package com.squareup.example

import com.squareup.tacos.createTaco
import com.squareup.tacos.isVegan

fun main() {
  val taco = createTaco()
  println(taco.isVegan)
}
```

As you can see, it's also possible to use `%M` to reference extension functions and properties. You
just need to make sure the member can be imported without simple name collisions, otherwise
importing will fail and the code generator output will not pass compilation. There's a way to work
around such cases though - use `FileSpec.addAliasedImport()` to create an alias for a clashing
`MemberName`:

```kotlin
val createTaco = MemberName("com.squareup.tacos", "createTaco")
val createCake = MemberName("com.squareup.cakes", "createCake")
val isTacoVegan = MemberName("com.squareup.tacos", "isVegan")
val isCakeVegan = MemberName("com.squareup.cakes", "isVegan")
val file = FileSpec.builder("com.squareup.example", "Test")
  .addAliasedImport(isTacoVegan, "isTacoVegan")
  .addAliasedImport(isCakeVegan, "isCakeVegan")
  .addFunction(
    FunSpec.builder("main")
      .addStatement("val taco = %M()", createTaco)
      .addStatement("val cake = %M()", createCake)
      .addStatement("println(taco.%M)", isTacoVegan)
      .addStatement("println(cake.%M)", isCakeVegan)
      .build()
  )
  .build()
println(file)
```

KotlinPoet will produce an aliased import for `com.squareup.tacos2.isVegan`:

```kotlin
package com.squareup.example

import com.squareup.cakes.createCake
import com.squareup.tacos.createTaco
import com.squareup.cakes.isVegan as isCakeVegan
import com.squareup.tacos.isVegan as isTacoVegan

fun main() {
  val taco = createTaco()
  val cake = createCake()
  println(taco.isTacoVegan)
  println(cake.isCakeVegan)
}
```

## MemberName and operators

MemberName also supports operators, you can use `MemberName(String, KOperator)`
or `MemberName(ClassName, KOperator)` to import and reference operators.

```kotlin
val taco = ClassName("com.squareup.tacos", "Taco")
val meat = ClassName("com.squareup.tacos.ingredient", "Meat")
val iterator = MemberName("com.squareup.tacos.internal", KOperator.ITERATOR)
val minusAssign = MemberName("com.squareup.tacos.internal", KOperator.MINUS_ASSIGN)
val file = FileSpec.builder("com.example", "Test")
  .addFunction(
    FunSpec.builder("makeTacoHealthy")
      .addParameter("taco", taco)
      .beginControlFlow("for (ingredient %M taco)", iterator)
      .addStatement("if (ingredient is %T) taco %M ingredient", meat, minusAssign)
      .endControlFlow()
      .addStatement("return taco")
      .build()
  )
  .build()
println(file)
```

KotlinPoet will import the extension operator functions and emit the operator.

```kotlin
package com.example

import com.squareup.tacos.Taco
import com.squareup.tacos.ingredient.Meat
import com.squareup.tacos.internal.iterator
import com.squareup.tacos.internal.minusAssign

fun makeTacoHealthy(taco: Taco) {
  for (ingredient in taco) {
    if (ingredient is Meat) taco -= ingredient
  }
  return taco
}

```
