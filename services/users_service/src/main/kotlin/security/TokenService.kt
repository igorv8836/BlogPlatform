package security

interface TokenService {
    fun generate(
        config: TokenConfig,
        vararg tokenClaim: TokenClaim
    ): String
}