# Privacy

HzLab is designed to work entirely offline.

- **Microphone:** used only while the app is in the foreground, to analyse the pitch of an instrument or voice.
- **Audio handling:** samples are processed in memory on the device. HzLab does not record, save, upload or share audio.
- **Network:** the app has no `INTERNET` permission and contains no ads, analytics, crash reporting or tracking SDKs.
- **Storage:** only local preferences (tuning, temperament and display settings) are stored. No audio is stored.
- **Permissions:** `RECORD_AUDIO` is required for tuning. `MODIFY_AUDIO_SETTINGS` is used only to ensure microphone input is available on devices that mute it in certain audio modes.

If you find a privacy issue, please open an issue in the repository.
