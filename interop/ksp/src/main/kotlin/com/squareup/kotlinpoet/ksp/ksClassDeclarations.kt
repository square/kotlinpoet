package com.squareup.kotlinpoet.ksp

import com.google.devtools.ksp.isLocal
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName

/** Returns the [ClassName] representation of this [KSClassDeclaration]. */
@KotlinPoetKspPreview
public fun KSClassDeclaration.toClassName(): ClassName {
  require(!isLocal()) {
    "Local/anonymous classes are not supported!"
  }
  val pkgName = packageName.asString()
  val typesString = checkNotNull(qualifiedName).asString().removePrefix("$pkgName.")

  val simpleNames = typesString
    .split(".")
  return ClassName(pkgName, simpleNames)
}
