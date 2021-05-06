package com.squareup.kotlinpoet

import kotlin.reflect.KClass

public object Dynamic : TypeName(false, emptyList(), TagMap(emptyMap())) {

  override fun copy(
    nullable: Boolean,
    annotations: List<AnnotationSpec>,
    tags: Map<KClass<*>, Any>
  ): Nothing = throw UnsupportedOperationException("dynamic doesn't support copying")

  override fun emit(out: CodeWriter) = out.apply {
    emit("dynamic")
  }
}
