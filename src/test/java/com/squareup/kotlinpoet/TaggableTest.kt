package com.squareup.kotlinpoet

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertFails

@RunWith(Parameterized::class)
class TaggableTest(val builder: Taggable.Builder) {

  companion object {
    @JvmStatic
    @Parameterized.Parameters
    fun data() = listOf(
        arrayOf(AnnotationSpec.builder(JvmStatic::class)),
        arrayOf(FileSpec.builder("test", "Test")),
        arrayOf(FunSpec.builder("test")),
        arrayOf(ParameterSpec.builder("test", String::class.asClassName())),
        arrayOf(PropertySpec.builder("test", String::class.asClassName())),
        arrayOf(TypeAliasSpec.builder("Test", String::class.asClassName())),
        arrayOf(TypeSpec.classBuilder("Test"))
    )
  }

  @Before fun setUp() {
    builder.tags.clear()
  }

  @Test fun classCastOnValueTypeMismatch() {
    builder.tag<String>("test")
    val taggable = builder.buildTaggable()
    assertFails("Should have a class cast exception!") {
      val tag = taggable.tag<String, Int>()
    }
  }

  @Test fun builderShouldMakeDefensiveCopy() {
    builder.tag<String>("test")
    val taggable = builder.buildTaggable()
    builder.tags.remove(String::class)
    assertThat(taggable.tag<String, String>()).isEqualTo("test")
  }

  @Test fun missingShouldBeNull() {
    builder.tag<String>("test")
    val taggable = builder.buildTaggable()
    assertThat(taggable.tag<Int, String>()).isNull()
  }

  @Test fun kclassParamFlow() {
    builder.tag(Int::class, "test")
    val taggable = builder.buildTaggable()
    assertThat(taggable.tag<String>(Int::class)).isEqualTo("test")
  }

  @Test fun javaClassParamFlow() {
    builder.tag(Int::class.java, "test")
    val taggable = builder.buildTaggable()
    assertThat(taggable.tag<String>(Int::class.java)).isEqualTo("test")
  }

  @Test fun kclassInJavaClassOut() {
    builder.tag(Int::class, "test")
    val taggable = builder.buildTaggable()
    assertThat(taggable.tag<String>(Int::class.java)).isEqualTo("test")
  }

  @Test fun javaClassInkClassOut() {
    builder.tag(Int::class.java, "test")
    val taggable = builder.buildTaggable()
    assertThat(taggable.tag<String>(Int::class)).isEqualTo("test")
  }

  private fun Taggable.Builder.buildTaggable(): Taggable {
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