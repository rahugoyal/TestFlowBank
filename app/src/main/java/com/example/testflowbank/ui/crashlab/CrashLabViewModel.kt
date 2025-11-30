package com.example.testflowbank.ui.crashlab

import androidx.lifecycle.ViewModel
import com.example.testflowbank.core.crash.GlobalCoroutineErrorHandler
import com.example.testflowbank.core.logging.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

data class CrashLabUiState(
    val lastAction: String? = null,
    val lastError: String? = null
)

@HiltViewModel
class CrashLabViewModel @Inject constructor(
    private val logger: AppLogger
) : ViewModel() {

    private val vmScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate + GlobalCoroutineErrorHandler
    )

    private val _state = MutableStateFlow(CrashLabUiState())
    val state = _state.asStateFlow()

    init {
        vmScope.launch {
            logger.screenView()
            logger.journeyStep(

                step = "OPEN_CRASH_LAB",
                detail = "User opened Crash Lab module"
            )
        }
    }

    fun onScenarioClick(scenario: CrashScenario) {
        when (scenario) {
            CrashScenario.NPE_CRASH -> triggerNpeCrash()
            CrashScenario.ILLEGAL_STATE_CRASH -> triggerIllegalStateCrash()
            CrashScenario.ANR_FREEZE -> triggerAnrFreeze()
            CrashScenario.JSON_PARSE_ERROR -> triggerJsonParseError()
        }
    }

    private fun updateLastAction(text: String) {
        _state.value = _state.value.copy(
            lastAction = text,
            lastError = null
        )
    }

    private fun updateLastError(text: String) {
        _state.value = _state.value.copy(
            lastError = text
        )
    }

    /**
     * Uncaught NullPointerException crash.
     * Global CrashReportingExceptionHandler will log it as CRASH.
     */
    private fun triggerNpeCrash() {
        vmScope.launch {
            logger.userAction(
                actionName = "CLICK_SCENARIO",
                target = "CrashLabCard",
                detail = "scenario=NPE_CRASH"
            )
            logger.journeyStep(
                step = "TRIGGER_NPE_CRASH",
                detail = "Forced NullPointerException"
            )
        }

        updateLastAction("Triggering forced NullPointerException crash…")
        // Crash on main thread → uncaught, handled by default handler + your crash logger
        throw NullPointerException("Forced NPE from CrashLab scenario")
    }

    /**
     * Uncaught IllegalStateException crash.
     */
    private fun triggerIllegalStateCrash() {
        vmScope.launch {
            logger.userAction(
                actionName = "CLICK_SCENARIO",
                target = "CrashLabCard",
                detail = "scenario=ILLEGAL_STATE_CRASH"
            )
            logger.journeyStep(
                step = "TRIGGER_ILLEGAL_STATE_CRASH",
                detail = "Forced IllegalStateException"
            )
        }

        updateLastAction("Triggering forced IllegalStateException crash…")
        throw IllegalStateException("Forced IllegalStateException from CrashLab scenario")
    }

    /**
     * Simulate ANR by blocking main thread for a few seconds.
     * Your ANR watchdog should detect and log it (type=ANR).
     */
    private fun triggerAnrFreeze() {
        vmScope.launch {
            logger.userAction(
                actionName = "CLICK_SCENARIO",
                target = "CrashLabCard",
                detail = "scenario=ANR_FREEZE"
            )
            logger.journeyStep(
                step = "TRIGGER_ANR_FREEZE",
                detail = "Blocking main thread to simulate ANR"
            )

            updateLastAction("Blocking main thread for ~8s to simulate ANR…")

            // Block main thread to simulate ANR – DO NOT use in production apps.
            try {
                Thread.sleep(8000L)
            } catch (_: InterruptedException) {
                // ignore
            }
        }
    }

    /**
     * Handled JSON parsing error (no crash, only logs).
     * Good for showing ERROR vs CRASH distinction.
     */
    private fun triggerJsonParseError() {
        vmScope.launch {
            logger.userAction(
                actionName = "CLICK_SCENARIO",
                target = "CrashLabCard",
                detail = "scenario=JSON_PARSE_ERROR"
            )
            logger.journeyStep(
                step = "TRIGGER_JSON_PARSE_ERROR",
                detail = "Simulating malformed JSON parsing"
            )

            updateLastAction("Simulating JSON parse error (handled, no crash)…")

            try {
                // This will throw a JSONException
                val badJson = "{ invalid-json: }"
                JSONObject(badJson)
            } catch (t: Throwable) {
                val msg = "Handled JSON parse error: ${t.message ?: t.toString()}"

                logger.error(
                    message = msg,
                    api = "localJsonParsing",
                    throwable = t,
                    action = "JSON_PARSE_ERROR"
                )

                updateLastError(msg)
            }
        }
    }
}