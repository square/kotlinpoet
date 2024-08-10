/*
 * Copyright (C) 2018 Square, Inc.
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
@file:JvmName("JvmAnnotations")

package com.squareup.kotlinpoet.jvm

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.AnnotationSpec.UseSiteTarget.FILE
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.FunSpec.Companion.isAccessor
import com.squareup.kotlinpoet.FunSpec.Companion.isConstructor
import com.squareup.kotlinpoet.JvmFieldClassName
import com.squareup.kotlinpoet.JvmInlineClassName
import com.squareup.kotlinpoet.JvmMultifileClassClassName
import com.squareup.kotlinpoet.JvmNameClassName
import com.squareup.kotlinpoet.JvmOverloadsClassName
import com.squareup.kotlinpoet.JvmRecordClassName
import com.squareup.kotlinpoet.JvmStaticClassName
import com.squareup.kotlinpoet.JvmSuppressWildcardsClassName
import com.squareup.kotlinpoet.JvmThrowsClassName
import com.squareup.kotlinpoet.JvmVolatileClassName
import com.squareup.kotlinpoet.JvmWildcardClassName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.StrictfpClassName
import com.squareup.kotlinpoet.SynchronizedClassName
import com.squareup.kotlinpoet.TransientClassName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.jvm.alias.JvmType
import kotlin.jvm.JvmName
import kotlin.reflect.KClass

public fun FileSpec.Builder.jvmName(name: String): FileSpec.Builder = addAnnotation(
  AnnotationSpec.builder(JvmNameClassName)
    .useSiteTarget(FILE)
    .addMember("%S", name)
    .build(),
)

public fun FileSpec.Builder.jvmMultifileClass(): FileSpec.Builder = addAnnotation(
  AnnotationSpec.builder(JvmMultifileClassClassName)
    .useSiteTarget(FILE)
    .build(),
)

public fun TypeSpec.Builder.jvmSuppressWildcards(suppress: Boolean = true): TypeSpec.Builder =
  addAnnotation(jvmSuppressWildcardsAnnotation(suppress))

private fun jvmSuppressWildcardsAnnotation(suppress: Boolean = true) =
  AnnotationSpec.builder(JvmSuppressWildcardsClassName)
    .apply { if (!suppress) addMember("suppress = false") }
    .build()

public fun TypeSpec.Builder.jvmInline(): TypeSpec.Builder = addAnnotation(JvmInlineClassName)

public fun TypeSpec.Builder.jvmRecord(): TypeSpec.Builder = addAnnotation(JvmRecordClassName)

public fun FunSpec.Builder.jvmStatic(): FunSpec.Builder = apply {
  check(!name.isConstructor) { "Can't apply @JvmStatic to a constructor!" }
  addAnnotation(JvmStaticClassName)
}

public fun FunSpec.Builder.jvmOverloads(): FunSpec.Builder = apply {
  check(!name.isAccessor) {
    "Can't apply @JvmOverloads to a " + if (name == FunSpec.GETTER) "getter!" else "setter!"
  }
  addAnnotation(JvmOverloadsClassName)
}

public fun FunSpec.Builder.jvmName(name: String): FunSpec.Builder = apply {
  check(!this.name.isConstructor) { "Can't apply @JvmName to a constructor!" }
  addAnnotation(
    AnnotationSpec.builder(JvmNameClassName)
      .addMember("%S", name)
      .build(),
  )
}

public fun FunSpec.Builder.throws(vararg exceptionClasses: KClass<out Throwable>): FunSpec.Builder =
  throws(exceptionClasses.map(KClass<*>::asTypeName))

public fun FunSpec.Builder.throws(vararg exceptionClasses: JvmType): FunSpec.Builder =
  throws(exceptionClasses.map(JvmType::asTypeName))

public fun FunSpec.Builder.throws(vararg exceptionClasses: TypeName): FunSpec.Builder =
  throws(exceptionClasses.toList())

public fun FunSpec.Builder.throws(exceptionClasses: Iterable<TypeName>): FunSpec.Builder =
  addAnnotation(
    AnnotationSpec.builder(JvmThrowsClassName)
      .apply { exceptionClasses.forEach { addMember("%T::class", it) } }
      .build(),
  )

public fun FunSpec.Builder.jvmSuppressWildcards(suppress: Boolean = true): FunSpec.Builder = apply {
  check(!name.isConstructor) { "Can't apply @JvmSuppressWildcards to a constructor!" }
  check(!name.isAccessor) {
    "Can't apply @JvmSuppressWildcards to a " + if (name == FunSpec.GETTER) "getter!" else "setter!"
  }
  addAnnotation(jvmSuppressWildcardsAnnotation(suppress))
}

public fun FunSpec.Builder.synchronized(): FunSpec.Builder = apply {
  check(!name.isConstructor) { "Can't apply @Synchronized to a constructor!" }
  addAnnotation(SynchronizedClassName)
}

public fun FunSpec.Builder.strictfp(): FunSpec.Builder = addAnnotation(StrictfpClassName)

public fun PropertySpec.Builder.jvmField(): PropertySpec.Builder = addAnnotation(JvmFieldClassName)

public fun PropertySpec.Builder.jvmStatic(): PropertySpec.Builder = addAnnotation(JvmStaticClassName)

public fun PropertySpec.Builder.jvmSuppressWildcards(
  suppress: Boolean = true,
): PropertySpec.Builder = addAnnotation(jvmSuppressWildcardsAnnotation(suppress))

public fun PropertySpec.Builder.transient(): PropertySpec.Builder = addAnnotation(TransientClassName)

public fun PropertySpec.Builder.volatile(): PropertySpec.Builder = addAnnotation(JvmVolatileClassName)

public fun TypeName.jvmSuppressWildcards(suppress: Boolean = true): TypeName =
  copy(annotations = this.annotations + jvmSuppressWildcardsAnnotation(suppress))

public fun TypeName.jvmWildcard(): TypeName =
  copy(annotations = this.annotations + AnnotationSpec.builder(JvmWildcardClassName).build())

@Suppress("DEPRECATION")
@Deprecated(
  """
  'JvmDefault' is deprecated. Switch to new -Xjvm-default modes: `all` or `all-compatibility`.
  In KotlinPoet 1.15.0 and newer, this method is a no-op. It will be deleted in KotlinPoet 2.0.
""",
)
public fun PropertySpec.Builder.jvmDefault(): PropertySpec.Builder = this

@Suppress("DEPRECATION")
@Deprecated(
  """
  'JvmDefault' is deprecated. Switch to new -Xjvm-default modes: `all` or `all-compatibility`.
  In KotlinPoet 1.15.0 and newer, this method is a no-op. It will be deleted in KotlinPoet 2.0.
""",
)
public fun FunSpec.Builder.jvmDefault(): FunSpec.Builder = this
