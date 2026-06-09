/*
 * Copyright (C) 2026 Square, Inc.
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

/** A spec which contains a body of code. */
public interface CodeBlockHolder {
  public val body: CodeBlock

  public interface Builder<out T : Builder<T>> {
    public val body: CodeBlock.Builder

    @Suppress("UNCHECKED_CAST")
    public fun addCode(format: String, vararg args: Any?): T =
      apply { body.add(format, *args) } as T

    @Suppress("UNCHECKED_CAST")
    public fun addCode(codeBlock: CodeBlock): T = apply { body.add(codeBlock) } as T

    @Suppress("UNCHECKED_CAST")
    public fun addNamedCode(format: String, args: Map<String, *>): T =
      apply { body.addNamed(format, args) } as T

    @Suppress("UNCHECKED_CAST")
    public fun addStatement(format: String, vararg args: Any?): T =
      apply { body.addStatement(format, *args) } as T

    /**
     * @param controlFlow the control flow construct and its code, such as "if (foo == 5)".
     *   Shouldn't contain braces or newline characters.
     */
    @Suppress("UNCHECKED_CAST")
    public fun beginControlFlow(controlFlow: String, vararg args: Any?): T =
      apply { body.beginControlFlow(controlFlow, *args) } as T

    /**
     * @param controlFlow the control flow construct and its code, such as "else if (foo == 10)".
     *   Shouldn't contain braces or newline characters.
     */
    @Suppress("UNCHECKED_CAST")
    public fun nextControlFlow(controlFlow: String, vararg args: Any?): T =
      apply { body.nextControlFlow(controlFlow, *args) } as T

    @Suppress("UNCHECKED_CAST")
    public fun endControlFlow(): T = apply { body.endControlFlow() } as T

    @Suppress("UNCHECKED_CAST") public fun clearBody(): T = apply { body.clear() } as T
  }
}
