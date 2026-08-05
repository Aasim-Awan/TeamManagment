package com.teamfund.manager.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.teamfund.manager.data.local.entity.ExpenseEntity
import com.teamfund.manager.data.local.entity.ExpenseShareEntity

data class ExpenseWithShares(
    @Embedded val expense: ExpenseEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "expenseId"
    )
    val shares: List<ExpenseShareEntity>
)
