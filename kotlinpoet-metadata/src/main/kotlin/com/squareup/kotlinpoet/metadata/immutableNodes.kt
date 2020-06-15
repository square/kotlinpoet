/*
 * Copyright (C) 2019 Square, Inc.
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

@file:Suppress("unused")
@file:JvmName("ImmutableKmTypes")

package com.squareup.kotlinpoet.metadata

import java.util.Collections
import kotlinx.metadata.ClassName
import kotlinx.metadata.Flag
import kotlinx.metadata.Flags
import kotlinx.metadata.KmAnnotation
import kotlinx.metadata.KmClass
import kotlinx.metadata.KmClassifier
import kotlinx.metadata.KmConstantValue
import kotlinx.metadata.KmConstructor
import kotlinx.metadata.KmContract
import kotlinx.metadata.KmDeclarationContainer
import kotlinx.metadata.KmEffect
import kotlinx.metadata.KmEffectExpression
import kotlinx.metadata.KmEffectInvocationKind
import kotlinx.metadata.KmEffectType
import kotlinx.metadata.KmFlexibleTypeUpperBound
import kotlinx.metadata.KmFunction
import kotlinx.metadata.KmLambda
import kotlinx.metadata.KmPackage
import kotlinx.metadata.KmProperty
import kotlinx.metadata.KmType
import kotlinx.metadata.KmTypeAlias
import kotlinx.metadata.KmTypeParameter
import kotlinx.metadata.KmTypeProjection
import kotlinx.metadata.KmValueParameter
import kotlinx.metadata.KmVariance
import kotlinx.metadata.KmVersion
import kotlinx.metadata.KmVersionRequirement
import kotlinx.metadata.KmVersionRequirementLevel
import kotlinx.metadata.KmVersionRequirementVersionKind
import kotlinx.metadata.KmVersionRequirementVisitor
import kotlinx.metadata.jvm.JvmFieldSignature
import kotlinx.metadata.jvm.JvmFlag
import kotlinx.metadata.jvm.JvmMethodSignature
import kotlinx.metadata.jvm.KotlinClassMetadata
import kotlinx.metadata.jvm.annotations
import kotlinx.metadata.jvm.anonymousObjectOriginName
import kotlinx.metadata.jvm.fieldSignature
import kotlinx.metadata.jvm.getterSignature
import kotlinx.metadata.jvm.isRaw
import kotlinx.metadata.jvm.jvmFlags
import kotlinx.metadata.jvm.lambdaClassOriginName
import kotlinx.metadata.jvm.localDelegatedProperties
import kotlinx.metadata.jvm.moduleName
import kotlinx.metadata.jvm.setterSignature
import kotlinx.metadata.jvm.signature
import kotlinx.metadata.jvm.syntheticMethodForAnnotations

/**
 * Represents an immutable kotlinx-metadata type with a common [Flags] property.
 */
@KotlinPoetMetadataPreview
public interface ImmutableKmWithFlags {
  public val flags: Flags
}

/**
 * Immutable representation of [KmDeclarationContainer].
 *
 * Represents a Kotlin declaration container, such as a class or a package fragment.
 */
@KotlinPoetMetadataPreview
public interface ImmutableKmDeclarationContainer {
  /** Functions in the container. */
  public val functions: List<ImmutableKmFunction>

  /** Properties in the container. */
  public val properties: List<ImmutableKmProperty>

  /** Type aliases in the container. */
  public val typeAliases: List<ImmutableKmTypeAlias>
}

/**
 * Visits metadata of this class with a new [KmClass] instance and returns an [ImmutableKmClass]
 * instance of its values.
 */
@KotlinPoetMetadataPreview
public fun KotlinClassMetadata.Class.toImmutableKmClass(): ImmutableKmClass =
    toKmClass().toImmutable()

/**
 * Visits metadata of this class with a new [KmPackage] instance and returns an [ImmutableKmPackage]
 * instance of its values.
 */
@KotlinPoetMetadataPreview
public fun KotlinClassMetadata.FileFacade.toImmutableKmPackage(): ImmutableKmPackage =
    toKmPackage().toImmutable()

/** @return an immutable representation of this [KmClass]. */
@KotlinPoetMetadataPreview
@JvmName("immutableOf")
public fun KmClass.toImmutable(): ImmutableKmClass {
  return ImmutableKmClass(
      flags,
      name,
      typeParameters.map { it.toImmutable() },
      supertypes.map { it.toImmutable() },
      functions.map { it.toImmutable() },
      properties.map { it.toImmutable() },
      typeAliases.map { it.toImmutable() },
      constructors.map { it.toImmutable() },
      companionObject,
      nestedClasses.toImmutableList(),
      enumEntries.toImmutableList(),
      sealedSubclasses.toImmutableList(),
      versionRequirements.map { it.toImmutable() },
      localDelegatedProperties.map { it.toImmutable() },
      moduleName,
      anonymousObjectOriginName
  )
}

/**
 * Immutable representation of [KmClass].
 *
 * Represents a Kotlin class.
 *
 * @property flags Class flags, consisting of [Flag.HAS_ANNOTATIONS], visibility flag, modality flag
 * and [Flag.Class] flags.
 * @property name Name of the class.
 * @property typeParameters Type parameters of the class.
 * @property supertypes Supertypes of the class. The first element is the superclass (or [Any]).
 * @property functions Functions in the class.
 * @property properties Properties in the class.
 * @property typeAliases Type aliases in the class.
 * @property constructors Constructors of the class.
 * @property companionObject Name of the companion object of this class, if it has one.
 * @property nestedClasses Names of nested classes of this class.
 * @property enumEntries Names of enum entries, if this class is an enum class.
 * @property sealedSubclasses Names of direct subclasses of this class, if this class is `sealed`.
 * @property versionRequirements Version requirements on this class.
 * @property moduleName Name of the module where this class is declared.
 */
@KotlinPoetMetadataPreview
public data class ImmutableKmClass internal constructor(
  override val flags: Flags,
  val name: ClassName,
  val typeParameters: List<ImmutableKmTypeParameter>,
  val supertypes: List<ImmutableKmType>,
  override val functions: List<ImmutableKmFunction>,
  override val properties: List<ImmutableKmProperty>,
  override val typeAliases: List<ImmutableKmTypeAlias>,
  val constructors: List<ImmutableKmConstructor>,
  val companionObject: String?,
  val nestedClasses: List<String>,
  val enumEntries: List<String>,
  val sealedSubclasses: List<ClassName>,
  val versionRequirements: List<ImmutableKmVersionRequirement>,
  /**
   * Metadata of local delegated properties used somewhere inside this class (but not in a nested
   * class).
   * Note that for classes produced by the Kotlin compiler, such properties will have default
   * accessors.
   *
   * The order of local delegated properties in this list is important. The Kotlin compiler
   * generates the corresponding property's index at the call site, so that reflection would be able
   * to load the metadata of the property with that index at runtime. If an incorrect index is used,
   * either the `KProperty<*>` object passed to delegate methods will point to the wrong property
   * at runtime, or an exception will be thrown.
   */
  val localDelegatedProperties: List<ImmutableKmProperty>,
  val moduleName: String?,
  /**
   * JVM internal name of the original class this anonymous object is copied from. This value is set
   * for anonymous objects copied from bodies of inline functions to the use site by the Kotlin
   * compiler.
   */
  val anonymousObjectOriginName: String?
) : ImmutableKmDeclarationContainer, ImmutableKmWithFlags {
  public fun toMutable(): KmClass {
    return KmClass().apply {
      flags = this@ImmutableKmClass.flags
      name = this@ImmutableKmClass.name
      typeParameters += this@ImmutableKmClass.typeParameters.map { it.toMutable() }
      supertypes += this@ImmutableKmClass.supertypes.map { it.toMutable() }
      functions += this@ImmutableKmClass.functions.map { it.toMutable() }
      properties += this@ImmutableKmClass.properties.map { it.toMutable() }
      typeAliases += this@ImmutableKmClass.typeAliases.map { it.toMutable() }
      constructors += this@ImmutableKmClass.constructors.map { it.toMutable() }
      companionObject = this@ImmutableKmClass.companionObject
      nestedClasses += this@ImmutableKmClass.nestedClasses
      enumEntries += this@ImmutableKmClass.enumEntries
      sealedSubclasses += this@ImmutableKmClass.sealedSubclasses
      versionRequirements += this@ImmutableKmClass.versionRequirements.map { it.toMutable() }
    }
  }
}

/** @return an immutable representation of this [KmPackage]. */
@KotlinPoetMetadataPreview
@JvmName("immutableOf")
public fun KmPackage.toImmutable(): ImmutableKmPackage {
  return ImmutableKmPackage(
      functions.map { it.toImmutable() },
      properties.map { it.toImmutable() },
      typeAliases.map { it.toImmutable() },
      localDelegatedProperties.map { it.toImmutable() },
      moduleName
  )
}

/**
 * Immutable representation of [KmPackage].
 *
 * Represents a Kotlin package fragment, including single file facades and multi-file class parts.
 *
 * @property functions Functions in the package fragment.
 * @property properties Properties in the package fragment.
 * @property typeAliases TypeAliases in the package fragment.
 * @property moduleName Name of the module where this class is declared.
 */
@KotlinPoetMetadataPreview
public data class ImmutableKmPackage internal constructor(
  override val functions: List<ImmutableKmFunction>,
  override val properties: List<ImmutableKmProperty>,
  override val typeAliases: List<ImmutableKmTypeAlias>,
  /**
   * Metadata of local delegated properties used somewhere inside this package fragment (but not in
   * any class).
   * Note that for classes produced by the Kotlin compiler, such properties will have default
   * accessors.
   *
   * The order of local delegated properties in this list is important. The Kotlin compiler
   * generates the corresponding property's index at the call site, so that reflection would be able
   * to load the metadata of the property with that index at runtime. If an incorrect index is used,
   * either the `KProperty<*>` object passed to delegate methods will point to the wrong property
   * at runtime, or an exception will be thrown.
   */
  val localDelegatedProperties: List<ImmutableKmProperty>,
  val moduleName: String?
) : ImmutableKmDeclarationContainer {
  public fun toMutable(): KmPackage {
    return KmPackage().apply {
      functions += this@ImmutableKmPackage.functions.map { it.toMutable() }
      properties += this@ImmutableKmPackage.properties.map { it.toMutable() }
      typeAliases += this@ImmutableKmPackage.typeAliases.map { it.toMutable() }
    }
  }
}

/** @return an immutable representation of this [KmLambda]. */
@KotlinPoetMetadataPreview
@JvmName("immutableOf")
public fun KmLambda.toImmutable(): ImmutableKmLambda {
  return ImmutableKmLambda(function.toImmutable())
}

/**
 * Immutable representation of [KmLambda].
 *
 * Represents a synthetic class generated for a Kotlin lambda.
 *
 * @property function Signature of the synthetic anonymous function, representing the lambda.
 */
@KotlinPoetMetadataPreview
public data class ImmutableKmLambda internal constructor(val function: ImmutableKmFunction) {
  public fun toMutable(): KmLambda {
    return KmLambda().apply {
      function = this@ImmutableKmLambda.function.toMutable()
    }
  }
}

/** @return an immutable representation of this [KmConstructor]. */
@KotlinPoetMetadataPreview
@JvmName("immutableOf")
public fun KmConstructor.toImmutable(): ImmutableKmConstructor {
  return ImmutableKmConstructor(
      flags = flags,
      valueParameters = valueParameters.map { it.toImmutable() },
      versionRequirements = versionRequirements.map { it.toImmutable() },
      signature = signature
  )
}

/**
 * Immutable representation of [KmConstructor].
 *
 * Represents a constructor of a Kotlin class.
 *
 * @property flags Constructor flags, consisting of [Flag.HAS_ANNOTATIONS], visibility flag and
 * [Flag.Constructor] flags.
 * @property valueParameters Value parameters of the constructor.
 * @property versionRequirements Version requirements on the constructor.
 */
@KotlinPoetMetadataPreview
public data class ImmutableKmConstructor internal constructor(
  override val flags: Flags,
  val valueParameters: List<ImmutableKmValueParameter>,
  val versionRequirements: List<ImmutableKmVersionRequirement>,
  /**
   * JVM signature of the constructor, or null if the JVM signature of this constructor is unknown.
   *
   * Example: `JvmMethodSignature("<init>", "(Ljava/lang/Object;)V")`.
   */
  val signature: JvmMethodSignature?
) : ImmutableKmWithFlags {
  public fun toMutable(): KmConstructor {
    return KmConstructor(flags).apply {
      valueParameters += this@ImmutableKmConstructor.valueParameters.map { it.toMutable() }
      versionRequirements += this@ImmutableKmConstructor.versionRequirements.map { it.toMutable() }
    }
  }
}

/** @return an immutable representation of this [KmFunction]. */
@KotlinPoetMetadataPreview
@JvmName("immutableOf")
public fun KmFunction.toImmutable(): ImmutableKmFunction {
  return ImmutableKmFunction(
      flags,
      name,
      typeParameters.map { it.toImmutable() },
      receiverParameterType?.toImmutable(),
      valueParameters.map { it.toImmutable() },
      returnType.toImmutable(),
      versionRequirements.map { it.toImmutable() },
      contract?.toImmutable(),
      signature,
      lambdaClassOriginName
  )
}

/**
 * Immutable representation of [KmFunction].
 *
 * Represents a Kotlin function declaration.
 *
 * @property flags Function flags, consisting of [Flag.HAS_ANNOTATIONS], visibility flag, modality
 * flag and [Flag.Function] flags.
 * @property name The name of the function
 * @property typeParameters Type parameters of the function.
 * @property receiverParameterType Type of the receiver of the function, if this is an extension
 * function.
 * @property valueParameters Value parameters of the function.
 * @property returnType Return type of the function.
 * @property versionRequirements Version requirements on the function.
 * @property contract Contract of the function.
 */
@KotlinPoetMetadataPreview
public data class ImmutableKmFunction internal constructor(
  override val flags: Flags,
  val name: String,
  val typeParameters: List<ImmutableKmTypeParameter>,
  val receiverParameterType: ImmutableKmType?,
  val valueParameters: List<ImmutableKmValueParameter>,
  val returnType: ImmutableKmType,
  val versionRequirements: List<ImmutableKmVersionRequirement>,
  val contract: ImmutableKmContract?,
  /**
   * JVM signature of the function, or null if the JVM signature of this function is unknown.
   *
   * Example: `JvmMethodSignature("equals", "(Ljava/lang/Object;)Z")`.
   */
  val signature: JvmMethodSignature?,
  /**
   * JVM internal name of the original class the lambda class for this function is copied from. This
   * value is set for lambdas copied from bodies of inline functions to the use site by the Kotlin
   * compiler.
   */
  val lambdaClassOriginName: String?
) : ImmutableKmWithFlags {
  public fun toMutable(): KmFunction {
    return KmFunction(flags, name).apply {
      typeParameters += this@ImmutableKmFunction.typeParameters.map { it.toMutable() }
      receiverParameterType = this@ImmutableKmFunction.receiverParameterType?.toMutable()
      valueParameters += this@ImmutableKmFunction.valueParameters.map { it.toMutable() }
      returnType = this@ImmutableKmFunction.returnType.toMutable()
      versionRequirements += this@ImmutableKmFunction.versionRequirements.map { it.toMutable() }
      contract = this@ImmutableKmFunction.contract?.toMutable()
    }
  }
}

/** @return an immutable representation of this [KmProperty]. */
@KotlinPoetMetadataPreview
@JvmName("immutableOf")
public fun KmProperty.toImmutable(): ImmutableKmProperty {
  return ImmutableKmProperty(
      flags,
      name,
      getterFlags,
      setterFlags,
      typeParameters.map { it.toImmutable() },
      receiverParameterType?.toImmutable(),
      setterParameter?.toImmutable(),
      returnType.toImmutable(),
      versionRequirements.map { it.toImmutable() },
      jvmFlags,
      fieldSignature,
      getterSignature,
      setterSignature,
      syntheticMethodForAnnotations
  )
}

/**
 * Immutable representation of [KmProperty].
 *
 * Represents a Kotlin property declaration.
 *
 * @property flags Property flags, consisting of [Flag.HAS_ANNOTATIONS], visibility flag, modality
 * flag and [Flag.Property] flags.
 * @property name The name of the property.
 * @property getterFlags Property accessor flags, consisting of [Flag.HAS_ANNOTATIONS], visibility
 * flag, modality flag and [Flag.PropertyAccessor] flags.
 * @property setterFlags Property accessor flags, consisting of [Flag.HAS_ANNOTATIONS], visibility
 * flag, modality flag and [Flag.PropertyAccessor] flags.
 * @property typeParameters Type parameters of the property.
 * @property receiverParameterType Type of the receiver of the property, if this is an extension
 * property.
 * @property setterParameter Value parameter of the setter of this property, if this is a `var`
 * property.
 * @property returnType Type of the property.
 * @property versionRequirements Version requirements on the property.
 * @property jvmFlags JVM-specific flags of the property, consisting of [JvmFlag.Property] flags.
 */
@KotlinPoetMetadataPreview
public data class ImmutableKmProperty internal constructor(
  override val flags: Flags,
  val name: String,
  val getterFlags: Flags,
  val setterFlags: Flags,
  val typeParameters: List<ImmutableKmTypeParameter>,
  val receiverParameterType: ImmutableKmType?,
  val setterParameter: ImmutableKmValueParameter?,
  val returnType: ImmutableKmType,
  val versionRequirements: List<ImmutableKmVersionRequirement>,
  val jvmFlags: Flags,
  /**
   * JVM signature of the backing field of the property, or `null` if this property has no backing
   * field.
   *
   * Example: `JvmFieldSignature("X", "Ljava/lang/Object;")`.
   */
  val fieldSignature: JvmFieldSignature?,
  /**
   * JVM signature of the property getter, or `null` if this property has no getter or its signature
   * is unknown.
   *
   * Example: `JvmMethodSignature("getX", "()Ljava/lang/Object;")`.
   */
  val getterSignature: JvmMethodSignature?,
  /**
   * JVM signature of the property setter, or `null` if this property has no setter or its signature
   * is unknown.
   *
   * Example: `JvmMethodSignature("setX", "(Ljava/lang/Object;)V")`.
   */
  val setterSignature: JvmMethodSignature?,
  /**
   * JVM signature of a synthetic method which is generated to store annotations on a property in
   * the bytecode.
   *
   * Example: `JvmMethodSignature("getX$annotations", "()V")`.
   */
  val syntheticMethodForAnnotations: JvmMethodSignature?
) : ImmutableKmWithFlags {
  public fun toMutable(): KmProperty {
    return KmProperty(flags, name, getterFlags, setterFlags).apply {
      typeParameters += this@ImmutableKmProperty.typeParameters.map { it.toMutable() }
      receiverParameterType = this@ImmutableKmProperty.receiverParameterType?.toMutable()
      setterParameter = this@ImmutableKmProperty.setterParameter?.toMutable()
      returnType = this@ImmutableKmProperty.returnType.toMutable()
      versionRequirements += this@ImmutableKmProperty.versionRequirements.map { it.toMutable() }
    }
  }
}

/** @return an immutable representation of this [KmTypeAlias]. */
@KotlinPoetMetadataPreview
@JvmName("immutableOf")
public fun KmTypeAlias.toImmutable(): ImmutableKmTypeAlias {
  return ImmutableKmTypeAlias(
      flags,
      name,
      typeParameters.map { it.toImmutable() },
      underlyingType.toImmutable(),
      expandedType.toImmutable(),
      annotations.toImmutableList(),
      versionRequirements.map { it.toImmutable() }
  )
}

/**
 * Immutable representation of [KmTypeAlias].
 *
 * Represents a Kotlin type alias declaration.
 *
 * @property flags TypeAlias flags, consisting of [Flag.HAS_ANNOTATIONS] and visibility flag.
 * @property name The name of the type alias.
 * @property typeParameters Type parameters of the type alias.
 * @property underlyingType Underlying type of the type alias, i.e. the type in the right-hand side
 * of the type alias declaration.
 * @property expandedType Expanded type of the type alias, i.e. the full expansion of the underlying
 *                        type, where all type aliases are substituted with their expanded types. If
 *                        no type aliases are used in the underlying type, expanded type is equal to
 *                        the underlying type.
 * @property annotations Annotations on the type alias.
 * @property versionRequirements Version requirements on the type alias.
 */
@KotlinPoetMetadataPreview
public data class ImmutableKmTypeAlias internal constructor(
  override val flags: Flags,
  val name: String,
  val typeParameters: List<ImmutableKmTypeParameter>,
  val underlyingType: ImmutableKmType,
  val expandedType: ImmutableKmType,
  val annotations: List<KmAnnotation>,
  val versionRequirements: List<ImmutableKmVersionRequirement>
) : ImmutableKmWithFlags {
  public fun toMutable(): KmTypeAlias {
    return KmTypeAlias(flags, name).apply {
      typeParameters += this@ImmutableKmTypeAlias.typeParameters.map { it.toMutable() }
      underlyingType = this@ImmutableKmTypeAlias.underlyingType.toMutable()
      expandedType = this@ImmutableKmTypeAlias.expandedType.toMutable()
      annotations += this@ImmutableKmTypeAlias.annotations
      versionRequirements += this@ImmutableKmTypeAlias.versionRequirements.map { it.toMutable() }
    }
  }
}

/** @return an immutable representation of this [KmValueParameter]. */
@KotlinPoetMetadataPreview
@JvmName("immutableOf")
public fun KmValueParameter.toImmutable(): ImmutableKmValueParameter {
  return ImmutableKmValueParameter(
      flags,
      name,
      type?.toImmutable(),
      varargElementType?.toImmutable()
  )
}

/**
 * Immutable representation of [KmValueParameter].
 *
 * Represents a value parameter of a Kotlin constructor, function or property setter.
 *
 * @property flags Value parameter flags, consisting of [Flag.ValueParameter] flags.
 * @property name The name of the value parameter.
 * @property type Type of the value parameter, if this is **not** a `vararg` parameter.
 * @property varargElementType Type of the value parameter, if this is a `vararg` parameter.
 */
@KotlinPoetMetadataPreview
public data class ImmutableKmValueParameter internal constructor(
  override val flags: Flags,
  val name: String,
  val type: ImmutableKmType?,
  val varargElementType: ImmutableKmType?
) : ImmutableKmWithFlags {
  public fun toMutable(): KmValueParameter {
    return KmValueParameter(flags, name).apply {
      type = this@ImmutableKmValueParameter.type?.toMutable()
      varargElementType = this@ImmutableKmValueParameter.varargElementType?.toMutable()
    }
  }
}

/** @return an immutable representation of this [KmTypeParameter]. */
@KotlinPoetMetadataPreview
@JvmName("immutableOf")
public fun KmTypeParameter.toImmutable(): ImmutableKmTypeParameter {
  return ImmutableKmTypeParameter(
      flags,
      name,
      id,
      variance,
      upperBounds.map { it.toImmutable() },
      annotations.toImmutableList()
  )
}

/**
 * Immutable representation of [KmTypeParameter].
 *
 * Represents a type parameter of a Kotlin class, function, property or type alias.
 *
 * @property flags Type parameter flags, consisting of [Flag.TypeParameter] flags.
 * @property name The name of the type parameter.
 * @property id The id of the type parameter, useful to be able to uniquely identify the type
 * parameter in different contexts where the name isn't enough
 * (e.g. `class A<T> { fun <T> foo(t: T) }`).
 * @property variance The declaration-site variance of the type parameter.
 * @property upperBounds Upper bounds of the type parameter.
 * @property annotations Annotations on the type parameter.
 */
@KotlinPoetMetadataPreview
public data class ImmutableKmTypeParameter internal constructor(
  override val flags: Flags,
  val name: String,
  val id: Int,
  val variance: KmVariance,
  val upperBounds: List<ImmutableKmType>,
  val annotations: List<KmAnnotation>
) : ImmutableKmWithFlags {
  public fun toMutable(): KmTypeParameter {
    return KmTypeParameter(flags, name, id, variance).apply {
      upperBounds += this@ImmutableKmTypeParameter.upperBounds.map { it.toMutable() }
    }
  }
}

/** @return an immutable representation of this [KmType]. */
@KotlinPoetMetadataPreview
@JvmName("immutableOf")
public fun KmType.toImmutable(): ImmutableKmType {
  return ImmutableKmType(
      flags,
      classifier,
      arguments.map { it.toImmutable() },
      abbreviatedType?.toImmutable(),
      outerType?.toImmutable(),
      flexibleTypeUpperBound?.toImmutable(),
      isRaw,
      annotations.toImmutableList()
  )
}

/**
 * Immutable representation of [KmType].
 *
 * Represents a type.
 *
 * @property flags Type flags, consisting of [Flag.Type] flags.
 * @property classifier Classifier of the type.
 * @property arguments Arguments of the type, if the type's classifier is a class or a type alias.
 * @property isRaw `true` if the type is seen as a raw type in Java.
 * @property annotations Annotations on the type.
 */
@KotlinPoetMetadataPreview
public data class ImmutableKmType internal constructor(
  override val flags: Flags,
  val classifier: KmClassifier,
  val arguments: List<ImmutableKmTypeProjection>,
  /**
   * Abbreviation of this type. Note that all types are expanded for metadata produced by the Kotlin
   * compiler. For example:
   *
   *     typealias A<T> = MutableList<T>
   *
   *     fun foo(a: A<Any>) {}
   *
   * The type of the `foo`'s parameter in the metadata is actually `MutableList<Any>`, and its
   * abbreviation is `A<Any>`.
   */
  val abbreviatedType: ImmutableKmType?,
  /**
   * Outer type of this type, if this type's classifier is an inner class. For example:
   *
   *     class A<T> { inner class B<U> }
   *
   *     fun foo(a: A<*>.B<Byte?>) {}
   *
   * The type of the `foo`'s parameter in the metadata is `B<Byte>` (a type whose classifier is
   * class `B`, and it has one type argument, type `Byte?`), and its outer type is `A<*>` (a type
   * whose classifier is class `A`, and it has one type argument, star projection).
   */
  val outerType: ImmutableKmType?,
  /**
   * Upper bound of this type, if this type is flexible. In that case, all other data refers to the
   * lower bound of the type.
   *
   * Flexible types in Kotlin include platform types in Kotlin/JVM and `dynamic` type in Kotlin/JS.
   */
  val flexibleTypeUpperBound: ImmutableKmFlexibleTypeUpperBound?,
  val isRaw: Boolean,
  val annotations: List<KmAnnotation>
) : ImmutableKmWithFlags {
  /**
   * `true` if this is an extension type (i.e. String.() -> Unit vs (String) -> Unit).
   *
   * See details: https://discuss.kotlinlang.org/t/announcing-kotlinx-metadata-jvm-library-for-reading-modifying-metadata-of-kotlin-jvm-class-files/7980/27
   */
  val isExtensionType: Boolean by lazy {
    annotations.any { it.className == "kotlin/ExtensionFunctionType" }
  }

  public fun toMutable(): KmType {
    return KmType(flags).apply {
      classifier = this@ImmutableKmType.classifier
      arguments += this@ImmutableKmType.arguments.map { it.toMutable() }
      abbreviatedType = this@ImmutableKmType.abbreviatedType?.toMutable()
      outerType = this@ImmutableKmType.outerType?.toMutable()
      flexibleTypeUpperBound = this@ImmutableKmType.flexibleTypeUpperBound?.toMutable()
    }
  }
}

/** @return an immutable representation of this [KmVersionRequirement]. */
@KotlinPoetMetadataPreview
@JvmName("immutableOf")
public fun KmVersionRequirement.toImmutable(): ImmutableKmVersionRequirement {
  return ImmutableKmVersionRequirement(
      kind,
      level,
      errorCode,
      message,
      version
  )
}

/**
 * Immutable representation of [KmVersionRequirement].
 *
 * Represents a version requirement on a Kotlin declaration.
 *
 * Version requirement is an internal feature of the Kotlin compiler and the standard Kotlin
 * library, enabled for example with the internal [kotlin.internal.RequireKotlin] annotation.
 *
 * @property kind Kind of the version that this declaration requires.
 * @property level Level of the diagnostic that must be reported on the usages of the declaration
 * in case the version requirement is not satisfied.
 * @property errorCode Optional error code to be displayed in the diagnostic.
 * @property message Optional message to be displayed in the diagnostic.
 * @property version Version required by this requirement.
 */
@KotlinPoetMetadataPreview
public data class ImmutableKmVersionRequirement internal constructor(
  val kind: KmVersionRequirementVersionKind,
  val level: KmVersionRequirementLevel,
  val errorCode: Int?,
  val message: String?,
  val version: KmVersion
) : KmVersionRequirementVisitor() {
  public fun toMutable(): KmVersionRequirement {
    return KmVersionRequirement().apply {
      kind = this@ImmutableKmVersionRequirement.kind
      level = this@ImmutableKmVersionRequirement.level
      errorCode = this@ImmutableKmVersionRequirement.errorCode
      message = this@ImmutableKmVersionRequirement.message
      version = this@ImmutableKmVersionRequirement.version
    }
  }
}

/** @return an immutable representation of this [KmContract]. */
@KotlinPoetMetadataPreview
@JvmName("immutableOf")
public fun KmContract.toImmutable(): ImmutableKmContract {
  return ImmutableKmContract(effects.map { it.toImmutable() })
}

/**
 * Immutable representation of [KmContract].
 *
 * Represents a contract of a Kotlin function.
 *
 * Contracts are an internal feature of the standard Kotlin library, and their behavior and/or
 * binary format may change in a subsequent release.
 *
 * @property effects Effects of this contract.
 */
@KotlinPoetMetadataPreview
public data class ImmutableKmContract internal constructor(val effects: List<ImmutableKmEffect>) {
  public fun toMutable(): KmContract {
    return KmContract().apply {
      effects += this@ImmutableKmContract.effects.map { it.toMutable() }
    }
  }
}

/** @return an immutable representation of this [KmEffect]. */
@KotlinPoetMetadataPreview
@JvmName("immutableOf")
public fun KmEffect.toImmutable(): ImmutableKmEffect {
  return ImmutableKmEffect(
      type,
      invocationKind,
      constructorArguments.map { it.toImmutable() },
      conclusion?.toImmutable()
  )
}

/**
 * Immutable representation of [KmEffect].
 *
 * Represents an effect (a part of the contract of a Kotlin function).
 *
 * Contracts are an internal feature of the standard Kotlin library, and their behavior and/or
 * binary format may change in a subsequent release.
 *
 * @property type Type of the effect
 * @property invocationKind Optional number of invocations of the lambda parameter of this function,
 *   specified further in the effect expression.
 * @property constructorArguments Arguments of the effect constructor, i.e. the constant value for
 *                                the [KmEffectType.RETURNS_CONSTANT] effect, or the parameter
 *                                reference for the [KmEffectType.CALLS] effect.
 * @property conclusion Conclusion of the effect. If this value is set, the effect represents an
 * implication with this value as the right-hand side.
 */
@KotlinPoetMetadataPreview
public data class ImmutableKmEffect internal constructor(
  val type: KmEffectType,
  val invocationKind: KmEffectInvocationKind?,
  val constructorArguments: List<ImmutableKmEffectExpression>,
  val conclusion: ImmutableKmEffectExpression?
) {
  public fun toMutable(): KmEffect {
    return KmEffect(type, invocationKind).apply {
      constructorArguments += this@ImmutableKmEffect.constructorArguments.map { it.toMutable() }
      conclusion = this@ImmutableKmEffect.conclusion?.toMutable()
    }
  }
}

/** @return an immutable representation of this [KmEffectExpression]. */
@KotlinPoetMetadataPreview
@JvmName("immutableOf")
public fun KmEffectExpression.toImmutable(): ImmutableKmEffectExpression {
  return ImmutableKmEffectExpression(
      flags,
      parameterIndex,
      constantValue,
      isInstanceType?.toImmutable(),
      andArguments.map { it.toImmutable() },
      orArguments.map { it.toImmutable() }
  )
}

/**
 * Immutable representation of [KmEffectExpression].
 *
 * Represents an effect expression, the contents of an effect (a part of the contract of a Kotlin
 * function).
 *
 * Contracts are an internal feature of the standard Kotlin library, and their behavior and/or
 * binary format may change in a subsequent release.
 *
 * @property flags Effect expression flags, consisting of [Flag.EffectExpression] flags.
 * @property parameterIndex Optional 1-based index of the value parameter of the function, for
 *                          effects which assert something about the function parameters. The index
 *                          0 means the extension receiver parameter.
 * @property constantValue Constant value used in the effect expression.
 * @property isInstanceType Type used as the target of an `is`-expression in the effect expression.
 * @property andArguments Arguments of an `&&`-expression. If this list is non-empty, the resulting
 *                        effect expression is a conjunction of this expression and elements of the
 *                        list.
 * @property orArguments Arguments of an `||`-expression. If this list is non-empty, the resulting
 *                       effect expression is a disjunction of this expression and elements of the
 *                       list.
 */
@KotlinPoetMetadataPreview
public data class ImmutableKmEffectExpression internal constructor(
  override val flags: Flags,
  val parameterIndex: Int?,
  val constantValue: KmConstantValue?,
  val isInstanceType: ImmutableKmType?,
  val andArguments: List<ImmutableKmEffectExpression>,
  val orArguments: List<ImmutableKmEffectExpression>
) : ImmutableKmWithFlags {
  public fun toMutable(): KmEffectExpression {
    return KmEffectExpression().apply {
      flags = this@ImmutableKmEffectExpression.flags
      parameterIndex = this@ImmutableKmEffectExpression.parameterIndex
      constantValue = this@ImmutableKmEffectExpression.constantValue
      isInstanceType = this@ImmutableKmEffectExpression.isInstanceType?.toMutable()
      andArguments += this@ImmutableKmEffectExpression.andArguments.map { it.toMutable() }
      orArguments += this@ImmutableKmEffectExpression.orArguments.map { it.toMutable() }
    }
  }
}

/** @return an immutable representation of this [KmTypeProjection]. */
@KotlinPoetMetadataPreview
@JvmName("immutableOf")
public fun KmTypeProjection.toImmutable(): ImmutableKmTypeProjection {
  return ImmutableKmTypeProjection(variance, type?.toImmutable())
}

/**
 * Immutable representation of [KmTypeProjection].
 *
 * Represents type projection used in a type argument of the type based on a class or on a type
 * alias. For example, in `MutableMap<in String?, *>`, `in String?` is the type projection which is
 * the first type argument of the type.
 *
 * @property variance The variance of the type projection, or `null` if this is a star projection.
 * @property type The projected type, or `null` if this is a star projection.
 */
@KotlinPoetMetadataPreview
public data class ImmutableKmTypeProjection internal constructor(
  val variance: KmVariance?,
  val type: ImmutableKmType?
) {

  public fun toMutable(): KmTypeProjection {
    return KmTypeProjection(variance, type?.toMutable())
  }

  public companion object {
    /**
     * Star projection (`*`).
     * For example, in `MutableMap<in String?, *>`, `*` is the star projection which is the second
     * type argument of the type.
     */
    @JvmField
    public val STAR: KmTypeProjection = KmTypeProjection.STAR
  }
}

/** @return an immutable representation of this [KmFlexibleTypeUpperBound]. */
@KotlinPoetMetadataPreview
@JvmName("immutableOf")
public fun KmFlexibleTypeUpperBound.toImmutable(): ImmutableKmFlexibleTypeUpperBound {
  return ImmutableKmFlexibleTypeUpperBound(type.toImmutable(), typeFlexibilityId)
}

/**
 * Immutable representation of [KmFlexibleTypeUpperBound].
 *
 * Represents an upper bound of a flexible Kotlin type.
 *
 * @property type Upper bound of the flexible type.
 * @property typeFlexibilityId Id of the kind of flexibility this type has. For example,
 * "kotlin.jvm.PlatformType" for JVM platform types, or "kotlin.DynamicType" for JS dynamic type.
 */
@KotlinPoetMetadataPreview
public data class ImmutableKmFlexibleTypeUpperBound internal constructor(
  val type: ImmutableKmType,
  val typeFlexibilityId: String?
) {
  public fun toMutable(): KmFlexibleTypeUpperBound {
    return KmFlexibleTypeUpperBound(type.toMutable(), typeFlexibilityId)
  }
}

@Suppress("NOTHING_TO_INLINE")
private inline fun <E> List<E>.toImmutableList(): List<E> = Collections.unmodifiableList(this)
