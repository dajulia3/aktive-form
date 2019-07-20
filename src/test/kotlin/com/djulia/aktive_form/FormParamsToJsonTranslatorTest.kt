package com.djulia.aktive_form

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import strikt.api.Assertion
import strikt.api.expectThat
import uk.co.datumedge.hamcrest.json.SameJSONAs.sameJSONAs

data class Fizzer(
    var fizziness: String? = null,
    var temperature: Int? = null
)

data class WizzBanger(var id: String? = null, var name: String? = null, var fizzer: Fizzer? = null)

class FormParamsToJsonTranslatorTest {
    private val translator =
        FormParamsToJsonTranslator(objectMapper = objectMapper())

    @Test
    @DisplayName("it creates nested json Objects when params follow naming convention of outerObj[innerObj][field]")
    fun createsNestedJson() {
        val params =
            "notConventionNameParam=woohoo&" +
                    "car[make]=BMW&" +
                    "car[model]=330i&" +
                    "car[features][]=windows&" + "car[features][]=doors&" +
                    "car[engine][id]=12345&" +
                    "car[engine][volume]=2276cc&" +
                    "car[engine][cylinders]=4"

        val result = translator.jsonFromFormBody(params)

        expectThat(result).isSameJsonAs("""
            {
                "notConventionNameParam": "woohoo",
                "features": ["windows", "doors"],
                "make": "BMW",
                "model": "330i",
                "engine": {
                    "id": "12345",
                    "volume": "2276cc",
                    "cylinders": "4"
                }
            }
        """.trimIndent())
    }

    @Test
    fun `it returns an empty json object for an empty body`(){
        val result = translator.jsonFromFormBody("")
        expectThat(result).isSameJsonAs("{}")
    }

    @Test
    fun `it can return arrays of one element`() {
        val params = "zoo[animals][]=elephant"

        val result = translator.jsonFromFormBody(params)

        expectThat(result).isSameJsonAs(
            """
            { 
                "animals": ["elephant"]
            }
        """.trimIndent()
        )
    }

    @Test
    fun `it creates deeply nested json`() {
        val params =
            "icecream[cone][wrapper]=paper&" +
                    "icecream[cone][flavors][][name]=vanilla&" +
                    "icecream[cone][flavors][][topping]=cherry&" +
                    "icecream[cone][flavors][][name]=chocolate&" +
                    "icecream[cone][flavors][][topping]=caramel&" +
                    "icecream[topping][sprinkles][type]=rainbow"

        val result = translator.jsonFromFormBody(params)

        expectThat(result).isSameJsonAs(
            """
            { 
                "cone": { 
                        "wrapper": "paper", 
                        "flavors":[
                                    {"name":"vanilla", "topping": "cherry"}, 
                                    {"name":"chocolate", "topping": "caramel"}
                                  ] 
                },
             "topping": {"sprinkles": {"type": "rainbow"} }
            }
        """.trimIndent()
        )
    }

    @Test
    fun `always interprets repeated non-rails style param names as as arrays`() {
        val params = "discount=50%25&" + "car[features][]=windows&" + "car[features][]=doors&" +
                "dealerships=SuperAuto&" + "dealerships=MegaAuto"

        val result = translator.jsonFromFormBody(params)

        expectThat(result).isSameJsonAs("""
            {
                "discount": "50%",
                "features": ["windows", "doors"],
                "dealerships": ["SuperAuto", "MegaAuto"]
            }
        """.trimIndent())
    }
}

private fun Assertion.Builder<String>.isSameJsonAs(expected: String): Assertion.Builder<String> =
    assert(description = "is same json", expected = expected) {
        val matcher = sameJSONAs(expected)
        when (matcher.matches(it)) {
            true -> pass()
            false -> fail(actual = it, description = "json did not match")
        }
    }


private fun objectMapper() : ObjectMapper {
    return ObjectMapper().apply {
        registerModule(KotlinModule())
        registerModule(JavaTimeModule())
    }
}


