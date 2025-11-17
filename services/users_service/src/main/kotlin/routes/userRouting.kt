package com.example.routes

import com.example.ClientConfig
import com.example.clients.SupportServiceClient
import com.example.clients.WalletServiceClient
import com.example.commonPlugins.TokenClaim
import com.example.commonPlugins.TokenService
import com.example.config.ServiceConfig
import com.example.constants.ConflictException
import com.example.constants.UnauthorizedException
import com.example.createServiceHttpClient
import com.example.data.repositories.BanRepository
import com.example.data.repositories.FollowRepository
import com.example.data.repositories.UserRepository
import com.example.hashing.HashingService
import com.example.utils.tokenOrNull
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.basicProperties
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.basicPublish
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.rabbitmq
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import notification.NotificationType
import notification.request.NotificationAsyncRequest
import org.koin.ktor.ext.inject
import payment.request.PaymentAsyncRequest
import posts.request.ComplaintRequest
import users.request.*
import users.response.AuthResponse
import users.response.RecoveryResponse
import java.util.UUID
import kotlin.getValue

const val NOTIFICATION_ROUTING_KEY = "notification-service"

fun Application.userRouting(config: ServiceConfig) {
    val userRepository: UserRepository by inject()
    val banRepository: BanRepository by inject()
    val followRepository: FollowRepository by inject()
    val hashingService: HashingService by inject()
    val tokenService: TokenService by inject()

    val walletServiceClient by inject<WalletServiceClient>()
    val supportServiceClient by inject<SupportServiceClient>()

    routing {
        post("/api/v1/register") {
            val req = call.receive<RegisterRequest>()

            if (userRepository.findByLogin(req.login) != null) {
                throw ConflictException("Login already exists")
            }

            val hashedPassword = hashingService.generateSaltedHash(req.password)

           userRepository.register(req, hashedPassword)

            val token = tokenService.generate(
                config = config,
                TokenClaim(
                    name = "id",
                    value = userRepository.findByLogin(req.login)!!.id.toString()
                ),
                TokenClaim(
                    name = "role",
                    value = userRepository.findByLogin(req.login)!!.role.toString()
                )
            )

            val response: HttpStatusCode = WalletServiceClient(
                createServiceHttpClient(ClientConfig(baseUrl = "http://0.0.0.0:8084"))
            ).createWallet(
                jwtToken = token
            )

            call.respond(
                status = response,
                message = AuthResponse(
                    token = token
                )
            )

        }

        post("/api/v1/login") {
            val req = call.receive<AuthRequest>()

            val user = userRepository.findByLogin(req.login)
                ?: throw UnauthorizedException("Invalid credentials")

            if (!hashingService.verify(req.password, userRepository.getSaltedHash(req.login))) {
                throw UnauthorizedException("Invalid credentials")
            }

            val token = tokenService.generate(
                config = config,
                TokenClaim(
                    name = "id",
                    value = userRepository.findByLogin(req.login)!!.id.toString()
                ),
                TokenClaim(
                    name = "role",
                    value = userRepository.findByLogin(req.login)!!.role.toString()
                )
            )

            rabbitmq {
                basicPublish {
                    exchange = config.ktor.rabbitmq.exchange
                    routingKey = NOTIFICATION_ROUTING_KEY
                    properties = basicProperties {
                        correlationId = UUID.randomUUID().toString()
                        replyTo = config.ktor.jwt.audience
                    }
                    message {
                        NotificationAsyncRequest.CreateNotificationRequest(
                            targetUserId = user.id,
                            message = "Вы вошли в свой аккаунт",
                            notificationType = NotificationType.AUTH
                        ) as NotificationAsyncRequest
                    }
                }
            }

            call.respond(AuthResponse(token))
        }

        post("/api/v1/recovery") {
            val req = call.receive<RecoveryRequest>()
            val user = userRepository.findByLogin(req.login)
                ?: throw NotFoundException("User not found")

            call.respond(RecoveryResponse("Recovery link sent to email"))
        }

        authenticate("jwt") {
            route("/api/v1") {
                route("/profile"){
                    get {
                        val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.NotFound)

                        val principal = call.principal<JWTPrincipal>()
                        val userId = principal?.getClaim("id", String::class) ?: throw UnauthorizedException()

                        val user = userRepository.findById(id.toLong())
                            ?: throw NotFoundException("User not found")

                        call.respond(user)
                    }

                    put {
                        val id = call.parameters["id"]?.toLong() ?: return@put call.respond(HttpStatusCode.NotFound)

                        val principal = call.principal<JWTPrincipal>()
                        principal?.getClaim("id", String::class) ?: throw UnauthorizedException()

                        val req = call.receive<UpdateProfileRequest>()

                        req.desc?.let {
                            userRepository.updateDesc(id, it)
                        }
                        req.avatar?.let {
                            userRepository.updateAvatar(id, it)
                        }
                        req.password?.let {
                            userRepository.updatePassword(id, hashingService.generateSaltedHash(it))
                        }

                        call.respond(HttpStatusCode.OK)
                    }
                }

                post("/report") {
                    val id = call.parameters["id"]?.toLong() ?: return@post call.respond(HttpStatusCode.NotFound)

                    val token: String = call.tokenOrNull() ?: throw BadRequestException("token is null")

                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.getClaim("id", String::class) ?: throw UnauthorizedException()


                    val request = call.receive<ComplaintRequest>()

                    val response = supportServiceClient.complaint(
                        subject = "Report user with id=$id",
                        body = request.reason,
                        jwtToken = token
                    )

                    call.respond(response)
                }

                post("/follow") {
                    val id = call.parameters["id"]?.toLong() ?: return@post call.respond(HttpStatusCode.NotFound)

                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.getClaim("id", String::class) ?: throw UnauthorizedException()

                    if (userRepository.findById(id) == null) {
                        throw NotFoundException("Target user not found")
                    }
                    if (userRepository.findById(userId.toLong()) == null) {
                        throw NotFoundException("User not found")
                    }

                    val success = followRepository.follow(userId.toLong(), id)
                    call.respond(if (success) HttpStatusCode.OK else HttpStatusCode.NoContent)
                }

                get("/followers") {
                    val id = call.parameters["id"]?.toLong() ?: return@get call.respond(HttpStatusCode.NotFound)

                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.getClaim("id", String::class) ?: throw UnauthorizedException()

                    if (userRepository.findById(id) == null) {
                        throw NotFoundException("Target user not found")
                    }

                    val success = followRepository.getFollowers(userId.toLong())
                    call.respond(success)
                }

                delete("/follow") {
                    val id = call.parameters["id"]?.toLong() ?: return@delete call.respond(HttpStatusCode.NotFound)

                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.getClaim("id", String::class) ?: throw UnauthorizedException()

                    if (userRepository.findById(id) == null) {
                        throw NotFoundException("Target user not found")
                    }
                    if (userRepository.findById(userId.toLong()) == null) {
                        throw NotFoundException("User not found")
                    }

                    val success = followRepository.unfollow(userId.toLong(), id)
                    call.respond(if (success) HttpStatusCode.OK else HttpStatusCode.NoContent)
                }

                post("/ban") {
                    val id = call.parameters["id"]?.toLong() ?: return@post call.respond(HttpStatusCode.NotFound)

                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.getClaim("id", String::class) ?: throw UnauthorizedException()

                    val req = call.receive<BanRequest>()

                    if (userRepository.findById(id) == null) {
                        throw NotFoundException("Target user not found")
                    }
                    if (userRepository.findById(userId.toLong()) == null) {
                        throw NotFoundException("Moderator user not found")
                    }

                    banRepository.banUser(
                        targetUserId = id,
                        moderatorId = userId.toLong(),
                        durationDays = req.duration,
                        message = req.message
                    )

                    call.respond(HttpStatusCode.OK)
                }

                delete("/ban") {
                    val id = call.parameters["id"]?.toLong() ?: return@delete call.respond(HttpStatusCode.NotFound)

                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.getClaim("id", String::class) ?: throw UnauthorizedException()

                    if (userRepository.findById(id) == null) {
                        throw NotFoundException("Target user not found")
                    }
                    if (userRepository.findById(userId.toLong()) == null) {
                        throw NotFoundException("Moderator user not found")
                    }

                    banRepository.unbanUser(
                        targetUserId = id,
                        moderatorId = userId.toLong(),
                    )

                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}
