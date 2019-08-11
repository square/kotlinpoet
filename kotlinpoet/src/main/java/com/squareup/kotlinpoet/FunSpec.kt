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

import com.squareup.kotlinpoet.KModifier.ABSTRACT
import com.squareup.kotlinpoet.KModifier.EXPECT
import com.squareup.kotlinpoet.KModifier.EXTERNAL
import com.squareup.kotlinpoet.KModifier.INLINE
import com.squareup.kotlinpoet.KModifier.VARARG
import java.lang.reflect.Type
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.ExecutableType
import javax.lang.model.type.TypeVariable
import javax.lang.model.util.Types
import kotlin.reflect.KClass

/** A generated function declaration.  */
class FunSpec private constructor(
  builder: Builder,
  private val tagMap: TagMap = builder.buildTagMap(),
  private val delegateOriginatingElementsHolder: OriginatingElementsHolder = builder.buildOriginatingElements()
) : Taggable by tagMap, OriginatingElementsHolder by delegateOriginatingElementsHolder {
  val name = builder.name
  val kdoc = builder.kdoc.build()
  val returnKdoc = builder.returnKdoc
  val receiverKdoc = builder.receiverKdoc
  val annotations = builder.annotations.toImmutableList()
  val modifiers = builder.modifiers.toImmutableSet()
  val typeVariables = builder.typeVariables.toImmutableList()
  val receiverType = builder.receiverType
  val returnType = builder.returnType
  val parameters = builder.parameters.toImmutableList()
  val delegateConstructor = builder.delegateConstructor
  val delegateConstructorArguments = builder.delegateConstructorArguments.toImmutableList()
  val body = builder.body.build()
  private val isEmptySetter = name == SETTER && parameters.isEmpty()

  init {
    require(body.isEmpty() || ABSTRACT !in builder.modifiers) {
      "abstract function ${builder.name} cannot have code"
    }
    require(name != SETTER || parameters.size <= 1) {
      "$name can have at most one parameter"
    }
    require(INLINE in modifiers || typeVariables.none { it.isReified }) {
      "only type parameters of inline functions can be reified!"
    }
  }

  internal fun parameter(name: String) = parameters.firstOrNull { it.name == name }

  internal fun emit(
    codeWriter: CodeWriter,
    enclosingName: String?,
    implicitModifiers: Set<KModifier>,
    includeKdocTags: Boolean = false
  ) {
    if (includeKdocTags) {
      codeWriter.emitKdoc(kdocWithTags())
    } else {
      codeWriter.emitKdoc(kdoc.ensureEndsWithNewLine())
    }
    codeWriter.emitAnnotations(annotations, false)
    codeWriter.emitModifiers(modifiers, implicitModifiers)

    if (!isConstructor && !name.isAccessor) {
      codeWriter.emitCode("fun·")
    }

    if (typeVariables.isNotEmpty()) {
      codeWriter.emitTypeVariables(typeVariables)
      codeWriter.emit(" ")
    }
    emitSignature(codeWriter, enclosingName)
    codeWriter.emitWhereBlock(typeVariables)

    val isEmptyConstructor = isConstructor && body.isEmpty()
    if (modifiers.containsAnyOf(ABSTRACT, EXTERNAL, EXPECT) || EXPECT in implicitModifiers ||
        isEmptyConstructor) {
      codeWriter.emit("\n")
      return
    }

    val asExpressionBody = body.asExpressionBody()

    if (asExpressionBody != null) {
      codeWriter.emitCode(" = %L", asExpressionBody)
    } else if (!isEmptySetter) {
      codeWriter.emitCode("·{\n")
      codeWriter.indent()
      codeWriter.emitCode(body, ensureTrailingNewline = true)
      codeWriter.unindent()
      codeWriter.emit("}\n")
    }
  }

  private fun emitSignature(codeWriter: CodeWriter, enclosingName: String?) {
    if (isConstructor) {
      codeWriter.emitCode("constructor", enclosingName)
    } else if (name == GETTER) {
      codeWriter.emitCode("get")
    } else if (name == SETTER) {
      codeWriter.emitCode("set")
    } else {
      if (receiverType != null) {
        if (receiverType is LambdaTypeName) {
          codeWriter.emitCode("(%T).", receiverType)
        } else {
          codeWriter.emitCode("%T.", receiverType)
        }
      }
      codeWriter.emitCode("%N", this)
    }

    if (!isEmptySetter) {
      parameters.emit(codeWriter) { param ->
        param.emit(codeWriter, includeType = name != SETTER)
      }
    }

    if (emitReturnType(returnType)) {
      codeWriter.emitCode(": %T", returnType)
    }

    if (delegateConstructor != null) {
      codeWriter.emitCode(delegateConstructorArguments
          .joinToCode(prefix = " : $delegateConstructor(", suffix = ")"))
    }
  }

  val isConstructor get() = name.isConstructor

  val isAccessor get() = name.isAccessor

  private fun kdocWithTags(): CodeBlock {
    return with(kdoc.ensureEndsWithNewLine().toBuilder()) {
      var newLineAdded = false
      val isNotEmpty = isNotEmpty()
      if (receiverKdoc.isNotEmpty()) {
        if (isNotEmpty) {
          add("\n")
          newLineAdded = true
        }
        add("@receiver %L", receiverKdoc.ensureEndsWithNewLine())
      }
      parameters.forEachIndexed { index, parameterSpec ->
        if (parameterSpec.kdoc.isNotEmpty()) {
          if (!newLineAdded && index == 0 && isNotEmpty) {
            add("\n")
            newLineAdded = true
          }
          add("@param %L %L", parameterSpec.name, parameterSpec.kdoc.ensureEndsWithNewLine())
        }
      }
      if (returnKdoc.isNotEmpty()) {
        if (!newLineAdded && isNotEmpty) {
          add("\n")
          newLineAdded = true
        }
        add("@return %L", returnKdoc.ensureEndsWithNewLine())
      }
      build()
    }
  }

  /**
   * Returns whether we should emit the return type for given [returnType].
   *
   * For return types like [Unit], we can omit emitting the return type, only
   * if it's not a single expression body.
   * If it is a single expression body, we want to emit [Unit] so that any type
   * change in the delegated function doesn't inadvertently carry over to the
   * delegating function.
   */
  private fun emitReturnType(returnType: TypeName?): Boolean {
    if (returnType != null) {
      return returnType != Unit::class.asTypeName() || body.asExpressionBody() != null
    }
    return false
  }

  private fun CodeBlock.asExpressionBody(): CodeBlock? {
    val codeBlock = this.trim()
    val asReturnExpressionBody = codeBlock.withoutPrefix(RETURN_EXPRESSION_BODY_PREFIX)
    if (asReturnExpressionBody != null) {
      return asReturnExpressionBody
    }
    if (codeBlock.withoutPrefix(THROW_EXPRESSION_BODY_PREFIX) != null) {
      return codeBlock
    }
    return null
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null) return false
    if (javaClass != other.javaClass) return false
    return toString() == other.toString()
  }

  override fun hashCode() = toString().hashCode()

  override fun toString() = buildCodeString {
    emit(
        codeWriter = this,
        enclosingName = "Constructor",
        implicitModifiers = TypeSpec.Kind.CLASS.implicitFunctionModifiers(),
        includeKdocTags = true)
  }

  @JvmOverloads
  fun toBuilder(name: String = this.name): Builder {
    val builder = Builder(name)
    builder.kdoc.add(kdoc)
    builder.returnKdoc = returnKdoc
    builder.receiverKdoc = receiverKdoc
    builder.annotations += annotations
    builder.modifiers += modifiers
    builder.typeVariables += typeVariables
    builder.returnType = returnType
    builder.parameters += parameters
    builder.delegateConstructor = delegateConstructor
    builder.delegateConstructorArguments += delegateConstructorArguments
    builder.body.add(body)
    builder.receiverType = receiverType
    builder.tags += tagMap.tags
    builder.originatingElements += originatingElements
    return builder
  }

  class Builder internal constructor(
    internal val name: String
  ) : Taggable.Builder<FunSpec.Builder>, OriginatingElementsHolder.Builder<FunSpec.Builder> {
    internal val kdoc = CodeBlock.builder()
    internal var returnKdoc = CodeBlock.EMPTY
    internal var receiverKdoc = CodeBlock.EMPTY
    internal var receiverType: TypeName? = null
    internal var returnType: TypeName? = null
    internal var delegateConstructor: String? = null
    internal var delegateConstructorArguments = listOf<CodeBlock>()
    internal val body = CodeBlock.builder()

    val annotations = mutableListOf<AnnotationSpec>()
    val modifiers = mutableListOf<KModifier>()
    val typeVariables = mutableListOf<TypeVariableName>()
    val parameters = mutableListOf<ParameterSpec>()
    override val tags = mutableMapOf<KClass<*>, Any>()
    override val originatingElements = mutableListOf<Element>()

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
          Modifier.DEFAULT -> Unit
          Modifier.STATIC -> addAnnotation(JvmStatic::class)
          Modifier.SYNCHRONIZED -> addAnnotation(Synchronized::class)
          Modifier.STRICTFP -> addAnnotation(Strictfp::class)
          else -> throw IllegalArgumentException("unexpected fun modifier $modifier")
        }
      }
      this.modifiers += visibility
    }

    fun addTypeVariables(typeVariables: Iterable<TypeVariableName>) = apply {
      this.typeVariables += typeVariables
    }

    fun addTypeVariable(typeVariable: TypeVariableName) = apply {
      typeVariables += typeVariable
    }

    @JvmOverloads fun receiver(
      receiverType: TypeName,
      kdoc: CodeBlock = CodeBlock.EMPTY
    ) = apply {
      check(!name.isConstructor) { "$name cannot have receiver type" }
      this.receiverType = receiverType
      this.receiverKdoc = kdoc
    }

    @JvmOverloads fun receiver(
      receiverType: Type,
      kdoc: CodeBlock = CodeBlock.EMPTY
    ) = receiver(receiverType.asTypeName(), kdoc)

    fun receiver(
      receiverType: Type,
      kdoc: String,
      vararg args: Any
    ) = receiver(receiverType, CodeBlock.of(kdoc, args))

    @JvmOverloads fun receiver(
      receiverType: KClass<*>,
      kdoc: CodeBlock = CodeBlock.EMPTY
    ) = receiver(receiverType.asTypeName(), kdoc)

    fun receiver(
      receiverType: KClass<*>,
      kdoc: String,
      vararg args: Any
    ) = receiver(receiverType, CodeBlock.of(kdoc, args))

    @JvmOverloads fun returns(returnType: TypeName, kdoc: CodeBlock = CodeBlock.EMPTY) = apply {
      check(!name.isConstructor && !name.isAccessor) { "$name cannot have a return type" }
      this.returnType = returnType
      this.returnKdoc = kdoc
    }

    @JvmOverloads fun returns(returnType: Type, kdoc: CodeBlock = CodeBlock.EMPTY) = returns(returnType.asTypeName(), kdoc)

    fun returns(returnType: Type, kdoc: String, vararg args: Any) = returns(returnType.asTypeName(), CodeBlock.of(kdoc, args))

    @JvmOverloads fun returns(returnType: KClass<*>, kdoc: CodeBlock = CodeBlock.EMPTY) = returns(returnType.asTypeName(), kdoc)

    fun returns(returnType: KClass<*>, kdoc: String, vararg args: Any) =
        returns(returnType.asTypeName(), CodeBlock.of(kdoc, args))

    fun addParameters(parameterSpecs: Iterable<ParameterSpec>) = apply {
      for (parameterSpec in parameterSpecs) {
        addParameter(parameterSpec)
      }
    }

    fun addParameter(parameterSpec: ParameterSpec) = apply {
      parameters += parameterSpec
    }

    fun callThisConstructor(args: List<CodeBlock>) = apply {
      callConstructor("this", args)
    }

    fun callThisConstructor(args: Iterable<CodeBlock>) = apply {
      callConstructor("this", args.toList())
    }

    fun callThisConstructor(vararg args: String) = apply {
      callConstructor("this", args.map { CodeBlock.of(it) })
    }

    fun callThisConstructor(vararg args: CodeBlock = emptyArray()) = apply {
      callConstructor("this", args.toList())
    }

    fun callSuperConstructor(args: Iterable<CodeBlock>) = apply {
      callConstructor("super", args.toList())
    }

    fun callSuperConstructor(args: List<CodeBlock>) = apply {
      callConstructor("super", args)
    }

    fun callSuperConstructor(vararg args: String) = apply {
      callConstructor("super", args.map { CodeBlock.of(it) })
    }

    fun callSuperConstructor(vararg args: CodeBlock = emptyArray()) = apply {
      callConstructor("super", args.toList())
    }

    private fun callConstructor(constructor: String, args: List<CodeBlock>) {
      check(name.isConstructor) { "only constructors can delegate to other constructors!" }
      delegateConstructor = constructor
      delegateConstructorArguments = args
    }

    fun addParameter(name: String, type: TypeName, vararg modifiers: KModifier) =
        addParameter(ParameterSpec.builder(name, type, *modifiers).build())

    fun addParameter(name: String, type: Type, vararg modifiers: KModifier) =
        addParameter(name, type.asTypeName(), *modifiers)

    fun addParameter(name: String, type: KClass<*>, vararg modifiers: KModifier) =
        addParameter(name, type.asTypeName(), *modifiers)

    fun addParameter(name: String, type: TypeName, modifiers: Iterable<KModifier>) =
        addParameter(ParameterSpec.builder(name, type, modifiers).build())

    fun addParameter(name: String, type: Type, modifiers: Iterable<KModifier>) =
        addParameter(name, type.asTypeName(), modifiers)

    fun addParameter(name: String, type: KClass<*>, modifiers: Iterable<KModifier>) =
        addParameter(name, type.asTypeName(), modifiers)

    fun addCode(format: String, vararg args: Any?) = apply {
      body.add(format, *args)
    }

    fun addNamedCode(format: String, args: Map<String, *>) = apply {
      body.addNamed(format, args)
    }

    fun addCode(codeBlock: CodeBlock) = apply {
      body.add(codeBlock)
    }

    fun addComment(format: String, vararg args: Any) = apply {
      body.add("//·${format.replace(' ', '·')}\n", *args)
    }

    /**
     * @param controlFlow the control flow construct and its code, such as "if (foo == 5)".
     * * Shouldn't contain braces or newline characters.
     */
    fun beginControlFlow(controlFlow: String, vararg args: Any) = apply {
      body.beginControlFlow(controlFlow, *args)
    }

    /**
     * @param controlFlow the control flow construct and its code, such as "else if (foo == 10)".
     * *     Shouldn't contain braces or newline characters.
     */
    fun nextControlFlow(controlFlow: String, vararg args: Any) = apply {
      body.nextControlFlow(controlFlow, *args)
    }

    fun endControlFlow() = apply {
      body.endControlFlow()
    }

    fun addStatement(format: String, vararg args: Any) = apply {
      body.addStatement(format, *args)
    }

    fun clearBody() = apply {
      body.clear()
    }

    fun build(): FunSpec {
      check(typeVariables.isEmpty() || !name.isAccessor) { "$name cannot have type variables" }
      check(!(name == GETTER && parameters.isNotEmpty())) { "$name cannot have parameters" }
      check(!(name == SETTER && parameters.size > 1)) { "$name can have at most one parameter" }
      return FunSpec(this)
    }
  }

  companion object {
    private const val CONSTRUCTOR = "constructor()"
    internal const val GETTER = "get()"
    internal const val SETTER = "set()"

    internal val String.isConstructor get() = this == CONSTRUCTOR
    internal val String.isAccessor get() = this.isOneOf(GETTER, SETTER)

    private val RETURN_EXPRESSION_BODY_PREFIX = CodeBlock.of("return ")
    private val THROW_EXPRESSION_BODY_PREFIX = CodeBlock.of("throw ")

    @JvmStatic fun builder(name: String) = Builder(name)

    @JvmStatic fun constructorBuilder() = Builder(CONSTRUCTOR)

    @JvmStatic fun getterBuilder() = Builder(GETTER)

    @JvmStatic fun setterBuilder() = Builder(SETTER)

    /**
     * Returns a new fun spec builder that overrides `method`.

     *
     * This will copy its visibility modifiers, type parameters, return type, name, parameters, and
     * throws declarations. An `override` modifier will be added.
     */
    @JvmStatic fun overriding(method: ExecutableElement): Builder {
      var modifiers: Set<Modifier> = method.modifiers
      require(Modifier.PRIVATE !in modifiers &&
          Modifier.FINAL !in modifiers &&
          Modifier.STATIC !in modifiers) {
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
            .addMember(throwsValueString, *method.thrownTypes.toTypedArray())
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
      method: ExecutableElement,
      enclosing: DeclaredType,
      types: Types
    ): Builder {
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
