package clients

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*


open class UsersServiceClient(
    private val httpClient: HttpClient
) {
    open suspend fun followAuthor(
        authorId: Long,
        jwtToken: String
    ): HttpStatusCode {
        try {
            val response: HttpResponse = httpClient.post("/api/v1/follow") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $jwtToken")
                    append(HttpHeaders.ContentType, ContentType.Application.Json)
                }
                url {
                    parameters.append("id", "$authorId")
                }
            }
            return response.status

        } catch (_: Exception) {
            return HttpStatusCode.BadRequest
        }
    }

    open suspend fun unfollowAuthor(
        authorId: Long,
        jwtToken: String
    ): HttpStatusCode {
        try {
            val response: HttpResponse = httpClient.delete("/api/v1/follow") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $jwtToken")
                    append(HttpHeaders.ContentType, ContentType.Application.Json)
                }
                url {
                    parameters.append("id", "$authorId")
                }
            }
            return response.status

        } catch (_: Exception) {
            return HttpStatusCode.BadRequest
        }
    }
}