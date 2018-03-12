/*
 * Copyright (C) 2018 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import java.lang.reflect.Type
import kotlin.reflect.KClass

fun FileSpec.Builder.jvmName(name: String) = addAnnotation(
    AnnotationSpec.builder(JvmName::class)
        .useSiteTarget(FILE)
        .addMember("%S", name)
        .build())

fun FileSpec.Builder.jvmMultifileClass() = addAnnotation(
    AnnotationSpec.builder(JvmMultifileClass::class)
        .useSiteTarget(FILE)
        .build())

fun TypeSpec.Builder.jvmSuppressWildcards(suppress: Boolean = true) =
    addAnnotation(jvmSuppressWildcardsAnnotation(suppress))

private fun jvmSuppressWildcardsAnnotation(suppress: Boolean = true) =
    AnnotationSpec.builder(JvmSuppressWildcards::class)
        .apply { if (!suppress) addMember("suppress = false") }
        .build()

fun FunSpec.Builder.jvmStatic() = apply {
  check(!name.isConstructor) { "Can't apply @JvmStatic to a constructor!" }
  addAnnotation(JvmStatic::class)
}

fun FunSpec.Builder.jvmOverloads() = apply {
  check(!name.isAccessor) {
    "Can't apply @JvmOverloads to a " + if (name == FunSpec.GETTER) "getter!" else "setter!"
  }
  addAnnotation(JvmOverloads::class)
}

fun FunSpec.Builder.jvmName(name: String) = apply {
  check(!this.name.isConstructor) { "Can't apply @JvmName to a constructor!" }
  addAnnotation(AnnotationSpec.builder(JvmName::class)
      .addMember("%S", name)
      .build())
}

fun FunSpec.Builder.throws(vararg exceptionClasses: KClass<out Throwable>) =
    throws(*(exceptionClasses.map(KClass<*>::asTypeName).toTypedArray()))

fun FunSpec.Builder.throws(vararg exceptionClasses: Type) =
    throws(*(exceptionClasses.map(Type::asTypeName).toTypedArray()))

fun FunSpec.Builder.throws(vararg exceptionClasses: TypeName) = addAnnotation(
    AnnotationSpec.builder(Throws::class)
        .apply { exceptionClasses.forEach { addMember("%T::class", it) } }
        .build())

fun FunSpec.Builder.jvmSuppressWildcards(suppress: Boolean = true) = apply {
  check(!name.isConstructor) { "Can't apply @JvmSuppressWildcards to a constructor!" }
  check(!name.isAccessor) {
    "Can't apply @JvmSuppressWildcards to a " + if (name == FunSpec.GETTER) "getter!" else "setter!"
  }
  addAnnotation(jvmSuppressWildcardsAnnotation(suppress))
}

fun FunSpec.Builder.synchronized() = apply {
  check(!name.isConstructor) { "Can't apply @Synchronized to a constructor!" }
  addAnnotation(Synchronized::class)
}

fun FunSpec.Builder.strictfp() = addAnnotation(Strictfp::class)

fun PropertySpec.Builder.jvmField() = addAnnotation(JvmField::class)

fun PropertySpec.Builder.jvmStatic() = addAnnotation(JvmStatic::class)

fun PropertySpec.Builder.jvmSuppressWildcards(suppress: Boolean = true) =
    addAnnotation(jvmSuppressWildcardsAnnotation(suppress))

fun PropertySpec.Builder.transient() = addAnnotation(Transient::class)

fun PropertySpec.Builder.volatile() = addAnnotation(Volatile::class)

fun TypeName.jvmSuppressWildcards(suppress: Boolean = true) =
    annotated(jvmSuppressWildcardsAnnotation(suppress))

fun TypeName.jvmWildcard() = annotated(AnnotationSpec.builder(JvmWildcard::class).build())
