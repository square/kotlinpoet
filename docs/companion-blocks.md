Companion Blocks
================

KotlinPoet supports companion blocks, a feature added in Kotlin 2.5.0. Use the `CompanionBlockSpec.Builder`
API to create a companion block, and add it to a `TypeSpec` using `TypeSpec.Builder.addCompanionBlock`:

```kotlin
val vector =
  vectorTypeSpec()
    .addCompanionBlock(
      CompanionBlockSpec.Builder()
        .addKdoc("This is a companion block.")
        .addProperty(
          PropertySpec.builder("Zero", VECTOR)
            .getter(FunSpec.getterBuilder().addStatement("return %T(0.0, 0.0)", VECTOR).build())
            .build()
        )
        .addFunction(
          FunSpec.builder("invoke")
            .addModifiers(SUSPEND, OPERATOR)
            .addParameter("file", ClassName(TACOS_PACKAGE, "StructureFormat"))
            .returns(VECTOR)
            .addStatement("val x = file.readDouble()")
            .addStatement("val y = file.readDouble()")
            .addStatement("return %T(x, y)", VECTOR)
            .build()
        )
        .build()
    )
    .build()

val kotlinFile = FileSpec.builder("com.example.vector", "Vector")
  .addType(vector)
  .build()

kotlinFile.writeTo(System.out)
```

Which would generate the following `.kt` file:

```kotlin
package com.example.vector

import kotlin.Double

public data class Vector(
  public val x: Double,
  public val y: Double,
) {
  /**
   * This is a companion block.
   */
  companion {
    public val Zero: Vector
      get() = Vector(0.0, 0.0)

    public suspend operator fun invoke(`file`: StructureFormat): Vector {
      val x = file.readDouble()
      val y = file.readDouble()
      return Vector(x, y)
    }
  }
}
```

Refer to [KEEP-0449][keep-0449] to learn more about companion blocks.

 [keep-0449]: https://github.com/Kotlin/KEEP/blob/main/proposals/KEEP-0449-companions-block-extension.md
