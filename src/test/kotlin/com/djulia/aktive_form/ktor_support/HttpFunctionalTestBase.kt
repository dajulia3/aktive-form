package com.djulia.aktive_form.ktor_support

import io.ktor.server.engine.ApplicationEngine
import io.restassured.RestAssured
import io.restassured.response.ResponseBodyExtractionOptions
import io.restassured.specification.RequestSpecification
import org.junit.jupiter.api.BeforeAll
import java.io.File
import java.io.IOException
import java.net.ConnectException
import java.net.Socket
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

abstract class ServerTest {

    protected fun RequestSpecification.When(): RequestSpecification {
        return this.`when`()
    }

    protected inline fun <reified T> ResponseBodyExtractionOptions.toObject(): T {
        return this.`as`(T::class.java)
    }

    companion object {

        private var serverStarted = false

        private lateinit var server: ApplicationEngine

        @BeforeAll
        @JvmStatic
        @Suppress("unused")
        fun startServer() {
            if (!serverStarted) {
                val host = "0.0.0.0"
                var port = System.getProperty("serverPort")?.toInt() ?: 8888
                val runServerInSeparateProcess = System.getProperty("runServerInSeparateProcess")

                if (runServerInSeparateProcess?.equals("true") == true) {
                    runServerAsSeparateProcessWithGraalTracerAgent(
                        port,
                        host
                    )
                } else {
                    server =
                        startServer(port = port, wait = false)
                    Runtime.getRuntime().addShutdownHook(Thread { server.stop(0, 0, TimeUnit.SECONDS) })
                }

                waitForServerToStart(host, port)

                serverStarted = true

                RestAssured.baseURI = "http://$host"
                RestAssured.port = port
            }
        }

        private fun runServerAsSeparateProcessWithGraalTracerAgent(port: Int, host: String) {
            val configOutputDir = System.getProperty("configOutputDir")
                ?: throw Exception("JVM property 'configOutputDir' must be set for this to work!")

            val process = execClassAsProcess(
                clazz = App::class.java,
                jvmArgs = listOf(
                    "-agentlib:native-image-agent=config-output-dir=$configOutputDir"
                ),
                args = listOf("-port=$port")
            )

            waitForServerToStart(host, port)

            Runtime.getRuntime().addShutdownHook(Thread {
                if (process.isAlive) {
                    process.destroy()
                }
            })
        }

        fun waitForServerToStart(host: String, port: Int) {
            val startTime = Instant.now()
            val timeOut = Duration.of(2, ChronoUnit.SECONDS)
            var elapsedTime = Duration.ZERO

            while (timeOut.minus(elapsedTime).toMillis() > 0) {
                try {
                    val s = Socket(host, port)
                    s.close()
                    return

                } catch (e: ConnectException) {
                }
                elapsedTime = Duration.between(startTime, Instant.now())
            }

            throw Exception(
                "Timed out before the server started. " +
                        "The server has not come online on port $port before the timeout of ${timeOut.seconds} seconds"
            )
        }

        @Throws(IOException::class, InterruptedException::class)
        private fun execClassAsProcess(
            clazz: Class<*>,
            args: List<String> = emptyList(),
            jvmArgs: List<String> = emptyList()
        ): Process {
            val javaHome = System.getProperty("java.home")
            val javaBin = javaHome + File.separator + "bin" + File.separator + "java"
            val classpath = System.getProperty("java.class.path")
            val className = clazz.name
            val command = ArrayList<String>()
            command.add(javaBin)
            command.addAll(jvmArgs)
            command.add("-cp")
            command.add(classpath)
            command.add(className)
            command.addAll(args)

            val builder = ProcessBuilder(command)
            return builder.inheritIO().start() ?: throw Exception("Unexpected error starting process. Exiting.")
        }
    }


}
