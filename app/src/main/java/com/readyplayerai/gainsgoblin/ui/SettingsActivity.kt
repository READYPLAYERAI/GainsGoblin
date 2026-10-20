package com.readyplayerai.gainsgoblin.ui

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.Toast

import androidx.activity.viewModels
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager

import com.readyplayerai.gainsgoblin.R
import com.readyplayerai.gainsgoblin.ui.compose.BottomNavTab
import com.readyplayerai.gainsgoblin.ui.compose.setupBottomNavBar

import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        setupBottomNavBar(BottomNavTab.SETTINGS)
    }

    /**
     * Clears workouts through [SettingsViewModel] and invokes [cb] once the
     * clear has completed, so the caller can toast and restart on fresh data.
     */
    fun clearWorkouts(cb: () -> Unit) {
        lifecycleScope.launch {
            viewModel.clearWorkouts()
            cb()
        }
    }

    class SettingsFragment : PreferenceFragmentCompat(), PreferenceManager.OnPreferenceTreeClickListener {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
        }

        override fun onPreferenceTreeClick(preference: Preference?): Boolean {
            val key = preference!!.getKey()

            // Backup & Restore
            if (key == "importcsv") {
                // Prepare to show exercise dialog box
                val inflater = LayoutInflater.from(getContext())
                val view = inflater.inflate(R.layout.import_warning_dialog, null)
                val alertDialog = AlertDialog.Builder(getContext()!!).setView(view).create()

                val bt_yes3 = view.findViewById<Button>(R.id.bt_yes3)
                val bt_no3 = view.findViewById<Button>(R.id.bt_no3)

                bt_yes3.setOnClickListener { view ->
                    val intent = Intent(getActivity(), MainActivity::class.java)
                    intent.putExtra("doit", "importcsv")
                    startActivity(intent)
                }

                bt_no3.setOnClickListener { view ->
                    alertDialog.dismiss()
                }
                alertDialog.show()
            } else if (key == "exportcsv") {
                val intent = Intent(getActivity(), MainActivity::class.java)
                intent.putExtra("doit", "exportcsv")
                startActivity(intent)
            } else if (key == "deletedata") {
                deleteData()
            } else if (key == "theme") {
                // General
                Toast.makeText(getContext(), "Dark Theme not implemented yet", Toast.LENGTH_SHORT).show()
            } else if (key == "github") {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/READYPLAYERAI/GainsGoblin"))
                startActivity(browserIntent)
            } else if (key == "version") {
                Toast.makeText(getContext(), "Nothing to see here", Toast.LENGTH_SHORT).show()
            } else if (key == "privacy_policy") {
                // Prepare to show exercise dialog box
                val inflater = LayoutInflater.from(getContext())
                val view = inflater.inflate(R.layout.privacy_policy_dialog, null)
                val alertDialog = AlertDialog.Builder(getContext()!!).setView(view).create()

                alertDialog.show()
            } else if (key == "licence") {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.gnu.org/licenses/gpl-3.0.en.html"))
                startActivity(browserIntent)
            }
            return true
        }

        // Delete all currently saved workout data
        fun deleteData() {
            // Prepare to show exercise dialog box
            val inflater = LayoutInflater.from(getContext())
            val view = inflater.inflate(R.layout.delete_all_dialog, null)
            val alertDialog = AlertDialog.Builder(getContext()!!).setView(view).create()

            val bt_yes = view.findViewById<Button>(R.id.bt_yes3)
            val bt_no = view.findViewById<Button>(R.id.bt_no3)

            bt_yes.setOnClickListener { view ->
                (activity as? SettingsActivity)?.clearWorkouts {
                    alertDialog.dismiss()
                    Toast.makeText(getContext(), "Data Deleted", Toast.LENGTH_SHORT).show()
                    val intent = Intent(getContext()!!, MainActivity::class.java)
                    startActivity(intent)
                }
            }

            bt_no.setOnClickListener { view ->
                alertDialog.dismiss()
            }

            // Show Exercise Dialog Box
            alertDialog.show()
        }
    }
}