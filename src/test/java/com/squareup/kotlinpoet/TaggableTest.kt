package com.squareup.kotlinpoet

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class TaggableTest(val builder: Taggable.Builder<*>) {

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "{0}")
    fun data() = arrayOf(
        AnnotationSpec.builder(JvmStatic::class),
        FileSpec.builder("test", "Test"),
        FunSpec.builder("test"),
        ParameterSpec.builder("test", String::class.asClassName()),
        PropertySpec.builder("test", String::class.asClassName()),
        TypeAliasSpec.builder("Test", String::class.asClassName()),
        TypeSpec.classBuilder("Test")
    )
  }

  @Before fun setUp() {
    builder.tags.clear()
  }

  @Test fun builderShouldMakeDefensiveCopy() {
    builder.tag(String::class, "test")
    val taggable = builder.buildTaggable()
    builder.tags.remove(String::class)
    assertThat(taggable.tag<String>()).isEqualTo("test")
  }

  @Test fun missingShouldBeNull() {
    val taggable = builder.buildTaggable()
    assertThat(taggable.tag<Int>()).isNull()
  }

  @Test fun kclassParamFlow() {
    builder.tag(String::class, "test")
    val taggable = builder.buildTaggable()
    assertThat(taggable.tag(String::class)).isEqualTo("test")
  }

  @Test fun javaClassParamFlow() {
    builder.tag(String::class.java, "test")
    val taggable = builder.buildTaggable()
    assertThat(taggable.tag(String::class.java)).isEqualTo("test")
  }

  @Test fun kclassInJavaClassOut() {
    builder.tag(String::class, "test")
    val taggable = builder.buildTaggable()
    assertThat(taggable.tag(String::class.java)).isEqualTo("test")
  }

  @Test fun javaClassInkClassOut() {
    builder.tag(String::class.java, "test")
    val taggable = builder.buildTaggable()
    assertThat(taggable.tag(String::class)).isEqualTo("test")
  }

  private fun Taggable.Builder<*>.buildTaggable(): Taggable {
    return when (this) {
      is AnnotationSpec.Builder -> build()
      is FileSpec.Builder -> build()
      is FunSpec.Builder -> build()
      is ParameterSpec.Builder -> build()
      is PropertySpec.Builder -> build()
      is TypeAliasSpec.Builder -> build()
      is TypeSpec.Builder -> build()
      else -> TODO("Unsupported type ${this::class.simpleName}")
    }
  }
}