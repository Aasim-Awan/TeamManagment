package com.teamfund.manager.core.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Runs one save at a time so a double-tap on a Save button cannot insert duplicate rows.
 * Screens observe [isSaving] to disable their Save button while a save is in progress.
 */
class SaveGuard {
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    fun launch(scope: CoroutineScope, block: suspend CoroutineScope.() -> Unit) {
        if (!_isSaving.compareAndSet(expect = false, update = true)) return
        scope.launch {
            try {
                block()
            } finally {
                _isSaving.value = false
            }
        }
    }
}
