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
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.*

/**
 * A simple processor that generates a skeleton API of classes annotated with [ExampleAnnotation]
 * for test and verification purposes.
 */
class TestProcessor(private val env: SymbolProcessorEnvironment) : SymbolProcessor {

  private val unwrapTypeAliases = env.options["unwrapTypeAliases"]?.toBooleanStrictOrNull() ?: false
  private val toTypeNameMode = ToTypeNameMode.valueOf(env.options["toTypeNameMode"] ?: "REFERENCE")

  override fun process(resolver: Resolver): List<KSAnnotated> {
    resolver.getSymbolsWithAnnotation(ExampleAnnotation::class.java.canonicalName)
      .forEach(::process)
    return emptyList()
  }

  enum class ToTypeNameMode {
    REFERENCE, TYPE
  }

  private fun KSTypeReference.toTypeName(mode: ToTypeNameMode, resolver: TypeParameterResolver) = when (mode) {
    ToTypeNameMode.REFERENCE -> {
      this.toTypeName(resolver)
    }
    ToTypeNameMode.TYPE -> {
      this.resolve().toTypeName(resolver)
    }
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
        val allSupertypes = decl.superTypes.toList()
        val (superclassReference, superInterfaces) = if (allSupertypes.isNotEmpty()) {
          val superClass = allSupertypes.firstOrNull {
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
          val typeName = it.toTypeName(toTypeNameMode, decl.typeParameters.toTypeParameterResolver())
          if (typeName != ANY) {
            superclass(typeName)
          }
        }
        addSuperinterfaces(
          superInterfaces.map { it.toTypeName(toTypeNameMode, decl.typeParameters.toTypeParameterResolver()) }
            .filterNot { it == ANY }
            .toList(),
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
          property.type.toTypeName(toTypeNameMode, classTypeParams).let {
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
              val parameterType = parameter.type.toTypeName(toTypeNameMode, functionTypeParams).let {
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
