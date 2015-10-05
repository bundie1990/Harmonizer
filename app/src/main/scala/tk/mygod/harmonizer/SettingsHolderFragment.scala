package tk.mygod.harmonizer

import android.os.Bundle
import android.support.v7.preference.Preference
import tk.mygod.preference.{DropDownPreference, NumberPickerPreference, NumberPickerPreferenceDialogFragment, PreferenceFragmentPlus}

/**
 * @author Mygod
 */
final class SettingsHolderFragment extends PreferenceFragmentPlus {
  def onCreatePreferences(savedInstanceState: Bundle, rootKey: String) {
    getPreferenceManager.setSharedPreferencesName("settings")
    addPreferencesFromResource(R.xml.settings)
    val config = new AudioConfig(getActivity)
    val samplingRate = findPreference("audio.samplingRate").asInstanceOf[NumberPickerPreference]
    samplingRate.setMin(AudioConfig.minSamplingRate)
    samplingRate.setMax(AudioConfig.maxSamplingRate)
    samplingRate.setValue(config.getSamplingRate)
    findPreference("audio.bitDepth").asInstanceOf[DropDownPreference].setValue(config.getFormat.toString)
  }

  override def onDisplayPreferenceDialog(preference: Preference) =
    if (preference.isInstanceOf[NumberPickerPreference])
      displayPreferenceDialog(new NumberPickerPreferenceDialogFragment(preference.getKey))
    else super.onDisplayPreferenceDialog(preference)
}
