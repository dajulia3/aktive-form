package com.djulia.aktive_form

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.features.ContentConverter
import io.ktor.http.ContentType
import io.ktor.http.content.PartData
import io.ktor.http.content.streamProvider
import io.ktor.request.ApplicationReceiveRequest
import io.ktor.request.isMultipart
import io.ktor.request.receiveMultipart
import io.ktor.util.AttributeKey
import io.ktor.util.KtorExperimentalAPI
import io.ktor.util.pipeline.PipelineContext
import io.ktor.util.toByteArray
import kotlinx.coroutines.io.ByteReadChannel
import java.io.InputStream
import java.net.URLEncoder

object RequestAttributes {
    val filesKey = AttributeKey<List<PartData.FileItem>>("files")
}

class ActiveFormUrlEncodedToContentTypeConverter(private val objectMapper: ObjectMapper) : ContentConverter {
    @KtorExperimentalAPI
    @UseExperimental(ExperimentalStdlibApi::class)
    override suspend fun convertForReceive(context: PipelineContext<ApplicationReceiveRequest, ApplicationCall>): Any? {
        val request = context.subject
        val channel = request.value as? ByteReadChannel ?: return null

        val translator = FormParamsToJsonTranslator(objectMapper)

        val formBody: String = if (context.isMultipartRequest()) {
            processFormToStringAndSaveFilesAsAttributes(context)
        } else {
            channel.toByteArray().decodeToString()
        }

        val json = translator.jsonFromFormBody(formBody)

        //TODO: eventually make this nonblocking?
        return objectMapper.readValue(json, request.type.javaObjectType)
    }

    private fun PipelineContext<ApplicationReceiveRequest, ApplicationCall>.isMultipartRequest(): Boolean {
        return this.context.request.isMultipart()
    }

    suspend fun ApplicationCall.receiveFileAsInputStream(): InputStream? {
        val multipartData = receiveMultipart()

        var part: PartData?
        do {
            part = multipartData.readPart()
            if (part is PartData.FileItem) {
                return part.streamProvider().buffered()
            }
        } while (part != null)

        return null
    }

    private suspend fun processFormToStringAndSaveFilesAsAttributes(context: PipelineContext<ApplicationReceiveRequest, ApplicationCall>): String {
        val formItemParts = mutableListOf<PartData.FormItem>()
        val files = mutableListOf<PartData.FileItem>()
        val multipartData = context.call.receiveMultipart()

        var part: PartData?
        do {
            part = multipartData.readPart()
            when (part) {
                is PartData.FileItem -> {
                    files.add(part)
                }
                is PartData.FormItem -> {
                    formItemParts.add(part)
                }
            }
        } while (part != null)

        context.call.attributes.put(RequestAttributes.filesKey, files)

        return formItemParts.map {
            "${URLEncoder.encode(it.name, "UTF-8")}=${URLEncoder.encode(it.value, "UTF-8")}"
        }.joinToString("&")
    }

    override suspend fun convertForSend(
        context: PipelineContext<Any, ApplicationCall>,
        contentType: ContentType,
        value: Any
    ): Any? {
        throw NotImplementedError("not implemented: we generally do not return form-url-encoded data in web apps, so this is not a priority to implement")
    }

}
