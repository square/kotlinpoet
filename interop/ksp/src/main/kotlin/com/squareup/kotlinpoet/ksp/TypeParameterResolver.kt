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
package com.squareup.kotlinpoet.ksp

import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.squareup.kotlinpoet.TypeVariableName

/**
 * A resolver for enclosing declarations' type parameters. Parent declarations can be anything with
 * generics that child nodes declare as defined by [KSType.arguments].
 *
 * This is important for resolving inherited generics on child declarations, as KSP interop
 * otherwise can't resolve them.
 *
 * In general, you want to retrieve an instance of this via [toTypeParameterResolver].
 *
 * @see toTypeParameterResolver
 */
public interface TypeParameterResolver {
  public val parametersMap: Map<String, TypeVariableName>

  public operator fun get(index: String): TypeVariableName

  public companion object {
    /**
     * An empty instance of [TypeParameterResolver], only should be used if enclosing declarations
     * are known to not have arguments, such as top-level classes.
     */
    public val EMPTY: TypeParameterResolver =
      object : TypeParameterResolver {
        override val parametersMap: Map<String, TypeVariableName> = emptyMap()

        override fun get(index: String): TypeVariableName =
          throw NoSuchElementException("No TypeParameter found for index $index")
      }
  }
}

/**
 * Returns a [TypeParameterResolver] for this list of [KSTypeParameters][KSTypeParameter] for use
 * with enclosed declarations.
 *
 * @param parent the optional parent resolver, if any. An example of this is cases where you might
 *   create a resolver for a [KSFunction] and supply a parent resolved from the enclosing
 *   [KSClassDeclaration].
 * @param sourceTypeHint an optional hint for error messages. Unresolvable parameter IDs will
 *   include this hint in the thrown error's message.
 */
public fun List<KSTypeParameter>.toTypeParameterResolver(
  parent: TypeParameterResolver? = null,
  sourceTypeHint: String = "<unknown>",
): TypeParameterResolver {
  val parametersMap = LinkedHashMap<String, TypeVariableName>()
  val typeParamResolver = { id: String ->
    parametersMap[id]
      ?: parent?.get(id)
      ?: throw IllegalStateException(
        "No type argument found for $id! Analyzed $sourceTypeHint with known parameters " +
          "${parametersMap.keys}"
      )
  }

  val resolver =
    object : TypeParameterResolver {
      override val parametersMap: Map<String, TypeVariableName> = parametersMap

      override operator fun get(index: String): TypeVariableName = typeParamResolver(index)
    }

  // Fill the parametersMap. Need to do sequentially and allow for referencing previously defined
  // params
  for (typeVar in this) {
    // Put the simple typevar in first, then it can be referenced in the full toTypeVariable()
    // replacement later that may add bounds referencing this.
    val id = typeVar.name.getShortName()
    parametersMap[id] = TypeVariableName(id)
  }

  for (typeVar in this) {
    val id = typeVar.name.getShortName()
    // Now replace it with the full version.
    parametersMap[id] = typeVar.toTypeVariableName(resolver)
  }

  return resolver
}
