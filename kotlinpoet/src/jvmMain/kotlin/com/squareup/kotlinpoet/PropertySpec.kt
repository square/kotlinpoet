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

import com.squareup.kotlinpoet.FunSpec.Companion.GETTER
import com.squareup.kotlinpoet.FunSpec.Companion.SETTER
import com.squareup.kotlinpoet.KModifier.Target.PROPERTY
import java.lang.reflect.Type
import java.util.EnumSet
import javax.lang.model.element.Element
import kotlin.reflect.KClass

/** A generated property declaration. */
@OptIn(ExperimentalKotlinPoetApi::class)
public class PropertySpec
private constructor(
  builder: Builder,
  private val tagMap: TagMap = builder.buildTagMap(),
  private val delegateOriginatingElementsHolder: OriginatingElementsHolder =
    builder.buildOriginatingElements(),
  private val contextReceivers: ContextReceivers = builder.buildContextReceivers(),
  private val contextParams: ContextParameters = builder.buildContextParameters(),
) :
  Taggable by tagMap,
  OriginatingElementsHolder by delegateOriginatingElementsHolder,
  ContextReceivable by contextReceivers,
  ContextParameterizable by contextParams,
  Annotatable,
  Documentable {
  public val mutable: Boolean = builder.mutable
  public val name: String = builder.name
  public val type: TypeName = builder.type
  override val kdoc: CodeBlock = builder.kdoc.build()
  override val annotations: List<AnnotationSpec> = builder.annotations.toImmutableList()
  public val modifiers: Set<KModifier> = builder.modifiers.toImmutableSet()
  public val typeVariables: List<TypeVariableName> = builder.typeVariables.toImmutableList()
  public val initializer: CodeBlock? = builder.initializer
  public val delegated: Boolean = builder.delegated
  public val getter: FunSpec? = builder.getter
  public val setter: FunSpec? = builder.setter
  public val receiverType: TypeName? = builder.receiverType

  init {
    require(
      typeVariables.none { it.isReified } ||
        (getter != null || setter != null) &&
          (getter == null || KModifier.INLINE in getter.modifiers) &&
          (setter == null || KModifier.INLINE in setter.modifiers)
    ) {
      "only type parameters of properties with inline getters and/or setters can be reified!"
    }
    require(mutable || setter == null) { "only a mutable property can have a setter" }
  }

  internal fun emit(
    codeWriter: CodeWriter,
    implicitModifiers: Set<KModifier>,
    withInitializer: Boolean = true,
    emitKdoc: Boolean = true,
    inline: Boolean = false,
    inlineAnnotations: Boolean = inline,
  ) {
    val isInlineProperty =
      getter?.modifiers?.contains(KModifier.INLINE) ?: false &&
        (!mutable || setter?.modifiers?.contains(KModifier.INLINE) ?: false)
    val propertyModifiers = if (isInlineProperty) modifiers + KModifier.INLINE else modifiers
    if (emitKdoc) {
      codeWriter.emitKdoc(kdoc.ensureEndsWithNewLine())
    }
    if (contextParameters.isNotEmpty()) {
      codeWriter.emitContextParameters(contextParameters, suffix = "\n")
    } else {
      codeWriter.emitContextReceivers(contextReceiverTypes, suffix = "\n")
    }
    codeWriter.emitAnnotations(annotations, inlineAnnotations)
    codeWriter.emitModifiers(propertyModifiers, implicitModifiers)
    codeWriter.emitCode(if (mutable) "var " else "val ")
    if (typeVariables.isNotEmpty()) {
      codeWriter.emitTypeVariables(typeVariables)
      codeWriter.emit("♢")
    }
    if (receiverType != null) {
      if (receiverType is LambdaTypeName) {
        codeWriter.emitCode("(%T).", receiverType)
      } else {
        codeWriter.emitCode("%T.", receiverType)
      }
    }
    codeWriter.emitCode("%N:♢%T", this, type)
    if (withInitializer && initializer != null) {
      if (delegated) {
        codeWriter.emit("♢by♢")
      } else {
        codeWriter.emitCode("♢=♢")
      }
      val initializerFormat = if (initializer.hasStatements()) "%L" else "«%L»"
      codeWriter.emitCode(
        codeBlock = CodeBlock.of(initializerFormat, initializer.trimTrailingNewLine()),
        isConstantContext = KModifier.CONST in modifiers,
      )
    }
    codeWriter.emitWhereBlock(typeVariables)
    if (!inline) codeWriter.emit("\n")
    val implicitAccessorModifiers = EnumSet.noneOf(KModifier::class.java)
    for (modifier in implicitModifiers) {
      // Omit visibility modifiers, accessor visibility will default to the property's visibility.
      if (modifier !in VISIBILITY_MODIFIERS) {
        implicitAccessorModifiers.add(modifier)
      }
    }
    if (isInlineProperty) {
      implicitAccessorModifiers.add(KModifier.INLINE)
    }
    if (getter != null) {
      codeWriter.emitCode("⇥")
      getter.emit(codeWriter, null, implicitAccessorModifiers, false)
      codeWriter.emitCode("⇤")
    }
    if (setter != null) {
      codeWriter.emitCode("⇥")
      setter.emit(codeWriter, null, implicitAccessorModifiers, false)
      codeWriter.emitCode("⇤")
    }
  }

  internal fun fromPrimaryConstructorParameter(parameter: ParameterSpec): PropertySpec {
    val builder = toBuilder().addAnnotations(parameter.annotations)
    builder.isPrimaryConstructorParameter = true
    builder.modifiers += parameter.modifiers
    if (builder.kdoc.isEmpty()) {
      builder.addKdoc(parameter.kdoc)
    }
    return builder.build()
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null) return false
    if (javaClass != other.javaClass) return false
    return toString() == other.toString()
  }

  override fun hashCode(): Int = toString().hashCode()

  override fun toString(): String = buildCodeString { emit(this, emptySet()) }

  @JvmOverloads
  public fun toBuilder(name: String = this.name, type: TypeName = this.type): Builder {
    val builder = Builder(name, type)
    builder.mutable = mutable
    builder.kdoc.add(kdoc)
    builder.annotations += annotations
    builder.modifiers += modifiers
    builder.typeVariables += typeVariables
    builder.initializer = initializer
    builder.delegated = delegated
    builder.setter = setter
    builder.getter = getter
    builder.receiverType = receiverType
    builder.tags += tagMap.tags
    builder.originatingElements += originatingElements
    builder.contextReceiverTypes += contextReceiverTypes
    builder.contextParameters += contextParameters
    return builder
  }

  public class Builder
  internal constructor(internal val name: String, internal val type: TypeName) :
    Taggable.Builder<Builder>,
    OriginatingElementsHolder.Builder<Builder>,
    ContextReceivable.Builder<Builder>,
    ContextParameterizable.Builder<Builder>,
    Annotatable.Builder<Builder>,
    Documentable.Builder<Builder> {
    internal var isPrimaryConstructorParameter = false
    internal var mutable = false
    internal var initializer: CodeBlock? = null
    internal var delegated = false
    internal var getter: FunSpec? = null
    internal var setter: FunSpec? = null
    internal var receiverType: TypeName? = null

    public val modifiers: MutableList<KModifier> = mutableListOf()
    public val typeVariables: MutableList<TypeVariableName> = mutableListOf()
    override val tags: MutableMap<KClass<*>, Any> = mutableMapOf()
    override val kdoc: CodeBlock.Builder = CodeBlock.builder()
    override val originatingElements: MutableList<Element> = mutableListOf()
    override val annotations: MutableList<AnnotationSpec> = mutableListOf()

    @ExperimentalKotlinPoetApi
    override val contextReceiverTypes: MutableList<TypeName> = mutableListOf()

    @ExperimentalKotlinPoetApi
    override val contextParameters: MutableList<ContextParameter> = mutableListOf()

    /** True to create a `var` instead of a `val`. */
    public fun mutable(mutable: Boolean = true): Builder = apply { this.mutable = mutable }

    public fun addModifiers(vararg modifiers: KModifier): Builder = apply {
      this.modifiers += modifiers
    }

    public fun addModifiers(modifiers: Iterable<KModifier>): Builder = apply {
      this.modifiers += modifiers
    }

    public fun addTypeVariables(typeVariables: Iterable<TypeVariableName>): Builder = apply {
      this.typeVariables += typeVariables
    }

    public fun addTypeVariable(typeVariable: TypeVariableName): Builder = apply {
      typeVariables += typeVariable
    }

    public fun initializer(format: String, vararg args: Any?): Builder =
      initializer(CodeBlock.of(format, *args))

    public fun initializer(codeBlock: CodeBlock?): Builder = apply {
      this.initializer = codeBlock
      this.delegated = false
    }

    public fun delegate(format: String, vararg args: Any?): Builder =
      delegate(CodeBlock.of(format, *args))

    public fun delegate(codeBlock: CodeBlock): Builder = apply {
      this.initializer = codeBlock
      this.delegated = true
    }

    public fun getter(getter: FunSpec?): Builder = apply {
      require(getter == null || getter.name == GETTER) { "${getter!!.name} is not a getter" }
      this.getter = getter
    }

    public fun setter(setter: FunSpec?): Builder = apply {
      require(setter == null || setter.name == SETTER) { "${setter!!.name} is not a setter" }
      this.setter = setter
    }

    public fun receiver(receiverType: TypeName?): Builder = apply {
      this.receiverType = receiverType
    }

    @DelicateKotlinPoetApi(
      message =
        "Java reflection APIs don't give complete information on Kotlin types. Consider " +
          "using the kotlinpoet-metadata APIs instead."
    )
    public fun receiver(receiverType: Type): Builder = receiver(receiverType.asTypeName())

    public fun receiver(receiverType: KClass<*>): Builder = receiver(receiverType.asTypeName())

    // region Overrides for binary compatibility
    @Suppress("RedundantOverride")
    override fun addAnnotation(annotationSpec: AnnotationSpec): Builder =
      super.addAnnotation(annotationSpec)

    @Suppress("RedundantOverride")
    override fun addAnnotations(annotationSpecs: Iterable<AnnotationSpec>): Builder =
      super.addAnnotations(annotationSpecs)

    @Suppress("RedundantOverride")
    override fun addAnnotation(annotation: ClassName): Builder = super.addAnnotation(annotation)

    @DelicateKotlinPoetApi(
      message =
        "Java reflection APIs don't give complete information on Kotlin types. Consider " +
          "using the kotlinpoet-metadata APIs instead."
    )
    override fun addAnnotation(annotation: Class<*>): Builder = super.addAnnotation(annotation)

    @Suppress("RedundantOverride")
    override fun addAnnotation(annotation: KClass<*>): Builder = super.addAnnotation(annotation)

    @Suppress("RedundantOverride")
    override fun addKdoc(format: String, vararg args: Any): Builder = super.addKdoc(format, *args)

    @Suppress("RedundantOverride")
    override fun addKdoc(block: CodeBlock): Builder = super.addKdoc(block)

    // endregion

    public fun build(): PropertySpec {
      if (KModifier.INLINE in modifiers) {
        throw IllegalArgumentException(
          "KotlinPoet doesn't allow setting the inline modifier on " +
            "properties. You should mark either the getter, the setter, or both inline."
        )
      }
      for (it in modifiers) {
        if (!isPrimaryConstructorParameter) it.checkTarget(PROPERTY)
      }
      check(contextReceiverTypes.isEmpty() || contextParameters.isEmpty()) {
        "Using both context receivers and context parameters is not allowed"
      }
      return PropertySpec(this)
    }
  }

  public companion object {
    @JvmStatic
    public fun builder(name: String, type: TypeName, vararg modifiers: KModifier): Builder {
      return Builder(name, type).addModifiers(*modifiers)
    }

    @JvmStatic
    public fun builder(name: String, type: Type, vararg modifiers: KModifier): Builder =
      builder(name, type.asTypeName(), *modifiers)

    @JvmStatic
    public fun builder(name: String, type: KClass<*>, vararg modifiers: KModifier): Builder =
      builder(name, type.asTypeName(), *modifiers)

    @JvmStatic
    public fun builder(name: String, type: TypeName, modifiers: Iterable<KModifier>): Builder {
      return Builder(name, type).addModifiers(modifiers)
    }

    @DelicateKotlinPoetApi(
      message =
        "Java reflection APIs don't give complete information on Kotlin types. Consider " +
          "using the kotlinpoet-metadata APIs instead."
    )
    @JvmStatic
    public fun builder(name: String, type: Type, modifiers: Iterable<KModifier>): Builder =
      builder(name, type.asTypeName(), modifiers)

    @JvmStatic
    public fun builder(name: String, type: KClass<*>, modifiers: Iterable<KModifier>): Builder =
      builder(name, type.asTypeName(), modifiers)
  }
}
