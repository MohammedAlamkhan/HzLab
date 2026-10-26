// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mohammedalamkhan.hzlab;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** User-defined instrument and temperament editors. */
public final class CustomEditors {
    private CustomEditors() {
    }

    public static void editInstrumentPicker(final Activity activity, final Settings settings, final Runnable onChange) {
        final List<Tuning> custom = settings.customTunings;
        if (custom.isEmpty()) {
            new AlertDialog.Builder(activity)
                    .setTitle("Custom Instrument")
                    .setMessage("No custom instruments yet. Create one by entering a name and up to 12 notes (for example: D2 A2 D3 G3 B3 E4).")
                    .setPositiveButton("New", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            showInstrumentEditor(activity, settings, null, onChange);
                        }
                    })
                    .setNegativeButton("Close", null)
                    .show();
            return;
        }
        String[] labels = new String[custom.size()];
        for (int i = 0; i < custom.size(); i++) {
            labels[i] = custom.get(i).displayName() + "   (" + custom.get(i).noteText(settings.noteStyle) + ")";
        }
        new AlertDialog.Builder(activity)
                .setTitle("Custom Instrument")
                .setItems(labels, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        settings.instrumentName = custom.get(which).name;
                        settings.save(activity);
                        onChange.run();
                    }
                })
                .setPositiveButton("New", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showInstrumentEditor(activity, settings, null, onChange);
                    }
                })
                .setNeutralButton("Edit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        chooseInstrumentToEdit(activity, settings, onChange, false);
                    }
                })
                .setNegativeButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        chooseInstrumentToEdit(activity, settings, onChange, true);
                    }
                })
                .show();
    }

    private static void chooseInstrumentToEdit(final Activity activity, final Settings settings,
                                               final Runnable onChange, final boolean delete) {
        final List<Tuning> custom = settings.customTunings;
        String[] labels = new String[custom.size()];
        for (int i = 0; i < custom.size(); i++) {
            labels[i] = custom.get(i).displayName();
        }
        new AlertDialog.Builder(activity)
                .setTitle(delete ? "Delete Instrument?" : "Edit Instrument")
                .setItems(labels, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final Tuning tuning = custom.get(which);
                        if (delete) {
                            new AlertDialog.Builder(activity)
                                    .setTitle("Delete Instrument?")
                                    .setMessage("Delete custom instrument '" + tuning.displayName()
                                            + "'? This operation cannot be undone.")
                                    .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface d, int w) {
                                            custom.remove(tuning);
                                            if (tuning.name.equals(settings.instrumentName)) {
                                                settings.instrumentName = InstrumentLibrary.builtIns().get(0).name;
                                            }
                                            settings.save(activity);
                                            onChange.run();
                                        }
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .show();
                        } else {
                            showInstrumentEditor(activity, settings, tuning, onChange);
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static void showInstrumentEditor(final Activity activity, final Settings settings,
                                             final Tuning existing, final Runnable onChange) {
        int padding = Math.round(18 * activity.getResources().getDisplayMetrics().density);
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(padding, padding / 2, padding, 0);

        TextView nameLabel = new TextView(activity);
        nameLabel.setText("Name");
        nameLabel.setTextSize(13);
        box.addView(nameLabel);

        final EditText name = new EditText(activity);
        name.setSingleLine(true);
        name.setText(existing == null ? "" : existing.displayName());
        name.setHint("My Tuning");
        box.addView(name);

        TextView notesLabel = new TextView(activity);
        notesLabel.setText("Notes (1-12, space separated: E2 A2 D3 G3 B3 E4)");
        notesLabel.setTextSize(13);
        notesLabel.setPadding(0, padding / 2, 0, 0);
        box.addView(notesLabel);

        final EditText notes = new EditText(activity);
        notes.setSingleLine(false);
        notes.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        notes.setText(existing == null
                ? settings.selectedTuning().noteText(0)
                : existing.noteText(0));
        box.addView(notes, new LinearLayout.LayoutParams(-1, -2));

        TextView help = new TextView(activity);
        help.setText("Use C, C#, Db, ... and an octave number. A4 = 440 Hz reference. "
                + "The name is stored as a custom tuning and can be edited later.");
        help.setTextSize(12);
        help.setPadding(0, padding / 2, 0, 0);
        box.addView(help);

        final AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(existing == null ? "New Instrument" : "Edit Instrument")
                .setView(box)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface d) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new android.view.View.OnClickListener() {
                    @Override
                    public void onClick(android.view.View v) {
                        String cleanName = name.getText().toString().trim();
                        if (cleanName.length() == 0) {
                            Toast.makeText(activity, "Enter a tuning name", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (cleanName.charAt(0) == '!') {
                            cleanName = cleanName.substring(1).trim();
                        }
                        if (cleanName.length() == 0) {
                            Toast.makeText(activity, "Enter a tuning name", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String noteText = notes.getText().toString().trim();
                        String[] parts = Notes.parseNotes(noteText);
                        if (parts.length < 1 || parts.length > 12) {
                            Toast.makeText(activity, "Enter between 1 and 12 notes", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        int[] parsed = new int[parts.length];
                        try {
                            for (int i = 0; i < parts.length; i++) {
                                parsed[i] = Notes.parse(parts[i]);
                            }
                        } catch (RuntimeException e) {
                            Toast.makeText(activity, "Check the note names and octaves", Toast.LENGTH_LONG).show();
                            return;
                        }
                        String storedName = "!" + cleanName;
                        if (settings.customTunings.size() >= 100 && InstrumentLibrary.findIn(settings.customTunings, storedName) == null) {
                            Toast.makeText(activity, "HzLab supports up to 100 custom instruments", Toast.LENGTH_LONG).show();
                            return;
                        }
                        Tuning replacement = new Tuning(storedName, parsed);
                        boolean overwrite = InstrumentLibrary.findIn(settings.customTunings, storedName) != null;
                        if (overwrite && (existing == null || !existing.name.equals(storedName))) {
                            new AlertDialog.Builder(activity)
                                    .setTitle("Overwrite Instrument?")
                                    .setMessage("Overwrite custom instrument '" + cleanName + "'? This cannot be undone.")
                                    .setPositiveButton("Overwrite", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface di, int w) {
                                            saveInstrument(activity, settings, replacement, existing, onChange);
                                            dialog.dismiss();
                                        }
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .show();
                            return;
                        }
                        saveInstrument(activity, settings, replacement, existing, onChange);
                        dialog.dismiss();
                    }
                });
            }
        });
        dialog.show();
    }

    private static void saveInstrument(Activity activity, Settings settings, Tuning tuning,
                                       Tuning existing, Runnable onChange) {
        if (existing != null && !existing.name.equals(tuning.name)) {
            settings.customTunings.remove(existing);
        }
        InstrumentLibrary.upsert(settings.customTunings, tuning);
        settings.instrumentName = tuning.name;
        settings.save(activity);
        onChange.run();
    }

    // ---------------------------------------------------------------------
    // Temperaments

    public static void editTemperamentPicker(final Activity activity, final Settings settings, final Runnable onChange) {
        final List<Temperament> custom = settings.customTemperaments;
        if (custom.isEmpty()) {
            new AlertDialog.Builder(activity)
                    .setTitle("Custom Temperament")
                    .setMessage("No custom temperaments yet. Create one by entering a name and 12 cents offsets for C through B.")
                    .setPositiveButton("New", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            showTemperamentEditor(activity, settings, null, onChange);
                        }
                    })
                    .setNegativeButton("Close", null)
                    .show();
            return;
        }
        String[] labels = new String[custom.size()];
        for (int i = 0; i < custom.size(); i++) {
            labels[i] = custom.get(i).displayName();
        }
        new AlertDialog.Builder(activity)
                .setTitle("Custom Temperament")
                .setItems(labels, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        settings.temperamentName = custom.get(which).name;
                        settings.save(activity);
                        onChange.run();
                    }
                })
                .setPositiveButton("New", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showTemperamentEditor(activity, settings, null, onChange);
                    }
                })
                .setNeutralButton("Edit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        chooseTemperamentToEdit(activity, settings, onChange, false);
                    }
                })
                .setNegativeButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        chooseTemperamentToEdit(activity, settings, onChange, true);
                    }
                })
                .show();
    }

    private static void chooseTemperamentToEdit(final Activity activity, final Settings settings,
                                                final Runnable onChange, final boolean delete) {
        final List<Temperament> custom = settings.customTemperaments;
        String[] labels = new String[custom.size()];
        for (int i = 0; i < custom.size(); i++) {
            labels[i] = custom.get(i).displayName();
        }
        new AlertDialog.Builder(activity)
                .setTitle(delete ? "Delete Temperament?" : "Edit Temperament")
                .setItems(labels, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final Temperament temperament = custom.get(which);
                        if (delete) {
                            new AlertDialog.Builder(activity)
                                    .setTitle("Delete Temperament?")
                                    .setMessage("Delete custom temperament '" + temperament.displayName()
                                            + "'? This operation cannot be undone.")
                                    .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface d, int w) {
                                            custom.remove(temperament);
                                            if (temperament.name.equals(settings.temperamentName)) {
                                                settings.temperamentName = TemperamentLibrary.builtIns().get(0).name;
                                            }
                                            settings.save(activity);
                                            onChange.run();
                                        }
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .show();
                        } else {
                            showTemperamentEditor(activity, settings, temperament, onChange);
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static void showTemperamentEditor(final Activity activity, final Settings settings,
                                              final Temperament existing, final Runnable onChange) {
        int padding = Math.round(18 * activity.getResources().getDisplayMetrics().density);
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(padding, padding / 2, padding, 0);

        TextView nameLabel = new TextView(activity);
        nameLabel.setText("Name");
        nameLabel.setTextSize(13);
        box.addView(nameLabel);
        final EditText name = new EditText(activity);
        name.setSingleLine(true);
        name.setText(existing == null ? "" : existing.displayName());
        name.setHint("My Temperament");
        box.addView(name);

        TextView centsLabel = new TextView(activity);
        centsLabel.setText("Cents offsets for C, C#, D, D#, E, F, F#, G, G#, A, A#, B");
        centsLabel.setTextSize(13);
        centsLabel.setPadding(0, padding / 2, 0, 0);
        box.addView(centsLabel);

        final EditText cents = new EditText(activity);
        cents.setSingleLine(false);
        cents.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        if (existing == null) {
            cents.setText("0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0");
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 12; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(String.format(Locale.US, "%.1f", existing.cents[i]));
            }
            cents.setText(sb.toString());
        }
        box.addView(cents, new LinearLayout.LayoutParams(-1, -2));

        TextView help = new TextView(activity);
        help.setText("Each value shifts that pitch class from equal temperament. "
                + "Values are in cents, normally between -35 and +35.");
        help.setTextSize(12);
        help.setPadding(0, padding / 2, 0, 0);
        box.addView(help);

        final AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(existing == null ? "New Temperament" : "Edit Temperament")
                .setView(box)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface d) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new android.view.View.OnClickListener() {
                    @Override
                    public void onClick(android.view.View v) {
                        String cleanName = name.getText().toString().trim();
                        if (cleanName.startsWith("!")) {
                            cleanName = cleanName.substring(1).trim();
                        }
                        if (cleanName.length() == 0) {
                            Toast.makeText(activity, "Enter a temperament name", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String[] parts = cents.getText().toString().trim().split("[,;\\s]+");
                        if (parts.length != 12) {
                            Toast.makeText(activity, "Enter exactly 12 cents values", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        float[] values = new float[12];
                        try {
                            for (int i = 0; i < 12; i++) {
                                values[i] = Float.parseFloat(parts[i]);
                                if (values[i] < -100 || values[i] > 100) {
                                    Toast.makeText(activity, "Cents values must be between -100 and 100", Toast.LENGTH_LONG).show();
                                    return;
                                }
                            }
                        } catch (NumberFormatException e) {
                            Toast.makeText(activity, "Check the cents values", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String storedName = "!" + cleanName;
                        if (settings.customTemperaments.size() >= 100
                                && TemperamentLibrary.findIn(settings.customTemperaments, storedName) == null) {
                            Toast.makeText(activity, "HzLab supports up to 100 custom temperaments", Toast.LENGTH_LONG).show();
                            return;
                        }
                        Temperament replacement = new Temperament(storedName, values);
                        boolean overwrite = TemperamentLibrary.findIn(settings.customTemperaments, storedName) != null;
                        if (overwrite && (existing == null || !existing.name.equals(storedName))) {
                            new AlertDialog.Builder(activity)
                                    .setTitle("Overwrite Temperament?")
                                    .setMessage("Overwrite custom temperament '" + cleanName + "'? This cannot be undone.")
                                    .setPositiveButton("Overwrite", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface di, int w) {
                                            saveTemperament(activity, settings, replacement, existing, onChange);
                                            dialog.dismiss();
                                        }
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .show();
                            return;
                        }
                        saveTemperament(activity, settings, replacement, existing, onChange);
                        dialog.dismiss();
                    }
                });
            }
        });
        dialog.show();
    }

    private static void saveTemperament(Activity activity, Settings settings, Temperament temperament,
                                        Temperament existing, Runnable onChange) {
        if (existing != null && !existing.name.equals(temperament.name)) {
            settings.customTemperaments.remove(existing);
        }
        TemperamentLibrary.upsert(settings.customTemperaments, temperament);
        settings.temperamentName = temperament.name;
        settings.save(activity);
        onChange.run();
    }
}
