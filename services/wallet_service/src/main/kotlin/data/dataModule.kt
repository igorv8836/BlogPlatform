package data

import data.repositories.PaymentMethodRepository
import data.repositories.SubscriptionRepository
import data.repositories.WalletRepository
import data.repositories.WithdrawalRequestRepository
import org.koin.dsl.module

internal fun dataModule() = module {
    single { WalletRepository() }
    single { PaymentMethodRepository() }
    single { WithdrawalRequestRepository() }
    single { SubscriptionRepository() }
}