package com.squareup.kotlinpoet

class LambdaTypeName internal constructor(
    private val parameters: List<TypeName> = emptyList(),
    private val returnType: TypeName = TypeName.get(Unit::class),
    nullable: Boolean = false,
    annotations: List<AnnotationSpec> = emptyList()
) : TypeName(nullable, annotations) {
  override fun asNullable() = LambdaTypeName(parameters, returnType, true, annotations)

  override fun asNonNullable() = LambdaTypeName(parameters, returnType, false, annotations)

  override fun annotated(annotations: List<AnnotationSpec>): TypeName
      = LambdaTypeName(parameters, returnType, nullable, annotations)

  override fun withoutAnnotations(): TypeName = LambdaTypeName(parameters, returnType, nullable)

  override fun abstractEmit(out: CodeWriter): CodeWriter {
    annotations.forEach { it.emit(out, true) }
    if (nullable) {
      out.emit("(")
    }
    out.emit("(")
    if (!parameters.isEmpty()) {
      parameters.forEachIndexed { i, it ->
        if (i != 0) {
          out.emit(", ")
        }
        out.emit("%T", it)
      }
    }
    out.emit(") -> %T", returnType)
    if (nullable) {
      out.emit(")")
    }
    return out
  }

  companion object {
    /** Returns a lambda type with `returnType` and parameters of listed in `parameters` **/
    @JvmStatic fun get(returnType: TypeName, parameters: List<TypeName>)
        = LambdaTypeName(parameters, returnType)

    /** Returns a lambda type with `returnType` and parameters of listed in `parameters` **/
    @JvmStatic fun get(returnType: TypeName, vararg parameters: TypeName)
        = LambdaTypeName(parameters.toList(), returnType)
  }
}