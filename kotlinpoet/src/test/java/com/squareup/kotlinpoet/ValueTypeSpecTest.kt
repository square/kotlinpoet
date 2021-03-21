package com.squareup.kotlinpoet

import com.google.common.truth.Truth.assertThat
import com.squareup.kotlinpoet.KModifier.INLINE
import com.squareup.kotlinpoet.KModifier.PRIVATE
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ValueTypeSpecTest(private val useValue: Boolean) {

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "value={0}")
    fun data(): Collection<Array<Any>> {
      return listOf(
        arrayOf(true),
        arrayOf(false)
      )
    }
  }

  private val modifier = if (useValue) KModifier.VALUE else INLINE
  private val modifierString = modifier.keyword

  private fun classBuilder() = if (useValue) {
    TypeSpec.valueClassBuilder("Guacamole")
  } else {
    TypeSpec.classBuilder("Guacamole")
      .addModifiers(modifier)
  }

  @Test fun validInlineClass() {
    val guacamole = classBuilder()
      .primaryConstructor(
        FunSpec.constructorBuilder()
          .addParameter("avacado", String::class)
          .build()
      )
      .addProperty(
        PropertySpec.builder("avacado", String::class)
          .initializer("avacado")
          .build()
      )
      .build()

    assertThat(guacamole.toString()).isEqualTo(
      """
      |public $modifierString class Guacamole(
      |  public val avacado: kotlin.String
      |)
      |""".trimMargin()
    )
  }

  @Test fun inlineClassWithInitBlock() {
    val guacamole = classBuilder()
      .primaryConstructor(
        FunSpec.constructorBuilder()
          .addParameter("avacado", String::class)
          .build()
      )
      .addProperty(
        PropertySpec.builder("avacado", String::class)
          .initializer("avacado")
          .build()
      )
      .addInitializerBlock(CodeBlock.EMPTY)
      .build()

    assertThat(guacamole.toString()).isEqualTo(
      """
      |public $modifierString class Guacamole(
      |  public val avacado: kotlin.String
      |) {
      |  init {
      |  }
      |}
      |""".trimMargin()
    )
  }

  class InlineSuperClass

  @Test fun inlineClassWithSuperClass() {
    assertThrows<IllegalStateException> {
      classBuilder()
        .primaryConstructor(
          FunSpec.constructorBuilder()
            .addParameter("avocado", String::class)
            .build()
        )
        .addProperty(
          PropertySpec.builder("avocado", String::class)
            .initializer("avocado")
            .build()
        )
        .superclass(InlineSuperClass::class)
        .build()
    }.hasMessageThat().isEqualTo("value/inline classes cannot have super classes")
  }

  interface InlineSuperInterface

  @Test fun inlineClassInheritsFromInterface() {
    val guacamole = classBuilder()
      .primaryConstructor(
        FunSpec.constructorBuilder()
          .addParameter("avocado", String::class)
          .build()
      )
      .addProperty(
        PropertySpec.builder("avocado", String::class)
          .initializer("avocado")
          .build()
      )
      .addSuperinterface(InlineSuperInterface::class)
      .build()

    assertThat(guacamole.toString()).isEqualTo(
      """
      |public $modifierString class Guacamole(
      |  public val avocado: kotlin.String
      |) : com.squareup.kotlinpoet.ValTypeSpecTest.InlineSuperInterface
      |""".trimMargin()
    )
  }

  @Test fun inlineClassWithoutBackingProperty() {
    assertThrows<IllegalArgumentException> {
      classBuilder()
        .primaryConstructor(
          FunSpec.constructorBuilder()
            .addParameter("avocado", String::class)
            .build()
        )
        .addProperty("garlic", String::class)
        .build()
    }.hasMessageThat().isEqualTo("value/inline classes must have a single read-only (val) property parameter.")
  }

  @Test fun inlineClassWithoutProperties() {
    assertThrows<IllegalStateException> {
      classBuilder()
        .primaryConstructor(
          FunSpec.constructorBuilder()
            .addParameter("avocado", String::class)
            .build()
        )
        .build()
    }.hasMessageThat().isEqualTo("value/inline classes must have at least 1 property")
  }

  @Test fun inlineClassWithMutableProperties() {
    assertThrows<IllegalStateException> {
      classBuilder()
        .primaryConstructor(
          FunSpec.constructorBuilder()
            .addParameter("avocado", String::class)
            .build()
        )
        .addProperty(
          PropertySpec.builder("avocado", String::class)
            .initializer("avocado")
            .mutable()
            .build()
        )
        .build()
    }.hasMessageThat().isEqualTo("value/inline classes must have a single read-only (val) property parameter.")
  }

  @Test
  fun inlineClassWithPrivateConstructor() {
    val guacamole = classBuilder()
      .primaryConstructor(
        FunSpec.constructorBuilder()
          .addParameter("avocado", String::class)
          .addModifiers(PRIVATE)
          .build()
      )
      .addProperty(
        PropertySpec.builder("avocado", String::class)
          .initializer("avocado")
          .build()
      )
      .build()

    assertThat(guacamole.toString()).isEqualTo(
      """
      |public $modifierString class Guacamole private constructor(
      |  public val avocado: kotlin.String
      |)
      |""".trimMargin()
    )
  }

  @Test fun inlineEnumClass() {
    val guacamole = TypeSpec.enumBuilder("Foo")
      .addModifiers(modifier)
      .primaryConstructor(
        FunSpec.constructorBuilder()
          .addParameter("x", Int::class)
          .build()
      )
      .addEnumConstant(
        "A",
        TypeSpec.anonymousClassBuilder()
          .addSuperclassConstructorParameter("%L", 1)
          .build()
      )
      .addEnumConstant(
        "B",
        TypeSpec.anonymousClassBuilder()
          .addSuperclassConstructorParameter("%L", 2)
          .build()
      )
      .addProperty(
        PropertySpec.builder("x", Int::class)
          .initializer("x")
          .build()
      )
      .build()
    assertThat(guacamole.toString()).isEqualTo(
      """
      |public enum $modifierString class Foo(
      |  public val x: kotlin.Int
      |) {
      |  A(1),
      |  B(2),
      |  ;
      |}
      |""".trimMargin()
    )
  }
}
