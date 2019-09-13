package com.squareup.kotlinpoet.metadata.specs

import com.squareup.kotlinpoet.AnnotationSpec

interface JvmModifier {
  fun annotationSpec(): AnnotationSpec
}
