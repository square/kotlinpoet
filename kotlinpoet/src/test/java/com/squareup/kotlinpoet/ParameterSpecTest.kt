package com.squareup.kotlinpoet

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

class ParameterSpecTest {
  @Test fun equalsAndHashCode() {
    var a = ParameterSpec.builder("foo", Int::class)
      .build()
    var b = ParameterSpec.builder("foo", Int::class)
      .build()
    assertThat(a == b).isTrue()
    assertThat(a.hashCode()).isEqualTo(b.hashCode())
    a = ParameterSpec.builder("i", Int::class)
      .addModifiers(KModifier.FINAL)
      .build()
    b = ParameterSpec.builder("i", Int::class)
      .addModifiers(KModifier.FINAL)
      .build()
    assertThat(a == b).isTrue()
    assertThat(a.hashCode()).isEqualTo(b.hashCode())
  }

  @Test fun escapeKeywordInParameterName() {
    val parameterSpec = ParameterSpec.builder("if", String::class)
      .build()
    assertThat(parameterSpec.toString()).isEqualTo("`if`: kotlin.String")
  }

  @Test fun escapePunctuationInParameterName() {
    val parameterSpec = ParameterSpec.builder("with-hyphen", String::class)
      .build()
    assertThat(parameterSpec.toString()).isEqualTo("`with-hyphen`: kotlin.String")
  }

  @Test fun generalBuilderEqualityTest() {
    val parameterSpec = ParameterSpec.builder("Nuts", String::class)
      .addAnnotation(ClassName("com.squareup.kotlinpoet", "Food"))
      .addModifiers(KModifier.VARARG)
      .defaultValue("Almonds")
      .build()

    assertThat(parameterSpec.toBuilder().build()).isEqualTo(parameterSpec)
  }

  @Test fun modifyModifiers() {
    val builder = ParameterSpec
      .builder("word", String::class)
      .addModifiers(KModifier.PRIVATE)

    builder.modifiers.clear()
    builder.modifiers.add(KModifier.INTERNAL)

    assertThat(builder.build().modifiers).containsExactly(KModifier.INTERNAL)
  }

  @Test fun modifyAnnotations() {
    val builder = ParameterSpec
      .builder("word", String::class)
      .addAnnotation(
        AnnotationSpec.builder(JvmName::class.asClassName())
          .addMember("name = %S", "jvmWord")
          .build()
      )

    val javaWord = AnnotationSpec.builder(JvmName::class.asClassName())
      .addMember("name = %S", "javaWord")
      .build()
    builder.annotations.clear()
    builder.annotations.add(javaWord)

    assertThat(builder.build().annotations).containsExactly(javaWord)
  }

  // https://github.com/square/kotlinpoet/issues/462
  @Test fun codeBlockDefaultValue() {
    val param = ParameterSpec.builder("arg", ANY).build()
    val defaultValue = CodeBlock.builder()
      .beginControlFlow("{ %L ->", param)
      .addStatement("println(\"arg=\$%N\")", param)
      .endControlFlow()
      .build()
    val lambdaTypeName = ClassName.bestGuess("com.example.SomeTypeAlias")
    val paramSpec = ParameterSpec.builder("parameter", lambdaTypeName)
      .defaultValue(defaultValue)
      .build()
    assertThat(paramSpec.toString()).isEqualTo(
      """
      |parameter: com.example.SomeTypeAlias = { arg: kotlin.Any ->
      |  println("arg=${'$'}arg")
      |}
      |""".trimMargin()
    )
  }

  @Test fun annotatedLambdaType() {
    val annotation = AnnotationSpec.builder(ClassName("com.squareup.tacos", "Annotation")).build()
    val type = LambdaTypeName.get(returnType = UNIT).copy(annotations = listOf(annotation))
    val spec = FileSpec.builder("com.squareup.tacos", "Taco")
      .addFunction(
        FunSpec.builder("foo")
          .addParameter("bar", type)
          .build()
      )
      .build()
    assertThat(spec.toString()).isEqualTo(
      """
      |package com.squareup.tacos
      |
      |import kotlin.Unit
      |
      |public fun foo(bar: @Annotation () -> Unit): Unit {
      |}
      |""".trimMargin()
    )
  }

  @Test fun doublePropertyInitialization() {
    val codeBlockDefaultValue = ParameterSpec.builder("listA", String::class)
      .defaultValue(CodeBlock.builder().add("foo").build())
      .defaultValue(CodeBlock.builder().add("bar").build())
      .build()

    assertThat(CodeBlock.of("bar")).isEqualTo(codeBlockDefaultValue.defaultValue)

    val formatDefaultValue = ParameterSpec.builder("listA", String::class)
      .defaultValue("foo")
      .defaultValue("bar")
      .build()

    assertThat(CodeBlock.of("bar")).isEqualTo(formatDefaultValue.defaultValue)
  }
}
