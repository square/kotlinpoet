/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
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
import kotlin.DeprecationLevel.WARNING
import kotlin.reflect.KClass

/** A generated function declaration. */
@OptIn(ExperimentalKotlinPoetApi::class)
public class FunSpec private constructor(
  builder: Builder,
  private val tagMap: TagMap = builder.buildTagMap(),
  private val delegateOriginatingElementsHolder: OriginatingElementsHolder = builder.buildOriginatingElements(),
  private val contextReceivers: ContextReceivers = builder.buildContextReceivers(),
  private val contextParams: ContextParameters = builder.buildContextParameters(),
) : Taggable by tagMap,
  OriginatingElementsHolder by delegateOriginatingElementsHolder,
  ContextReceivable by contextReceivers,
  ContextParameterizable by contextParams,
  Annotatable,
  Documentable {
  public val name: String = builder.name
  override val kdoc: CodeBlock = builder.kdoc.build()
  public val returnKdoc: CodeBlock = builder.returnKdoc
  public val receiverKdoc: CodeBlock = builder.receiverKdoc
  override val annotations: List<AnnotationSpec> = builder.annotations.toImmutableList()
  public val modifiers: Set<KModifier> = builder.modifiers.toImmutableSet()
  public val typeVariables: List<TypeVariableName> = builder.typeVariables.toImmutableList()
  public val receiverType: TypeName? = builder.receiverType

  public val returnType: TypeName = builder.returnType
  public val parameters: List<ParameterSpec> = builder.parameters.toImmutableList()
  public val delegateConstructor: String? = builder.delegateConstructor
  public val delegateConstructorArguments: List<CodeBlock> =
    builder.delegateConstructorArguments.toImmutableList()
  public val body: CodeBlock = builder.body.build()
  private val isExternalGetter = name == GETTER && builder.modifiers.contains(EXTERNAL)
  private val isEmptySetter = name == SETTER && parameters.isEmpty()

  init {
    require(body.isEmpty() || !builder.modifiers.containsAnyOf(ABSTRACT, EXPECT)) {
      "abstract or expect function ${builder.name} cannot have code"
    }
    if (name == GETTER) {
      require(!isExternalGetter || body.isEmpty()) {
        "external getter cannot have code"
      }
    } else if (name == SETTER) {
      require(parameters.size <= 1) {
        "$name can have at most one parameter"
      }
      require(parameters.isNotEmpty() || body.isEmpty()) {
        "parameterless setter cannot have code"
      }
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
    includeKdocTags: Boolean = false,
  ) {
    if (includeKdocTags) {
      codeWriter.emitKdoc(kdocWithTags())
    } else {
      codeWriter.emitKdoc(kdoc.ensureEndsWithNewLine())
    }
    if (contextParameters.isNotEmpty()) {
      codeWriter.emitContextParameters(contextParameters, suffix = "\n")
    } else {
      codeWriter.emitContextReceivers(contextReceiverTypes, suffix = "\n")
    }
    codeWriter.emitAnnotations(annotations, false)
    codeWriter.emitModifiers(modifiers, implicitModifiers)

    if (!isConstructor && !name.isAccessor) {
      codeWriter.emitCode("fun ")
    }

    if (typeVariables.isNotEmpty()) {
      codeWriter.emitTypeVariables(typeVariables)
      codeWriter.emit(" ")
    }
    emitSignature(codeWriter, enclosingName)
    codeWriter.emitWhereBlock(typeVariables)

    if (shouldOmitBody(implicitModifiers)) {
      codeWriter.emit("\n")
      return
    }

    val asExpressionBody = body.asExpressionBody()

    if (asExpressionBody != null) {
      codeWriter.emitCode(CodeBlock.of(" = %L", asExpressionBody), ensureTrailingNewline = true)
    } else if (!isEmptySetter) {
      codeWriter.emitCode(" {\n")
      codeWriter.indent()
      codeWriter.emitCode(body.returnsWithoutLinebreak(), ensureTrailingNewline = true)
      codeWriter.unindent()
      codeWriter.emit("}\n")
    } else {
      codeWriter.emit("\n")
    }
  }

  private fun shouldOmitBody(implicitModifiers: Set<KModifier>): Boolean {
    if (canNotHaveBody(implicitModifiers)) {
      check(body.isEmpty()) { "function $name cannot have code" }
      return true
    }
    return canBodyBeOmitted(implicitModifiers) && body.isEmpty()
  }

  private fun canNotHaveBody(implicitModifiers: Set<KModifier>) =
    ABSTRACT in modifiers || EXPECT in modifiers + implicitModifiers

  private fun canBodyBeOmitted(implicitModifiers: Set<KModifier>) = isConstructor ||
    EXTERNAL in (modifiers + implicitModifiers) ||
    ABSTRACT in modifiers

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

    if (!isEmptySetter && !isExternalGetter) {
      parameters.emit(codeWriter) { param ->
        param.emit(codeWriter, includeType = name != SETTER)
      }
    }

    if (returnType != UNIT || emitUnitReturnType()) {
      codeWriter.emitCode(": %T", returnType)
    }

    if (delegateConstructor != null) {
      codeWriter.emitCode(
        delegateConstructorArguments
          .joinToCode(prefix = " : $delegateConstructor(", suffix = ")"),
      )
    }
  }

  public val isConstructor: Boolean get() = name.isConstructor

  public val isAccessor: Boolean get() = name.isAccessor

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
   * Returns whether [Unit] should be emitted as the return type.
   *
   * [Unit] is emitted as return type on a function only if it is an expression body.
   */
  private fun emitUnitReturnType(): Boolean {
    if (isConstructor) {
      return false
    }
    if (name == GETTER || name == SETTER) {
      // Getter/setters don't emit return types
      return false
    }

    return body.asExpressionBody() != null
  }

  private fun CodeBlock.asExpressionBody(): CodeBlock? {
    val codeBlock = this.trim()

    // If after trimming there are unmatched closing statement symbols, we can't have an expression body.
    if (codeBlock.hasUnmatchedClosingStatement()) {
      return null
    }

    val asReturnExpressionBody = codeBlock.withoutPrefix(RETURN_EXPRESSION_BODY_PREFIX_SPACE)
      ?: codeBlock.withoutPrefix(RETURN_EXPRESSION_BODY_PREFIX_NBSP)
    if (asReturnExpressionBody != null) {
      return asReturnExpressionBody
    }
    if (codeBlock.withoutPrefix(THROW_EXPRESSION_BODY_PREFIX_SPACE) != null ||
      codeBlock.withoutPrefix(THROW_EXPRESSION_BODY_PREFIX_NBSP) != null
    ) {
      return codeBlock
    }
    return null
  }

  private fun CodeBlock.returnsWithoutLinebreak(): CodeBlock {
    val returnWithSpace = RETURN_EXPRESSION_BODY_PREFIX_SPACE.formatParts[0]
    val returnWithNbsp = RETURN_EXPRESSION_BODY_PREFIX_NBSP.formatParts[0]
    var originCodeBlockBuilder: CodeBlock.Builder? = null
    for ((i, formatPart) in formatParts.withIndex()) {
      if (formatPart.startsWith(returnWithSpace)) {
        val builder = originCodeBlockBuilder ?: toBuilder()
        originCodeBlockBuilder = builder
        builder.formatParts[i] = formatPart.replaceFirst(returnWithSpace, returnWithNbsp)
      }
    }
    return originCodeBlockBuilder?.build() ?: this
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null) return false
    if (javaClass != other.javaClass) return false
    return toString() == other.toString()
  }

  override fun hashCode(): Int = toString().hashCode()

  override fun toString(): String = buildCodeString {
    emit(
      codeWriter = this,
      enclosingName = "Constructor",
      implicitModifiers = TypeSpec.Kind.CLASS.implicitFunctionModifiers(),
      includeKdocTags = true,
    )
  }

  @JvmOverloads
  public fun toBuilder(name: String = this.name): Builder {
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
    builder.contextReceiverTypes += contextReceiverTypes
    builder.contextParameters += contextParameters
    return builder
  }

  public class Builder internal constructor(
    internal val name: String,
  ) : Taggable.Builder<Builder>,
    OriginatingElementsHolder.Builder<Builder>,
    ContextReceivable.Builder<Builder>,
    ContextParameterizable.Builder<Builder>,
    Annotatable.Builder<Builder>,
    Documentable.Builder<Builder> {
    internal var returnKdoc = CodeBlock.EMPTY
    internal var receiverKdoc = CodeBlock.EMPTY
    internal var receiverType: TypeName? = null
    internal var returnType: TypeName = UNIT
    internal var delegateConstructor: String? = null
    internal var delegateConstructorArguments = listOf<CodeBlock>()
    internal val body = CodeBlock.builder()
    override val kdoc: CodeBlock.Builder = CodeBlock.builder()
    override val annotations: MutableList<AnnotationSpec> = mutableListOf()
    public val modifiers: MutableList<KModifier> = mutableListOf()
    public val typeVariables: MutableList<TypeVariableName> = mutableListOf()
    public val parameters: MutableList<ParameterSpec> = mutableListOf()
    override val tags: MutableMap<KClass<*>, Any> = mutableMapOf()
    override val originatingElements: MutableList<Element> = mutableListOf()

    @ExperimentalKotlinPoetApi
    override val contextReceiverTypes: MutableList<TypeName> = mutableListOf()

    @ExperimentalKotlinPoetApi
    override val contextParameters: MutableList<ContextParameter> = mutableListOf()

    public fun addModifiers(vararg modifiers: KModifier): Builder = apply {
      this.modifiers += modifiers
    }

    public fun addModifiers(modifiers: Iterable<KModifier>): Builder = apply {
      this.modifiers += modifiers
    }

    public fun jvmModifiers(modifiers: Iterable<Modifier>) {
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

    public fun addTypeVariables(typeVariables: Iterable<TypeVariableName>): Builder = apply {
      this.typeVariables += typeVariables
    }

    public fun addTypeVariable(typeVariable: TypeVariableName): Builder = apply {
      typeVariables += typeVariable
    }

    @ExperimentalKotlinPoetApi
    override fun contextReceivers(receiverTypes: Iterable<TypeName>): Builder = apply {
      check(!name.isConstructor) { "constructors cannot have context receivers" }
      check(!name.isAccessor) { "$name cannot have context receivers" }
      contextReceiverTypes += receiverTypes
    }

    /**
     * Adds context parameters to this function.
     */
    @ExperimentalKotlinPoetApi
    override fun contextParameters(parameters: Iterable<ContextParameter>): Builder = apply {
      checkContextParametersAllowed()
      contextParameters += parameters
    }

    private fun checkContextParametersAllowed() {
      check(!name.isConstructor) { "constructors cannot have context parameters" }
      check(!name.isAccessor) { "$name cannot have context parameters" }
    }

    @JvmOverloads public fun receiver(
      receiverType: TypeName,
      kdoc: CodeBlock = CodeBlock.EMPTY,
    ): Builder = apply {
      check(!name.isConstructor) { "$name cannot have receiver type" }
      this.receiverType = receiverType
      this.receiverKdoc = kdoc
    }

    @JvmOverloads public fun receiver(
      receiverType: Type,
      kdoc: CodeBlock = CodeBlock.EMPTY,
    ): Builder = receiver(receiverType.asTypeName(), kdoc)

    public fun receiver(
      receiverType: Type,
      kdoc: String,
      vararg args: Any,
    ): Builder = receiver(receiverType, CodeBlock.of(kdoc, args))

    @JvmOverloads public fun receiver(
      receiverType: KClass<*>,
      kdoc: CodeBlock = CodeBlock.EMPTY,
    ): Builder = receiver(receiverType.asTypeName(), kdoc)

    public fun receiver(
      receiverType: KClass<*>,
      kdoc: String,
      vararg args: Any,
    ): Builder = receiver(receiverType, CodeBlock.of(kdoc, args))

    @JvmOverloads public fun returns(
      returnType: TypeName,
      kdoc: CodeBlock = CodeBlock.EMPTY,
    ): Builder = apply {
      check(!name.isConstructor && !name.isAccessor) { "$name cannot have a return type" }
      this.returnType = returnType
      this.returnKdoc = kdoc
    }

    @JvmOverloads public fun returns(returnType: Type, kdoc: CodeBlock = CodeBlock.EMPTY): Builder =
      returns(returnType.asTypeName(), kdoc)

    public fun returns(returnType: Type, kdoc: String, vararg args: Any): Builder =
      returns(returnType.asTypeName(), CodeBlock.of(kdoc, args))

    @JvmOverloads public fun returns(
      returnType: KClass<*>,
      kdoc: CodeBlock = CodeBlock.EMPTY,
    ): Builder = returns(returnType.asTypeName(), kdoc)

    public fun returns(returnType: KClass<*>, kdoc: String, vararg args: Any): Builder =
      returns(returnType.asTypeName(), CodeBlock.of(kdoc, args))

    public fun addParameters(parameterSpecs: Iterable<ParameterSpec>): Builder = apply {
      for (parameterSpec in parameterSpecs) {
        addParameter(parameterSpec)
      }
    }

    public fun addParameter(parameterSpec: ParameterSpec): Builder = apply {
      parameters += parameterSpec
    }

    public fun callThisConstructor(args: List<CodeBlock>): Builder = apply {
      callConstructor("this", args)
    }

    public fun callThisConstructor(args: Iterable<CodeBlock>): Builder = apply {
      callConstructor("this", args.toList())
    }

    public fun callThisConstructor(vararg args: String): Builder = apply {
      callConstructor("this", args.map { CodeBlock.of(it) })
    }

    public fun callThisConstructor(vararg args: CodeBlock = emptyArray()): Builder = apply {
      callConstructor("this", args.toList())
    }

    public fun callSuperConstructor(args: Iterable<CodeBlock>): Builder = apply {
      callConstructor("super", args.toList())
    }

    public fun callSuperConstructor(args: List<CodeBlock>): Builder = apply {
      callConstructor("super", args)
    }

    public fun callSuperConstructor(vararg args: String): Builder = apply {
      callConstructor("super", args.map { CodeBlock.of(it) })
    }

    public fun callSuperConstructor(vararg args: CodeBlock = emptyArray()): Builder = apply {
      callConstructor("super", args.toList())
    }

    private fun callConstructor(constructor: String, args: List<CodeBlock>) {
      check(name.isConstructor) { "only constructors can delegate to other constructors!" }
      delegateConstructor = constructor
      delegateConstructorArguments = args
    }

    public fun addParameter(name: String, type: TypeName, vararg modifiers: KModifier): Builder =
      addParameter(ParameterSpec.builder(name, type, *modifiers).build())

    public fun addParameter(name: String, type: Type, vararg modifiers: KModifier): Builder =
      addParameter(name, type.asTypeName(), *modifiers)

    public fun addParameter(name: String, type: KClass<*>, vararg modifiers: KModifier): Builder =
      addParameter(name, type.asTypeName(), *modifiers)

    public fun addParameter(name: String, type: TypeName, modifiers: Iterable<KModifier>): Builder =
      addParameter(ParameterSpec.builder(name, type, modifiers).build())

    public fun addParameter(name: String, type: Type, modifiers: Iterable<KModifier>): Builder =
      addParameter(name, type.asTypeName(), modifiers)

    public fun addParameter(
      name: String,
      type: KClass<*>,
      modifiers: Iterable<KModifier>,
    ): Builder = addParameter(name, type.asTypeName(), modifiers)

    public fun addCode(format: String, vararg args: Any?): Builder = apply {
      body.add(format, *args)
    }

    public fun addNamedCode(format: String, args: Map<String, *>): Builder = apply {
      body.addNamed(format, args)
    }

    public fun addCode(codeBlock: CodeBlock): Builder = apply {
      body.add(codeBlock)
    }

    public fun addComment(format: String, vararg args: Any): Builder = apply {
      body.add("// $format\n", *args)
    }

    /**
     * @param controlFlow the control flow construct and its code, such as "if (foo == 5)".
     * * Shouldn't contain braces or newline characters.
     */
    public fun beginControlFlow(controlFlow: String, vararg args: Any?): Builder = apply {
      body.beginControlFlow(controlFlow, *args)
    }

    /**
     * @param controlFlow the control flow construct and its code, such as "else if (foo == 10)".
     * *     Shouldn't contain braces or newline characters.
     */
    public fun nextControlFlow(controlFlow: String, vararg args: Any): Builder = apply {
      body.nextControlFlow(controlFlow, *args)
    }

    public fun endControlFlow(): Builder = apply {
      body.endControlFlow()
    }

    public fun addStatement(format: String, vararg args: Any): Builder = apply {
      body.addStatement(format, *args)
    }

    public fun clearBody(): Builder = apply {
      body.clear()
    }

    //region Overrides for binary compatibility
    @Suppress("RedundantOverride")
    override fun addAnnotation(annotationSpec: AnnotationSpec): Builder = super.addAnnotation(annotationSpec)

    @Suppress("RedundantOverride")
    override fun addAnnotations(annotationSpecs: Iterable<AnnotationSpec>): Builder =
      super.addAnnotations(annotationSpecs)

    @Suppress("RedundantOverride")
    override fun addAnnotation(annotation: ClassName): Builder = super.addAnnotation(annotation)

    @DelicateKotlinPoetApi(
      message = "Java reflection APIs don't give complete information on Kotlin types. Consider " +
        "using the kotlinpoet-metadata APIs instead.",
    )
    override fun addAnnotation(annotation: Class<*>): Builder = super.addAnnotation(annotation)

    @Suppress("RedundantOverride")
    override fun addAnnotation(annotation: KClass<*>): Builder = super.addAnnotation(annotation)

    @Suppress("RedundantOverride")
    override fun addKdoc(format: String, vararg args: Any): Builder = super.addKdoc(format, *args)

    @Suppress("RedundantOverride")
    override fun addKdoc(block: CodeBlock): Builder = super.addKdoc(block)
    //endregion

    public fun build(): FunSpec {
      check(typeVariables.isEmpty() || !name.isAccessor) { "$name cannot have type variables" }
      check(!(name == GETTER && parameters.isNotEmpty())) { "$name cannot have parameters" }
      check(!(name == SETTER && parameters.size > 1)) { "$name can have at most one parameter" }
      check(contextReceiverTypes.isEmpty() || contextParameters.isEmpty()) {
        "Using both context receivers and context parameters is not allowed"
      }
      return FunSpec(this)
    }
  }

  public companion object {
    private const val CONSTRUCTOR = "constructor()"
    internal const val GETTER = "get()"
    internal const val SETTER = "set()"

    internal val String.isConstructor get() = this == CONSTRUCTOR
    internal val String.isAccessor get() = this.isOneOf(GETTER, SETTER)

    private val RETURN_EXPRESSION_BODY_PREFIX_SPACE = CodeBlock.of("return ")
    private val RETURN_EXPRESSION_BODY_PREFIX_NBSP = CodeBlock.of("return·")
    private val THROW_EXPRESSION_BODY_PREFIX_SPACE = CodeBlock.of("throw ")
    private val THROW_EXPRESSION_BODY_PREFIX_NBSP = CodeBlock.of("throw·")

    @JvmStatic public fun builder(name: String): Builder = Builder(name)

    /** Create a new function builder from [MemberName.simpleName] */
    @JvmStatic public fun builder(memberName: MemberName): Builder = Builder(memberName.simpleName)

    @JvmStatic public fun constructorBuilder(): Builder = Builder(CONSTRUCTOR)

    @JvmStatic public fun getterBuilder(): Builder = Builder(GETTER)

    @JvmStatic public fun setterBuilder(): Builder = Builder(SETTER)

    @DelicateKotlinPoetApi(
      message = "Element APIs don't give complete information on Kotlin types. Consider using" +
        " the kotlinpoet-metadata APIs instead.",
    )
    @JvmStatic
    public fun overriding(method: ExecutableElement): Builder {
      var modifiers: Set<Modifier> = method.modifiers
      require(
        Modifier.PRIVATE !in modifiers &&
          Modifier.FINAL !in modifiers &&
          Modifier.STATIC !in modifiers,
      ) {
        "cannot override method with modifiers: $modifiers"
      }

      val methodName = method.simpleName.toString()
      val funBuilder = builder(methodName)

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
        funBuilder.addAnnotation(
          AnnotationSpec.builder(Throws::class)
            .addMember(throwsValueString, *method.thrownTypes.toTypedArray())
            .build(),
        )
      }

      return funBuilder
    }

    @Deprecated(
      message = "Element APIs don't give complete information on Kotlin types. Consider using" +
        " the kotlinpoet-metadata APIs instead.",
      level = WARNING,
    )
    @JvmStatic
    public fun overriding(
      method: ExecutableElement,
      enclosing: DeclaredType,
      types: Types,
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
