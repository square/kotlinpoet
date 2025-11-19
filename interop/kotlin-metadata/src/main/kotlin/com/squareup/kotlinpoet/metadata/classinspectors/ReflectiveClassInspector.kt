/*
 * Copyright (C) 2021 Square, Inc.
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
package com.squareup.kotlinpoet.metadata.classinspectors

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.AnnotationSpec.UseSiteTarget.FILE
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.DelicateKotlinPoetApi
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.metadata.classinspectors.ClassInspectorUtil.JAVA_DEPRECATED
import com.squareup.kotlinpoet.metadata.classinspectors.ClassInspectorUtil.filterOutNullabilityAnnotations
import com.squareup.kotlinpoet.metadata.isDeclaration
import com.squareup.kotlinpoet.metadata.readKotlinClassMetadata
import com.squareup.kotlinpoet.metadata.specs.ClassData
import com.squareup.kotlinpoet.metadata.specs.ClassInspector
import com.squareup.kotlinpoet.metadata.specs.ConstructorData
import com.squareup.kotlinpoet.metadata.specs.ContainerData
import com.squareup.kotlinpoet.metadata.specs.EnumEntryData
import com.squareup.kotlinpoet.metadata.specs.FieldData
import com.squareup.kotlinpoet.metadata.specs.FileData
import com.squareup.kotlinpoet.metadata.specs.JvmFieldModifier
import com.squareup.kotlinpoet.metadata.specs.JvmFieldModifier.TRANSIENT
import com.squareup.kotlinpoet.metadata.specs.JvmFieldModifier.VOLATILE
import com.squareup.kotlinpoet.metadata.specs.JvmMethodModifier
import com.squareup.kotlinpoet.metadata.specs.JvmMethodModifier.DEFAULT
import com.squareup.kotlinpoet.metadata.specs.JvmMethodModifier.STATIC
import com.squareup.kotlinpoet.metadata.specs.JvmMethodModifier.SYNCHRONIZED
import com.squareup.kotlinpoet.metadata.specs.KM_CONSTRUCTOR_COMPARATOR
import com.squareup.kotlinpoet.metadata.specs.KM_FUNCTION_COMPARATOR
import com.squareup.kotlinpoet.metadata.specs.KM_PROPERTY_COMPARATOR
import com.squareup.kotlinpoet.metadata.specs.MethodData
import com.squareup.kotlinpoet.metadata.specs.PropertyData
import com.squareup.kotlinpoet.metadata.toKmClass
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Parameter
import java.util.TreeMap
import java.util.concurrent.ConcurrentHashMap
import kotlin.LazyThreadSafetyMode.NONE
import kotlin.metadata.ClassKind
import kotlin.metadata.KmClass
import kotlin.metadata.KmDeclarationContainer
import kotlin.metadata.KmPackage
import kotlin.metadata.hasAnnotations
import kotlin.metadata.hasConstant
import kotlin.metadata.isConst
import kotlin.metadata.isValue
import kotlin.metadata.jvm.JvmFieldSignature
import kotlin.metadata.jvm.JvmMethodSignature
import kotlin.metadata.jvm.KotlinClassMetadata
import kotlin.metadata.jvm.fieldSignature
import kotlin.metadata.jvm.getterSignature
import kotlin.metadata.jvm.hasAnnotationsInBytecode
import kotlin.metadata.jvm.setterSignature
import kotlin.metadata.jvm.signature
import kotlin.metadata.jvm.syntheticMethodForAnnotations
import kotlin.metadata.kind

public class ReflectiveClassInspector
private constructor(private val lenient: Boolean, private val classLoader: ClassLoader?) :
  ClassInspector {

  private val classCache = ConcurrentHashMap<ClassName, Optional<Class<*>>>()
  private val methodCache = ConcurrentHashMap<Pair<Class<*>, String>, Optional<Method>>()
  private val constructorCache =
    ConcurrentHashMap<Pair<Class<*>, String>, Optional<Constructor<*>>>()
  private val fieldCache = ConcurrentHashMap<Pair<Class<*>, String>, Optional<Field>>()
  private val enumCache = ConcurrentHashMap<Pair<Class<*>, String>, Optional<Enum<*>>>()

  private fun lookupClass(className: ClassName): Class<*>? {
    return classCache
      .getOrPut(className) {
        try {
            if (classLoader == null) {
              Class.forName(className.reflectionName())
            } else {
              Class.forName(className.reflectionName(), true, classLoader)
            }
          } catch (e: ClassNotFoundException) {
            null
          }
          .toOptional()
      }
      .nullableValue
  }

  override val supportsNonRuntimeRetainedAnnotations: Boolean = false

  override fun declarationContainerFor(className: ClassName): KmDeclarationContainer {
    val clazz = lookupClass(className) ?: error("No type element found for: $className.")

    val metadata = clazz.getAnnotation(Metadata::class.java)
    return when (val kotlinClassMetadata = metadata.readKotlinClassMetadata(lenient)) {
      is KotlinClassMetadata.Class -> kotlinClassMetadata.kmClass
      is KotlinClassMetadata.FileFacade -> kotlinClassMetadata.kmPackage
      else -> TODO("Not implemented yet: ${kotlinClassMetadata.javaClass.simpleName}")
    }
  }

  override fun isInterface(className: ClassName): Boolean {
    if (className in ClassInspectorUtil.KOTLIN_INTRINSIC_INTERFACES) {
      return true
    }
    return lookupClass(className)?.isInterface ?: false
  }

  private fun Class<*>.lookupField(fieldSignature: JvmFieldSignature): Field? {
    return try {
      val signatureString = fieldSignature.toString()
      fieldCache
        .getOrPut(this to signatureString) {
          declaredFields
            .asSequence()
            .onEach { it.isAccessible = true }
            .find { signatureString == it.jvmFieldSignature }
            .toOptional()
        }
        .nullableValue
    } catch (e: ClassNotFoundException) {
      null
    }
  }

  private fun Class<*>.lookupMethod(methodSignature: JvmMethodSignature): Method? {
    val signatureString = methodSignature.toString()
    return methodCache
      .getOrPut(this to signatureString) {
        declaredMethods
          .asSequence()
          .onEach { it.isAccessible = true }
          .find { signatureString == it.jvmMethodSignature }
          .toOptional()
      }
      .nullableValue
  }

  private fun Class<*>.lookupConstructor(
    constructorSignature: JvmMethodSignature
  ): Constructor<*>? {
    val signatureString = constructorSignature.toString()
    return constructorCache
      .getOrPut(this to signatureString) {
        declaredConstructors
          .asSequence()
          .onEach { it.isAccessible = true }
          .find { signatureString == it.jvmMethodSignature }
          .toOptional()
      }
      .nullableValue
  }

  private fun Field.jvmModifiers(): Set<JvmFieldModifier> {
    return mutableSetOf<JvmFieldModifier>().apply {
      if (Modifier.isTransient(modifiers)) {
        add(TRANSIENT)
      }
      if (Modifier.isVolatile(modifiers)) {
        add(VOLATILE)
      }
      if (Modifier.isStatic(modifiers)) {
        add(JvmFieldModifier.STATIC)
      }
    }
  }

  @OptIn(DelicateKotlinPoetApi::class)
  private fun Field.annotationSpecs(): List<AnnotationSpec> {
    return filterOutNullabilityAnnotations(
      declaredAnnotations.orEmpty().map { AnnotationSpec.get(it, includeDefaultValues = true) }
    )
  }

  @OptIn(DelicateKotlinPoetApi::class)
  private fun Constructor<*>.annotationSpecs(): List<AnnotationSpec> {
    return filterOutNullabilityAnnotations(
      declaredAnnotations.orEmpty().map { AnnotationSpec.get(it, true) }
    )
  }

  private fun Method.jvmModifiers(): Set<JvmMethodModifier> {
    return methodJvmModifiers(modifiers, isDefault)
  }

  private fun Constructor<*>.jvmModifiers(): Set<JvmMethodModifier> {
    return methodJvmModifiers(modifiers, false)
  }

  private fun methodJvmModifiers(modifiers: Int, isDefault: Boolean): Set<JvmMethodModifier> {
    val jvmMethodModifiers = mutableSetOf<JvmMethodModifier>()
    if (Modifier.isSynchronized(modifiers)) {
      jvmMethodModifiers += SYNCHRONIZED
    }
    if (Modifier.isStatic(modifiers)) {
      jvmMethodModifiers += STATIC
    }
    if (isDefault) {
      jvmMethodModifiers += DEFAULT
    }
    return jvmMethodModifiers
  }

  @OptIn(DelicateKotlinPoetApi::class)
  private fun Method.annotationSpecs(): List<AnnotationSpec> {
    return filterOutNullabilityAnnotations(
      declaredAnnotations.orEmpty().map { AnnotationSpec.get(it, includeDefaultValues = true) }
    )
  }

  @OptIn(DelicateKotlinPoetApi::class)
  private fun Parameter.annotationSpecs(): List<AnnotationSpec> {
    return filterOutNullabilityAnnotations(
      declaredAnnotations.map { AnnotationSpec.get(it, includeDefaultValues = true) }
    )
  }

  private fun Method.exceptionTypeNames(): List<TypeName> {
    return exceptionTypes.orEmpty().mapTo(mutableListOf()) { it.asTypeName() }
  }

  private fun Constructor<*>.exceptionTypeNames(): List<TypeName> {
    return exceptionTypes.orEmpty().mapTo(mutableListOf()) { it.asTypeName() }
  }

  override fun enumEntry(enumClassName: ClassName, memberName: String): EnumEntryData {
    val clazz = lookupClass(enumClassName) ?: error("No class found for: $enumClassName.")
    check(clazz.isEnum) { "Class must be an enum but isn't: $clazz" }
    val enumEntry =
      enumCache
        .getOrPut(clazz to memberName) {
          clazz.enumConstants
            .asSequence()
            .map { it as Enum<*> }
            .find { it.name == memberName }
            .toOptional()
        }
        .nullableValue
    checkNotNull(enumEntry) { "Could not find $memberName on $enumClassName" }
    return EnumEntryData(
      declarationContainer =
        if (enumEntry.javaClass == clazz) {
          // For simple enums with no class bodies, the entry class will be the same as the original
          // class.
          null
        } else {
          enumEntry.javaClass.getAnnotation(Metadata::class.java)?.toKmClass(lenient)
        },
      annotations = clazz.getField(enumEntry.name).annotationSpecs(),
    )
  }

  private fun Field.constantValue(): CodeBlock? {
    if (!Modifier.isStatic(modifiers)) {
      return null
    }
    return get(null) // Constant means we can do a static get on it.
      .let(ClassInspectorUtil::codeLiteralOf)
  }

  private fun JvmMethodSignature.isOverriddenIn(clazz: Class<*>): Boolean {
    val signatureString = toString()
    val classPackage = clazz.`package`.name
    val interfaceMethods = clazz.interfaces.asSequence().flatMap { it.methods.asSequence() }
    val superClassMethods = clazz.superclass?.methods.orEmpty().asSequence()
    return interfaceMethods
      .plus(superClassMethods)
      .filterNot { Modifier.isFinal(it.modifiers) }
      .filterNot { Modifier.isStatic(it.modifiers) }
      .filterNot { Modifier.isPrivate(it.modifiers) }
      .filter {
        Modifier.isPublic(it.modifiers) ||
          Modifier.isProtected(it.modifiers) ||
          // Package private
          it.declaringClass.`package`.name == classPackage
      }
      .map { it.jvmMethodSignature }
      .any { it == signatureString }
  }

  override fun methodExists(className: ClassName, methodSignature: JvmMethodSignature): Boolean {
    return lookupClass(className)?.lookupMethod(methodSignature) != null
  }

  @OptIn(DelicateKotlinPoetApi::class)
  override fun containerData(
    declarationContainer: KmDeclarationContainer,
    className: ClassName,
    parentClassName: ClassName?,
  ): ContainerData {
    val targetClass = lookupClass(className) ?: error("No class found for: $className.")
    val isCompanionObject: Boolean =
      when (declarationContainer) {
        is KmClass -> {
          declarationContainer.kind == ClassKind.COMPANION_OBJECT
        }
        is KmPackage -> {
          false
        }
        else -> TODO("Not implemented yet: ${declarationContainer.javaClass.simpleName}")
      }

    // Should only be called if parentName has been null-checked
    val classIfCompanion by
      lazy(NONE) {
        if (isCompanionObject && parentClassName != null) {
          lookupClass(parentClassName) ?: error("No class found for: $parentClassName.")
        } else {
          targetClass
        }
      }

    val propertyData =
      declarationContainer.properties
        .asSequence()
        .filter { it.kind.isDeclaration }
        .associateWithTo(TreeMap(KM_PROPERTY_COMPARATOR)) { property ->
          val isJvmField =
            ClassInspectorUtil.computeIsJvmField(
              property = property,
              classInspector = this,
              isCompanionObject = isCompanionObject,
              hasGetter = property.getterSignature != null,
              hasSetter = property.setterSignature != null,
              hasField = property.fieldSignature != null,
            )

          val fieldData =
            property.fieldSignature?.let { fieldSignature ->
              // Check the field in the parent first. For const/static/jvmField elements, these only
              // exist in the parent and we want to check that if necessary to avoid looking up a
              // non-existent field in the companion.
              val parentModifiers =
                if (isCompanionObject && parentClassName != null) {
                  classIfCompanion.lookupField(fieldSignature)?.jvmModifiers().orEmpty()
                } else {
                  emptySet()
                }

              val isStatic = JvmFieldModifier.STATIC in parentModifiers

              // TODO we looked up field once, let's reuse it
              val classForOriginalField =
                targetClass.takeUnless {
                  isCompanionObject && (property.isConst || isJvmField || isStatic)
                } ?: classIfCompanion

              val field =
                classForOriginalField.lookupField(fieldSignature)
                  ?: error("No field $fieldSignature found in $classForOriginalField.")
              val constant =
                if (property.hasConstant) {
                  val fieldWithConstant =
                    classIfCompanion
                      .takeIf { it != targetClass }
                      ?.let {
                        if (it.isInterface) {
                          field
                        } else {
                          // const properties are relocated to the enclosing class
                          it.lookupField(fieldSignature)
                            ?: error("No field $fieldSignature found in $it.")
                        }
                      } ?: field
                  fieldWithConstant.constantValue()
                } else {
                  null
                }

              val jvmModifiers = field.jvmModifiers() + parentModifiers

              // For static, const, or JvmField fields in a companion object, the companion
              // object's field is marked as synthetic to hide it from Java, but in this case
              // it's a false positive for this check in kotlin.
              val isSynthetic =
                field.isSynthetic &&
                  !(isCompanionObject &&
                    (property.isConst || isJvmField || JvmFieldModifier.STATIC in jvmModifiers))

              FieldData(
                annotations = field.annotationSpecs(),
                isSynthetic = isSynthetic,
                jvmModifiers =
                  jvmModifiers.filterNotTo(mutableSetOf()) {
                    // JvmField companion objects don't need JvmStatic, it's implicit
                    isCompanionObject && isJvmField && it == JvmFieldModifier.STATIC
                  },
                constant = constant,
              )
            }

          val getterData =
            property.getterSignature?.let { getterSignature ->
              val method = classIfCompanion.lookupMethod(getterSignature)
              method?.methodData(
                clazz = targetClass,
                signature = getterSignature,
                hasAnnotations = property.getter.hasAnnotationsInBytecode,
                jvmInformationMethod =
                  classIfCompanion.takeIf { it != targetClass }?.lookupMethod(getterSignature)
                    ?: method,
              ) ?: error("No getter method $getterSignature found in $classIfCompanion.")
            }

          val setterData =
            property.setterSignature?.let { setterSignature ->
              val method = classIfCompanion.lookupMethod(setterSignature)
              method?.methodData(
                clazz = targetClass,
                signature = setterSignature,
                hasAnnotations = property.setter?.hasAnnotationsInBytecode ?: false,
                jvmInformationMethod =
                  classIfCompanion.takeIf { it != targetClass }?.lookupMethod(setterSignature)
                    ?: method,
                knownIsOverride = getterData?.isOverride,
              ) ?: error("No setter method $setterSignature found in $classIfCompanion.")
            }

          val annotations = mutableListOf<AnnotationSpec>()
          if (property.hasAnnotationsInBytecode) {
            property.syntheticMethodForAnnotations?.let { annotationsHolderSignature ->
              targetClass.lookupMethod(annotationsHolderSignature)?.let { method ->
                annotations +=
                  method
                    .annotationSpecs()
                    // Cover for https://github.com/square/kotlinpoet/issues/1046
                    .filterNot { it.typeName == JAVA_DEPRECATED }
              }
            }
          }

          PropertyData(
            annotations = annotations,
            fieldData = fieldData,
            getterData = getterData,
            setterData = setterData,
            isJvmField = isJvmField,
          )
        }

    val methodData =
      declarationContainer.functions.associateWithTo(TreeMap(KM_FUNCTION_COMPARATOR)) { kmFunction
        ->
        val signature = kmFunction.signature
        if (signature != null) {
          val method = targetClass.lookupMethod(signature)
          method?.methodData(
            clazz = targetClass,
            signature = signature,
            hasAnnotations = kmFunction.hasAnnotationsInBytecode,
            jvmInformationMethod =
              classIfCompanion.takeIf { it != targetClass }?.lookupMethod(signature) ?: method,
          ) ?: error("No method $signature found in $targetClass.")
        } else {
          MethodData.EMPTY
        }
      }

    when (declarationContainer) {
      is KmClass -> {
        val classAnnotations =
          if (declarationContainer.hasAnnotationsInBytecode) {
            ClassInspectorUtil.createAnnotations {
              addAll(
                targetClass.annotations.map { AnnotationSpec.get(it, includeDefaultValues = true) }
              )
            }
          } else {
            emptyList()
          }
        val constructorData =
          declarationContainer.constructors.associateWithTo(TreeMap(KM_CONSTRUCTOR_COMPARATOR)) {
            kmConstructor ->
            if (
              declarationContainer.kind == ClassKind.ANNOTATION_CLASS ||
                declarationContainer.isValue
            ) {
              //
              // Annotations are interfaces in reflection, but kotlin metadata will still report a
              // constructor signature
              //
              // Inline classes have no constructors at runtime
              //
              return@associateWithTo ConstructorData.EMPTY
            }
            val signature = kmConstructor.signature
            if (signature != null) {
              val constructor =
                targetClass.lookupConstructor(signature)
                  ?: error("No constructor $signature found in $targetClass.")
              ConstructorData(
                annotations =
                  if (kmConstructor.hasAnnotationsInBytecode) {
                    constructor.annotationSpecs()
                  } else {
                    emptyList()
                  },
                parameterAnnotations = constructor.parameters.indexedAnnotationSpecs(),
                isSynthetic = constructor.isSynthetic,
                jvmModifiers = constructor.jvmModifiers(),
                exceptions = constructor.exceptionTypeNames(),
              )
            } else {
              ConstructorData.EMPTY
            }
          }
        return ClassData(
          declarationContainer = declarationContainer,
          className = className,
          annotations = classAnnotations,
          properties = propertyData,
          constructors = constructorData,
          methods = methodData,
        )
      }
      is KmPackage -> {
        // There's no flag for checking if there are annotations, so we just eagerly check in this
        // case. All annotations on this class are file: site targets in source. This does not
        // include @JvmName since it does not have RUNTIME retention. In practice this doesn't
        // really matter, but it does mean we can't know for certain if the file should be called
        // FooKt.kt or Foo.kt.
        val fileAnnotations =
          ClassInspectorUtil.createAnnotations(FILE) {
            addAll(
              targetClass.annotations.map { AnnotationSpec.get(it, includeDefaultValues = true) }
            )
          }
        return FileData(
          declarationContainer = declarationContainer,
          annotations = fileAnnotations,
          properties = propertyData,
          methods = methodData,
          className = className,
        )
      }
      else -> TODO("Not implemented yet: ${declarationContainer.javaClass.simpleName}")
    }
  }

  private fun Array<Parameter>.indexedAnnotationSpecs(): Map<Int, Collection<AnnotationSpec>> {
    return withIndex().associate { (index, parameter) ->
      index to ClassInspectorUtil.createAnnotations { addAll(parameter.annotationSpecs()) }
    }
  }

  private fun Method.methodData(
    clazz: Class<*>,
    signature: JvmMethodSignature,
    hasAnnotations: Boolean,
    jvmInformationMethod: Method = this,
    knownIsOverride: Boolean? = null,
  ): MethodData {
    return MethodData(
      annotations = if (hasAnnotations) annotationSpecs() else emptyList(),
      parameterAnnotations = parameters.indexedAnnotationSpecs(),
      isSynthetic = isSynthetic,
      jvmModifiers = jvmInformationMethod.jvmModifiers(),
      isOverride = knownIsOverride ?: signature.isOverriddenIn(clazz),
      exceptions = exceptionTypeNames(),
    )
  }

  public companion object {
    /**
     * @param lenient see docs on [KotlinClassMetadata.readStrict] and
     *   [KotlinClassMetadata.readLenient] for more details.
     */
    @JvmStatic
    public fun create(lenient: Boolean, classLoader: ClassLoader? = null): ClassInspector {
      return ReflectiveClassInspector(lenient, classLoader)
    }

    private val Class<*>.descriptor: String
      get() {
        return when {
          isPrimitive ->
            when (kotlin) {
              Byte::class -> "B"
              Char::class -> "C"
              Double::class -> "D"
              Float::class -> "F"
              Int::class -> "I"
              Long::class -> "J"
              Short::class -> "S"
              Boolean::class -> "Z"
              Void::class -> "V"
              else -> throw RuntimeException("Unrecognized primitive $this")
            }
          isArray -> name.replace('.', '/')
          else -> "L$name;".replace('.', '/')
        }
      }

    private val Method.descriptor: String
      get() =
        parameterTypes.joinToString(
          separator = "",
          prefix = "(",
          postfix = ")${returnType.descriptor}",
        ) {
          it.descriptor
        }

    /**
     * Returns the JVM signature in the form "$Name$MethodDescriptor", for example:
     * `equals(Ljava/lang/Object;)Z`.
     *
     * Useful for comparing with [JvmMethodSignature].
     *
     * For reference, see the
     * [JVM specification, section 4.3](https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.3).
     */
    private val Method.jvmMethodSignature: String
      get() = "$name$descriptor"

    private val Constructor<*>.descriptor: String
      get() =
        parameterTypes.joinToString(separator = "", prefix = "(", postfix = ")V") { it.descriptor }

    /**
     * Returns the JVM signature in the form "<init>$MethodDescriptor", for example:
     * `"<init>(Ljava/lang/Object;)V")`.
     *
     * Useful for comparing with [JvmMethodSignature].
     *
     * For reference, see the
     * [JVM specification, section 4.3](https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.3).
     */
    private val Constructor<*>.jvmMethodSignature: String
      get() = "<init>$descriptor"

    /**
     * Returns the JVM signature in the form "$Name:$FieldDescriptor", for example:
     * `"value:Ljava/lang/String;"`.
     *
     * Useful for comparing with [JvmFieldSignature].
     *
     * For reference, see the
     * [JVM specification, section 4.3](https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.3).
     */
    private val Field.jvmFieldSignature: String
      get() = "$name:${type.descriptor}"
  }
}
