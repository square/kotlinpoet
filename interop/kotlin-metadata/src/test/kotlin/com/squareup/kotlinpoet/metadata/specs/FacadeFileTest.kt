/*
 * Copyright (C) 2021 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.kotlinpoet.metadata.specs

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.metadata.specs.MultiClassInspectorTest.ClassInspectorType.ELEMENTS
import com.squareup.kotlinpoet.metadata.specs.MultiClassInspectorTest.ClassInspectorType.REFLECTIVE
import org.junit.Test

class FacadeFileTest : MultiClassInspectorTest() {

  @IgnoreForHandlerType(
    handlerType = ELEMENTS,
    reason = "Elements can detect JvmOverloads, JvmName not possible in reflection",
  )
  @Test
  fun facadeFile_reflective() {
    val fileSpec = Class.forName(
      "com.squareup.kotlinpoet.metadata.specs.FacadeFile",
    ).kotlin.toFileSpecWithTestHandler()
    assertThat(fileSpec.name).isEqualTo("FacadeFile")
    //language=kotlin
    assertThat(fileSpec.trimmedToString()).isEqualTo(
      """
      @file:JvmName(name = "FacadeFile")
      @file:FileAnnotation(value = "file annotations!")

      package com.squareup.kotlinpoet.metadata.specs

      import com.squareup.kotlinpoet.metadata.specs.FileAnnotation
      import kotlin.Boolean
      import kotlin.Double
      import kotlin.Float
      import kotlin.Int
      import kotlin.Long
      import kotlin.String
      import kotlin.collections.List
      import kotlin.jvm.JvmField
      import kotlin.jvm.JvmName
      import kotlin.jvm.JvmSynthetic
      import kotlin.jvm.Synchronized

      @JvmName(name = "jvmStaticFunction")
      public fun jvmNameFunction() {
      }

      public fun regularFun() {
      }

      @Synchronized
      public fun synchronizedFun() {
      }

      public fun jvmOverloads(
        param1: String,
        optionalParam2: String = throw NotImplementedError("Stub!"),
        nullableParam3: String? = throw NotImplementedError("Stub!"),
      ) {
      }

      public val BOOL_PROP: Boolean = false

      public val BINARY_PROP: Int = 11

      public val INT_PROP: Int = 1

      public val UNDERSCORES_PROP: Int = 1_000_000

      public val HEX_PROP: Int = 15

      public val UNDERSCORES_HEX_PROP: Long = 4_293_713_502L

      public val LONG_PROP: Long = 1L

      public val FLOAT_PROP: Float = 1.0F

      public val DOUBLE_PROP: Double = 1.0

      public val STRING_PROP: String = "prop"

      public var VAR_BOOL_PROP: Boolean = throw NotImplementedError("Stub!")

      public var VAR_BINARY_PROP: Int = throw NotImplementedError("Stub!")

      public var VAR_INT_PROP: Int = throw NotImplementedError("Stub!")

      public var VAR_UNDERSCORES_PROP: Int = throw NotImplementedError("Stub!")

      public var VAR_HEX_PROP: Int = throw NotImplementedError("Stub!")

      public var VAR_UNDERSCORES_HEX_PROP: Long = throw NotImplementedError("Stub!")

      public var VAR_LONG_PROP: Long = throw NotImplementedError("Stub!")

      public var VAR_FLOAT_PROP: Float = throw NotImplementedError("Stub!")

      public var VAR_DOUBLE_PROP: Double = throw NotImplementedError("Stub!")

      public var VAR_STRING_PROP: String = throw NotImplementedError("Stub!")

      public const val CONST_BOOL_PROP: Boolean = false

      public const val CONST_BINARY_PROP: Int = 11

      public const val CONST_INT_PROP: Int = 1

      public const val CONST_UNDERSCORES_PROP: Int = 1_000_000

      public const val CONST_HEX_PROP: Int = 15

      public const val CONST_UNDERSCORES_HEX_PROP: Long = 4_293_713_502L

      public const val CONST_LONG_PROP: Long = 1L

      public const val CONST_FLOAT_PROP: Float = 1.0F

      public const val CONST_DOUBLE_PROP: Double = 1.0

      public const val CONST_STRING_PROP: String = "prop"

      @field:JvmSynthetic
      @JvmField
      public val syntheticFieldProperty: String? = null

      @field:JvmSynthetic
      public val syntheticProperty: String? = null

      @get:JvmSynthetic
      public val syntheticPropertyGet: String? = null

      @get:JvmSynthetic
      @set:JvmSynthetic
      public var syntheticPropertyGetAndSet: String? = null

      @set:JvmSynthetic
      public var syntheticPropertySet: String? = null

      public typealias FacadeTypeAliasName = String

      public typealias FacadeGenericTypeAlias = List<String>

      public typealias FacadeNestedTypeAlias = List<GenericTypeAlias>
      """.trimIndent(),
    )
  }

  @IgnoreForHandlerType(
    handlerType = REFLECTIVE,
    reason = "Elements can detect JvmOverloads, JvmName not possible in reflection",
  )
  @Test
  fun facadeFile_elements() {
    val fileSpec = Class.forName(
      "com.squareup.kotlinpoet.metadata.specs.FacadeFile",
    ).kotlin.toFileSpecWithTestHandler()
    assertThat(fileSpec.name).isEqualTo("FacadeFile")
    //language=kotlin
    assertThat(fileSpec.trimmedToString()).isEqualTo(
      """
      @file:FileAnnotation(value = "file annotations!")
      @file:JvmName(name = "FacadeFile")

      package com.squareup.kotlinpoet.metadata.specs

      import com.squareup.kotlinpoet.metadata.specs.FileAnnotation
      import kotlin.Boolean
      import kotlin.Double
      import kotlin.Float
      import kotlin.Int
      import kotlin.Long
      import kotlin.String
      import kotlin.collections.List
      import kotlin.jvm.JvmName
      import kotlin.jvm.JvmOverloads
      import kotlin.jvm.JvmSynthetic
      import kotlin.jvm.Synchronized

      @JvmName(name = "jvmStaticFunction")
      public fun jvmNameFunction() {
      }

      public fun regularFun() {
      }

      @Synchronized
      public fun synchronizedFun() {
      }

      @JvmOverloads
      public fun jvmOverloads(
        param1: String,
        optionalParam2: String = throw NotImplementedError("Stub!"),
        nullableParam3: String? = throw NotImplementedError("Stub!"),
      ) {
      }

      public val BOOL_PROP: Boolean = throw NotImplementedError("Stub!")

      public val BINARY_PROP: Int = throw NotImplementedError("Stub!")

      public val INT_PROP: Int = throw NotImplementedError("Stub!")

      public val UNDERSCORES_PROP: Int = throw NotImplementedError("Stub!")

      public val HEX_PROP: Int = throw NotImplementedError("Stub!")

      public val UNDERSCORES_HEX_PROP: Long = throw NotImplementedError("Stub!")

      public val LONG_PROP: Long = throw NotImplementedError("Stub!")

      public val FLOAT_PROP: Float = throw NotImplementedError("Stub!")

      public val DOUBLE_PROP: Double = throw NotImplementedError("Stub!")

      public val STRING_PROP: String = throw NotImplementedError("Stub!")

      public var VAR_BOOL_PROP: Boolean = throw NotImplementedError("Stub!")

      public var VAR_BINARY_PROP: Int = throw NotImplementedError("Stub!")

      public var VAR_INT_PROP: Int = throw NotImplementedError("Stub!")

      public var VAR_UNDERSCORES_PROP: Int = throw NotImplementedError("Stub!")

      public var VAR_HEX_PROP: Int = throw NotImplementedError("Stub!")

      public var VAR_UNDERSCORES_HEX_PROP: Long = throw NotImplementedError("Stub!")

      public var VAR_LONG_PROP: Long = throw NotImplementedError("Stub!")

      public var VAR_FLOAT_PROP: Float = throw NotImplementedError("Stub!")

      public var VAR_DOUBLE_PROP: Double = throw NotImplementedError("Stub!")

      public var VAR_STRING_PROP: String = throw NotImplementedError("Stub!")

      public const val CONST_BOOL_PROP: Boolean = false

      public const val CONST_BINARY_PROP: Int = 11

      public const val CONST_INT_PROP: Int = 1

      public const val CONST_UNDERSCORES_PROP: Int = 1_000_000

      public const val CONST_HEX_PROP: Int = 15

      public const val CONST_UNDERSCORES_HEX_PROP: Long = 4_293_713_502L

      public const val CONST_LONG_PROP: Long = 1L

      public const val CONST_FLOAT_PROP: Float = 1.0F

      public const val CONST_DOUBLE_PROP: Double = 1.0

      public const val CONST_STRING_PROP: String = "prop"

      @field:JvmSynthetic
      public val syntheticFieldProperty: String? = null

      @field:JvmSynthetic
      public val syntheticProperty: String? = null

      @get:JvmSynthetic
      public val syntheticPropertyGet: String? = null

      @get:JvmSynthetic
      @set:JvmSynthetic
      public var syntheticPropertyGetAndSet: String? = null

      @set:JvmSynthetic
      public var syntheticPropertySet: String? = null

      public typealias FacadeTypeAliasName = String

      public typealias FacadeGenericTypeAlias = List<String>

      public typealias FacadeNestedTypeAlias = List<GenericTypeAlias>
      """.trimIndent(),
    )
  }

  @IgnoreForHandlerType(
    handlerType = ELEMENTS,
    reason = "JvmName not possible in reflection",
  )
  @Test
  fun noJvmName_reflective() {
    val fileSpec = Class.forName(
      "com.squareup.kotlinpoet.metadata.specs.NoJvmNameFacadeFileKt",
    ).kotlin.toFileSpecWithTestHandler()
    assertThat(fileSpec.name).isEqualTo("NoJvmNameFacadeFile")
    //language=kotlin
    assertThat(fileSpec.trimmedToString()).isEqualTo(
      """
      package com.squareup.kotlinpoet.metadata.specs

      import kotlin.String

      public val prop: String = ""
      """.trimIndent(),
    )
  }

  @IgnoreForHandlerType(
    handlerType = REFLECTIVE,
    reason = "JvmName not possible in reflection",
  )
  @Test
  fun noJvmName_elements() {
    val fileSpec = Class.forName(
      "com.squareup.kotlinpoet.metadata.specs.NoJvmNameFacadeFileKt",
    ).kotlin.toFileSpecWithTestHandler()
    assertThat(fileSpec.name).isEqualTo("NoJvmNameFacadeFile")
    //language=kotlin
    assertThat(fileSpec.trimmedToString()).isEqualTo(
      """
      package com.squareup.kotlinpoet.metadata.specs

      import kotlin.String

      public val prop: String = throw NotImplementedError("Stub!")
      """.trimIndent(),
    )
  }

  @IgnoreForHandlerType(
    handlerType = ELEMENTS,
    reason = "JvmName not possible in reflection",
  )
  @Test
  fun jvmName_with_kt_reflective() {
    val fileSpec = Class.forName(
      "com.squareup.kotlinpoet.metadata.specs.JvmNameKt",
    ).kotlin.toFileSpecWithTestHandler()
    assertThat(fileSpec.name).isEqualTo("JvmName")
    //language=kotlin
    assertThat(fileSpec.trimmedToString()).isEqualTo(
      """
      package com.squareup.kotlinpoet.metadata.specs

      import kotlin.String

      public val prop2: String = ""
      """.trimIndent(),
    )
  }

  @IgnoreForHandlerType(
    handlerType = REFLECTIVE,
    reason = "JvmName not possible in reflection",
  )
  @Test
  fun jvmName_with_kt_elements() {
    val fileSpec = Class.forName(
      "com.squareup.kotlinpoet.metadata.specs.JvmNameKt",
    ).kotlin.toFileSpecWithTestHandler()
    assertThat(fileSpec.name).isEqualTo("JvmName")
    //language=kotlin
    assertThat(fileSpec.trimmedToString()).isEqualTo(
      """
      @file:JvmName(name = "JvmNameKt")

      package com.squareup.kotlinpoet.metadata.specs

      import kotlin.String
      import kotlin.jvm.JvmName

      public val prop2: String = throw NotImplementedError("Stub!")
      """.trimIndent(),
    )
  }
}

private fun FileSpec.trimmedToString(): String {
  return buildString { writeTo(this) }.trim()
}
