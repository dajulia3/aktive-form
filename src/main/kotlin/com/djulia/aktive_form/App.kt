package com.djulia.aktive_form

import com.djulia.aktive_form.ktor_support.ActiveFormUrlEncodedToContentTypeConverter
import com.djulia.aktive_form.ktor_support.RequestAttributes
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.streamProvider
import io.ktor.jackson.jackson
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.BaseApplicationEngine
import io.ktor.server.engine.EngineAPI
import io.ktor.server.engine.commandLineEnvironment
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.KtorExperimentalAPI
import org.slf4j.event.Level
import java.nio.charset.Charset

object App {
    @UseExperimental(KtorExperimentalAPI::class)
    @JvmStatic
    fun main(args: Array<String>) {
        startServer(wait = true, args = args, port = null)
    }
}

@UseExperimental(EngineAPI::class)
fun startServer(port: Int?, wait: Boolean, args: Array<String> = emptyArray()): BaseApplicationEngine {
    var mergedArgs = args
    if (port != null) {
        mergedArgs = arrayOf("-port=$port") + args
    }
    val server = embeddedServer(factory = Netty, environment = commandLineEnvironment(mergedArgs))

    server.start(wait = wait)
    return server
}

@Suppress("unused") // Referenced in application.conf
fun Application.module() {

    install(ContentNegotiation) {
        lateinit var mapper: ObjectMapper
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
            disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            registerModule(JavaTimeModule())
            registerModule(KotlinModule())
            disableDefaultTyping()

            mapper = this
        }

        val formConverter = ActiveFormUrlEncodedToContentTypeConverter(mapper)
        register(ContentType.Application.FormUrlEncoded, formConverter)
        register(ContentType.MultiPart.FormData, formConverter)
    }

    routing {

        post("/wizz-banger") {
            val wizzBanger: WizzBanger = call.receive()
            call.respond(HttpStatusCode.Created, wizzBanger)
        }

        post("/widget") {
            val widget = call.receive<Widget>()
            call.respond(HttpStatusCode.Created, widget)
        }

        post("/form-with-fileContents") {
            val attrs = call.attributes
            print(attrs)
            val form: Form = call.receive()
            val files: List<PartData.FileItem> = call.attributes[RequestAttributes.filesKey]

            files.first().streamProvider().use {
                val fileContents = it.readBytes().toString(Charset.defaultCharset())
                val formWithFile =
                    FormWithFileResp(name = form.name, tag = form.tag, fileContents = fileContents)
                call.respond(HttpStatusCode.Created, formWithFile)
            }
        }

        install(CallLogging) {
            level = Level.INFO
        }
    }
}

data class Form(val name: String, val tag: String)
data class FormWithFileResp(val name: String, val tag: String, val fileContents: String)

data class Fizzer(
    var fizziness: String,
    var temperature: Int
)

data class WizzBanger(var id: String, var name: String, var fizzer: Fizzer)

data class Widget(
    var name: String,
    val jimbo: List<String>,
    val age: Int
)
