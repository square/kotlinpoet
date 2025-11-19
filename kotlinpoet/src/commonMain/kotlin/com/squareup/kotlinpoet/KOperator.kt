/*
 * Copyright (C) 2020 Square, Inc.
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

public enum class KOperator(internal val operator: String, internal val functionName: String) {
  UNARY_PLUS("+", "unaryPlus"),
  PLUS("+", "plus"),
  UNARY_MINUS("-", "unaryMinus"),
  MINUS("-", "minus"),
  TIMES("*", "times"),
  DIV("/", "div"),
  REM("%", "rem"),
  PLUS_ASSIGN("+=", "plusAssign"),
  MINUS_ASSIGN("-=", "minusAssign"),
  TIMES_ASSIGN("*=", "timesAssign"),
  DIV_ASSIGN("/=", "divAssign"),
  REM_ASSIGN("%=", "remAssign"),
  INC("++", "inc"),
  DEC("--", "dec"),
  EQUALS("==", "equals"),
  NOT_EQUALS("!=", "equals"),
  NOT("!", "not"),
  RANGE_TO("..", "rangeTo"),
  CONTAINS("in", "contains"),
  NOT_CONTAINS("!in", "contains"),
  GT(">", "compareTo"),
  LT("<", "compareTo"),
  GE(">=", "compareTo"),
  LE("<=", "compareTo"),
  ITERATOR("in", "iterator"),
}
