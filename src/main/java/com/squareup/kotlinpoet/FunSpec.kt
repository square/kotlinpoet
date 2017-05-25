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
class FunSpec private constructor(builder: Builder) {
  val name = builder.name
  val kdoc = builder.kdoc.build()
  val annotations = builder.annotations.toImmutableList()
  val modifiers = builder.modifiers.toImmutableSet()
  val typeVariables = builder.typeVariables.toImmutableList()
  val receiverType = builder.receiverType
  val returnType = builder.returnType
  val parameters = builder.parameters.toImmutableList()
  val varargs = builder.varargs
  val exceptions = builder.exceptions.toImmutableList()
  val code = builder.code.build()
  val defaultValue = builder.defaultValue

  init {
    require(code.isEmpty() || !builder.modifiers.contains(KModifier.ABSTRACT)) {
      "abstract function ${builder.name} cannot have code"
    }
    require(!builder.varargs || lastParameterIsArray(builder.parameters)) {
      "last parameter of varargs function ${builder.name} must be an array"
    }
    require(name != SETTER || parameters.size == 1) {
      "$name must have exactly one parameter"
    }
  }

  private fun lastParameterIsArray(parameters: List<ParameterSpec>): Boolean {
    return parameters.isNotEmpty()
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

    if (!isConstructor && !name.isAccessor) {
      codeWriter.emit("fun ")
    }

    if (typeVariables.isNotEmpty()) {
      codeWriter.emitTypeVariables(typeVariables)
      codeWriter.emit(" ")
    }

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

    if (defaultValue != null && !defaultValue.isEmpty()) {
      codeWriter.emit(" default ")
      codeWriter.emitCode(defaultValue)
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

    if (modifiers.contains(KModifier.ABSTRACT) || modifiers.contains(KModifier.EXTERNAL)) {
      codeWriter.emit("\n")
    } else {
      codeWriter.emit(" {\n")

      codeWriter.indent()
      codeWriter.emitCode(code)
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
    internal var receiverType: TypeName? = null
    internal var returnType: TypeName? = null
    internal val parameters = mutableListOf<ParameterSpec>()
    internal val exceptions = mutableSetOf<TypeName>()
    internal val code = CodeBlock.builder()
    internal var varargs: Boolean = false
    internal var defaultValue: CodeBlock? = null

    init {
      require(name.isConstructor || name.isAccessor || SourceVersion.isName(name)) {
        "not a valid name: $name"
      }
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
      check(!name.isAccessor) { "$name cannot have type variables" }
      this.typeVariables += typeVariables
      return this
    }

    fun addTypeVariable(typeVariable: TypeVariableName): Builder {
      check(!name.isAccessor) { "$name cannot have type variables" }
      typeVariables += typeVariable
      return this
    }

    fun receiver(receiverType: TypeName): Builder {
      check(!name.isConstructor) { "$name cannot have receiver type" }
      this.receiverType = receiverType
      return this
    }

    fun receiver(receiverType: Type) = receiver(TypeName.get(receiverType))

    fun receiver(receiverType: KClass<*>) = receiver(TypeName.get(receiverType))

    fun returns(returnType: TypeName): Builder {
      check(!name.isConstructor && !name.isAccessor) { "$name cannot have a return type" }
      this.returnType = returnType
      return this
    }

    fun returns(returnType: Type) = returns(TypeName.get(returnType))

    fun returns(returnType: KClass<*>) = returns(TypeName.get(returnType))

    fun addParameters(parameterSpecs: Iterable<ParameterSpec>): Builder {
      for (parameterSpec in parameterSpecs) {
        addParameter(parameterSpec)
      }
      return this
    }

    fun addParameter(parameterSpec: ParameterSpec): Builder {
      check(name != GETTER) { "$name cannot have parameters" }
      check(name != SETTER || parameters.size == 0) { "$name can have only one parameter" }
      parameters += parameterSpec
      return this
    }

    fun addParameter(name: String, type: TypeName, vararg modifiers: KModifier)
        = addParameter(ParameterSpec.builder(name, type, *modifiers).build())

    fun addParameter(name: String, type: Type, vararg modifiers: KModifier)
        = addParameter(name, TypeName.get(type), *modifiers)

    fun addParameter(name: String, type: KClass<*>, vararg modifiers: KModifier)
        = addParameter(name, TypeName.get(type), *modifiers)

    @JvmOverloads fun varargs(varargs: Boolean = true): Builder {
      check(!name.isAccessor) { "$name cannot have varargs" }
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
      check(!name.isAccessor && !name.isConstructor) { "$name cannot have a default value" }
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
    const internal val CONSTRUCTOR = "constructor()"
    const internal val GETTER = "get()"
    const internal val SETTER = "set()"

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
     * throws declarations. An `override` modifier will be added.
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
