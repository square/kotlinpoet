/*
 * Copyright (C) 2017 Square, Inc.
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

import java.lang.reflect.Type
import kotlin.reflect.KClass

// AnnotationSpec.Builder.addMember
fun AnnotationSpec.Builder.addMember(name: String, block: CodeBlock.Builder.() -> Unit) =
        addMember(name, CodeBlock.Builder().apply(block).build())

// FunSpec.Builder.addKdoc
fun FunSpec.Builder.addKdoc(block: CodeBlock.Builder.() -> Unit) = addKdoc(
        CodeBlock.Builder().apply(block).build())

// FunSpec.Builder.addAnnotation
fun FunSpec.Builder.addAnnotation(type: ClassName, block: AnnotationSpec.Builder.() -> Unit) =
        addAnnotation(AnnotationSpec.builder(type).apply(block).build())

fun FunSpec.Builder.addAnnotation(type: Class<*>, block: AnnotationSpec.Builder.() -> Unit) =
        addAnnotation(AnnotationSpec.builder(type).apply(block).build())

fun FunSpec.Builder.addAnnotation(type: KClass<*>, block: AnnotationSpec.Builder.() -> Unit) =
        addAnnotation(AnnotationSpec.builder(type).apply(block).build())

// FunSpec.Builder.addParameter
fun FunSpec.Builder.addParameter(name: String, type: TypeName, vararg modifiers: KModifier,
                                 block: ParameterSpec.Builder.() -> Unit) =
        addParameter(ParameterSpec.builder(name, type, *modifiers).apply(block).build())

fun FunSpec.Builder.addParameter(name: String, type: Type, vararg modifiers: KModifier,
                                 block: ParameterSpec.Builder.() -> Unit) =
        addParameter(ParameterSpec.builder(name, type, *modifiers).apply(block).build())

fun FunSpec.Builder.addParameter(name: String, type: KClass<*>, vararg modifiers: KModifier,
                                 block: ParameterSpec.Builder.() -> Unit) =
        addParameter(ParameterSpec.builder(name, type, *modifiers).apply(block).build())

// FunSpec.Builder.addCode
fun FunSpec.Builder.addCode(block: CodeBlock.Builder.() -> Unit) = addCode(
        CodeBlock.Builder().apply(block).build())

// FunSpec.Builder.defaultValue
fun FunSpec.Builder.defaultValue(block: CodeBlock.Builder.() -> Unit) = defaultValue(
        CodeBlock.Builder().apply(block).build())

// KotlinFile.Builder.addType
fun KotlinFile.Builder.addClassType(name: String, block: TypeSpec.Builder.() -> Unit) = addType(
        TypeSpec.classBuilder(name).apply(block).build())

fun KotlinFile.Builder.addClassType(className: ClassName, block: TypeSpec.Builder.() -> Unit) =
        addType(TypeSpec.classBuilder(className).apply(block).build())

fun KotlinFile.Builder.addObjectType(name: String, block: TypeSpec.Builder.() -> Unit) = addType(
        TypeSpec.objectBuilder(name).apply(block).build())

fun KotlinFile.Builder.addObjectType(className: ClassName, block: TypeSpec.Builder.() -> Unit) =
        addType(TypeSpec.objectBuilder(className).apply(block).build())

fun KotlinFile.Builder.addCompanionObjectType(block: TypeSpec.Builder.() -> Unit) = addType(
        TypeSpec.companionObjectBuilder().apply(block).build())

fun KotlinFile.Builder.addInterfaceType(name: String, block: TypeSpec.Builder.() -> Unit) = addType(
        TypeSpec.interfaceBuilder(name).apply(block).build())

fun KotlinFile.Builder.addInterfaceType(className: ClassName, block: TypeSpec.Builder.() -> Unit) =
        addType(TypeSpec.interfaceBuilder(className).apply(block).build())

fun KotlinFile.Builder.addEnumType(name: String, block: TypeSpec.Builder.() -> Unit) = addType(
        TypeSpec.enumBuilder(name).apply(block).build())

fun KotlinFile.Builder.addEnumType(className: ClassName, block: TypeSpec.Builder.() -> Unit) =
        addType(TypeSpec.enumBuilder(className).apply(block).build())

fun KotlinFile.Builder.addAnnotationType(name: String, block: TypeSpec.Builder.() -> Unit) =
        addType(TypeSpec.annotationBuilder(name).apply(block).build())

fun KotlinFile.Builder.addAnnotationType(className: ClassName, block: TypeSpec.Builder.() -> Unit) =
        addType(TypeSpec.annotationBuilder(className).apply(block).build())

fun KotlinFile.Builder.addAnonymousClassType(typeArgumentsFormat: String, vararg args: Any,
                                             block: TypeSpec.Builder.() -> Unit) =
        addType(TypeSpec.anonymousClassBuilder(typeArgumentsFormat, *args).apply(block).build())

// KotlinFile.Builder.addFun
fun KotlinFile.Builder.addFun(name: String, block: FunSpec.Builder.() -> Unit) = addFun(
        FunSpec.builder(name).apply(block).build())

// KotlinFile.Builder.addProperty
fun KotlinFile.Builder.addProperty(name: String, type: TypeName, vararg modifiers: KModifier,
                                   block: PropertySpec.Builder.() -> Unit) =
        addProperty(PropertySpec.builder(name, type, *modifiers).apply(block).build())

fun KotlinFile.Builder.addProperty(name: String, type: Type, vararg modifiers: KModifier,
                                   block: PropertySpec.Builder.() -> Unit) =
        addProperty(PropertySpec.builder(name, type, *modifiers).apply(block).build())

fun KotlinFile.Builder.addProperty(name: String, type: KClass<*>, vararg modifiers: KModifier,
                                   block: PropertySpec.Builder.() -> Unit) =
        addProperty(PropertySpec.builder(name, type, *modifiers).apply(block).build())

fun KotlinFile.Builder.addVarProperty(name: String, type: TypeName, vararg modifiers: KModifier,
                                      block: PropertySpec.Builder.() -> Unit) =
        addProperty(PropertySpec.varBuilder(name, type, *modifiers).apply(block).build())

fun KotlinFile.Builder.addVarProperty(name: String, type: Type, vararg modifiers: KModifier,
                                      block: PropertySpec.Builder.() -> Unit) =
        addProperty(PropertySpec.varBuilder(name, type, *modifiers).apply(block).build())

fun KotlinFile.Builder.addVarProperty(name: String, type: KClass<*>, vararg modifiers: KModifier,
                                      block: PropertySpec.Builder.() -> Unit) =
        addProperty(PropertySpec.varBuilder(name, type, *modifiers).apply(block).build())

// ParameterSpec.Builder.addAnnotation
fun ParameterSpec.Builder.addAnnotation(type: ClassName, block: AnnotationSpec.Builder.() -> Unit) =
        addAnnotation(AnnotationSpec.builder(type).apply(block).build())

fun ParameterSpec.Builder.addAnnotation(type: Class<*>, block: AnnotationSpec.Builder.() -> Unit) =
        addAnnotation(AnnotationSpec.builder(type).apply(block).build())

fun ParameterSpec.Builder.addAnnotation(type: KClass<*>, block: AnnotationSpec.Builder.() -> Unit) =
        addAnnotation(AnnotationSpec.builder(type).apply(block).build())

// ParameterSpec.Builder.defaultValue
fun ParameterSpec.Builder.defaultValue(block: CodeBlock.Builder.() -> Unit) = defaultValue(
        CodeBlock.Builder().apply(block).build())

// PropertySpec.Builder.addKdoc
fun PropertySpec.Builder.addKdoc(block: CodeBlock.Builder.() -> Unit) = addKdoc(
        CodeBlock.Builder().apply(block).build())

// PropertySpec.Builder.addAnnotation
fun PropertySpec.Builder.addAnnotation(type: ClassName, block: AnnotationSpec.Builder.() -> Unit) =
        addAnnotation(AnnotationSpec.builder(type).apply(block).build())

fun PropertySpec.Builder.addAnnotation(type: Class<*>, block: AnnotationSpec.Builder.() -> Unit) =
        addAnnotation(AnnotationSpec.builder(type).apply(block).build())

fun PropertySpec.Builder.addAnnotation(type: KClass<*>, block: AnnotationSpec.Builder.() -> Unit) =
        addAnnotation(AnnotationSpec.builder(type).apply(block).build())

// PropertySpec.Builder.initializer
fun PropertySpec.Builder.initializer(block: CodeBlock.Builder.() -> Unit) = initializer(
        CodeBlock.Builder().apply(block).build())

// PropertySpec.Builder.delegate
fun PropertySpec.Builder.delegate(block: CodeBlock.Builder.() -> Unit) = delegate(
        CodeBlock.Builder().apply(block).build())

// PropertySpec.Builder.getter
fun PropertySpec.Builder.getter(block: FunSpec.Builder.() -> Unit) = getter(
        FunSpec.getterBuilder().apply(block).build())

// PropertySpec.Builder.setter
fun PropertySpec.Builder.setter(block: FunSpec.Builder.() -> Unit) = setter(
        FunSpec.setterBuilder().apply(block).build())

// TypeSpec.Builder.addKdoc
fun TypeSpec.Builder.addKdoc(block: CodeBlock.Builder.() -> Unit) = addKdoc(
        CodeBlock.Builder().apply(block).build())

// TypeSpec.Builder.addAnnotation
fun TypeSpec.Builder.addAnnotation(type: ClassName, block: AnnotationSpec.Builder.() -> Unit) =
        addAnnotation(AnnotationSpec.builder(type).apply(block).build())

fun TypeSpec.Builder.addAnnotation(type: Class<*>, block: AnnotationSpec.Builder.() -> Unit) =
        addAnnotation(AnnotationSpec.builder(type).apply(block).build())

fun TypeSpec.Builder.addAnnotation(type: KClass<*>, block: AnnotationSpec.Builder.() -> Unit) =
        addAnnotation(AnnotationSpec.builder(type).apply(block).build())

// TypeSpec.Builder.companionObject
fun TypeSpec.Builder.companionObject(block: TypeSpec.Builder.() -> Unit) = companionObject(
        TypeSpec.companionObjectBuilder().apply(block).build())

// TypeSpec.Builder.primaryConstructor
fun TypeSpec.Builder.primaryConstructor(block: FunSpec.Builder.() -> Unit) = primaryConstructor(
        FunSpec.constructorBuilder().apply(block).build())

// TypeSpec.Builder.addProperty
fun TypeSpec.Builder.addProperty(name: String, type: TypeName, vararg modifiers: KModifier,
                                 block: PropertySpec.Builder.() -> Unit) =
        addProperty(PropertySpec.builder(name, type, *modifiers).apply(block).build())

fun TypeSpec.Builder.addProperty(name: String, type: Type, vararg modifiers: KModifier,
                                 block: PropertySpec.Builder.() -> Unit) =
        addProperty(PropertySpec.builder(name, type, *modifiers).apply(block).build())

fun TypeSpec.Builder.addProperty(name: String, type: KClass<*>, vararg modifiers: KModifier,
                                 block: PropertySpec.Builder.() -> Unit) =
        addProperty(PropertySpec.builder(name, type, *modifiers).apply(block).build())

fun TypeSpec.Builder.addVarProperty(name: String, type: TypeName, vararg modifiers: KModifier,
                                    block: PropertySpec.Builder.() -> Unit) =
        addProperty(PropertySpec.varBuilder(name, type, *modifiers).apply(block).build())

fun TypeSpec.Builder.addVarProperty(name: String, type: Type, vararg modifiers: KModifier,
                                    block: PropertySpec.Builder.() -> Unit) =
        addProperty(PropertySpec.varBuilder(name, type, *modifiers).apply(block).build())

fun TypeSpec.Builder.addVarProperty(name: String, type: KClass<*>, vararg modifiers: KModifier,
                                    block: PropertySpec.Builder.() -> Unit) =
        addProperty(PropertySpec.varBuilder(name, type, *modifiers).apply(block).build())

// TypeSpec.Builder.addInitializerBlock
fun TypeSpec.Builder.addInitializerBlock(block: CodeBlock.Builder.() -> Unit) = addInitializerBlock(
        CodeBlock.Builder().apply(block).build())

// TypeSpec.Builder.addFun
fun TypeSpec.Builder.addGetter(block: FunSpec.Builder.() -> Unit) = addFun(
        FunSpec.getterBuilder().apply(block).build())

fun TypeSpec.Builder.addSetter(block: FunSpec.Builder.() -> Unit) = addFun(
        FunSpec.setterBuilder().apply(block).build())

fun TypeSpec.Builder.addConstructor(block: FunSpec.Builder.() -> Unit) = addFun(
        FunSpec.constructorBuilder().apply(block).build())

fun TypeSpec.Builder.addFun(name: String, block: FunSpec.Builder.() -> Unit) = addFun(
        FunSpec.builder(name).apply(block).build())

// TypeSpec.Builder.addType
fun TypeSpec.Builder.addClassType(name: String, block: TypeSpec.Builder.() -> Unit) = addType(
        TypeSpec.classBuilder(name).apply(block).build())

fun TypeSpec.Builder.addClassType(className: ClassName, block: TypeSpec.Builder.() -> Unit) =
        addType(TypeSpec.classBuilder(className).apply(block).build())

fun TypeSpec.Builder.addObjectType(name: String, block: TypeSpec.Builder.() -> Unit) = addType(
        TypeSpec.objectBuilder(name).apply(block).build())

fun TypeSpec.Builder.addObjectType(className: ClassName, block: TypeSpec.Builder.() -> Unit) =
        addType(TypeSpec.objectBuilder(className).apply(block).build())

fun TypeSpec.Builder.addCompanionObjectType(block: TypeSpec.Builder.() -> Unit) = addType(
        TypeSpec.companionObjectBuilder().apply(block).build())

fun TypeSpec.Builder.addInterfaceType(name: String, block: TypeSpec.Builder.() -> Unit) = addType(
        TypeSpec.interfaceBuilder(name).apply(block).build())

fun TypeSpec.Builder.addInterfaceType(className: ClassName, block: TypeSpec.Builder.() -> Unit) =
        addType(TypeSpec.interfaceBuilder(className).apply(block).build())

fun TypeSpec.Builder.addEnumType(name: String, block: TypeSpec.Builder.() -> Unit) = addType(
        TypeSpec.enumBuilder(name).apply(block).build())

fun TypeSpec.Builder.addEnumType(className: ClassName, block: TypeSpec.Builder.() -> Unit) =
        addType(TypeSpec.enumBuilder(className).apply(block).build())

fun TypeSpec.Builder.addAnnotationType(name: String, block: TypeSpec.Builder.() -> Unit) =
        addType(TypeSpec.annotationBuilder(name).apply(block).build())

fun TypeSpec.Builder.addAnnotationType(className: ClassName, block: TypeSpec.Builder.() -> Unit) =
        addType(TypeSpec.annotationBuilder(className).apply(block).build())

fun TypeSpec.Builder.addAnonymousClassType(typeArgumentsFormat: String, vararg args: Any,
                                           block: TypeSpec.Builder.() -> Unit) =
        addType(TypeSpec.anonymousClassBuilder(typeArgumentsFormat, *args).apply(block).build())
