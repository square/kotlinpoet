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
    //language=kotlin
    assertThat(fileSpec.trimmedToString()).isEqualTo("""
      @file:JvmName(name = "FacadeFile")
      @file:FileAnnotation(value = "file annotations!")
      
      package com.squareup.kotlinpoet.metadata.specs.test
      
      import com.squareup.kotlinpoet.metadata.specs.test.FileAnnotation
      import kotlin
      import kotlin.jvm.JvmName
      import kotlin.jvm.JvmSynthetic
      import kotlin.jvm.Synchronized

      @JvmName(name = "jvmStaticFunction")
      fun jvmNameFunction() {
      }

      fun jvmOverloads(
        param1: kotlin.String,
        optionalParam2: kotlin.String = throw NotImplementedError("Stub!"),
        nullableParam3: kotlin.String? = throw NotImplementedError("Stub!")
      ) {
      }

      fun regularFun() {
      }

      @Synchronized
      fun synchronizedFun() {
      }

      val BINARY_PROP: kotlin.Int = 11

      val BOOL_PROP: kotlin.Boolean = false

      const val CONST_BINARY_PROP: kotlin.Int = 11

      const val CONST_BOOL_PROP: kotlin.Boolean = false

      const val CONST_DOUBLE_PROP: kotlin.Double = 1.0

      const val CONST_FLOAT_PROP: kotlin.Float = 1.0F

      const val CONST_HEX_PROP: kotlin.Int = 15

      const val CONST_INT_PROP: kotlin.Int = 1

      const val CONST_LONG_PROP: kotlin.Long = 1L

      const val CONST_STRING_PROP: kotlin.String = "prop"

      const val CONST_UNDERSCORES_HEX_PROP: kotlin.Long = 4293713502L

      const val CONST_UNDERSCORES_PROP: kotlin.Int = 1000000

      val DOUBLE_PROP: kotlin.Double = 1.0

      val FLOAT_PROP: kotlin.Float = 1.0F

      val HEX_PROP: kotlin.Int = 15

      val INT_PROP: kotlin.Int = 1

      val LONG_PROP: kotlin.Long = 1L

      val STRING_PROP: kotlin.String = "prop"

      val UNDERSCORES_HEX_PROP: kotlin.Long = 4293713502L

      val UNDERSCORES_PROP: kotlin.Int = 1000000

      var VAR_BINARY_PROP: kotlin.Int = throw NotImplementedError("Stub!")

      var VAR_BOOL_PROP: kotlin.Boolean = throw NotImplementedError("Stub!")

      var VAR_DOUBLE_PROP: kotlin.Double = throw NotImplementedError("Stub!")

      var VAR_FLOAT_PROP: kotlin.Float = throw NotImplementedError("Stub!")

      var VAR_HEX_PROP: kotlin.Int = throw NotImplementedError("Stub!")

      var VAR_INT_PROP: kotlin.Int = throw NotImplementedError("Stub!")

      var VAR_LONG_PROP: kotlin.Long = throw NotImplementedError("Stub!")

      var VAR_STRING_PROP: kotlin.String = throw NotImplementedError("Stub!")

      var VAR_UNDERSCORES_HEX_PROP: kotlin.Long = throw NotImplementedError("Stub!")

      var VAR_UNDERSCORES_PROP: kotlin.Int = throw NotImplementedError("Stub!")

      @field:JvmSynthetic
      val syntheticFieldProperty: kotlin.String? = null

      @field:JvmSynthetic
      val syntheticProperty: kotlin.String? = null

      @get:JvmSynthetic
      val syntheticPropertyGet: kotlin.String? = null

      @get:JvmSynthetic
      @set:JvmSynthetic
      var syntheticPropertyGetAndSet: kotlin.String? = null

      @set:JvmSynthetic
      var syntheticPropertySet: kotlin.String? = null

      typealias FacadeGenericTypeAlias = kotlin.collections.List<kotlin.String>

      typealias FacadeNestedTypeAlias = kotlin.collections.List<GenericTypeAlias>

      typealias FacadeTypeAliasName = kotlin.String
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
    //language=kotlin
    assertThat(fileSpec.trimmedToString()).isEqualTo("""
      @file:FileAnnotation(value = "file annotations!")
      @file:JvmName(name = "FacadeFile")
      
      package com.squareup.kotlinpoet.metadata.specs.test
      
      import com.squareup.kotlinpoet.metadata.specs.test.FileAnnotation
      import kotlin
      import kotlin.jvm.JvmName
      import kotlin.jvm.JvmOverloads
      import kotlin.jvm.JvmSynthetic
      import kotlin.jvm.Synchronized

      @JvmName(name = "jvmStaticFunction")
      fun jvmNameFunction() {
      }

      @JvmOverloads
      fun jvmOverloads(
        param1: kotlin.String,
        optionalParam2: kotlin.String = throw NotImplementedError("Stub!"),
        nullableParam3: kotlin.String? = throw NotImplementedError("Stub!")
      ) {
      }

      fun regularFun() {
      }

      @Synchronized
      fun synchronizedFun() {
      }

      val BINARY_PROP: kotlin.Int = 11

      val BOOL_PROP: kotlin.Boolean = false

      const val CONST_BINARY_PROP: kotlin.Int = 11

      const val CONST_BOOL_PROP: kotlin.Boolean = false

      const val CONST_DOUBLE_PROP: kotlin.Double = 1.0

      const val CONST_FLOAT_PROP: kotlin.Float = 1.0F

      const val CONST_HEX_PROP: kotlin.Int = 15

      const val CONST_INT_PROP: kotlin.Int = 1

      const val CONST_LONG_PROP: kotlin.Long = 1L

      const val CONST_STRING_PROP: kotlin.String = "prop"

      const val CONST_UNDERSCORES_HEX_PROP: kotlin.Long = 4293713502L

      const val CONST_UNDERSCORES_PROP: kotlin.Int = 1000000

      val DOUBLE_PROP: kotlin.Double = 1.0

      val FLOAT_PROP: kotlin.Float = 1.0F

      val HEX_PROP: kotlin.Int = 15

      val INT_PROP: kotlin.Int = 1

      val LONG_PROP: kotlin.Long = 1L

      val STRING_PROP: kotlin.String = "prop"

      val UNDERSCORES_HEX_PROP: kotlin.Long = 4293713502L

      val UNDERSCORES_PROP: kotlin.Int = 1000000

      var VAR_BINARY_PROP: kotlin.Int = throw NotImplementedError("Stub!")

      var VAR_BOOL_PROP: kotlin.Boolean = throw NotImplementedError("Stub!")

      var VAR_DOUBLE_PROP: kotlin.Double = throw NotImplementedError("Stub!")

      var VAR_FLOAT_PROP: kotlin.Float = throw NotImplementedError("Stub!")

      var VAR_HEX_PROP: kotlin.Int = throw NotImplementedError("Stub!")

      var VAR_INT_PROP: kotlin.Int = throw NotImplementedError("Stub!")

      var VAR_LONG_PROP: kotlin.Long = throw NotImplementedError("Stub!")

      var VAR_STRING_PROP: kotlin.String = throw NotImplementedError("Stub!")

      var VAR_UNDERSCORES_HEX_PROP: kotlin.Long = throw NotImplementedError("Stub!")

      var VAR_UNDERSCORES_PROP: kotlin.Int = throw NotImplementedError("Stub!")

      @field:JvmSynthetic
      val syntheticFieldProperty: kotlin.String? = null

      @field:JvmSynthetic
      val syntheticProperty: kotlin.String? = null

      @get:JvmSynthetic
      val syntheticPropertyGet: kotlin.String? = null

      @get:JvmSynthetic
      @set:JvmSynthetic
      var syntheticPropertyGetAndSet: kotlin.String? = null

      @set:JvmSynthetic
      var syntheticPropertySet: kotlin.String? = null

      typealias FacadeGenericTypeAlias = kotlin.collections.List<kotlin.String>

      typealias FacadeNestedTypeAlias = kotlin.collections.List<GenericTypeAlias>

      typealias FacadeTypeAliasName = kotlin.String
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
    //language=kotlin
    assertThat(fileSpec.trimmedToString()).isEqualTo("""
      package com.squareup.kotlinpoet.metadata.specs.test

      import kotlin

      val prop: kotlin.String = ""
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
    //language=kotlin
    assertThat(fileSpec.trimmedToString()).isEqualTo("""
      package com.squareup.kotlinpoet.metadata.specs.test

      import kotlin

      val prop: kotlin.String = ""
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
    //language=kotlin
    assertThat(fileSpec.trimmedToString()).isEqualTo("""
      package com.squareup.kotlinpoet.metadata.specs.test

      import kotlin

      val prop2: kotlin.String = ""
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
    //language=kotlin
    assertThat(fileSpec.trimmedToString()).isEqualTo("""
      @file:JvmName(name = "JvmNameKt")
  
      package com.squareup.kotlinpoet.metadata.specs.test

      import kotlin
      import kotlin.jvm.JvmName

      val prop2: kotlin.String = ""
    """.trimIndent())
  }

}

private fun FileSpec.trimmedToString(): String {
  return buildString { writeTo(this) }.trim()
}
