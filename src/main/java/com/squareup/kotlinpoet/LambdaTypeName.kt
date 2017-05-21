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

  override fun abstractEmit(out: CodeWriter): CodeWriter = out.apply {
    annotations.forEach { it.emit(out, true) }
    emit("(")
    parameters.forEach { it.emit(out) }
    emit(") -> (${returnType.emit(out)})")
  }

  companion object {
    @JvmStatic fun get(returnType: TypeName, parameters: List<TypeName>)
        = LambdaTypeName(parameters, returnType)

    @JvmStatic fun get(returnType: TypeName, vararg parameters: TypeName)
        = LambdaTypeName(parameters.toList(), returnType)
  }
}