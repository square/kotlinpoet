package com.squareup.kotlinpoet

fun TypeName.nullable() = NullableTypeName(this)

class NullableTypeName(
    val type: TypeName,
    annotations: List<AnnotationSpec> = emptyList()) : TypeName(annotations) {
  override fun annotated(annotations: List<AnnotationSpec>)
      = NullableTypeName(type, this.annotations + annotations)

  override fun withoutAnnotations() = NullableTypeName(type)

  override fun abstractEmit(out: CodeWriter): CodeWriter {
    type.emit(out)
    return out.emit("?")
  }
}