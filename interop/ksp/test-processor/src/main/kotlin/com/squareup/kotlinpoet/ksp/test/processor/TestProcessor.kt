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
package com.squareup.kotlinpoet.ksp.test.processor

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.ARRAY
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.ksp.TypeParameterResolver
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.kspDependencies
import com.squareup.kotlinpoet.ksp.originatingKSFiles
import com.squareup.kotlinpoet.ksp.toAnnotationSpec
import com.squareup.kotlinpoet.ksp.toKModifier
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.toTypeParameterResolver
import com.squareup.kotlinpoet.ksp.toTypeVariableName
import com.squareup.kotlinpoet.ksp.writeTo

/**
 * A simple processor that generates a skeleton API of classes annotated with [ExampleAnnotation]
 * for test and verification purposes.
 */
class TestProcessor(private val env: SymbolProcessorEnvironment) : SymbolProcessor {

  private val unwrapTypeAliases = env.options["unwrapTypeAliases"]?.toBooleanStrictOrNull() ?: false

  override fun process(resolver: Resolver): List<KSAnnotated> {
    resolver
      .getSymbolsWithAnnotation(ExampleAnnotation::class.java.canonicalName)
      .forEach(::process)
    return emptyList()
  }

  private fun KSTypeReference.toValidatedTypeName(resolver: TypeParameterResolver): TypeName {
    // Validates that both toTypeName() and resolve() return the same TypeName.
    // Regression for https://github.com/square/kotlinpoet/issues/1513.
    val typeName = toTypeName(resolver)
    val resolvedTypeName = resolve().toTypeName(resolver)
    check(typeName == resolvedTypeName)
    return typeName
  }

  private fun process(decl: KSAnnotated) {
    check(decl is KSClassDeclaration)

    val classBuilder =
      TypeSpec.classBuilder("Test${decl.simpleName.getShortName()}")
        .addOriginatingKSFile(decl.containingFile!!)
        .apply {
          decl.getVisibility().toKModifier()?.let { addModifiers(it) }
          addModifiers(decl.modifiers.mapNotNull { it.toKModifier() })
          addAnnotations(
            decl.annotations
              .filterNot { it.shortName.getShortName() == "ExampleAnnotation" }
              .map {
                it.toAnnotationSpec(it.shortName.getShortName() == "ExampleAnnotationWithDefaults")
              }
              .asIterable()
          )
          val allSupertypes = decl.superTypes.toList()
          val (superclassReference, superInterfaces) =
            if (allSupertypes.isNotEmpty()) {
              val superClass =
                allSupertypes.firstOrNull {
                  val resolved = it.resolve()
                  resolved is KSClassDeclaration && resolved.classKind == ClassKind.CLASS
                }
              if (superClass != null) {
                superClass to allSupertypes.filterNot { it == superClass }
              } else {
                null to allSupertypes
              }
            } else {
              null to allSupertypes
            }

          superclassReference?.let {
            val typeName = it.toValidatedTypeName(decl.typeParameters.toTypeParameterResolver())
            if (typeName != ANY) {
              superclass(typeName)
            }
          }
          addSuperinterfaces(
            superInterfaces
              .map { it.toValidatedTypeName(decl.typeParameters.toTypeParameterResolver()) }
              .filterNot { it == ANY }
              .toList()
          )
        }
    val classTypeParams = decl.typeParameters.toTypeParameterResolver()
    classBuilder.addTypeVariables(
      decl.typeParameters.map { typeParam ->
        typeParam.toTypeVariableName(classTypeParams).let {
          if (unwrapTypeAliases) {
            it.unwrapTypeAlias()
          } else {
            it
          }
        }
      }
    )

    // Add properties
    for (property in decl.getDeclaredProperties()) {
      classBuilder.addProperty(
        PropertySpec.builder(
            property.simpleName.getShortName(),
            property.type.toValidatedTypeName(classTypeParams).let {
              if (unwrapTypeAliases) {
                it.unwrapTypeAlias()
              } else {
                it
              }
            },
          )
          .addOriginatingKSFile(decl.containingFile!!)
          .mutable(property.isMutable)
          .apply {
            property.getVisibility().toKModifier()?.let { addModifiers(it) }
            addModifiers(property.modifiers.mapNotNull { it.toKModifier() })
            addAnnotations(property.annotations.map { it.toAnnotationSpec() }.asIterable())
            if (Modifier.LATEINIT !in property.modifiers) {
              initializer(CodeBlock.of("TODO()"))
            }
          }
          .build()
      )
    }

    // Add functions
    for (function in decl.getDeclaredFunctions().filterNot { it.isConstructor() }) {
      val functionTypeParams = function.typeParameters.toTypeParameterResolver(classTypeParams)
      classBuilder.addFunction(
        FunSpec.builder(function.simpleName.getShortName())
          .addOriginatingKSFile(decl.containingFile!!)
          .apply {
            function.getVisibility().toKModifier()?.let { addModifiers(it) }
            addModifiers(function.modifiers.mapNotNull { it.toKModifier() })
          }
          .addTypeVariables(
            function.typeParameters.map { typeParam ->
              typeParam.toTypeVariableName(functionTypeParams).let {
                if (unwrapTypeAliases) {
                  it.unwrapTypeAlias()
                } else {
                  it
                }
              }
            }
          )
          .addParameters(
            function.parameters.map { parameter ->
              val isVararg = parameter.isVararg
              val possibleVararg =
                if (isVararg) {
                  arrayOf(KModifier.VARARG)
                } else {
                  emptyArray()
                }
              // Function references can't be obtained from a resolved KSType because it resolves to
              // FunctionN<> which
              // loses the necessary context, skip validation in these cases as we know they won't
              // match.
              val typeName =
                if (parameter.type.resolve().run { isFunctionType || isSuspendFunctionType }) {
                    parameter.type.toTypeName(functionTypeParams)
                  } else {
                    parameter.type.toValidatedTypeName(functionTypeParams)
                  }
                  .let { paramType ->
                    // In KSP1, this just gives us the T type for the param
                    // In KSP2, this gives us an Array<out T> for the param
                    if (
                      paramType is ParameterizedTypeName && paramType.rawType == ARRAY && isVararg
                    ) {
                      paramType.typeArguments.single().let { componentType ->
                        if (componentType is WildcardTypeName) {
                          componentType.outTypes.single()
                        } else {
                          componentType
                        }
                      }
                    } else {
                      paramType
                    }
                  }
              val parameterType =
                typeName.let {
                  if (unwrapTypeAliases) {
                    it.unwrapTypeAlias()
                  } else {
                    it
                  }
                }
              parameter.name?.let {
                ParameterSpec.builder(it.getShortName(), parameterType, *possibleVararg).build()
              } ?: ParameterSpec.unnamed(parameterType)
            }
          )
          .returns(
            function.returnType!!.toValidatedTypeName(functionTypeParams).let {
              if (unwrapTypeAliases) {
                it.unwrapTypeAlias()
              } else {
                it
              }
            }
          )
          .addCode(CodeBlock.of("return TODO()"))
          .build()
      )
    }

    val typeSpec = classBuilder.build()
    val fileSpec =
      FileSpec.builder(decl.packageName.asString(), typeSpec.name!!).addType(typeSpec).build()

    // Ensure that we're properly de-duping these under the hood.
    check(fileSpec.originatingKSFiles().size == 1)

    val dependencies = fileSpec.kspDependencies(aggregating = true)
    check(dependencies.originatingFiles.size == 1)
    check(dependencies.originatingFiles[0] == decl.containingFile)

    fileSpec.writeTo(env.codeGenerator, dependencies)
  }
}
