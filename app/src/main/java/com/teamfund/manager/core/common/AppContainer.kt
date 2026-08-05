package com.teamfund.manager.core.common

import android.content.Context
import com.teamfund.manager.core.datastore.UserPreferencesRepository
import com.teamfund.manager.data.local.database.TeamFundDatabase
import com.teamfund.manager.data.repository.TeamFundRepositoryImpl
import com.teamfund.manager.domain.repository.TeamFundRepository
import com.teamfund.manager.domain.usecase.CalculateExpenseSharesUseCase
import com.teamfund.manager.domain.usecase.CalculatePlayerBalancesUseCase
import com.teamfund.manager.domain.usecase.CalculateTeamBalanceUseCase

class AppContainer(private val context: Context) {

    val database: TeamFundDatabase by lazy {
        TeamFundDatabase.getInstance(context)
    }

    val repository: TeamFundRepository by lazy {
        TeamFundRepositoryImpl(database)
    }

    val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(context)
    }

    val calculateExpenseSharesUseCase by lazy { CalculateExpenseSharesUseCase() }
    val calculatePlayerBalancesUseCase by lazy { CalculatePlayerBalancesUseCase() }
    val calculateTeamBalanceUseCase by lazy { CalculateTeamBalanceUseCase() }
}
