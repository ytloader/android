package io.github.ytloader.download

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** 서비스 ↔ UI가 공유하는 다운로드 상태. 프로세스가 죽으면 사라진다. */
class DownloadRepository {
    private val _state = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val state: StateFlow<DownloadState> = _state.asStateFlow()

    fun set(state: DownloadState) {
        _state.value = state
    }

    /** 진행 중이 아닐 때만 Preparing으로 바꾼다. 바꿨으면 true. */
    fun tryBegin(): Boolean {
        var begun = false
        _state.update { current ->
            if (current.isActive) current else DownloadState.Preparing.also { begun = true }
        }
        return begun
    }

    fun acknowledgeFailure() {
        _state.update { if (it is DownloadState.Failed) it.copy(acknowledged = true) else it }
    }

    /** 진행 중이 아니면 결과 표시를 지우고 대기 상태로. */
    fun clearResult() {
        _state.update { if (it.isActive) it else DownloadState.Idle }
    }

    /** 닫은 오류 대화상자를 다시 연다. */
    fun reopenFailure() {
        _state.update { if (it is DownloadState.Failed) it.copy(acknowledged = false) else it }
    }
}
