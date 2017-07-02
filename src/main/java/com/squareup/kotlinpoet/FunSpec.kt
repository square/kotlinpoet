/*
 * Copyright (C) 2015 Square, Inc.
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
package com.squareup.kotlinpoet

import com.squareup.kotlinpoet.FunSpec.Companion.CONSTRUCTOR
import com.squareup.kotlinpoet.FunSpec.Companion.GETTER
import com.squareup.kotlinpoet.FunSpec.Companion.SETTER
import com.squareup.kotlinpoet.KModifier.VARARG
import java.io.IOException
import java.lang.reflect.Type
import javax.lang.model.SourceVersion
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.ExecutableType
import javax.lang.model.type.TypeVariable
import javax.lang.model.util.Types
import kotlin.reflect.KClass

/** A generated function declaration.  */
class FunSpec private constructor(builder: Builder) {
  val name = builder.name
  val kdoc = builder.kdoc.build()
  val annotations = builder.annotations.toImmutableList()
  val modifiers = builder.modifiers.toImmutableSet()
  val typeVariables = builder.typeVariables.toImmutableList()
  val receiverType = builder.receiverType
  val returnType = builder.returnType
  val parameters = builder.parameters.toImmutableList()
  val exceptions = builder.exceptions.toImmutableList()
  val code = builder.code.build()

  init {
    require(code.isEmpty() || !builder.modifiers.contains(KModifier.ABSTRACT)) {
      "abstract function ${builder.name} cannot have code"
    }
    require(name != SETTER || parameters.size == 1) {
      "$name must have exactly one parameter"
    }
  }

  internal fun parameter(name: String) = parameters.firstOrNull { it.name == name }

  @Throws(IOException::class)
  internal fun emit(
      codeWriter: CodeWriter,
      enclosingName: String?,
      implicitModifiers: Set<KModifier>) {
    codeWriter.emitKdoc(kdoc)
    codeWriter.emitAnnotations(annotations, false)
    codeWriter.emitModifiers(modifiers, implicitModifiers)

    if (!isConstructor && !name.isAccessor) {
      codeWriter.emit("fun ")
    }

    if (typeVariables.isNotEmpty()) {
      codeWriter.emitTypeVariables(typeVariables)
      codeWriter.emit(" ")
    }
    emitSignature(codeWriter, enclosingName)
    codeWriter.emitWhereBlock(typeVariables)

    if (modifiers.contains(KModifier.ABSTRACT) || modifiers.contains(KModifier.EXTERNAL)) {
      codeWriter.emit("\n")
      return
    }

    val asExpressionBody = code.withoutPrefix(EXPRESSION_BODY_PREFIX)

    if (asExpressionBody != null) {
      codeWriter.indent()
      codeWriter.emitCode(" =%W%[")
      codeWriter.emitCode(asExpressionBody)
      codeWriter.unindent()
    } else {
      codeWriter.emit(" {\n")
      codeWriter.indent()
      codeWriter.emitCode(code)
      codeWriter.unindent()
      codeWriter.emit("}\n")
    }
  }

  @Throws(IOException::class)
  internal fun emitSignature(
      codeWriter: CodeWriter,
      enclosingName: String?) {
    if (isConstructor) {
      codeWriter.emitCode("constructor", enclosingName)
    } else if (name == GETTER) {
      codeWriter.emitCode("get")
    } else if (name == SETTER) {
      codeWriter.emitCode("set")
    } else {
      if (receiverType != null) {
        codeWriter.emitCode("%T.", receiverType)
      }
      codeWriter.emitCode("%L", name)
    }

    emitParameterList(codeWriter)

    if (returnType != null) {
      codeWriter.emitCode(": %T", returnType)
    }

    if (exceptions.isNotEmpty()) {
      codeWriter.emitWrappingSpace().emit("throws")
      var firstException = true
      for (exception in exceptions) {
        if (!firstException) codeWriter.emit(",")
        codeWriter.emitWrappingSpace().emitCode("%T", exception)
        firstException = false
      }
    }
  }

  @Throws(IOException::class)
  internal fun emitParameterList(codeWriter: CodeWriter) {
    codeWriter.emit("(")
    parameters.forEachIndexed { index, parameter ->
      if (index > 0) codeWriter.emit(",").emitWrappingSpace()
      parameter.emit(codeWriter)
    }
    codeWriter.emit(")")
  }

  val isConstructor: Boolean
    get() = name.isConstructor

  val isAccessor: Boolean
    get() = name.isAccessor

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null) return false
    if (javaClass != other.javaClass) return false
    return toString() == other.toString()
  }

  override fun hashCode() = toString().hashCode()

  override fun toString(): String {
    val out = StringBuilder()
    try {
      val codeWriter = CodeWriter(out)
      emit(codeWriter, "Constructor", TypeSpec.Kind.CLASS.implicitFunctionModifiers)
      return out.toString()
    } catch (e: IOException) {
      throw AssertionError()
    }
  }

  fun toBuilder(): Builder {
    val builder = Builder(name)
    builder.kdoc.add(kdoc)
    builder.annotations += annotations
    builder.modifiers += modifiers
    builder.typeVariables += typeVariables
    builder.returnType = returnType
    builder.parameters += parameters
    builder.exceptions += exceptions
    builder.code.add(code)
    return builder
  }

  class Builder internal constructor(internal val name: String) {
    internal val kdoc = CodeBlock.builder()
    internal val annotations = mutableListOf<AnnotationSpec>()
    internal val modifiers = mutableListOf<KModifier>()
    internal val typeVariables = mutableListOf<TypeVariableName>()
    internal var receiverType: TypeName? = null
    internal var returnType: TypeName? = null
    internal val parameters = mutableListOf<ParameterSpec>()
    internal val exceptions = mutableSetOf<TypeName>()
    internal val code = CodeBlock.builder()

    init {
      require(name.isConstructor || name.isAccessor || SourceVersion.isName(name)) {
        "not a valid name: $name"
      }
    }

    fun addKdoc(format: String, vararg args: Any) = apply {
      kdoc.add(format, *args)
    }

    fun addKdoc(block: CodeBlock) = apply {
      kdoc.add(block)
    }

    fun addAnnotations(annotationSpecs: Iterable<AnnotationSpec>) = apply {
      this.annotations += annotationSpecs
    }

    fun addAnnotation(annotationSpec: AnnotationSpec) = apply {
      annotations += annotationSpec
    }

    fun addAnnotation(annotation: ClassName) = apply {
      annotations += AnnotationSpec.builder(annotation).build()
    }

    fun addAnnotation(annotation: Class<*>) = addAnnotation(annotation.asClassName())

    fun addAnnotation(annotation: KClass<*>) = addAnnotation(annotation.asClassName())

    fun addModifiers(vararg modifiers: KModifier) = apply {
      this.modifiers += modifiers
    }

    fun addModifiers(modifiers: Iterable<KModifier>) = apply {
      this.modifiers += modifiers
    }

    fun jvmModifiers(modifiers: Iterable<Modifier>) {
      var visibility = KModifier.INTERNAL
      for (modifier in modifiers) {
        when (modifier) {
          Modifier.PUBLIC -> visibility = KModifier.PUBLIC
          Modifier.PROTECTED -> visibility = KModifier.PROTECTED
          Modifier.PRIVATE -> visibility = KModifier.PRIVATE
          Modifier.ABSTRACT -> this.modifiers += KModifier.ABSTRACT
          Modifier.FINAL -> this.modifiers += KModifier.FINAL
          Modifier.NATIVE -> this.modifiers += KModifier.EXTERNAL
          Modifier.DEFAULT -> {}
          Modifier.STATIC -> addAnnotation(JvmStatic::class)
          Modifier.SYNCHRONIZED -> addAnnotation(Synchronized::class)
          Modifier.STRICTFP -> addAnnotation(Strictfp::class)
          else -> throw IllegalArgumentException("unexpected fun modifier $modifier")
        }
      }
      this.modifiers += visibility
    }

    fun addTypeVariables(typeVariables: Iterable<TypeVariableName>) = apply {
      check(!name.isAccessor) { "$name cannot have type variables" }
      this.typeVariables += typeVariables
    }

    fun addTypeVariable(typeVariable: TypeVariableName) = apply {
      check(!name.isAccessor) { "$name cannot have type variables" }
      typeVariables += typeVariable
    }

    fun receiver(receiverType: TypeName) = apply {
      check(!name.isConstructor) { "$name cannot have receiver type" }
      this.receiverType = receiverType
    }

    fun receiver(receiverType: Type) = receiver(receiverType.asTypeName())

    fun receiver(receiverType: KClass<*>) = receiver(receiverType.asTypeName())

    fun returns(returnType: TypeName) = apply {
      check(!name.isConstructor && !name.isAccessor) { "$name cannot have a return type" }
      this.returnType = returnType
    }

    fun returns(returnType: Type) = returns(returnType.asTypeName())

    fun returns(returnType: KClass<*>) = returns(returnType.asTypeName())

    fun addParameters(parameterSpecs: Iterable<ParameterSpec>) = apply {
      for (parameterSpec in parameterSpecs) {
        addParameter(parameterSpec)
      }
    }

    fun addParameter(parameterSpec: ParameterSpec) = apply {
      check(name != GETTER) { "$name cannot have parameters" }
      check(name != SETTER || parameters.size == 0) { "$name can have only one parameter" }
      parameters += parameterSpec
    }

    fun addParameter(name: String, type: TypeName, vararg modifiers: KModifier)
        = addParameter(ParameterSpec.builder(name, type, *modifiers).build())

    fun addParameter(name: String, type: Type, vararg modifiers: KModifier)
        = addParameter(name, type.asTypeName(), *modifiers)

    fun addParameter(name: String, type: KClass<*>, vararg modifiers: KModifier)
        = addParameter(name, type.asTypeName(), *modifiers)

    fun addCode(format: String, vararg args: Any) = apply {
      code.add(format, *args)
    }

    fun addNamedCode(format: String, args: Map<String, *>) = apply {
      code.addNamed(format, args)
    }

    fun addCode(codeBlock: CodeBlock) = apply {
      code.add(codeBlock)
    }

    fun addComment(format: String, vararg args: Any) = apply {
      code.add("// " + format + "\n", *args)
    }

    /**
     * @param controlFlow the control flow construct and its code, such as "if (foo == 5)".
     * * Shouldn't contain braces or newline characters.
     */
    fun beginControlFlow(controlFlow: String, vararg args: Any) = apply {
      code.beginControlFlow(controlFlow, *args)
    }

    /**
     * @param controlFlow the control flow construct and its code, such as "else if (foo == 10)".
     * *     Shouldn't contain braces or newline characters.
     */
    fun nextControlFlow(controlFlow: String, vararg args: Any) = apply {
      code.nextControlFlow(controlFlow, *args)
    }

    fun endControlFlow() = apply {
      code.endControlFlow()
    }

    fun addStatement(format: String, vararg args: Any) = apply {
      code.addStatement(format, *args)
    }

    fun build() = FunSpec(this)
  }

  companion object {
    internal const val CONSTRUCTOR = "constructor()"
    internal const val GETTER = "get()"
    internal const val SETTER = "set()"

    @JvmStatic fun builder(name: String): Builder {
      return Builder(name)
    }

    @JvmStatic fun constructorBuilder(): Builder {
      return Builder(CONSTRUCTOR)
    }

    @JvmStatic fun getterBuilder(): Builder {
      return Builder(GETTER)
    }

    @JvmStatic fun setterBuilder(): Builder {
      return Builder(SETTER)
    }

    /**
     * Returns a new fun spec builder that overrides `method`.

     *
     * This will copy its visibility modifiers, type parameters, return type, name, parameters, and
     * throws declarations. An `override` modifier will be added.
     */
    @JvmStatic fun overriding(method: ExecutableElement): Builder {
      var modifiers: MutableSet<Modifier> = method.modifiers
      require(!modifiers.contains(Modifier.PRIVATE)
          && !modifiers.contains(Modifier.FINAL)
          && !modifiers.contains(Modifier.STATIC)) {
        "cannot override method with modifiers: $modifiers"
      }

      val methodName = method.simpleName.toString()
      val funBuilder = FunSpec.builder(methodName)

      funBuilder.addModifiers(KModifier.OVERRIDE)

      modifiers = modifiers.toMutableSet()
      modifiers.remove(Modifier.ABSTRACT)
      funBuilder.jvmModifiers(modifiers)

      method.typeParameters
          .map { it.asType() as TypeVariable }
          .map { it.asTypeVariableName() }
          .forEach { funBuilder.addTypeVariable(it) }

      funBuilder.returns(method.returnType.asTypeName())
      funBuilder.addParameters(ParameterSpec.parametersOf(method))
      if (method.isVarArgs) {
        funBuilder.parameters[funBuilder.parameters.lastIndex] = funBuilder.parameters.last()
            .toBuilder()
            .addModifiers(VARARG)
            .build()
      }

      if (method.thrownTypes.isNotEmpty()) {
        val throwsValueString = method.thrownTypes.joinToString { "%T::class" }
        funBuilder.addAnnotation(AnnotationSpec.builder(Throws::class)
            .addMember("value", throwsValueString, *method.thrownTypes.toTypedArray())
            .build())
      }

      return funBuilder
    }

    /**
     * Returns a new function spec builder that overrides `method` as a member of `enclosing`. This
     * will resolve type parameters: for example overriding [Comparable.compareTo] in a type that
     * implements `Comparable<Movie>`, the `T` parameter will be resolved to `Movie`.
     *
     * This will copy its visibility modifiers, type parameters, return type, name, parameters, and
     * throws declarations. An `override` modifier will be added.
     */
    @JvmStatic fun overriding(
        method: ExecutableElement, enclosing: DeclaredType, types: Types): Builder {
      val executableType = types.asMemberOf(enclosing, method) as ExecutableType
      val resolvedParameterTypes = executableType.parameterTypes
      val resolvedReturnType = executableType.returnType

      val builder = overriding(method)
      builder.returns(resolvedReturnType.asTypeName())
      var i = 0
      val size = builder.parameters.size
      while (i < size) {
        val parameter = builder.parameters[i]
        val type = resolvedParameterTypes[i].asTypeName()
        builder.parameters[i] = parameter.toBuilder(parameter.name, type).build()
        i++
      }

      return builder
    }
  }
}

internal val String.isConstructor: Boolean
  get() = this == CONSTRUCTOR

internal val String.isAccessor: Boolean
  get() = this == GETTER || this == SETTER

internal val EXPRESSION_BODY_PREFIX = CodeBlock.of("%[return ")
