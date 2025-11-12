package wallet

enum class PaymentType { CARD, DIGITAL_WALLET }

enum class SubscriptionStatus { ACTIVE, CANCELLED, FAILED }

enum class Currency { RUB, USD, EUR }

enum class WithdrawalStatus { PENDING, APPROVED, REJECTED }