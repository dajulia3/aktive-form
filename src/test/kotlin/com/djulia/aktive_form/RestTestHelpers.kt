package com.djulia.aktive_form

import io.restassured.response.ResponseBodyExtractionOptions
import io.restassured.specification.RequestSpecification

fun RequestSpecification.When(): RequestSpecification {
    return this.`when`()
}

// allows response.toType<Widget>() -> Widget instance
inline fun <reified T> ResponseBodyExtractionOptions.toType(): T {
    return this.`as`(T::class.java)
}
