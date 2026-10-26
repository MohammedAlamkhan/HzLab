// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mohammedalamkhan.hzlab;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

public final class MainActivity extends Activity implements HzLabView.Listener, PitchEngine.Listener {
    private static final int REQUEST_MIC = 17;

    private Settings settings;
    private HzLabView view;
    private PitchEngine engine;
    private boolean resumed;
    private boolean demo;
    private boolean askedThisSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = Settings.get(this);
        int demoMode = getIntent().getIntExtra("demo_mode", -1);
        if (demoMode >= 0 && demoMode < HzLabView.MODE_NAMES.length) {
            settings.viewMode = demoMode;
        }
        demo = (getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
                && getIntent().getBooleanExtra("demo_audio", false);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
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
        resumed = true;
        enterImmersive();
        applyOrientation();
        view.refreshSettings();
        settings.ensureValidSelection();
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
        final String[] items = new String[HzLabView.MODE_NAMES.length + 1];
        for (int i = 0; i < HzLabView.MODE_NAMES.length; i++) {
            items[i] = titleCase(HzLabView.MODE_NAMES[i]);
        }
        items[items.length - 1] = "Options";
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("HzLab Views");
        b.setSingleChoiceItems(items, view.getMode(), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                if (which < HzLabView.MODE_NAMES.length) {
                    view.setMode(which, true);
                } else {
                    openOptions();
                }
            }
        });
        b.show();
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
