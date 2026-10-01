package com.example.criminalintent

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CrimeListViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val crimeDao =
        CrimeDatabase.getInstance(application).crimeDao()

    val crimes = crimeDao.getCrimes().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun addCrime(onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val crime = Crime()
            crimeDao.addCrime(crime)
            onCreated(crime.id)
        }
    }
}