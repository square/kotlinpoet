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
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
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
    resolver.getSymbolsWithAnnotation(ExampleAnnotation::class.java.canonicalName)
      .forEach(::process)
    return emptyList()
  }

  private fun process(decl: KSAnnotated) {
    check(decl is KSClassDeclaration)

    val classBuilder = TypeSpec.classBuilder(decl.simpleName.getShortName())
      .addOriginatingKSFile(decl.containingFile!!)
      .apply {
        decl.getVisibility().toKModifier()?.let { addModifiers(it) }
        addModifiers(decl.modifiers.mapNotNull { it.toKModifier() })
        addAnnotations(
          decl.annotations
            .filterNot { it.shortName.getShortName() == "ExampleAnnotation" }
            .map { it.toAnnotationSpec() }.asIterable(),
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
      },
    )

    // Add properties
    for (property in decl.getDeclaredProperties()) {
      classBuilder.addProperty(
        PropertySpec.builder(
          property.simpleName.getShortName(),
          property.type.toTypeName(classTypeParams).let {
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
            addAnnotations(
              property.annotations
                .map { it.toAnnotationSpec() }.asIterable(),
            )
          }
          .build(),
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
            },
          )
          .addParameters(
            function.parameters.map { parameter ->
              val parameterType = parameter.type.toTypeName(functionTypeParams).let {
                if (unwrapTypeAliases) {
                  it.unwrapTypeAlias()
                } else {
                  it
                }
              }
              parameter.name?.let {
                ParameterSpec.builder(it.getShortName(), parameterType).build()
              } ?: ParameterSpec.unnamed(parameterType)
            },
          )
          .returns(
            function.returnType!!.toTypeName(functionTypeParams).let {
              if (unwrapTypeAliases) {
                it.unwrapTypeAlias()
              } else {
                it
              }
            },
          )
          .build(),
      )
    }

    val typeSpec = classBuilder.build()
    val fileSpec = FileSpec.builder(decl.packageName.asString(), "Test${typeSpec.name}")
      .addType(typeSpec)
      .build()

    // Ensure that we're properly de-duping these under the hood.
    check(fileSpec.originatingKSFiles().size == 1)

    val dependencies = fileSpec.kspDependencies(aggregating = true)
    check(dependencies.originatingFiles.size == 1)
    check(dependencies.originatingFiles[0] == decl.containingFile)

    fileSpec.writeTo(env.codeGenerator, dependencies)
  }
}
