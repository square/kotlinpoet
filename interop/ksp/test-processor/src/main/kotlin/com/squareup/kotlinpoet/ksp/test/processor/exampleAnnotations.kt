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
package com.squareup.kotlinpoet.ksp.test.processor

import kotlin.reflect.KClass

annotation class ExampleAnnotation

annotation class ComprehensiveAnnotation<T : CharSequence>(
  val boolean: Boolean,
  val booleanArray: BooleanArray,
  val byte: Byte,
  val byteArray: ByteArray,
  val short: Short,
  val shortArray: ShortArray,
  val int: Int,
  val intArray: IntArray,
  val long: Long,
  val longArray: LongArray,
  val float: Float,
  val floatArray: FloatArray,
  val double: Double,
  val doubleArray: DoubleArray,
  val string: String,
  val stringArray: Array<String>,
  val someClass: KClass<*>,
  val someClasses: Array<KClass<*>>,
  val enumValue: AnnotationEnumValue,
  val enumValueArray: Array<AnnotationEnumValue>,
  val anotherAnnotation: AnotherAnnotation,
  val anotherAnnotationArray: Array<AnotherAnnotation>,
  // This is still included even when the argument is omitted until https://github.com/google/ksp/issues/674
  val defaultingString: String = "defaultValue",
)

annotation class AnotherAnnotation(val input: String)

enum class AnnotationEnumValue {
  ONE, TWO, THREE
}
