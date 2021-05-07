package com.squareup.kotlinpoet

public enum class KOperator(
  internal val operator: String,
  internal val functionName: String
) {
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
