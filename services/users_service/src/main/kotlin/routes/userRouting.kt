package com.example.routes

import com.example.constants.ConflictException
import com.example.constants.UnauthorizedException
import com.example.data.repositories.BanRepository
import com.example.data.repositories.FollowRepository
import com.example.data.repositories.UserRepository
import com.example.hashing.HashingService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import com.example.security.TokenClaim
import com.example.security.TokenConfig
import com.example.security.TokenService
import users.request.*
import users.response.AuthResponse
import users.response.RecoveryResponse

object SupportRoutes {
    val tokenConfig = TokenConfig(
        issuer = "http://0.0.0.0:8082",
        audience = "all",
        expiresIn =  2L * 1000L * 60L * 60L * 24L,
        secretKey = "test"
    )
    const val BASE = "/api/v1"
}

fun Application.userRouting() {
    val userRepository: UserRepository by inject()
    val banRepository: BanRepository by inject()
    val followRepository: FollowRepository by inject()
    val hashingService: HashingService by inject()
    val tokenService: TokenService by inject()

    routing {
        post("/api/v1/register") {
            val req = call.receive<RegisterRequest>()

            if (userRepository.findByLogin(req.login) != null) {
                throw ConflictException("Login already exists")
            }

            val hashedPassword = hashingService.generateSaltedHash(req.password)

            userRepository.register(req, hashedPassword)

            val token = tokenService.generate(
                config = SupportRoutes.tokenConfig,
                TokenClaim(
                    name = "id",
                    value = userRepository.findByLogin(req.login)!!.id.toString()
                ),
                TokenClaim(
                    name = "role",
                    value = userRepository.findByLogin(req.login)!!.role.toString()
                )
            )

            call.respond(
                status = HttpStatusCode.OK,
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
                config = SupportRoutes.tokenConfig,
                TokenClaim(
                    name = "id",
                    value = userRepository.findByLogin(req.login)!!.id.toString()
                ),
                TokenClaim(
                    name = "role",
                    value = userRepository.findByLogin(req.login)!!.role.toString()
                )
            )
            call.respond(AuthResponse(token))
        }

        post("/api/v1/recovery") {
            val req = call.receive<RecoveryRequest>()
            val user = userRepository.findByLogin(req.login)
                ?: throw NotFoundException("User not found")

            call.respond(RecoveryResponse("Recovery link sent to email"))
        }

        authenticate {
            route("/api/v1") {
                route("/profile"){
                    get {
                        val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.NotFound)

                        val principal = call.principal<JWTPrincipal>()
                        val userLogin = principal?.getClaim("login", String::class) ?: throw UnauthorizedException()

                        val user = userRepository.findById(id.toLong())
                            ?: throw NotFoundException("User not found")

                        call.respond(user)
                    }

                    put {
                        val id = call.parameters["id"]?.toLong() ?: return@put call.respond(HttpStatusCode.NotFound)

                        val principal = call.principal<JWTPrincipal>()
                        principal?.getClaim("login", String::class) ?: throw UnauthorizedException()

                        val req = call.receive<UpdateProfileRequest>()

                        req.desc?.let {
                            userRepository.updateDesc(id, it)
                        }
                        req.avatar?.let {
                            userRepository.updateAvatar(id, it)
                        }
                        req.password?.let {
                            // require(it.length >= 8) { "Password too short" }
                            userRepository.updatePassword(id, hashingService.generateSaltedHash(it))
                        }

                        call.respond(HttpStatusCode.OK)
                    }
                }

                post("/report") {
                    val id = call.parameters["id"]?.toLong() ?: return@post call.respond(HttpStatusCode.NotFound)

                    val principal = call.principal<JWTPrincipal>()
                    val userLogin = principal?.getClaim("login", String::class) ?: throw UnauthorizedException()

                    //TODO to support service

                    call.respond(HttpStatusCode.Accepted)
                }

                post("/follow") {
                    val id = call.parameters["id"]?.toLong() ?: return@post call.respond(HttpStatusCode.NotFound)

                    val principal = call.principal<JWTPrincipal>()
                    val userLogin = principal?.getClaim("login", String::class) ?: throw UnauthorizedException()

                    if (userRepository.findById(id) == null) {
                        throw NotFoundException("Target user not found")
                    }
                    if (userRepository.findByLogin(userLogin) == null) {
                        throw NotFoundException("User not found")
                    }

                    val success = followRepository.follow(userRepository.findByLogin(userLogin)!!.id, id)
                    call.respond(if (success) HttpStatusCode.OK else HttpStatusCode.NoContent)
                }

                delete("/follow") {
                    val id = call.parameters["id"]?.toLong() ?: return@delete call.respond(HttpStatusCode.NotFound)

                    val principal = call.principal<JWTPrincipal>()
                    val userLogin = principal?.getClaim("login", String::class) ?: throw UnauthorizedException()

                    if (userRepository.findById(id) == null) {
                        throw NotFoundException("Target user not found")
                    }
                    if (userRepository.findByLogin(userLogin) == null) {
                        throw NotFoundException("User not found")
                    }

                    val success = followRepository.unfollow(userRepository.findByLogin(userLogin)!!.id, id)
                    call.respond(if (success) HttpStatusCode.OK else HttpStatusCode.NoContent)
                }

                post("/ban") {
                    val id = call.parameters["id"]?.toLong() ?: return@post call.respond(HttpStatusCode.NotFound)

                    val principal = call.principal<JWTPrincipal>()
                    val userLogin = principal?.getClaim("login", String::class) ?: throw UnauthorizedException()

                    val req = call.receive<BanRequest>()

                    if (userRepository.findById(id) == null) {
                        throw NotFoundException("Target user not found")
                    }
                    if (userRepository.findByLogin(userLogin) == null) {
                        throw NotFoundException("Moderator user not found")
                    }

                    banRepository.banUser(
                        targetUserId = id,
                        moderatorId = userRepository.findByLogin(userLogin)!!.id,
                        durationDays = req.duration,
                        message = req.message
                    )

                    call.respond(HttpStatusCode.OK)
                }

                delete("/ban") {
                    val id = call.parameters["id"]?.toLong() ?: return@delete call.respond(HttpStatusCode.NotFound)

                    val principal = call.principal<JWTPrincipal>()
                    val userLogin = principal?.getClaim("login", String::class) ?: throw UnauthorizedException()

                    if (userRepository.findById(id) == null) {
                        throw NotFoundException("Target user not found")
                    }
                    if (userRepository.findByLogin(userLogin) == null) {
                        throw NotFoundException("Moderator user not found")
                    }

                    banRepository.unbanUser(
                        targetUserId = id,
                        moderatorId = userRepository.findByLogin(userLogin)!!.id,
                    )

                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}
