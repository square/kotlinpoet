/*
 * Copyright (C) 2015 Square, Inc.
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
@file:JvmName("CodeBlocks")
@file:JvmMultifileClass

package com.squareup.kotlinpoet

import java.lang.reflect.Type
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import javax.lang.model.element.Element
import javax.lang.model.type.TypeMirror
import kotlin.math.max
import kotlin.reflect.KClass

internal actual fun formatNumericValue(o: Number): Any? {
  val format = DecimalFormatSymbols().apply {
    decimalSeparator = '.'
    groupingSeparator = '_'
    minusSign = '-'
  }

  val precision = when (o) {
    is Float -> max(o.toBigDecimal().stripTrailingZeros().scale(), 1)
    is Double -> max(o.toBigDecimal().stripTrailingZeros().scale(), 1)
    else -> 0
  }

  val pattern = when (o) {
    is Float, is Double -> "###,##0.0" + "#".repeat(precision - 1)
    else -> "###,##0"
  }

  return DecimalFormat(pattern, format).format(o)
}

internal actual inline fun argToType(
  o: Any?,
  logDeprecationWarning: (Any) -> Unit,
): TypeName = when (o) {
  is TypeName -> o
  is TypeMirror -> {
    logDeprecationWarning(o)
    o.asTypeName()
  }

  is Element -> {
    logDeprecationWarning(o)
    o.asType().asTypeName()
  }

  is Type -> o.asTypeName()
  is KClass<*> -> o.asTypeName()
  else -> throw IllegalArgumentException("expected type but was $o")
}
