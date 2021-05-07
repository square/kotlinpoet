package com.squareup.kotlinpoet

import com.google.common.truth.ThrowableSubject
import com.google.common.truth.Truth.assertThat

inline fun <reified T> assertThrows(block: () -> Unit): ThrowableSubject {
  try {
    block()
  } catch (e: Throwable) {
    if (e is T) {
      return assertThat(e)
    } else {
      throw e
    }
  }
  throw AssertionError("Expected ${T::class.simpleName}")
}
