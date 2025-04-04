/*
 * Copyright (C) 2025 The Nameless-CLO Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.nameless.settings.fragment

import android.app.settings.SettingsEnums.PAGE_UNKNOWN
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo.FLAG_SYSTEM
import android.content.pm.PackageManager.NameNotFoundException
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.ArraySet
import android.util.TypedValue
import android.view.Gravity
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast

import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.preference.Preference

import com.android.internal.util.nameless.HanziToPinyin

import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment

import com.android.settingslib.widget.TopIntroPreference

import com.google.android.material.appbar.AppBarLayout

abstract class BasePerAppConfigFragment : SettingsPreferenceFragment(), MenuItem.OnActionExpandListener {

    private val appBarLayout by lazy { requireActivity().findViewById<AppBarLayout>(R.id.app_bar)!! }

    private val emptyTextView by lazy { TextView(requireContext()) }

    private var searchItem: MenuItem? = null
    private var selectAllItem: MenuItem? = null
    private var deselectAllItem: MenuItem? = null
    private var resetItem: MenuItem? = null

    private val hanziToPinyin by lazy { HanziToPinyin.getInstance() }

    private val handlerThread by lazy {
        HandlerThread("BasePerAppConfigFragment-init").apply { start() }
    }
    private val handler by lazy { Handler(handlerThread.looper) }

    private val onBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            searchItem!!.collapseActionView()
            isEnabled = false
        }
    }

    val allAppData by lazy { collectApps() }
    val allPreference = mutableListOf<Preference>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.apply {
            title = resources.getString(getTitleResId())
        }
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handler.post {
            initPref(view.findViewById<ViewGroup>(android.R.id.list_container)!!)
        }
    }

    private fun initPref(listContainer: ViewGroup) {
        setLoading(true, false)

        // Show TopIntroPreference if resource id is valid
        if (getTopInfoResId() > 0) {
            requireContext().resources.getString(getTopInfoResId()).takeIf { !it.isNullOrBlank() }
                ?.let {
                    TopIntroPreference(prefContext).apply {
                        title = it
                    }.let { pref ->
                        preferenceScreen.addPreference(pref)
                    }
                }
        }

        // Rebuild the list of prefs
        allPreference.clear()
        allAppData.forEach { appData ->
            createAppPreference(prefContext, appData).let {
                allPreference.add(it)
                preferenceScreen.addPreference(it)
            }
        }

        emptyTextView.apply {
            gravity = Gravity.CENTER

            val textPadding = requireContext().resources.getDimensionPixelSize(R.dimen.empty_text_padding)
            setPadding(textPadding, 0, textPadding, 0)

            val value = TypedValue()
            requireContext().theme.resolveAttribute(android.R.attr.textAppearanceMedium, value, true)
            setTextAppearance(value.resourceId)

            text = getString(R.string.per_app_config_empty_text)
            isVisible = false

            val layoutHeight = requireContext().resources.getDimensionPixelSize(R.dimen.empty_text_layout_height)
            requireActivity().runOnUiThread {
                listContainer.addView(this, LayoutParams(LayoutParams.MATCH_PARENT, layoutHeight))
                requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackPressedCallback)
            }
        }

        requireActivity().runOnUiThread {
            setLoading(false, false)
            if (allAppData.isEmpty()) {
                emptyTextView.isVisible = true
            } else {
                searchItem?.setVisible(true)
                selectAllItem?.setVisible(getSelectAllRunnable() != null)
                deselectAllItem?.setVisible(getDeselectAllRunnable() != null)
                resetItem?.setVisible(getResetRunnable() != null)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.per_app_config_menu, menu)
        searchItem = menu.findItem(R.id.search)
        selectAllItem = menu.findItem(R.id.select_all)
        deselectAllItem = menu.findItem(R.id.deselect_all)
        resetItem = menu.findItem(R.id.reset)

        searchItem?.apply {
            setOnActionExpandListener(this@BasePerAppConfigFragment)
        }
        val searchView = searchItem!!.actionView as SearchView
        searchView.setQueryHint(getString(R.string.per_app_config_search))
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String) = false
            override fun onQueryTextChange(newText: String): Boolean {
                var hasVisiblePref = false
                allAppData.forEachIndexed { i, appData ->
                    allPreference[i].isVisible = newText.isNullOrEmpty() ||
                        appData.label.lowercase().contains(newText.lowercase())
                    hasVisiblePref = hasVisiblePref || allPreference[i].isVisible
                }
                emptyTextView.apply {
                    if (!hasVisiblePref) {
                        text = getString(R.string.per_app_config_no_result_text, newText)
                    }
                    isVisible = !hasVisiblePref
                }
                return true
            }
        })
    }

    override fun onMenuItemActionExpand(item: MenuItem): Boolean {
        appBarLayout.setExpanded(false, false)
        listView?.let {
            ViewCompat.setNestedScrollingEnabled(it, false)
        }
        onBackPressedCallback.isEnabled = true
        return true
    }

    override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
        appBarLayout.setExpanded(false, false)
        listView?.let {
            ViewCompat.setNestedScrollingEnabled(it, true)
        }
        onBackPressedCallback.isEnabled = false
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.select_all -> {
                getSelectAllRunnable()?.run()
                true
            }
            R.id.deselect_all -> {
                getDeselectAllRunnable()?.run()
                true
            }
            R.id.reset -> {
                getResetRunnable()?.run()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * @return the sorted list of AppData of all applications for current user,
     * with extra system applications defined in R.array.config_perAppConfAllowedSystemApps.
     */
    private fun collectApps(): List<AppData> {
        val apps = ArraySet<AppData>()
        packageManager.getInstalledPackages(0)?.forEach { pi ->
            pi.applicationInfo?.let { ai ->
                if ((ai.flags and FLAG_SYSTEM) == 0) {
                    val label = ai.loadLabel(packageManager).toString()
                    apps.add(AppData(
                        label,
                        pi.packageName,
                        if (label.isNotBlank()) hanziToPinyin.transliterate(label) else pi.packageName,
                        ai.uid
                    ))
                }
            }
        }
        if (getAllowedSystemAppListResId() > 0) {
            requireContext().resources.getStringArray(getAllowedSystemAppListResId())
                .forEach { app ->
                    try {
                        packageManager.getPackageInfo(app, 0).let { pi ->
                            pi.applicationInfo?.let { ai ->
                                val label = ai.loadLabel(packageManager).toString()
                                apps.add(AppData(
                                    label,
                                    app,
                                    if (label.isNotBlank()) hanziToPinyin.transliterate(label) else app,
                                    ai.uid
                                ))
                            }
                        }
                    } catch (e: NameNotFoundException) {}
                }
        }
        val launchableApps = packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }, 0
        ).map { it.activityInfo?.packageName }.toSet()
        val appList = mutableListOf<AppData>().also {
            it.addAll(apps.filter { app ->
                launchableApps.contains(app.packageName)
            })
            it.sortWith { a, b ->
                if (a.sortName != b.sortName) {
                    a.sortName.compareTo(b.sortName)
                } else {
                    a.packageName.compareTo(b.packageName)
                }
            }
        }
        return appList
    }

    protected fun getIcon(packageName: String): Drawable {
        var icon: Drawable? = null
        if (packageName != null) {
            try {
                icon = packageManager.getApplicationIcon(packageName)
            } catch (e: NameNotFoundException) {
            }
        }
        return icon ?: packageManager.defaultActivityIcon
    }

    override fun getMetricsCategory() = PAGE_UNKNOWN

    override fun getPreferenceScreenResId() = R.xml.per_app_config_screen

    abstract fun getTitleResId(): Int

    open fun getTopInfoResId() = 0

    open fun getAllowedSystemAppListResId() = R.array.config_perAppConfAllowedSystemApps

    abstract fun createAppPreference(prefContext: Context, appData: AppData): Preference

    open fun getSelectAllRunnable(): Runnable? = null

    open fun getDeselectAllRunnable(): Runnable? = null

    open fun getResetRunnable(): Runnable? = null

    fun showResetSuccessToast() {
        Toast.makeText(requireContext(), R.string.per_app_config_reset_success, Toast.LENGTH_LONG).show()
    }

    data class AppData(
        val label: String,
        val packageName: String,
        val sortName: String,
        val uid: Int
    )
}
