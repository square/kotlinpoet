package com.squareup.kotlinpoet.ksp

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName

internal fun TypeName.rawType(): ClassName {
  return findRawType() ?: throw IllegalArgumentException("Cannot get raw type from $this")
}

internal fun TypeName.findRawType(): ClassName? {
  return when (this) {
    is ClassName -> this
    is ParameterizedTypeName -> rawType
    is LambdaTypeName -> {
      var count = parameters.size
      if (receiver != null) {
        count++
      }
      val functionSimpleName = if (count >= 23) {
        "FunctionN"
      } else {
        "Function$count"
      }
      ClassName("kotlin.jvm.functions", functionSimpleName)
    }
    else -> null
  }
}
