package com.squareup.kotlinpoet

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

class ExpectDeclarationsTest {
  @Test fun expectFunDeclaration() {
    val methodSpec = FunSpec.builder("function")
      .addModifiers(KModifier.EXPECT)
      .build()

    assertThat(methodSpec.toString()).isEqualTo(
      """
      |public expect fun function(): kotlin.Unit
      |""".trimMargin()
    )
  }

  @Test fun implicitExpectFunDeclaration() {
    val builder = TypeSpec.classBuilder("Test")
      .addModifiers(KModifier.EXPECT)
    val methodSpec = FunSpec.builder("function")
      .build()
    builder.addFunction(methodSpec)

    assertThat(builder.build().toString()).isEqualTo(
      """
        |public expect class Test {
        |  public fun function(): kotlin.Unit
        |}
        |""".trimMargin()
    )
  }

  @Test fun expectPropertyDeclaration() {
    val propertySpec = PropertySpec.builder("prop", String::class)
      .addModifiers(KModifier.EXPECT)
      .build()

    assertThat(propertySpec.toString()).isEqualTo(
      """
      |expect val prop: kotlin.String
      |""".trimMargin()
    )
  }

  @Test fun implicitExpectPropertyDeclaration() {
    val builder = TypeSpec.classBuilder("Test")
      .addModifiers(KModifier.EXPECT)
    val propertySpec = PropertySpec.builder("prop", String::class)
      .build()
    builder.addProperty(propertySpec)

    assertThat(builder.build().toString()).isEqualTo(
      """
        |public expect class Test {
        |  public val prop: kotlin.String
        |}
        |""".trimMargin()
    )
  }
}
