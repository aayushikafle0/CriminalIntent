package com.example.criminalintent

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle

class MainActivity : AppCompatActivity() {

    private var pendingCrimeId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        pendingCrimeId = savedInstanceState?.getString("pending_crime")

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val mainView = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { view, insets ->
            val padding = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                        WindowInsetsCompat.Type.ime()
            )
            view.setPadding(
                padding.left,
                padding.top,
                padding.right,
                padding.bottom
            )
            insets
        }

        supportFragmentManager.setFragmentResultListener(
            "open_crime",
            this
        ) { _, result ->
            pendingCrimeId = result.getString("crime_id")
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                openPendingCrime()
            }
        }

        supportFragmentManager.addOnBackStackChangedListener {
            updateBackButton()
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(
                    R.id.fragment_container,
                    CrimeListFragment()
                )
                .commit()
        }

        updateBackButton()
    }

    override fun onResumeFragments() {
        super.onResumeFragments()
        openPendingCrime()
    }

    private fun openPendingCrime() {
        val crimeId = pendingCrimeId ?: return
        if (supportFragmentManager.isStateSaved) return

        val current = supportFragmentManager
            .findFragmentById(R.id.fragment_container)

        pendingCrimeId = null
        if (current !is CrimeListFragment) return

        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(
                R.id.fragment_container,
                CrimeDetailFragment.newInstance(crimeId)
            )
            .addToBackStack(null)
            .commit()
    }

    private fun updateBackButton() {
        supportActionBar?.setDisplayHomeAsUpEnabled(
            supportFragmentManager.backStackEntryCount > 0
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("pending_crime", pendingCrimeId)
        super.onSaveInstanceState(outState)
    }
}