/*
 * ======================================================================================
 * FILE: ItemActivity.java — THE ADD / EDIT FORM
 * ======================================================================================
 * WHAT IT IS: a screen with 13 controls, which reads the values from them, builds a
 *   `Movie` and sends it back to `MainActivity` through a result `Intent`.
 *
 * WHY IT EXISTS: it is the ONLY place that knows how to turn widgets into a model.
 *   `MainActivity` touches no `EditText`; it receives a ready-made `Movie`.
 *
 * WHAT IT DEPENDS ON:
 *   - `activity_item.xml` — for the 13 IDs (`etTitle` ... `btnMovieAction`).
 *   - `MainActivity` — for the constants `MOVIE_EXTRA` and `POSITION_EXTRA`.
 *   - `Movie` — the type it builds.
 *   - `IntentCompat` — for the typed reading of the serializable extra.
 *
 * WHO DEPENDS ON IT:
 *   `MainActivity` (starts it through `launcher`), `AndroidManifest.xml` (declares it).
 *
 * WHAT IT DOES NOT DO:
 *   - It validates nothing: an empty budget produces a `NumberFormatException`, not a message.
 *   - It persists nothing: it writes to no file, to no `SharedPreferences` and to no database.
 *   - It does not load the poster: `etPoster` is just text.
 *   - It does not know what happens to the `Movie` after `finish()`.
 *
 * WHAT BREAKS WITHOUT IT:
 *   The app would have a list, but no way to add or change a movie.
 *   The FAB would start a non-existent activity -> `ActivityNotFoundException`.
 * ======================================================================================
 */

// The same package as `MainActivity` — that is why it can use its constants
// without an import and without the fully qualified name.
package ro.ase.ie.g1096.activities;

// ======================================================================================
// IMPORTS — 24 NAMES, ONE OF WHICH IS DEAD
// ======================================================================================
// android.widget.*   — the 13 widget types + their listener interfaces
// androidx.*         — AppCompat, EdgeToEdge, insets, IntentCompat, @NonNull
// com.google.*       — MaterialSwitch (the only Material widget in the code)
// java.text.*        — SimpleDateFormat and its exception
// java.util.*        — Calendar, Date, Locale
//
// DEAD: `android.widget.Switch`. The field is `MaterialSwitch`, which extends
//   `SwitchCompat`, NOT `android.widget.Switch`. See difference 29.0.6.
// ======================================================================================

// DatePickerDialog: the calendar dialog that appears on a click on the date field.
import android.app.DatePickerDialog;

// Intent: the result object, built in `saveMovie()`.
import android.content.Intent;

// Bundle: only in the signature of onCreate.
import android.os.Bundle;

// View: the type of the parameter in OnClickListener.
import android.view.View;

// Button: the type of the `btMovieAction` field.
import android.widget.Button;

// CheckBox: the type of the `cbonWatchList` field.
import android.widget.CheckBox;

// CompoundButton: the base class for Switch/CheckBox; the listener interface is here.
import android.widget.CompoundButton;

// DatePicker: the first parameter of `onDateSet`.
import android.widget.DatePicker;

// EditText: the type of the `etTitle`, `etRelease`, `etBudget`, `etPoster` fields.
import android.widget.EditText;

// RadioButton: the type into which each child of `rgApproval` is CAST.
import android.widget.RadioButton;

// RadioGroup: the container that guarantees a single selection among the radio buttons.
import android.widget.RadioGroup;

// RatingBar: the type of the `rbRating` field.
import android.widget.RatingBar;

// SeekBar: the type of the `sbDuration` field.
import android.widget.SeekBar;

// Spinner: the type of the `spGenre` field.
import android.widget.Spinner;

// Switch: DEAD IMPORT. No `Switch` name appears in the file.
// DANGER! If you "fixed" the import by using it — `private Switch swWatched;` —
//   `findViewById(R.id.swWatched)` would return a `MaterialSwitch`, which is NOT
//   a subclass of `android.widget.Switch`, and you would get a `ClassCastException` at runtime.
import android.widget.Switch;

// TextView: the type of the `tvDurationValue` and `tvWatchedValue` fields.
import android.widget.TextView;

// EdgeToEdge: drawing under the system bars.
import androidx.activity.EdgeToEdge;

// NonNull: an annotation on the parameter of `onCheckedChanged`. Only documentation for
//   the compiler and for lint; it does not change the bytecode.
import androidx.annotation.NonNull;

// AppCompatActivity: the base class.
import androidx.appcompat.app.AppCompatActivity;

// IntentCompat: a compatible variant of reading the extras. `getSerializableExtra(
//   Intent, String, Class<T>)` is the typed variant, which no longer throws
//   ClassCastException and works on all APIs.
import androidx.core.content.IntentCompat;

// Insets / ViewCompat / WindowInsetsCompat: the same three classes as in MainActivity,
// for the same padding listener.
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// MaterialSwitch: the Material widget for the toggle. It extends SwitchCompat,
// which extends CompoundButton — that is why the listener is `CompoundButton.OnCheckedChangeListener`.
import com.google.android.material.materialswitch.MaterialSwitch;

// ParseException: the exception thrown by `SimpleDateFormat.parse` on invalid text.
import java.text.ParseException;

// SimpleDateFormat: formats and parses "dd/MM/yyyy".
import java.text.SimpleDateFormat;

// Calendar: the type of `releaseCal` and the YEAR / MONTH / DAY_OF_MONTH constants.
import java.util.Calendar;

// Date: the type returned by `dateFormat.parse(...)`.
import java.util.Date;

// Locale: the formatting depends on the locale (month names, separators).
import java.util.Locale;

// The R class — for the 13 IDs.
import ro.ase.ie.g1096.R;

// The model built by the form.
import ro.ase.ie.g1096.model.Movie;

/*
 * ======================================================================================
 * THEORY BLOCK: THE FORM AS A FACTORY AND THE TWO MODES
 * ======================================================================================
 *
 * 1. ONE SCREEN, TWO MODES
 *    There is no `AddActivity` and no `EditActivity`. There is one `ItemActivity` that
 *    behaves differently depending on what it receives in the `Intent`:
 *
 *      Intent WITHOUT MOVIE_EXTRA   -> INSERT mode: empty form, "Save Movie" button
 *      Intent WITH    MOVIE_EXTRA  -> EDIT mode:   filled form, "Update Movie" button
 *
 *    The discriminant is `existing != null`, not a separate boolean flag. Less
 *    code, fewer places where the flag can be wrong.
 *
 * 2. THE DIRECTION OF THE DATA
 *    MainActivity --(Intent with Movie)--> ItemActivity --(form)--> new Movie
 *    ItemActivity --(setResult with new Movie)--> MainActivity
 *    The data flows in a single direction, a single time. There are no observers,
 *    no intermediate callbacks.
 *
 * 3. THE RESULT `Intent` IS NEVER STARTED
 *    `new Intent()` without a target does not describe a navigation. It is just a container
 *    for `putExtra`, which `setResult` sends back to the caller.
 *    The classic confusion: "why does it not have `IntentActivity.class`?" — because it
 *    starts nothing.
 *
 * 4. WHO IS THE SOURCE OF TRUTH FOR THE DATE
 *    NOT the text in `etRelease`. But `releaseCal`, a `Calendar`. The text is only
 *    a representation; the calendar is the value. `saveMovie()` reads from the calendar.
 *    The consequence: if you forget to sync the calendar with the text, you save another date.
 *    Here this forgetting EXISTS — see BUG 2.
 *
 * 5. THE LIFECYCLE OF A FORM
 *    onCreate -> (the user fills it in) -> saveMovie -> setResult -> finish
 *    -> onPause -> onStop -> onDestroy. There is no autosave, there is no
 *    "draft". If you press Back, everything you typed is lost, with no warning.
 * ======================================================================================
 */

// The same base class as MainActivity.
public class ItemActivity extends AppCompatActivity {

    // ==================================================================================
    // THE 13 WIDGET FIELDS — THE ORDER FOLLOWS THE FORM, NOT THE XML
    // ==================================================================================
    // They are declared in the order in which they appear on screen: title, date, genre, budget,
    // duration, rating, guidance, watchlist, switch, poster, button, two labels.
    // The type of each is exactly the class from the XML — `EditText`, not `TextInputEditText`,
    // because `TextInputEditText` is a subclass of `EditText`.
    // ==================================================================================

    // The movie title. `TextInputEditText` in the XML is an `EditText`, so it works.
    private EditText etTitle;

    // The release date. It is NOT typed: the click opens a `DatePickerDialog`.
    private EditText etRelease;

    // The genre, chosen from `@array/movie_genre`.
    private Spinner spGenre;

    // The budget, typed on a numeric keyboard with decimals.
    private EditText etBudget;

    // The duration, chosen with the 0..300 slider.
    private SeekBar sbDuration;

    // The rating, 0..5 stars with a 0.5 step.
    private RatingBar rbRating;

    // The radio group for the parental guidance.
    private RadioGroup rgApproval;

    // The "Watchlist" checkbox. The field name has an extra `b` compared to the ID: `cbonWatchList`.
    private CheckBox cbonWatchList;

    // The "Recommended" switch. THE TYPE IS MATERIAL, not `android.widget.Switch`.
    private MaterialSwitch swWatched;

    // The poster URL, typed with a URI-type keyboard.
    private EditText etPoster;

    // The save button. Its text changes in edit mode.
    private Button btMovieAction;

    // The "148 min." label next to the slider — it updates on every movement.
    private TextView tvDurationValue;

    // The "Yes"/"No" label next to the switch — it updates on every toggle.
    private TextView tvWatchedValue;

    // ==================================================================================
    // INTERNAL STATE — THREE FIELDS THAT ARE NOT WIDGETS
    // ==================================================================================
    // `position` = the mode. -1 means insert; any other value means edit.
    //   It is the ONLY mode flag in the class. There is no `boolean isEdit`.
    //
    // `releaseCal` = the source of truth for the date. Initialized with "now", so that the dialog
    //   starts somewhere even if the user has chosen nothing.
    //
    // `dateFormat` = the common formatter for display and parsing. `final` because
    //   the reference does not change; `SimpleDateFormat` is internally mutable, but here
    //   it is used only on the main thread.
    // ==================================================================================
    private int position = -1;   // -1 = insert mode
    private Calendar releaseCal = Calendar.getInstance();
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    // ==================================================================================
    // onCreate — BUILDS THE FORM, THEN FILLS IT IF NEEDED
    // ==================================================================================
    // WHAT IT DOES: inflates the layout, binds the 13 views, attaches the 4 listeners,
    //   then reads the `Intent` and decides whether the form starts empty or filled.
    // HOW: 1. super 2. EdgeToEdge 3. setContentView 4. insets 5. initControls
    //   6. initListeners 7. getIntent 8. read the extra 9. if it exists, fillForm + texts.
    // RISK: the order 5-6-9 is essential. `fillForm` calls `setChecked` and
    //   `setProgress`, which trigger the listeners. If the listeners were not attached
    //   before, the labels `tvWatchedValue` and `tvDurationValue` would stay at
    //   the values from the XML ("No", "0 min").
    // REF: the same order as in MainActivity.onCreate, plus the step of reading the Intent.
    // ==================================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // 1. The base class chain. Mandatory first.
        super.onCreate(savedInstanceState);

        // 2. Drawing under the system bars.
        EdgeToEdge.enable(this);

        // 3. Inflates `activity_item.xml`. From this moment `findViewById` finds the views.
        setContentView(R.layout.activity_item);

        // 4. Padding for the system bars, on the root `R.id.main` (the ScrollView).
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 5. Binds the 13 fields to the views.
        initControls();

        // 6. Attaches the 4 listeners. It MUST be before `fillForm`, because
        //    `fillForm` will trigger `onCheckedChanged` and `onProgressChanged`.
        initListeners();

        // 7. Gets the Intent that started the activity.
        //    - On insert: an empty Intent, built by the FAB.
        //    - On edit: an Intent with MOVIE_EXTRA and POSITION_EXTRA, built by the row click.
        Intent intent = getIntent();

        // 8. Reads the serialized object, in the TYPED variant.
        //    `IntentCompat.getSerializableExtra(intent, key, Movie.class)` returns
        //    `Movie` or `null` directly, with no manual cast and without the deprecated method.
        //    WATCH OUT: the object received is a deserialized COPY, not the original from the list.
        Movie existing = IntentCompat.getSerializableExtra(intent, MainActivity.MOVIE_EXTRA, Movie.class);

        // 9. The mode discriminant: does the object exist or not.
        if (existing != null) {

            // 9a. Keeps the position for `saveMovie`. If it is missing, it stays -1 and
            //     `saveMovie` would send a new movie instead of replacing.
            position = intent.getIntExtra(MainActivity.POSITION_EXTRA, -1);

            // 9b. Fills the 13 controls with the movie's values.
            fillForm(existing);

            // 9c. `setTitle` is the method of the ACTIVITY (it changes the title in the ActionBar),
            //     NOT `Movie.setTitle`. Same name, another class — a reading trap.
            setTitle("Edit movie");

            // 9d. Changes the button text. The user sees that they are saving a change.
            btMovieAction.setText("Update Movie");
        }
        // If `existing` is null, nothing is done: the form stays empty,
        // the button stays "Save Movie" from the XML, and `position` stays -1.
    }

    // ==================================================================================
    // fillForm — MODEL -> WIDGETS
    // ==================================================================================
    // WHAT IT DOES: copies the 10 values of a `Movie` into the 13 controls.
    // HOW: 8 direct setters, then 2 loops — one over the Spinner (by TEXT), one over the
    //   RadioGroup (by TEXT).
    // RISK: the two loops fail SILENTLY if the text does not match. The Spinner
    //   stays on the first position, the radio group stays on the button checked by default.
    // REF: the pair of this method is `saveMovie()` — there it goes the other way, widget -> model.
    // ==================================================================================
    private void fillForm(Movie existing) {

        // Plain text. `setText` accepts `null` and displays an empty field.
        etTitle.setText(existing.getTitle());

        // The date: `Date` -> text with `SimpleDateFormat`. WATCH OUT: this line does NOT
        // update `releaseCal`. See BUG 2 below.
        etRelease.setText(dateFormat.format(existing.getRelease()));

        // The budget: `double` -> text. `String.valueOf(double)` produces "1.6E8" for
        // 160 000 000, not "160000000". See BUG 3.
        etBudget.setText(String.valueOf(existing.getBudget()));

        // The duration: `int` -> progress on the slider. The value is automatically limited to 0..300.
        // This line TRIGGERS `onProgressChanged`, which updates the label.
        sbDuration.setProgress(existing.getDuration());

        // The checkbox: `boolean` -> `setChecked`. It triggers the listener only if the value
        // CHANGES compared to the current state.
        cbonWatchList.setChecked(existing.isOnWatchlist());

        // The switch: `boolean` -> `setChecked`. It triggers `onCheckedChanged`,
        // which writes "Yes"/"No" into `tvWatchedValue`.
        swWatched.setChecked(existing.isWatched());

        // The rating: `float` -> `setRating(float)`. The types match exactly.
        rbRating.setRating(existing.getRating());

        // The poster: `String` -> text. It can be `null`; `setText(null)` is legal.
        etPoster.setText(existing.getPosterUrl());

        // LOOP 1 — the Spinner has no "setValue(String)". It only has "setSelection(index)".
        // So we look for the index by text, comparing each element with the movie's genre.
        for(int index=0; index<spGenre.getCount(); index++)
        {
            // `getItemAtPosition` returns the element from `@array/movie_genre`.
            // `.equals` on the object from the adapter; if it were null, it would throw an NPE.
            if(spGenre.getItemAtPosition(index).equals(existing.getGenre()))
            {
                // We found the genre — select it.
                spGenre.setSelection(index);

                // Exit the loop: the first match is enough.
                break;
            }
            // If no match is found, the spinner stays at position 0.
            // RISK: no message, no error. Only the displayed genre is different.
        }

        // LOOP 2 — the RadioGroup has no "select by text" either.
        // We iterate over the CHILDREN of the group, not over a separate array.
        for(int index=0; index<rgApproval.getChildCount(); index++)
        {
            // CAST to RadioButton. It works because all the children in the XML are
            // `MaterialRadioButton`, which extends `RadioButton`.
            // RISK: if someone adds a separator `View` in the RadioGroup, this cast
            //   throws `ClassCastException` on edit.
            RadioButton rb = (RadioButton) rgApproval.getChildAt(index);

            // Compares the TEXT of the button ("G", "PG", "PG-13", "R", "NC-17") with the value.
            if(rb.getText().toString().equals(existing.getApproval()))
            {
                // `check(id)` checks the button and automatically unchecks the other —
                // this is the guarantee that the RadioGroup provides.
                rgApproval.check(rgApproval.getChildAt(index).getId());

                // Exit the loop.
                break;
            }
        }
    }

    // ==================================================================================
    // initControls — 13 FINDVIEWBYID CALLS, ONE FOR EACH ID IN THE XML
    // ==================================================================================
    // WHAT IT DOES: binds each Java field to the view with the corresponding ID.
    // HOW: 13 mechanical assignments. The order follows the order of the fields.
    // RISK: a wrong ID -> `findViewById` returns null -> `NullPointerException`
    //   on first use, not here. The hardest class of bugs to debug.
    // REF: `MainActivity.initControls()` has only 3 lines; here there are 13.
    // ==================================================================================
    private void initControls() {

        // Each line has the same shape: `field = findViewById(R.id.id_from_xml);`
        // `findViewById` is generic since API 26: the type is inferred from the field on the left.
        etTitle = findViewById(R.id.etTitle);
        etRelease = findViewById(R.id.etRelease);
        spGenre = findViewById(R.id.spGenre);
        etBudget = findViewById(R.id.etBudget);
        sbDuration = findViewById(R.id.sbDuration);
        rbRating = findViewById(R.id.rbRating);
        rgApproval = findViewById(R.id.rgApproval);

        // WATCH THE NAMES: the field is `cbonWatchList`, the ID is `cbOnWatchList`.
        // An extra `b` in the field, a capital `O` in the ID. Both are correct.
        cbonWatchList = findViewById(R.id.cbOnWatchList);

        swWatched = findViewById(R.id.swWatched);
        etPoster = findViewById(R.id.etPoster);

        // The button: the field `btMovieAction`, the ID `btnMovieAction`.
        btMovieAction = findViewById(R.id.btnMovieAction);

        // The two labels that are updated from the listeners.
        tvDurationValue = findViewById(R.id.tvDurationValue);
        tvWatchedValue = findViewById(R.id.tvWatchedValue);
    }

    // ==================================================================================
    // initListeners — FOUR BEHAVIOURS
    // ==================================================================================
    // WHAT IT DOES: attaches: click on the date -> dialog; click on the button -> save;
    //   switch change -> label; slider movement -> label.
    // HOW: 4 anonymous classes, one of which implements an interface with 3 methods.
    // RISK: the order matters relative to `fillForm` — see onCreate, step 6.
    // REF: `MainActivity.initListeners()` has 2 listeners; here there are 4.
    // ==================================================================================
    private void initListeners() {

        // LISTENER 1 — click on the date field.
        // An `EditText` has no "onDateClick"; we use the generic click.
        etRelease.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Opens the calendar dialog. The method is below.
                showReleaseDatePicker();
            }
        });

        // LISTENER 2 — click on the save button.
        btMovieAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Builds the Movie, puts it in the Intent, sets the result, closes.
                saveMovie();
            }
        });

        // LISTENER 3 — the switch change.
        // The interface is `CompoundButton.OnCheckedChangeListener`, not one of MaterialSwitch's own:
        // MaterialSwitch is a CompoundButton.
        swWatched.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull CompoundButton buttonView, boolean isChecked) {
                // Ternary operator: a single expression for both cases.
                // `isChecked` is the NEW value, not the old one.
                tvWatchedValue.setText(isChecked ? "Yes" : "No");
            }
        });

        // LISTENER 4 — the duration slider.
        // The interface has THREE methods. All must be implemented; two stay empty.
        // There are no default methods in a classic Java interface.
        sbDuration.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // `progress` = the new value, 0..300.
                // `fromUser` = true if it comes from the user's finger, false if it
                //   comes from `setProgress(...)`. Here it is IGNORED on purpose:
                //   we want the label to be correct also when `fillForm` calls
                //   `setProgress`, not only when the user drags.
                tvDurationValue.setText(progress + " min.");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Empty: we do not care about the moment the drag starts.
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Empty: we do not care about the moment the drag ends.
                // Here you could save the value or show a message, if needed.
            }
        });
    }

    // ==================================================================================
    // saveMovie — WIDGETS -> MODEL -> INTENT -> RESULT
    // ==================================================================================
    // WHAT IT DOES: reads the 13 controls, builds a `Movie`, puts it in a result
    //   `Intent` together with the position (if it exists) and closes the screen.
    // HOW: 1. new Movie() 2. nine setters 3. loop over the RadioGroup 4. Intent
    //   5. putExtra 6. conditional position 7. setResult 8. finish.
    // RISK: `Double.parseDouble` on empty text throws `NumberFormatException`.
    //   See BUG 1 — it is the only reproducible crash in the app.
    // REF: the pair of `fillForm`; the consumer is the callback in MainActivity.
    // ==================================================================================
    private void saveMovie() {

        // 1. An empty object, filled immediately with setters. That is why the no-argument
        //    constructor is mandatory in the model.
        Movie movie = new Movie();

        // 2. The nine setters that correspond to one widget each.
        //    The order does not matter; only the widget -> setter pairs matter.
        movie.setTitle(etTitle.getText().toString());

        // THE DATE DOES NOT COME FROM THE TEXT, but from the calendar. The text can be invalid; the calendar cannot.
        // `getTime()` turns the Calendar -> Date.
        movie.setRelease(releaseCal.getTime());

        // The genre: the text selected in the Spinner. `getSelectedItem()` is not null as long as
        // the spinner has at least one element (the values from `@array/movie_genre`).
        movie.setGenre(spGenre.getSelectedItem().toString());

        // BUG 1: if `etBudget` is empty, `Double.parseDouble("")` throws
        // `NumberFormatException`, and the app closes. There is no try/catch
        // and there is no `isEmpty()` check.
        // The same line also rejects "1,5" (decimal comma, Romanian locale), but
        // accepts "1.6E8" (scientific notation).
        movie.setBudget(Double.parseDouble(etBudget.getText().toString()));

        // The duration: straight from the progress, with no conversion.
        movie.setDuration(sbDuration.getProgress());

        // The rating: `getRating()` returns `float`, exactly the field's type.
        movie.setRating(rbRating.getRating());

        // The switch: `isChecked()` -> boolean.
        movie.setWatched(swWatched.isChecked());

        // The checkbox: `isChecked()` -> boolean. Independent of the switch.
        movie.setOnWatchlist(cbonWatchList.isChecked());

        // The poster: free text. It can be empty — the model accepts it.
        movie.setPosterUrl(etPoster.getText().toString());

        // 3. The parental guidance: there is no direct getter on the RadioGroup, so we look for
        //    the checked button. The same loop as in `fillForm`, but with `isChecked()`.
        for(int index=0; index<rgApproval.getChildCount(); index++)
        {
            RadioButton rb = (RadioButton) rgApproval.getChildAt(index);

            // Exactly one button is checked (RadioGroup guarantee). If none were
            // checked, `approval` would stay null — but the XML checks `rbGeneral` by default.
            if(rb.isChecked())
            {
                // The saved value is the TEXT of the button, not the ID.
                movie.setApproval(rb.getText().toString());
                break;
            }
        }

        // 4. An Intent WITHOUT a target. It starts no activity: it is the container of the
        //    result, read by the callback in MainActivity.
        Intent intent = new Intent();

        // 5. Puts the movie. `putExtra(String, Serializable)` serializes the object.
        intent.putExtra(MainActivity.MOVIE_EXTRA, movie);

        // 6. The position is set ONLY in edit mode. In insert mode, its absence is
        //    exactly the signal that `MainActivity` reads as "add at the end".
        //    No braces: the `if` controls a single statement.
        if (position != -1)
            intent.putExtra(MainActivity.POSITION_EXTRA, position);

        // 7. Sets the result code. `RESULT_OK` is -1 — the same value as
        //    "the position is missing", but completely unrelated. Here it is a result code,
        //    there it is a list index. A numeric coincidence, not a conflict.
        setResult(RESULT_OK, intent);

        // 8. Closes the activity. Without `finish()`, `setResult` never reaches
        //    the caller, because the activity would stay open.
        finish();
    }

    // ==================================================================================
    // showReleaseDatePicker — THE CALENDAR DIALOG
    // ==================================================================================
    // WHAT IT DOES: opens a `DatePickerDialog` positioned on the current date from
    //   `releaseCal`, and on confirmation writes the date into the calendar AND into the text.
    // HOW: 1. parses the existing text (if valid) 2. builds the dialog
    //   3. on confirmation, updates the calendar 4. formats it back into the EditText.
    // RISK: if the text in the field was typed by hand and is invalid, the parse
    //   fails silently and the dialog starts from the old date in the calendar.
    // REF: `SimpleDateFormat` is also used in `fillForm`; here in the opposite direction.
    // ==================================================================================
    private void showReleaseDatePicker() {

        // The current text in the field. It can be empty (new form) or a date formatted
        // by `fillForm` (edit mode) or text typed by hand by the user.
        String current = etRelease.getText().toString();

        // We parse only if there is something to parse.
        if (!current.isEmpty()) {
            try {
                // `parse` returns a `Date` or throws a `ParseException`.
                Date parsed = dateFormat.parse(current);

                // `parse` can theoretically return null; we check before using it.
                if (parsed != null)
                    // Syncs the calendar with the text. Here is where the text -> Calendar
                    // link happens, the one missing from `fillForm` (BUG 2).
                    releaseCal.setTime(parsed);
            } catch (ParseException ignored) { }
            // The exception is IGNORED on purpose: invalid text must not block the
            // dialog. It starts from the old date in the calendar.
            // RISK: the user never learns that their text was ignored.
        }

        // Builds the dialog. The parameters:
        //   1. `this` = Context
        //   2. the confirmation listener (anonymous class)
        //   3-5. the year, the month, the start day — THE MONTH IS 0-BASED (`Calendar.MONTH`)
        // `.show()` at the end: the dialog does not appear until it is called.
        new DatePickerDialog(
                this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int day) {

                        // `month` also comes 0-based from DatePicker, so it matches
                        // directly with `Calendar.set(year, month, day)`. No conversion.
                        releaseCal.set(year, month, day);

                        // Clears any error previously displayed on the field. Here nothing
                        // ever sets an error, but the call is defensive.
                        etRelease.setError(null);

                        // Writes the date back into the field, in the same "dd/MM/yyyy" format.
                        etRelease.setText(dateFormat.format(releaseCal.getTime()));
                    }
                },
                // The start values: exactly the current state of the calendar.
                releaseCal.get(Calendar.YEAR),
                releaseCal.get(Calendar.MONTH),
                releaseCal.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    // ==================================================================================
    // THE THREE REAL BUGS IN THIS FILE
    // ==================================================================================
    // BUG 1 — EMPTY BUDGET -> CRASH
    //   `Double.parseDouble("")` throws `NumberFormatException`. The user presses
    //   "Save Movie" without filling in the budget, the app closes.
    //   100% reproducible. There is no validation, there is no try/catch.
    //
    // BUG 2 — THE DATE IS LOST ON EDIT
    //   `fillForm` writes the date into `etRelease`, but does NOT write it into `releaseCal`.
    //   `releaseCal` stays on `Calendar.getInstance()`, that is, "now", from the moment
    //   the activity was created. If the user edits a movie and does NOT touch the date
    //   field, `saveMovie` saves `releaseCal.getTime()` = TODAY's date.
    //   The movie "Inception" (2010) becomes "today", with no error.
    //   The correct thing would be for `fillForm` to also do `releaseCal.setTime(existing.getRelease())`.
    //
    // BUG 3 — SCIENTIFIC NOTATION IN THE BUDGET FIELD
    //   `String.valueOf(160000000.0)` produces "1.6E8", not "160000000".
    //   On edit, the field displays "1.6E8". Parsing it back works, so
    //   the data is correct — only the display is ugly. Cosmetic, but visible.
    //
    // AN ADDITIONAL RISK — THE GENRE THAT IS NOT FOUND
    //   If `@array/movie_genre` changes and the saved value no longer exists,
    //   `fillForm` leaves the spinner at position 0, with no message. On save, the movie's
    //   genre changes to the first element in the list.
    // ==================================================================================
}
