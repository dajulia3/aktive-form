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

The html form for this endpoint should look like this:
```html
<form action="/wizz-banger" method="post">
    <input type="text" name="wizzbanger[id]"/>
    <input type="text" name="wizzbanger[name]"/>
    <input type="text" name="wizzbanger[fizzer][fizziness]"/>
    <input type="text" name="wizzbanger[fizzer][temperature]"/>
    <input type="submit">
 </form>
```

#### Ommitting classnames for top level files
For convenience sake, you can omit the class name for top-level fields. 
This is convenient when you don't have any nested objects.
```html
<form action="/wizz-banger" method="post">
    <input type="text" name="id"/>
    <input type="text" name="name"/>
    
    <!-- still need the class name for attributes of child objects --> 
    <input type="text" name="wizzbanger[fizzer][fizziness]"/>
    <input type="text" name="wizzbanger[fizzer][temperature]"/>
    <input type="submit">
 </form>
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