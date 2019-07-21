#AktiveForm  

Better form processing for Ktor inspired by Ruby on Rails/Rack's form handling. Bind forms to kotlin data classes. Just follow the Ruby on Rails naming convention for form fields. You will still have access to File data parts via request attributes. 

###Installation
```$kotlin
fun Application.module() {
    install(ContentNegotiation) {
        lateinit var mapper: ObjectMapper
        jackson {
            disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            registerModule(JavaTimeModule())
            mapper = this
        }

        //register your own jackson mapper since we customized it above
        //that way the form binding respects our customizations.
        val formConverter = ActiveFormUrlEncodedToContentTypeConverter(mapper)
        register(ContentType.Application.FormUrlEncoded, formConverter)
        register(ContentType.MultiPart.FormData, formConverter)
    }
}
```

###Handling form submissions
```$kotlin
data class Fizzer(
    var fizziness: String,
    var temperature: Int
)
data class WizzBanger(var id: String, var name: String, var fizzer: Fizzer)
    
fun Application.module() {
    routing {
        post("/wizz-banger") {
            val wizzBanger: WizzBanger = call.receive()
            call.respond(HttpStatusCode.Created, wizzBanger)
        }

    }
}
```

###Handling forms with file uploads 
```$kotlin
data class Form(val name: String, val tag: String)
data class FormWithFileResp(val name: String, val tag: String, val fileContents: String)
    
fun Application.module() {
    routing {
        post("/form-with-fileContents") {
            val attrs = call.attributes
            val form: Form = call.receive() //just use plain call.receive()
            
            //still have access to Files via the filesKey attribute 
            val files: List<PartData.FileItem> = call.attributes[RequestAttributes.filesKey]

            files.first().streamProvider().use {
                val fileContents = it.readBytes().toString(Charset.defaultCharset())
                val formWithFile =
                    FormWithFileResp(name = form.name, tag = form.tag, fileContents = fileContents)
                call.respond(HttpStatusCode.Created, formWithFile)
            }
        }
    }
}
``` 