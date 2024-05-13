package org.qosp.notes.ui.sync

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import org.qosp.notes.R
import org.qosp.notes.databinding.FragmentSyncSettingsBinding
import org.qosp.notes.preferences.AppPreferences
import org.qosp.notes.preferences.PreferenceRepository
import org.qosp.notes.ui.common.BaseFragment
import org.qosp.notes.ui.settings.SettingsViewModel
import org.qosp.notes.ui.settings.showPreferenceDialog
import org.qosp.notes.ui.sync.nextcloud.NextcloudAccountDialog
import org.qosp.notes.ui.sync.nextcloud.NextcloudServerDialog
import org.qosp.notes.ui.utils.StorageLocationContract
import org.qosp.notes.ui.utils.collect
import org.qosp.notes.ui.utils.liftAppBarOnScroll
import org.qosp.notes.ui.utils.viewBinding

@AndroidEntryPoint
class SyncSettingsFragment : BaseFragment(R.layout.fragment_sync_settings) {
    private val binding by viewBinding(FragmentSyncSettingsBinding::bind)
    private val model: SettingsViewModel by activityViewModels()

    override val hasMenu = false
    override val toolbar: Toolbar
        get() = binding.layoutAppBar.toolbar
    override val toolbarTitle: String
        get() = getString(R.string.preferences_header_syncing)

    private var appPreferences = AppPreferences()
    private var nextcloudUrl = ""
    private var storageLocation: Uri? = null

    private val locationListener = registerForActivityResult(StorageLocationContract) { uri ->
        uri?.let {
            model.setEncryptedString(PreferenceRepository.STORAGE_LOCATION, it.toString())
            Log.i(TAG, "Storing location: $it")
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context?.contentResolver?.takePersistableUriPermission(it, takeFlags)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.model = model
        binding.lifecycleOwner = this
        binding.scrollView.liftAppBarOnScroll(
            binding.layoutAppBar.appBar,
            requireContext().resources.getDimension(R.dimen.app_bar_elevation)
        )

        setupPreferenceObservers()
        setupSyncServiceListener()
        setupSyncModeListener()
        setupBackgroundSyncListener()
        setupNewNotesSyncableListener()

        setupNextcloudServerListener()
        setupNextcloudAccountListener()
        setupClearNextcloudCredentialsListener()

        setupLocalLocationListener()
    }


    private fun setupPreferenceObservers() {
        model.appPreferences.collect(viewLifecycleOwner) { appPreferences = it }

        // ENCRYPTED
        model.getEncryptedString(PreferenceRepository.NEXTCLOUD_INSTANCE_URL).collect(viewLifecycleOwner) {
            nextcloudUrl = it
            binding.settingNextcloudServer.subText =
                nextcloudUrl.ifEmpty { getString(R.string.preferences_nextcloud_set_server_url) }
        }

        model.loggedInUsername.collect(viewLifecycleOwner) {
            binding.settingNextcloudAccount.subText = if (it != null) {
                getString(R.string.indicator_nextcloud_currently_logged_in_as, it)
            } else {
                getString(R.string.preferences_nextcloud_set_your_credentials)
            }
        }

        model.getEncryptedString(PreferenceRepository.STORAGE_LOCATION).collect(viewLifecycleOwner) { u ->
            val uri = Uri.parse(u)
            val pm = context?.packageManager
            storageLocation = uri
            val appName = pm?.getInstalledPackages(PackageManager.GET_PROVIDERS)
                ?.firstOrNull { it?.providers?.any { p -> p?.authority == uri.authority } ?: false }
                ?.applicationInfo
                ?.let { pm.getApplicationLabel(it) }?.toString() ?: uri.authority
            binding.settingStorageLocation.subText = appName ?: getString(R.string.preferences_file_storage_select)
        }
    }

    private fun setupLocalLocationListener() = binding.settingStorageLocation.setOnClickListener {
        locationListener.launch(storageLocation)
    }

    private fun setupNextcloudServerListener() = binding.settingNextcloudServer.setOnClickListener {
        NextcloudServerDialog.build(nextcloudUrl).show(childFragmentManager, null)
    }

    private fun setupNextcloudAccountListener() = binding.settingNextcloudAccount.setOnClickListener {
        NextcloudAccountDialog().show(childFragmentManager, null)
    }

    private fun setupSyncServiceListener() = binding.settingSyncProvider.setOnClickListener {
        showPreferenceDialog(R.string.preferences_cloud_service, appPreferences.cloudService) { selected ->
            model.setPreference(selected)
        }
    }

    private fun setupSyncModeListener() = binding.settingSyncMode.setOnClickListener {
        showPreferenceDialog(R.string.preferences_sync_when_on, appPreferences.syncMode) { selected ->
            model.setPreference(selected)
        }
    }

    private fun setupBackgroundSyncListener() = binding.settingBackgroundSync.setOnClickListener {
        showPreferenceDialog(R.string.preferences_background_sync, appPreferences.backgroundSync) { selected ->
            model.setPreference(selected)
        }
    }

    private fun setupNewNotesSyncableListener() = binding.settingNotesSyncableByDefault.setOnClickListener {
        showPreferenceDialog(
            R.string.preferences_new_notes_synchronizable,
            appPreferences.newNotesSyncable
        ) { selected ->
            model.setPreference(selected)
        }
    }

    private fun setupClearNextcloudCredentialsListener() = binding.settingNextcloudClearCredentials.setOnClickListener {
        model.clearNextcloudCredentials()
    }
}
