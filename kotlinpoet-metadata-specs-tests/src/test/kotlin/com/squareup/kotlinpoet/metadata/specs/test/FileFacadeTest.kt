package com.squareup.kotlinpoet.metadata.specs.test

import com.google.common.truth.Truth.assertThat
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.specs.test.MultiClassInspectorTest.ClassInspectorType.ELEMENTS
import com.squareup.kotlinpoet.metadata.specs.test.MultiClassInspectorTest.ClassInspectorType.REFLECTIVE
import org.junit.Test

@KotlinPoetMetadataPreview
class FacadeFileTest : MultiClassInspectorTest() {

  @IgnoreForHandlerType(
      handlerType = ELEMENTS,
      reason = "Elements can detect JvmOverloads, JvmName not possible in reflection"
  )
  @Test
  fun facadeFile_reflective() {
    val fileSpec = Class.forName(
        "com.squareup.kotlinpoet.metadata.specs.test.FacadeFile").kotlin.toFileSpecWithTestHandler()
    assertThat(fileSpec.name).isEqualTo("FacadeFile")
    //language=kotlin
    assertThat(fileSpec.trimmedToString()).isEqualTo("""
      @file:JvmName(name = "FacadeFile")
      @file:FileAnnotation(value = "file annotations!")

      package com.squareup.kotlinpoet.metadata.specs.test

      import com.squareup.kotlinpoet.metadata.specs.test.FileAnnotation
      import kotlin.Boolean
      import kotlin.Double
      import kotlin.Float
      import kotlin.Int
      import kotlin.Long
      import kotlin.String
      import kotlin.Unit
      import kotlin.collections.List
      import kotlin.jvm.JvmField
      import kotlin.jvm.JvmName
      import kotlin.jvm.JvmSynthetic
      import kotlin.jvm.Synchronized

      @JvmName(name = "jvmStaticFunction")
      fun jvmNameFunction(): Unit {
      }

      fun jvmOverloads(
        param1: String,
        optionalParam2: String = throw NotImplementedError("Stub!"),
        nullableParam3: String? = throw NotImplementedError("Stub!")
      ): Unit {
      }

      fun regularFun(): Unit {
      }

      @Synchronized
      fun synchronizedFun(): Unit {
      }

      val BINARY_PROP: Int = 11

      val BOOL_PROP: Boolean = false

      const val CONST_BINARY_PROP: Int = 11

      const val CONST_BOOL_PROP: Boolean = false

      const val CONST_DOUBLE_PROP: Double = 1.0

      const val CONST_FLOAT_PROP: Float = 1.0F

      const val CONST_HEX_PROP: Int = 15

      const val CONST_INT_PROP: Int = 1

      const val CONST_LONG_PROP: Long = 1L

      const val CONST_STRING_PROP: String = "prop"

      const val CONST_UNDERSCORES_HEX_PROP: Long = 4293713502L

      const val CONST_UNDERSCORES_PROP: Int = 1000000

      val DOUBLE_PROP: Double = 1.0

      val FLOAT_PROP: Float = 1.0F

      val HEX_PROP: Int = 15

      val INT_PROP: Int = 1

      val LONG_PROP: Long = 1L

      val STRING_PROP: String = "prop"

      val UNDERSCORES_HEX_PROP: Long = 4293713502L

      val UNDERSCORES_PROP: Int = 1000000

      var VAR_BINARY_PROP: Int = throw NotImplementedError("Stub!")

      var VAR_BOOL_PROP: Boolean = throw NotImplementedError("Stub!")

      var VAR_DOUBLE_PROP: Double = throw NotImplementedError("Stub!")

      var VAR_FLOAT_PROP: Float = throw NotImplementedError("Stub!")

      var VAR_HEX_PROP: Int = throw NotImplementedError("Stub!")

      var VAR_INT_PROP: Int = throw NotImplementedError("Stub!")

      var VAR_LONG_PROP: Long = throw NotImplementedError("Stub!")

      var VAR_STRING_PROP: String = throw NotImplementedError("Stub!")

      var VAR_UNDERSCORES_HEX_PROP: Long = throw NotImplementedError("Stub!")

      var VAR_UNDERSCORES_PROP: Int = throw NotImplementedError("Stub!")

      @field:JvmSynthetic
      @JvmField
      val syntheticFieldProperty: String? = null

      @field:JvmSynthetic
      val syntheticProperty: String? = null

      @get:JvmSynthetic
      val syntheticPropertyGet: String? = null

      @get:JvmSynthetic
      @set:JvmSynthetic
      var syntheticPropertyGetAndSet: String? = null

      @set:JvmSynthetic
      var syntheticPropertySet: String? = null

      typealias FacadeGenericTypeAlias = List<String>

      typealias FacadeNestedTypeAlias = List<GenericTypeAlias>

      typealias FacadeTypeAliasName = String
    """.trimIndent())
  }

  @IgnoreForHandlerType(
      handlerType = REFLECTIVE,
      reason = "Elements can detect JvmOverloads, JvmName not possible in reflection"
  )
  @Test
  fun facadeFile_elements() {
    val fileSpec = Class.forName(
        "com.squareup.kotlinpoet.metadata.specs.test.FacadeFile").kotlin.toFileSpecWithTestHandler()
    assertThat(fileSpec.name).isEqualTo("FacadeFile")
    //language=kotlin
    assertThat(fileSpec.trimmedToString()).isEqualTo("""
      @file:FileAnnotation(value = "file annotations!")
      @file:JvmName(name = "FacadeFile")

      package com.squareup.kotlinpoet.metadata.specs.test

      import com.squareup.kotlinpoet.metadata.specs.test.FileAnnotation
      import kotlin.Boolean
      import kotlin.Double
      import kotlin.Float
      import kotlin.Int
      import kotlin.Long
      import kotlin.String
      import kotlin.Unit
      import kotlin.collections.List
      import kotlin.jvm.JvmName
      import kotlin.jvm.JvmOverloads
      import kotlin.jvm.JvmSynthetic
      import kotlin.jvm.Synchronized

      @JvmName(name = "jvmStaticFunction")
      fun jvmNameFunction(): Unit {
      }

      @JvmOverloads
      fun jvmOverloads(
        param1: String,
        optionalParam2: String = throw NotImplementedError("Stub!"),
        nullableParam3: String? = throw NotImplementedError("Stub!")
      ): Unit {
      }

      fun regularFun(): Unit {
      }

      @Synchronized
      fun synchronizedFun(): Unit {
      }

      val BINARY_PROP: Int = 11

      val BOOL_PROP: Boolean = false

      const val CONST_BINARY_PROP: Int = 11

      const val CONST_BOOL_PROP: Boolean = false

      const val CONST_DOUBLE_PROP: Double = 1.0

      const val CONST_FLOAT_PROP: Float = 1.0F

      const val CONST_HEX_PROP: Int = 15

      const val CONST_INT_PROP: Int = 1

      const val CONST_LONG_PROP: Long = 1L

      const val CONST_STRING_PROP: String = "prop"

      const val CONST_UNDERSCORES_HEX_PROP: Long = 4293713502L

      const val CONST_UNDERSCORES_PROP: Int = 1000000

      val DOUBLE_PROP: Double = 1.0

      val FLOAT_PROP: Float = 1.0F

      val HEX_PROP: Int = 15

      val INT_PROP: Int = 1

      val LONG_PROP: Long = 1L

      val STRING_PROP: String = "prop"

      val UNDERSCORES_HEX_PROP: Long = 4293713502L

      val UNDERSCORES_PROP: Int = 1000000

      var VAR_BINARY_PROP: Int = throw NotImplementedError("Stub!")

      var VAR_BOOL_PROP: Boolean = throw NotImplementedError("Stub!")

      var VAR_DOUBLE_PROP: Double = throw NotImplementedError("Stub!")

      var VAR_FLOAT_PROP: Float = throw NotImplementedError("Stub!")

      var VAR_HEX_PROP: Int = throw NotImplementedError("Stub!")

      var VAR_INT_PROP: Int = throw NotImplementedError("Stub!")

      var VAR_LONG_PROP: Long = throw NotImplementedError("Stub!")

      var VAR_STRING_PROP: String = throw NotImplementedError("Stub!")

      var VAR_UNDERSCORES_HEX_PROP: Long = throw NotImplementedError("Stub!")

      var VAR_UNDERSCORES_PROP: Int = throw NotImplementedError("Stub!")

      @field:JvmSynthetic
      val syntheticFieldProperty: String? = null

      @field:JvmSynthetic
      val syntheticProperty: String? = null

      @get:JvmSynthetic
      val syntheticPropertyGet: String? = null

      @get:JvmSynthetic
      @set:JvmSynthetic
      var syntheticPropertyGetAndSet: String? = null

      @set:JvmSynthetic
      var syntheticPropertySet: String? = null

      typealias FacadeGenericTypeAlias = List<String>

      typealias FacadeNestedTypeAlias = List<GenericTypeAlias>

      typealias FacadeTypeAliasName = String
    """.trimIndent())
  }

  @IgnoreForHandlerType(
      handlerType = ELEMENTS,
      reason = "JvmName not possible in reflection"
  )
  @Test
  fun noJvmName_reflective() {
    val fileSpec = Class.forName(
        "com.squareup.kotlinpoet.metadata.specs.test.NoJvmNameFacadeFileKt").kotlin.toFileSpecWithTestHandler()
    assertThat(fileSpec.name).isEqualTo("NoJvmNameFacadeFile")
    //language=kotlin
    assertThat(fileSpec.trimmedToString()).isEqualTo("""
      package com.squareup.kotlinpoet.metadata.specs.test

      import kotlin.String

      val prop: String = ""
    """.trimIndent())
  }

  @IgnoreForHandlerType(
      handlerType = REFLECTIVE,
      reason = "JvmName not possible in reflection"
  )
  @Test
  fun noJvmName_elements() {
    val fileSpec = Class.forName(
        "com.squareup.kotlinpoet.metadata.specs.test.NoJvmNameFacadeFileKt").kotlin.toFileSpecWithTestHandler()
    assertThat(fileSpec.name).isEqualTo("NoJvmNameFacadeFile")
    //language=kotlin
    assertThat(fileSpec.trimmedToString()).isEqualTo("""
      package com.squareup.kotlinpoet.metadata.specs.test

      import kotlin.String

      val prop: String = ""
    """.trimIndent())
  }

  @IgnoreForHandlerType(
      handlerType = ELEMENTS,
      reason = "JvmName not possible in reflection"
  )
  @Test
  fun jvmName_with_kt_reflective() {
    val fileSpec = Class.forName(
        "com.squareup.kotlinpoet.metadata.specs.test.JvmNameKt").kotlin.toFileSpecWithTestHandler()
    assertThat(fileSpec.name).isEqualTo("JvmName")
    //language=kotlin
    assertThat(fileSpec.trimmedToString()).isEqualTo("""
      package com.squareup.kotlinpoet.metadata.specs.test

      import kotlin.String

      val prop2: String = ""
    """.trimIndent())
  }

  @IgnoreForHandlerType(
      handlerType = REFLECTIVE,
      reason = "JvmName not possible in reflection"
  )
  @Test
  fun jvmName_with_kt_elements() {
    val fileSpec = Class.forName(
        "com.squareup.kotlinpoet.metadata.specs.test.JvmNameKt").kotlin.toFileSpecWithTestHandler()
    assertThat(fileSpec.name).isEqualTo("JvmName")
    //language=kotlin
    assertThat(fileSpec.trimmedToString()).isEqualTo("""
      @file:JvmName(name = "JvmNameKt")

      package com.squareup.kotlinpoet.metadata.specs.test

      import kotlin.String
      import kotlin.jvm.JvmName

      val prop2: String = ""
    """.trimIndent())
  }
}

private fun FileSpec.trimmedToString(): String {
  return buildString { writeTo(this) }.trim()
}
