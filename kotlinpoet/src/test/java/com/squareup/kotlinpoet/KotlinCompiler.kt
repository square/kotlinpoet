/*
 * Copyright (C) 2019 Square, Inc.
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

import java.io.File
import java.nio.file.Path
import java.util.ArrayList
import java.util.concurrent.atomic.AtomicInteger
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.Services

object KotlinCompiler {
    fun compile(kotlinSourceDirectory: Path): MessageCollector {
        val compilerOutputDir = File(kotlinSourceDirectory.toAbsolutePath().toString() + "-output")
        compilerOutputDir.mkdirs()
        compilerOutputDir.deleteOnExit()

        // System.out.println("kotlinSourceDirectory=" + kotlinSourceDirectory.toString())
        // System.out.println("compilerOutputDir=" + compilerOutputDir.toString())

        val freeArgList = ArrayList<String>()
        freeArgList.add(kotlinSourceDirectory.toString())
        val msgCollector = MessageCollectorImpl()
        val compiler = K2JVMCompiler()
        val args = K2JVMCompilerArguments()
        args.freeArgs = freeArgList
        args.classpath = System.getProperty("java.class.path")
        args.compileJava = true
        args.noStdlib = true
        args.noReflect = true
        args.destination = compilerOutputDir.getAbsolutePath()
        val exitCode = compiler.exec(msgCollector, Services.EMPTY, args)
        if (exitCode != ExitCode.OK) {
            System.err.println("KotlinCompiler: exitCode=" + exitCode)
        }
        return msgCollector
    }

    class MessageCollectorImpl : MessageCollector {
        private val errorCount = AtomicInteger(0)
        override fun hasErrors(): Boolean { return (errorCount.get() > 0) }
        override fun clear() { errorCount.set(0) }
        override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation?) {
            if (severity.isError) {
                errorCount.incrementAndGet()
                System.err.println("KotlinTestHelper ERROR: " + message + " " + location)
            }
        }
    }
}
