package com.djulia.aktive_form.ktor_support

import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import uk.co.datumedge.hamcrest.json.SameJSONAs

class KtorFormSerializationFunctionalTest : ServerTest() {

    @Test
    fun `processes basic form data according to rails param conventions`() {
        RestAssured.given()
            .contentType(ContentType.URLENC)
            .formParam("name", "wizzy woozy")
            .formParam("jimbo", "jambo")
            .formParam("jimbo", 12345)
            .formParam("age", 21)
            .When()
            .post("/widget")
            .then()
            .statusCode(201)
            .body(
                SameJSONAs.sameJSONAs(
                    """
                        {
                            "name": "wizzy woozy",
                            "jimbo":["jambo","12345"],
                            "age": 21
                        }
                        """.trimIndent()
                )
            )
    }

    @Test
    fun `processes multipart form data with file uploads correctly`() {
        val documentContents = "Hello World!"
        RestAssured.given()
            .multiPart("document", "document.txt", documentContents.byteInputStream())
            .formParam("name", "Greeting Doc")
            .formParam("tag", "cool-doc")
            .When()
            .post("/form-with-fileContents")
            .then()
            .statusCode(201)
            .body(
                SameJSONAs.sameJSONAs(
                    """
                        {
                            "name": "Greeting Doc",
                            "fileContents": "$documentContents",
                            "tag": "cool-doc"
                        }
                        """.trimIndent()
                )
            )
    }

    @Test
    fun `deals with nested objects like Rack & Rails does`() {
        val result = RestAssured.given()
            .contentType(ContentType.URLENC)
            .accept(ContentType.JSON)
            .formParam("id", "ABC123U&ME_GRL")
            .formParam("wizzbanger[name]", "Whiz Kid") // Now do we need the naming convention???
            .formParam("wizzbanger[fizzer][fizziness]", "really fizzy") // Now do we need the naming convention???
            .formParam("wizzbanger[fizzer][temperature]", 98) // Now do we need the naming convention???
            .When()
            .post("/wizz-banger")
            .then()
            .statusCode(201)
            .body(
                SameJSONAs.sameJSONAs(
                    """
                        {
                            "id":"ABC123U&ME_GRL",
                            "name":"Whiz Kid",
                            "fizzer":{"fizziness":"really fizzy", "temperature": 98}
                        }
                    """.trimIndent()
                )
            )
            .extract()
            .toObject<WizzBanger>()

        expectThat(result).isEqualTo(
            WizzBanger(
                id = "ABC123U&ME_GRL",
                name = "Whiz Kid",
                fizzer = Fizzer(fizziness = "really fizzy", temperature = 98)
            )
        )
    }
}
