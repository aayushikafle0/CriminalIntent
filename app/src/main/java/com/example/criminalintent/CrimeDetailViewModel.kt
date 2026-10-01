package com.example.criminalintent

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class CrimeDetailUiState(
    val crime: Crime? = null,
    val busy: Boolean = false,
    val finished: Boolean = false,
    val error: String? = null
)

class CrimeDetailViewModel(
    application: Application,
    private val savedState: SavedStateHandle
) : AndroidViewModel(application) {

    private val crimeDao =
        CrimeDatabase.getInstance(application).crimeDao()

    private val _uiState = MutableStateFlow(CrimeDetailUiState())
    val uiState = _uiState.asStateFlow()

    var pendingPhotoName: String?
        get() = savedState["pending_photo"]
        set(value) {
            savedState["pending_photo"] = value
        }

    init {
        viewModelScope.launch {
            try {
                val id = savedState.get<String>("crime_id")
                val stored = id?.let {
                    crimeDao.getCrime(it).first()
                }

                if (stored == null) {
                    _uiState.value = CrimeDetailUiState(
                        error = "This crime could not be found."
                    )
                } else {
                    val draft = stored.copy(
                        title = savedState["draft_title"] ?: stored.title,
                        date = savedState["draft_date"] ?: stored.date,
                        isSolved = savedState["draft_solved"]
                            ?: stored.isSolved,
                        photoFileName = if (savedState.contains("draft_photo")) {
                            savedState["draft_photo"]
                        } else {
                            stored.photoFileName
                        }
                    )
                    _uiState.value = CrimeDetailUiState(crime = draft)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.value = CrimeDetailUiState(
                    error = "Unable to load this crime."
                )
            }
        }
    }

    fun setTitle(title: String) {
        updateDraft { it.copy(title = title) }
    }

    fun setDate(date: Long) {
        updateDraft { it.copy(date = date) }
    }

    fun setSolved(solved: Boolean) {
        updateDraft { it.copy(isSolved = solved) }
    }

    fun setPhoto(fileName: String) {
        savedState["draft_photo"] = fileName
        updateDraft { it.copy(photoFileName = fileName) }
    }

    private fun updateDraft(change: (Crime) -> Crime) {
        val current = _uiState.value
        val crime = current.crime ?: return
        if (current.busy || current.finished) return

        val updated = change(crime)
        savedState["draft_title"] = updated.title
        savedState["draft_date"] = updated.date
        savedState["draft_solved"] = updated.isSolved
        savedState["draft_photo"] = updated.photoFileName
        _uiState.value = current.copy(crime = updated, error = null)
    }

    fun saveCrime() {
        finishCrime(delete = false)
    }

    fun deleteCrime() {
        finishCrime(delete = true)
    }

    private fun finishCrime(delete: Boolean) {
        val current = _uiState.value
        val crime = current.crime ?: return
        if (current.busy || current.finished) return

        _uiState.value = current.copy(busy = true, error = null)

        viewModelScope.launch {
            try {
                if (delete) {
                    crimeDao.deleteCrime(crime)
                } else {
                    crimeDao.updateCrime(crime)
                }
                _uiState.value = _uiState.value.copy(
                    busy = false,
                    finished = true
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    busy = false,
                    error = "Unable to save changes. Please try again."
                )
            }
        }
    }
}