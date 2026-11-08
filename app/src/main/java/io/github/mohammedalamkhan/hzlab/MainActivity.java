// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mohammedalamkhan.hzlab;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.util.List;

public final class MainActivity extends Activity implements HzLabView.Listener, PitchEngine.Listener {
    private static final int REQUEST_MIC = 17;

    private Settings settings;
    private HzLabView view;
    private PitchEngine engine;
    private boolean resumed;
    private boolean demo;
    private boolean askedThisSession;

    private int appliedTheme = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        appliedTheme = Settings.readTheme(this);
        setTheme(appliedTheme == Theme.DAYLIGHT ? R.style.Theme_HzLab_Light : R.style.Theme_HzLab);
        super.onCreate(savedInstanceState);
        settings = Settings.get(this);
        Theme.apply(settings.themeMode);
        int demoMode = getIntent().getIntExtra("demo_mode", -1);
        if (demoMode >= 0 && demoMode < HzLabView.MODE_NAMES.length) {
            settings.viewMode = demoMode;
        }
        demo = (getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
                && getIntent().getBooleanExtra("demo_audio", false);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(Theme.bgTop);
        getWindow().setNavigationBarColor(Theme.bgBottom);
        updateSystemBarIcons();

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Theme.bgBottom);
        view = new HzLabView(this, settings, this);
        root.addView(view, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);
        enterImmersive();

        applyOrientation();
        if (demo) {
            view.setPermissionNeeded(false);
        } else if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            view.setPermissionNeeded(false);
        } else {
            view.setPermissionNeeded(true);
            requestMic();
        }

        if (!settings.welcomeShown) {
            showFirstRunHelp();
        }
    }

    private void enterImmersive() {
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getDecorView().getWindowInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.systemBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            enterImmersive();
        }
    }

    private void requestMic() {
        if (askedThisSession) {
            view.setPermissionNeeded(true);
            return;
        }
        askedThisSession = true;
        requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, REQUEST_MIC);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_MIC) {
            return;
        }
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            view.setPermissionNeeded(false);
            startEngine();
        } else {
            view.setPermissionNeeded(true);
            if (!shouldShowRequestPermissionRationale(android.Manifest.permission.RECORD_AUDIO)) {
                showPermissionSettings();
            }
        }
    }

    private void showPermissionSettings() {
        new AlertDialog.Builder(this)
                .setTitle("Microphone access needed")
                .setMessage("Enable the Microphone permission for HzLab in Android Settings, then return to the app. Audio is processed only on this device.")
                .setPositiveButton("Open settings", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(android.net.Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void startEngine() {
        stopEngine();
        engine = new PitchEngine(this, settings, this);
        engine.setDemo(demo);
        view.setAudioStarted(false);
        engine.start();
    }

    private void stopEngine() {
        if (engine != null) {
            engine.stop();
            engine = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (settings != null && appliedTheme != settings.themeMode) {
            recreate();
            return;
        }
        resumed = true;
        Theme.apply(settings.themeMode);
        updateSystemBarIcons();
        enterImmersive();
        applyOrientation();
        view.refreshSettings();
        settings.ensureValidSelection();
        if (view.getMode() != settings.viewMode) {
            view.setMode(settings.viewMode, false);
        }
        if (demo) {
            view.setPermissionNeeded(false);
            startEngine();
        } else if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            view.setPermissionNeeded(false);
            startEngine();
        } else {
            view.setPermissionNeeded(true);
        }
    }

    @Override
    protected void onPause() {
        resumed = false;
        stopEngine();
        settings.viewMode = view.getMode();
        settings.save(this);
        super.onPause();
    }

    private void updateSystemBarIcons() {
        int flags = getWindow().getDecorView().getSystemUiVisibility();
        if (Theme.light) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        } else {
            flags &= ~(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
        getWindow().getDecorView().setSystemUiVisibility(flags);
    }

    private void applyOrientation() {
        switch (settings.screenOrientation) {
            case 1:
                setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                break;
            case 2:
                setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;
            default:
                setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR);
                break;
        }
    }

    @Override
    public void onRequestOptions() {
        showViewMenu();
    }

    private void showViewMenu() {
        final String[] items = new String[HzLabView.MODE_NAMES.length + 4];
        for (int i = 0; i < HzLabView.MODE_NAMES.length; i++) {
            items[i] = titleCase(HzLabView.MODE_NAMES[i]);
        }
        items[HzLabView.MODE_NAMES.length] = "Quick presets";
        items[HzLabView.MODE_NAMES.length + 1] = "Metronome";
        items[HzLabView.MODE_NAMES.length + 2] = "Reference tone";
        items[HzLabView.MODE_NAMES.length + 3] = "Options";
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Views & tools");
        b.setSingleChoiceItems(items, view.getMode(), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                if (which < HzLabView.MODE_NAMES.length) {
                    view.setMode(which, true);
                } else if (which == HzLabView.MODE_NAMES.length) {
                    showPresetMenu();
                } else if (which == HzLabView.MODE_NAMES.length + 1) {
                    startActivity(new Intent(MainActivity.this, MetronomeActivity.class));
                } else if (which == HzLabView.MODE_NAMES.length + 2) {
                    startActivity(new Intent(MainActivity.this, ToneActivity.class));
                } else {
                    openOptions();
                }
            }
        });
        b.show();
    }

    private void showPresetMenu() {
        final List<Tuning> all = InstrumentLibrary.withCustom(settings.customTunings);
        String[] labels = new String[all.size()];
        int selected = 0;
        for (int i = 0; i < all.size(); i++) {
            labels[i] = all.get(i).displayName() + "   (" + all.get(i).noteText(settings.noteStyle) + ")";
            if (all.get(i).name.equals(settings.instrumentName)) {
                selected = i;
            }
        }
        new AlertDialog.Builder(this)
                .setTitle("Quick presets")
                .setSingleChoiceItems(labels, selected, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        settings.instrumentName = all.get(which).name;
                        settings.save(MainActivity.this);
                        view.setMode(HzLabView.MODE_INSTRUMENT, true);
                        view.refreshSettings();
                        Toast.makeText(MainActivity.this, all.get(which).displayName(), Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static String titleCase(String value) {
        String lower = value.toLowerCase(java.util.Locale.US);
        StringBuilder sb = new StringBuilder();
        boolean start = true;
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if (start && Character.isLetter(c)) {
                sb.append(Character.toUpperCase(c));
                start = false;
            } else {
                sb.append(c);
                if (c == ' ' || c == '&') {
                    start = true;
                }
            }
        }
        return sb.toString();
    }

    private void openOptions() {
        startActivity(new Intent(this, OptionsActivity.class));
    }

    @Override
    public void onRequestPermission() {
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            view.setPermissionNeeded(false);
            view.setAudioError(null);
            startEngine();
            return;
        }
        if (shouldShowRequestPermissionRationale(android.Manifest.permission.RECORD_AUDIO)) {
            new AlertDialog.Builder(this)
                    .setTitle("Microphone access")
                    .setMessage("HzLab uses the microphone only to analyse the pitch of your instrument. Audio is processed on the device and is never recorded or uploaded.")
                    .setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            requestMic();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            requestMic();
        }
    }

    @Override
    public void onModeChanged(int mode) {
        settings.viewMode = mode;
        settings.viewedMask |= 1 << mode;
        settings.save(this);
    }

    @Override
    public void onPitch(PitchResult result) {
        view.setPitch(result);
    }

    @Override
    public void onAudioError(String message) {
        if (message != null && message.toLowerCase(java.util.Locale.US).contains("permission")) {
            view.setPermissionNeeded(true);
        } else {
            view.setAudioError(message);
        }
    }

    @Override
    public void onAudioStarted(int sampleRate) {
        view.setAudioStarted(true);
        if (sampleRate != settings.sampleRate) {
            Toast.makeText(this, "Using " + sampleRate + " Hz microphone input", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            openOptions();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void showFirstRunHelp() {
        settings.welcomeShown = true;
        settings.save(this);
        new AlertDialog.Builder(this)
                .setTitle("HzLab")
                .setMessage("Swipe left or right to change view.\n\n"
                        + "Tap the three dots or long-press the display to open the view menu and settings.\n\n"
                        + "The Instrument view follows the selected tuning and shows a radial strobe, string indicators and an exact cents readout.")
                .setPositiveButton("Start tuning", null)
                .show();
    }
}
