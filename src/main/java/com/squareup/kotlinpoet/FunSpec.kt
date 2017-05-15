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

import java.io.IOException
import java.io.StringWriter
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
class FunSpec private constructor(builder: FunSpec.Builder) {
  val name: String
  val kdoc: CodeBlock
  val annotations: List<AnnotationSpec>
  val modifiers: Set<KModifier>
  val typeVariables: List<TypeVariableName>
  val returnType: TypeName?
  val parameters: List<ParameterSpec>
  val varargs: Boolean
  val exceptions: List<TypeName>
  val code: CodeBlock
  val defaultValue: CodeBlock?

  init {
    val code = builder.code.build()
    require(code.isEmpty() || !builder.modifiers.contains(KModifier.ABSTRACT)) {
      "abstract function ${builder.name} cannot have code"
    }
    require(!builder.varargs || lastParameterIsArray(builder.parameters)) {
      "last parameter of varargs function ${builder.name} must be an array"
    }
    this.name = builder.name
    this.kdoc = builder.kdoc.build()
    this.annotations = builder.annotations.toImmutableList()
    this.modifiers = builder.modifiers.toImmutableSet()
    this.typeVariables = builder.typeVariables.toImmutableList()
    this.returnType = builder.returnType
    this.parameters = builder.parameters.toImmutableList()
    this.varargs = builder.varargs
    this.exceptions = builder.exceptions.toImmutableList()
    this.defaultValue = builder.defaultValue
    this.code = code
  }

  private fun lastParameterIsArray(parameters: List<ParameterSpec>): Boolean {
    return !parameters.isEmpty()
        && TypeName.arrayComponent(parameters[parameters.size - 1].type) != null
  }

  @Throws(IOException::class)
  internal fun emit(
      codeWriter: CodeWriter,
      enclosingName: String?,
      implicitModifiers: Set<KModifier>) {
    codeWriter.emitKdoc(kdoc)
    codeWriter.emitAnnotations(annotations, false)
    codeWriter.emitModifiers(modifiers, implicitModifiers)

    if (!isConstructor) {
      codeWriter.emit("fun ")
    }

    if (!typeVariables.isEmpty()) {
      codeWriter.emitTypeVariables(typeVariables)
      codeWriter.emit(" ")
    }

    if (isConstructor) {
      codeWriter.emit("constructor", enclosingName)
    } else {
      codeWriter.emit("%L", name)
    }

    emitParameterList(codeWriter)

    if (returnType != null) {
      codeWriter.emit(": %T", returnType)
    }

    if (defaultValue != null && !defaultValue.isEmpty()) {
      codeWriter.emit(" default ")
      codeWriter.emit(defaultValue)
    }

    if (!exceptions.isEmpty()) {
      codeWriter.emitWrappingSpace().emit("throws")
      var firstException = true
      for (exception in exceptions) {
        if (!firstException) codeWriter.emit(",")
        codeWriter.emitWrappingSpace().emit("%T", exception)
        firstException = false
      }
    }

    if (modifiers.contains(KModifier.ABSTRACT) || modifiers.contains(KModifier.EXTERNAL)) {
      codeWriter.emit("\n")
    } else {
      codeWriter.emit(" {\n")

      codeWriter.indent()
      codeWriter.emit(code)
      codeWriter.unindent()

      codeWriter.emit("}\n")
    }
  }

  @Throws(IOException::class)
  internal fun emitParameterList(codeWriter: CodeWriter) {
    codeWriter.emit("(")
    var firstParameter = true
    val i = parameters.iterator()
    while (i.hasNext()) {
      val parameter = i.next()
      if (!firstParameter) codeWriter.emit(",").emitWrappingSpace()
      parameter.emit(codeWriter, !i.hasNext() && varargs)
      firstParameter = false
    }
    codeWriter.emit(")")
  }

  val isConstructor: Boolean
    get() = name == CONSTRUCTOR

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null) return false
    if (javaClass != other.javaClass) return false
    return toString() == other.toString()
  }

  override fun hashCode() = toString().hashCode()

  override fun toString(): String {
    val out = StringWriter()
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
    builder.varargs = varargs
    builder.defaultValue = defaultValue
    return builder
  }

  class Builder internal constructor(internal val name: String) {
    internal val kdoc = CodeBlock.builder()
    internal val annotations = mutableListOf<AnnotationSpec>()
    internal val modifiers = mutableListOf<KModifier>()
    internal val typeVariables = mutableListOf<TypeVariableName>()
    internal var returnType: TypeName? = null
    internal val parameters = mutableListOf<ParameterSpec>()
    internal val exceptions = mutableSetOf<TypeName>()
    internal val code = CodeBlock.builder()
    internal var varargs: Boolean = false
    internal var defaultValue: CodeBlock? = null

    init {
      require(name == CONSTRUCTOR || SourceVersion.isName(name)) { "not a valid name: $name" }
    }

    fun addKdoc(format: String, vararg args: Any): Builder {
      kdoc.add(format, *args)
      return this
    }

    fun addKdoc(block: CodeBlock): Builder {
      kdoc.add(block)
      return this
    }

    fun addAnnotations(annotationSpecs: Iterable<AnnotationSpec>): Builder {
      this.annotations += annotationSpecs
      return this
    }

    fun addAnnotation(annotationSpec: AnnotationSpec): Builder {
      annotations += annotationSpec
      return this
    }

    fun addAnnotation(annotation: ClassName): Builder {
      annotations += AnnotationSpec.builder(annotation).build()
      return this
    }

    fun addAnnotation(annotation: Class<*>) = addAnnotation(ClassName.get(annotation))

    fun addAnnotation(annotation: KClass<*>) = addAnnotation(ClassName.get(annotation))

    fun addModifiers(vararg modifiers: KModifier): Builder {
      this.modifiers += modifiers
      return this
    }

    fun addModifiers(modifiers: Iterable<KModifier>): Builder {
      this.modifiers += modifiers
      return this
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

    fun addTypeVariables(typeVariables: Iterable<TypeVariableName>): Builder {
      this.typeVariables += typeVariables
      return this
    }

    fun addTypeVariable(typeVariable: TypeVariableName): Builder {
      typeVariables += typeVariable
      return this
    }

    fun returns(returnType: TypeName): Builder {
      check(name != CONSTRUCTOR) { "constructor cannot have return type." }
      this.returnType = returnType
      return this
    }

    fun returns(returnType: Type) = returns(TypeName.get(returnType))

    fun returns(returnType: KClass<*>) = returns(TypeName.get(returnType))

    fun addParameters(parameterSpecs: Iterable<ParameterSpec>): Builder {
      parameters += parameterSpecs
      return this
    }

    fun addParameter(parameterSpec: ParameterSpec): Builder {
      parameters += parameterSpec
      return this
    }

    fun addParameter(type: TypeName, name: String, vararg modifiers: KModifier)
        = addParameter(ParameterSpec.builder(type, name, *modifiers).build())

    fun addParameter(type: Type, name: String, vararg modifiers: KModifier)
        = addParameter(TypeName.get(type), name, *modifiers)

    fun addParameter(type: KClass<*>, name: String, vararg modifiers: KModifier)
        = addParameter(TypeName.get(type), name, *modifiers)

    @JvmOverloads fun varargs(varargs: Boolean = true): Builder {
      this.varargs = varargs
      return this
    }

    fun addExceptions(exceptions: Iterable<TypeName>): Builder {
      this.exceptions += exceptions
      return this
    }

    fun addException(exception: TypeName): Builder {
      exceptions += exception
      return this
    }

    fun addException(exception: Type) = addException(TypeName.get(exception))

    fun addException(exception: KClass<*>) = addException(TypeName.get(exception))

    fun addCode(format: String, vararg args: Any): Builder {
      code.add(format, *args)
      return this
    }

    fun addNamedCode(format: String, args: Map<String, *>): Builder {
      code.addNamed(format, args)
      return this
    }

    fun addCode(codeBlock: CodeBlock): Builder {
      code.add(codeBlock)
      return this
    }

    fun addComment(format: String, vararg args: Any): Builder {
      code.add("// " + format + "\n", *args)
      return this
    }

    fun defaultValue(format: String, vararg args: Any): Builder {
      return defaultValue(CodeBlock.of(format, *args))
    }

    fun defaultValue(codeBlock: CodeBlock): Builder {
      check(this.defaultValue == null) { "defaultValue was already set" }
      this.defaultValue = codeBlock
      return this
    }

    /**
     * @param controlFlow the control flow construct and its code, such as "if (foo == 5)".
     * * Shouldn't contain braces or newline characters.
     */
    fun beginControlFlow(controlFlow: String, vararg args: Any): Builder {
      code.beginControlFlow(controlFlow, *args)
      return this
    }

    /**
     * @param controlFlow the control flow construct and its code, such as "else if (foo == 10)".
     * *     Shouldn't contain braces or newline characters.
     */
    fun nextControlFlow(controlFlow: String, vararg args: Any): Builder {
      code.nextControlFlow(controlFlow, *args)
      return this
    }

    fun endControlFlow(): Builder {
      code.endControlFlow()
      return this
    }

    fun addStatement(format: String, vararg args: Any): Builder {
      code.addStatement(format, *args)
      return this
    }

    fun build() = FunSpec(this)
  }

  companion object {
    const internal val CONSTRUCTOR = "<init>"

    @JvmStatic fun builder(name: String): Builder {
      return Builder(name)
    }

    @JvmStatic fun constructorBuilder(): Builder {
      return Builder(CONSTRUCTOR)
    }

    /**
     * Returns a new fun spec builder that overrides `method`.

     *
     * This will copy its visibility modifiers, type parameters, return type, name, parameters, and
     * throws declarations. An [Override] annotation will be added.
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

      funBuilder.addAnnotation(Override::class)

      modifiers = modifiers.toMutableSet()
      modifiers.remove(Modifier.ABSTRACT)
      funBuilder.jvmModifiers(modifiers)

      for (typeParameterElement in method.typeParameters) {
        val typeVariable = typeParameterElement.asType() as TypeVariable
        funBuilder.addTypeVariable(TypeVariableName.get(typeVariable))
      }

      funBuilder.returns(TypeName.get(method.returnType))
      funBuilder.addParameters(ParameterSpec.parametersOf(method))
      funBuilder.varargs(method.isVarArgs)

      for (thrownType in method.thrownTypes) {
        funBuilder.addException(TypeName.get(thrownType))
      }

      return funBuilder
    }

    /**
     * Returns a new function spec builder that overrides `method` as a member of `enclosing`. This
     * will resolve type parameters: for example overriding [Comparable.compareTo] in a type that
     * implements `Comparable<Movie>`, the `T` parameter will be resolved to `Movie`.
     *
     * This will copy its visibility modifiers, type parameters, return type, name, parameters, and
     * throws declarations. An [Override] annotation will be added.
     */
    @JvmStatic fun overriding(
        method: ExecutableElement, enclosing: DeclaredType, types: Types): Builder {
      val executableType = types.asMemberOf(enclosing, method) as ExecutableType
      val resolvedParameterTypes = executableType.parameterTypes
      val resolvedReturnType = executableType.returnType

      val builder = overriding(method)
      builder.returns(TypeName.get(resolvedReturnType))
      var i = 0
      val size = builder.parameters.size
      while (i < size) {
        val parameter = builder.parameters[i]
        val type = TypeName.get(resolvedParameterTypes[i])
        builder.parameters[i] = parameter.toBuilder(type, parameter.name).build()
        i++
      }

      return builder
    }
  }
}
