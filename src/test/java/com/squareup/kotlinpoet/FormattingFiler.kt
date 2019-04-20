package com.squareup.kotlinpoet

import com.github.shyiko.ktlint.core.KtLint
import com.github.shyiko.ktlint.core.ParseException
import com.github.shyiko.ktlint.core.RuleExecutionException
import com.github.shyiko.ktlint.core.RuleSetProvider
import java.io.Writer
import java.util.ServiceLoader
import javax.annotation.processing.Filer
import javax.annotation.processing.Messager
import javax.lang.model.element.Element
import javax.tools.Diagnostic
import javax.tools.Diagnostic.Kind.ERROR
import javax.tools.Diagnostic.Kind.NOTE
import javax.tools.JavaFileObject

/**
 * Returns a decorating [Filer] implementation which [formats][KtLint.format] Kotlin source files
 * while writing them out and delegates to [this] for anything else.
 *
 * @receiver the filer to decorate.
 * @param ruleSetProviders optional custom [RuleSetProviders][RuleSetProvider] to use. Default is
 *                         to load via the [RuleSetProvider] SPI. Note we must accept providers
 *                         because individual [RuleSets][RuleSet] are not threadsafe.
 * @param userData optional custom user data mapping to supply. Default is to just specify 2 space
 *                 (continuation) indent sizes.
 * @param messager An optional [Messager] may be specified to make logs more visible.
 */
fun Filer.asFormatting(
    ruleSetProviders: Iterable<RuleSetProvider> = ServiceLoader.load(RuleSetProvider::class.java)
        .asIterable(),
    userData: Map<String, String> = mapOf(
        "indent_size" to "2",
        "continuation_indent_size" to "2"
    ),
    messager: Messager? = null
): Filer = FormattingFiler(this, ruleSetProviders, userData, messager)

private class FormattingFiler(
    private val delegate: Filer,
    private val ruleSetProviders: Iterable<RuleSetProvider>,
    private val userData: Map<String, String>,
    private val messager: Messager? = null
) : Filer by delegate {
  override fun createSourceFile(name: CharSequence,
      vararg originatingElements: Element): JavaFileObject {
    return FormattingJavaFileObject(
        delegate.createSourceFile(name, *originatingElements), ruleSetProviders, userData, messager)
  }
}

private class FormattingJavaFileObject(
    private val delegate: JavaFileObject,
    private val ruleSetProviders: Iterable<RuleSetProvider>,
    private val userData: Map<String, String>,
    private val messager: Messager?) : JavaFileObject by delegate {

  override fun openWriter(): Writer {
    val stringBuilder = StringBuilder(DEFAULT_FILE_SIZE)
    return object : Writer() {
      override fun write(chars: CharArray, start: Int, end: Int) {
        stringBuilder.append(chars, start, end - start)
      }

      override fun write(string: String) {
        stringBuilder.append(string)
      }

      override fun flush() {
      }

      override fun close() {
        val unformatted = stringBuilder.toString()
        try {
          val formatted = KtLint.format(
              unformatted,
              ruleSetProviders.map(RuleSetProvider::get),
              userData
          ) { _, _ ->
            // TODO in CR: Optionally record corrections here?
          }
          delegate.openWriter().use { writer -> writer.write(formatted) }
        } catch (exception: RuntimeException) {
          val diagnostic: Diagnostic.Kind
          val message: String
          when (exception) {
            is ParseException -> {
              message = "invalid syntax"
              diagnostic = ERROR
            }
            is RuleExecutionException -> {
              message = "rule error"
              diagnostic = NOTE
            }
            else -> {
              message = "unknown - ${exception.message}"
              diagnostic = NOTE
            }
          }
          // An exception will happen when the code being formatted has an error. It's better to
          // log the exception and emit unformatted code so the developer can view the code which
          // caused a problem.
          delegate.openWriter().use { writer -> writer.append(unformatted) }
          messager?.printMessage(diagnostic, "Error formatting ($message) $name")
        }
      }
    }
  }

  companion object {
    /** A rough estimate of the average file size: 80 chars per line, 500 lines. */
    private const val DEFAULT_FILE_SIZE = 80 * 500
  }
}
