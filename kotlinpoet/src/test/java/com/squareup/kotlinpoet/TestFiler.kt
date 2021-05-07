package com.squareup.kotlinpoet

import java.io.OutputStream
import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path
import javax.annotation.processing.Filer
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ElementVisitor
import javax.lang.model.element.Modifier
import javax.lang.model.element.Name
import javax.lang.model.type.TypeMirror
import javax.tools.FileObject
import javax.tools.JavaFileManager
import javax.tools.JavaFileObject
import javax.tools.SimpleJavaFileObject

internal class TestFiler(
  fileSystem: FileSystem,
  private val fileSystemRoot: Path
) : Filer {

  internal inner class Source(private val path: Path) :
    SimpleJavaFileObject(path.toUri(), JavaFileObject.Kind.SOURCE) {
    override fun openOutputStream(): OutputStream {
      val parent = path.parent
      if (!Files.exists(parent)) fileSystemProvider.createDirectory(parent)
      return fileSystemProvider.newOutputStream(path)
    }
  }

  private val separator = fileSystem.separator
  private val fileSystemProvider = fileSystem.provider()
  private val originatingElementsMap = mutableMapOf<Path, List<Element>>()

  fun getOriginatingElements(path: Path) = originatingElementsMap[path] ?: throw NullPointerException("Could not find $path")

  override fun createSourceFile(
    name: CharSequence,
    vararg originatingElements: Element
  ): JavaFileObject {
    val relative = name.toString().replace(".", separator) + ".kt" // Assumes well-formed.
    val path = fileSystemRoot.resolve(relative)
    originatingElementsMap[path] = originatingElements.toList()
    return Source(path)
  }

  override fun createClassFile(name: CharSequence, vararg originatingElements: Element) =
    throw UnsupportedOperationException("Not implemented.")

  override fun createResource(
    location: JavaFileManager.Location,
    pkg: CharSequence,
    relativeName: CharSequence,
    vararg originatingElements: Element
  ): FileObject {
    val relative = pkg.toString().replace(".", separator) + separator + relativeName
    val path = fileSystemRoot.resolve(relative)
    originatingElementsMap[path] = originatingElements.toList()
    return Source(path)
  }

  override fun getResource(
    location: JavaFileManager.Location,
    pkg: CharSequence,
    relativeName: CharSequence
  ) = throw UnsupportedOperationException("Not implemented.")
}

internal class FakeElement : Element {

  override fun getModifiers(): MutableSet<Modifier> {
    TODO()
  }

  override fun getSimpleName(): Name {
    TODO()
  }

  override fun getKind(): ElementKind {
    TODO()
  }

  override fun asType(): TypeMirror {
    TODO()
  }

  override fun getEnclosingElement(): Element {
    TODO()
  }

  override fun <R : Any?, P : Any?> accept(v: ElementVisitor<R, P>?, p: P): R {
    TODO()
  }

  override fun <A : Annotation?> getAnnotationsByType(annotationType: Class<A>?): Array<A> {
    TODO()
  }

  override fun <A : Annotation?> getAnnotation(annotationType: Class<A>?): A {
    TODO()
  }

  override fun getAnnotationMirrors(): MutableList<out AnnotationMirror> {
    TODO()
  }

  override fun getEnclosedElements(): MutableList<out Element> {
    TODO()
  }
}
