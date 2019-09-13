package com.squareup.kotlinpoet.metadata.specs

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.asClassName

/** Modifiers that are annotations in Kotlin but modifier keywords in bytecode. */
enum class JvmMethodModifier : JvmModifier {
  STATIC {
    override fun annotationSpec(): AnnotationSpec = AnnotationSpec.builder(
        JvmStatic::class.asClassName()).build()
  },
  SYNCHRONIZED {
    override fun annotationSpec(): AnnotationSpec = AnnotationSpec.builder(
        Synchronized::class.asClassName()).build()
  }
}
