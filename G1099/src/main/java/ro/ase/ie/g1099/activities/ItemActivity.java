/*
 * ======================================================================================
 * FILE: ItemActivity.java — THE INSERT AND EDIT FORM
 * ======================================================================================
 * WHAT IT IS: a single activity that serves TWO modes. If it receives a `Movie`
 *   in the Intent, it is editing; if it receives nothing, it is insertion.
 *
 * WHY IT EXISTS: it is the screen that turns widgets into the model. All 10
 *   fields of `Movie` are produced here, from `getText()`, `getProgress()`,
 *   `getRating()`, `isChecked()` and `getSelectedItem()`.
 *
 * WHAT IT DEPENDS ON: `activity_item.xml` (13 views), `Movie`, the constants
 *   `MainActivity.MOVIE_EXTRA` / `POSITION_EXTRA` and the Activity Result API
 *   (through `setResult`).
 *
 * WHO DEPENDS ON IT: `MainActivity`, which receives the result Intent in the callback.
 *
 * WHAT IT DOES NOT DO: it does not hold the list, it does not know what position the movie has in the list (it receives it),
 *   it saves nothing to disk. It does not validate the fields before saving.
 *
 * WHAT BREAKS WITHOUT IT: the app would have a list, but no way to add or
 *   modify a movie. The FAB would start a non-existent activity.
 *
 * WATCH OUT FOR THE IMPORTS: three of them are DEAD — `android.media.Rating`,
 *   `android.widget.Switch` and `Snackbar`. They are marked one by one below. See 31.0.5.
 * ======================================================================================
 */

// THE PACKAGE: the same `activities` as MainActivity — that is why the `protected`
//   constants of MainActivity are visible here without an import.
package ro.ase.ie.g1099.activities;

// DatePickerDialog: the wheel dialog for year/month/day.
import android.app.DatePickerDialog;

// Intent: the result object, built on save.
import android.content.Intent;

// Rating: the rating class for MEDIA content, from API 21.
//   DEAD IMPORT. It is used nowhere in the file — it appears exactly once, here.
//   The widget in the form is `RatingBar`, a completely different class. See 31.0.5.
import android.media.Rating;

// Bundle: only in the onCreate signature.
import android.os.Bundle;

// View: the parameter type in OnClickListener.
import android.view.View;

// Button: the save button (`btMovieAction`).
import android.widget.Button;

// CheckBox: the "Watchlist" check mark (`cbonWatchList`).
import android.widget.CheckBox;

// CompoundButton: the common interface for Switch and CheckBox; it appears in the signature
//   of the switch listener.
import android.widget.CompoundButton;

// DatePicker: the view type in the date dialog callback.
import android.widget.DatePicker;

// EditText: the title, the date, the budget and the poster.
import android.widget.EditText;

// RadioButton: the 5 parental guidance buttons.
import android.widget.RadioButton;

// RadioGroup: the container of the 5 buttons.
import android.widget.RadioGroup;

// RatingBar: the star bar. THIS is the rating widget, not `android.media.Rating`.
import android.widget.RatingBar;

// SeekBar: the duration bar.
import android.widget.SeekBar;

// Spinner: the drop-down list of genres.
import android.widget.Spinner;

// Switch: the classic toggle widget.
//   DEAD IMPORT. The field `swWatched` is declared `MaterialSwitch`, and
//   `MaterialSwitch` extends `SwitchCompat`, not `Switch`. The name `Switch` appears
//   nowhere outside this line. See 31.0.6.
import android.widget.Switch;

// TextView: the two dynamic labels (`tvDurationValue`, `tvWatchedValue`).
import android.widget.TextView;

// EdgeToEdge: drawing under the system bars.
import androidx.activity.EdgeToEdge;

// NonNull: annotation on the `buttonView` parameter of the switch listener.
//   It is a promise that the parameter is not null; the compiler does not check it,
//   but lint uses it.
import androidx.annotation.NonNull;

// AppCompatActivity: the base class of the activity.
import androidx.appcompat.app.AppCompatActivity;

// IntentCompat: the typed variant of `getSerializableExtra`, with `Class<T>`.
//   Unlike MainActivity, which uses the old method with a cast,
//   here the modern variant is used.
import androidx.core.content.IntentCompat;

// Insets / ViewCompat / WindowInsetsCompat: the same three classes as in MainActivity,
//   for the padding under the system bars.
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// MaterialSwitch: the Material 3 switch. It extends SwitchCompat -> CompoundButton.
import com.google.android.material.materialswitch.MaterialSwitch;

// Snackbar: a short message bar, at the bottom of the screen.
//   DEAD IMPORT. There is no `Snackbar.make(...)` in the file.
import com.google.android.material.snackbar.Snackbar;

// ParseException: the exception thrown by `SimpleDateFormat.parse` on invalid text.
import java.text.ParseException;

// SimpleDateFormat: the "dd/MM/yyyy" formatter for the date field.
import java.text.SimpleDateFormat;

// Calendar: the source of truth for the date.
import java.util.Calendar;

// Date: the type returned by `dateFormat.parse(...)`.
import java.util.Date;

// Locale: the regional format for the formatter.
import java.util.Locale;

// R: the generated resources.
import ro.ase.ie.g1099.R;

// Movie: the model built on save and read on fill.
import ro.ase.ie.g1099.model.Movie;

/*
 * ======================================================================================
 * THEORY BLOCK: THE FORM AS A STATE MACHINE
 * ======================================================================================
 *
 * 1. **Two modes, a single activity.** `ItemActivity` does not have two layouts and
 *    two classes. It has a `position` field that is `-1` (insert) or `>= 0` (edit).
 *    All the different behaviour derives from this field.
 *
 * 2. **The source of truth is not always the widget.** For 9 fields, the widget
 *    is the source (you read directly from it on save). For the date, the source is
 *    `releaseCal`, and the `EditText` is only a DISPLAY.
 *
 * 3. **The listeners are also a source of truth for the labels.** `tvDurationValue`
 *    and `tvWatchedValue` are not read on save; they are updated by the listeners
 *    of the widgets. If a listener does not fire, the label lags behind.
 *
 * 4. **The result is an Intent.** The activity does not return an object through `return`;
 *    it puts it in an `Intent` and hands it over through `setResult`.
 *
 * 5. **The order in `onCreate` is critical.** `initListeners()` runs BEFORE
 *    `fillForm()`. The reason: `setChecked` and `setProgress` trigger the listeners,
 *    which update the labels. Reversed, the labels would keep the values from the XML.
 *
 * VISUAL: the data flow in the form
 *
 *   [etTitle] -------------------------+
 *   [etRelease] --(display only)--+    |
 *                                 |    |
 *   [releaseCal] -----------------+----+---> saveMovie() ---> Movie ---> Intent
 *   [spGenre] -------------------------+                                    |
 *   [etBudget] ------------------------+                                    v
 *   [sbDuration] ----------------------+                            setResult(RESULT_OK)
 *   [rbRating] ------------------------+                                    |
 *   [swWatched] -----------------------+                                    v
 *   [cbonWatchList] -------------------+                                  finish()
 *   [rgApproval] --(loop)--+----------+
 *   [etPoster] ------------------------+
 *
 * VISUAL: the two modes
 *
 *   Intent WITHOUT MOVIE_EXTRA          Intent WITH MOVIE_EXTRA
 *          |                                  |
 *          v                                  v
 *   existing == null                   existing != null
 *          |                                  |
 *   position stays -1                  position = getIntExtra(..., -1)
 *   empty form                         fillForm(existing)
 *   button: "Save Movie"               button: "Update Movie"
 *          |                                  |
 *          +---------------+------------------+
 *                          v
 *                     saveMovie()
 *                          |
 *          position == -1 -> Intent WITHOUT POSITION_EXTRA
 *          position >= 0  -> Intent WITH POSITION_EXTRA
 * ======================================================================================
 */
public class ItemActivity extends AppCompatActivity {

    // ==================================================================================
    // THE 13 REFERENCES TO VIEWS
    // ==================================================================================
    // WHY 13: the form has 10 data fields + 2 dynamic labels + 1 button.
    //   The 7 `TextView`s of fixed labels (`tvTile`, `tvGenre`, `tvBudget`,
    //   `tvDuration`, `tvRating`, `tvWatched`, `tvGuidance`) do NOT appear here: they are not
    //   read and are not modified from code, so they need no reference.
    //
    // WHY THEY ARE ALL `private`: they are implementation details of the screen.
    // ==================================================================================

    // Field 1: the title. The real object in the XML is `TextInputEditText`, but the declared
    // type is `EditText` — the parent class. It is enough for `getText()`.
    private EditText etTitle;

    // Field 2: the date, as TEXT. The real value is in `releaseCal` (see below).
    private EditText etRelease;

    // Field 3: the genre. `Spinner` — the drop-down list from `@array/movie_genre`.
    private Spinner spGenre;

    // Field 4: the budget. `inputType="numberDecimal"` in the XML.
    private EditText etBudget;

    // Field 5: the duration. `SeekBar` with max 300.
    private SeekBar sbDuration;

    // Field 6: the rating. `RatingBar` with 5 stars and a step of 0.5.
    // WATCH OUT: the type is `RatingBar`, NOT `android.media.Rating`. That import
    //   is dead — see 31.0.5.
    private RatingBar rbRating;

    // Field 7: the parental guidance. `RadioGroup` with 5 buttons.
    private RadioGroup rgApproval;

    // Field 8: the "Watchlist" check mark.
    // WATCH OUT FOR THE NAME: the field is `cbonWatchList`, the ID is `cbOnWatchList`.
    //   An `o` missing on one side, an `O` extra on the other. It is not a typo to fix:
    //   that is how the source is. `findViewById(R.id.cbonWatchList)` would NOT compile.
    private CheckBox cbonWatchList;

    // Field 9: the "Recommended" switch. The real type is `MaterialSwitch`, which
    // extends `SwitchCompat` -> `CompoundButton`. Declared `MaterialSwitch` in order
    // to have access to all its methods.
    // THAT is why the `android.widget.Switch` import is dead: it is not the type of this field.
    private MaterialSwitch swWatched;

    // Field 10: the poster URL.
    private EditText etPoster;

    // Field 11: the save button.
    // WATCH OUT FOR THE NAME: the field is `btMovieAction`, the ID is `btnMovieAction`.
    private Button btMovieAction;

    // Field 12: the label that displays the duration ("148 min."). It is NOT read on
    // save — it is display only, updated by the SeekBar listener.
    private TextView tvDurationValue;

    // Field 13: the label that displays "Yes"/"No" for the switch.
    private TextView tvWatchedValue;

    // ==================================================================================
    // THE STATE OF THE FORM
    // ==================================================================================

    // THE MODE: -1 = insert, >= 0 = edit.
    // WHY `-1` and not `0`: `0` is a valid position in the list. The sentinel must
    //   be a value IMPOSSIBLE as a position.
    // WHO CHANGES IT: only `onCreate`, when it finds a movie in the Intent.
    private int position = -1;   // -1 = insert mode

    // THE SOURCE OF TRUTH FOR THE DATE.
    // `Calendar.getInstance()` = the CURRENT date and time. That is why the form starts
    //   with today's date, not with an empty date.
    // WHY NOT `null`: `saveMovie` calls `releaseCal.getTime()` unconditionally.
    //   A `null` would throw an NPE on the first save.
    // WHY `Calendar` and not `Date`: `Calendar` has fields (YEAR, MONTH, DAY_OF_MONTH)
    //   that `DatePickerDialog` reads and writes directly.
    // RISK: in edit mode, `fillForm` does NOT update this calendar — see BUG 4.
    private Calendar releaseCal = Calendar.getInstance();

    // THE DATE FORMATTER.
    // "dd/MM/yyyy" = day with 2 digits / month with 2 digits / year with 4 digits.
    //   WATCH OUT: `MM` = month, `mm` = minutes. Swapping them is a classic mistake.
    // `Locale.getDefault()` = the format of the device's language. Here the pattern is
    //   explicit, so Locale influences only the symbols and the internal order.
    // `final`: the reference does not change. The object, however, is MUTABLE.
    // RISK: `SimpleDateFormat` is not thread-safe. Here it is used exclusively on
    //   the UI thread, so there is no concurrency.
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    // ==================================================================================
    // onCreate — INFRASTRUCTURE, CONTROLS, LISTENERS, MODE
    // ==================================================================================
    // WHAT IT DOES: builds the screen, attaches the listeners, then decides whether to fill
    //   the form with an existing movie or leave it empty.
    // HOW: 1. infrastructure (super, edge-to-edge, layout, insets)
    //      2. initControls
    //      3. initListeners
    //      4. reads the Intent
    //      5. if a movie exists -> position, fillForm, title, button text
    // RISK: the order 2-3-5 is mandatory. `fillForm` needs the views (2)
    //   and must trigger the listeners (3) for the labels.
    // REF: `MainActivity.onCreate` has the same first infrastructure phase.
    // ==================================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Edge-to-edge: the same call as in MainActivity.
        EdgeToEdge.enable(this);

        // Inflates `activity_item.xml`. The tree has a root ScrollView with the id `main`.
        setContentView(R.layout.activity_item);

        // Insets on the root container. Identical to MainActivity, with a single
        //   difference of context: here `R.id.main` is the ScrollView, not the FrameLayout.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Phase 1: bind the 13 fields to the views in the XML.
        // WHY FIRST: `fillForm` needs them, and so does `initListeners`.
        initControls();

        // Phase 2: attach the listeners.
        // WHY BEFORE `fillForm`: `fillForm` calls `setChecked` and `setProgress`.
        //   These calls TRIGGER the listeners, which update the labels
        //   `tvWatchedValue` and `tvDurationValue`. If the listeners were attached after
        //   `fillForm`, the labels would keep the default values from the XML.
        initListeners();

        // Phase 3: read the incoming Intent.
        // `getIntent()` returns the Intent that started the activity. It is not null:
        //   any activity is started with an Intent, even an empty one.
        Intent intent = getIntent();

        // TYPED read of the serializable.
        // WHY `IntentCompat` AND NOT `intent.getSerializableExtra(...)`:
        //   The old method is deprecated since API 33. `IntentCompat.getSerializableExtra`
        //   also receives the expected class, so it checks the type before the cast
        //   and works identically on all versions.
        // WHY `MainActivity.MOVIE_EXTRA`: the same constant as on send. A typo
        //   here would produce `null` with no error.
        // THE VALUE: `null` in insert mode (the FAB puts no extra).
        Movie existing = IntentCompat.getSerializableExtra(intent, MainActivity.MOVIE_EXTRA, Movie.class);

        // EDIT MODE: there is a movie in the Intent.
        if (existing != null) {

            // Reads the position. Here `-1` would mean "movie sent without a position" —
            // a case that does not happen in the normal flow, but would turn the save
            // into an insert (duplicate).
            position = intent.getIntExtra(MainActivity.POSITION_EXTRA, -1);

            // Fills the form with the values of the received movie.
            // THE OBJECT IS A COPY: the changes in the form do NOT reach the list in
            // MainActivity except through the result Intent.
            fillForm(existing);

            // Changes the title of the screen. `setTitle` is the Activity method,
            //   NOT the model setter — there is no conflict, the receiver differs.
            // If the theme has no ActionBar, the title is not visible anywhere on screen;
            //   the visible feedback for the user is the button text, below.
            setTitle("Edit movie");

            // Changes the text of the button. The same button does insert or update;
            //   the label is the only difference visible to the user.
            btMovieAction.setText("Update Movie");
        }

        // INSERT MODE: `existing == null`. Nothing happens.
        //   `position` stays -1, the form keeps the default values from the XML:
        //   empty title, date = today, genre = the first in the list, empty budget, duration 0,
        //   rating 0, switch off, check mark off, "G" checked, empty poster.
    }

    // ==================================================================================
    // fillForm — MODEL -> WIDGETS
    // ==================================================================================
    // WHAT IT DOES: copies the 10 values from `Movie` into the 10 widgets.
    // HOW: 1. the simple fields, in the order of the model
    //      2. Spinner: loop over the positions, match on text
    //      3. RadioGroup: loop over the children, match on text
    // RISK: it does not update `releaseCal`. See BUG 4 — the date is lost on update.
    // REF: each line is the inverse of a line in `saveMovie`.
    // ==================================================================================
    private void fillForm(Movie existing) {

        // Text: `getText().toString()` on save, `setText(...)` on fill.
        // `setText` expects a `CharSequence`; `String` is a `CharSequence`.
        etTitle.setText(existing.getTitle());

        // BUG 2: `existing.getRelease()` can be `null`.
        // ==============================================================================
        // `SimpleDateFormat.format(null)` throws `NullPointerException`.
        // WHEN IT HAPPENS: when the edited movie has `release == null`. In the demo flow
        //   all 3 movies have a date, so the bug is not visible. It becomes real if a
        //   `Movie` is built with the empty constructor and saved without a date.
        // FIX: `if (existing.getRelease() != null) { ... } else { etRelease.setText(""); }`
        // ==============================================================================
        etRelease.setText(dateFormat.format(existing.getRelease()));

        // BUG 3: `String.valueOf(double)` produces scientific notation for large values.
        // ==============================================================================
        // `String.valueOf(160000000.0)` -> "1.6E8".
        // The user sees "1.6E8" in the budget field. `Double.parseDouble("1.6E8")`
        //   works, so the value is saved correctly — but the display is ugly
        //   and looks like an error.
        // FIX: `String.format(Locale.getDefault(), "%.0f", existing.getBudget())`
        //   or a `DecimalFormat`.
        // ==============================================================================
        etBudget.setText(String.valueOf(existing.getBudget()));

        // `SeekBar.setProgress(int)` — the value is automatically limited to 0..max.
        // If the duration in the model were 500, the bar would stop at 300.
        // SIDE EFFECT: if the value CHANGES, `onProgressChanged` is triggered
        //   and the label `tvDurationValue` is updated. If the value is already 0
        //   (the default), the listener does NOT fire — but the label in the XML is
        //   already "0 min", so the result is correct.
        sbDuration.setProgress(existing.getDuration());

        // `CheckBox.setChecked(boolean)` — it has no listener, so no reaction in a chain.
        cbonWatchList.setChecked(existing.isOnWatchlist());

        // `MaterialSwitch.setChecked(boolean)` — TRIGGERS the switch listener,
        //   which writes "Yes"/"No" into `tvWatchedValue`. That is why the listeners must be
        //   attached before `fillForm`.
        swWatched.setChecked(existing.isWatched());

        // `RatingBar.setRating(float)` — the parameter is `float`, exactly the type of the field
        //   in the model. A value of 4.5 lights 4 and a half stars.
        rbRating.setRating(existing.getRating());

        // `setText(null)` is legal: the field becomes empty. It throws no exception.
        // Here you see the naming difference from G1097: `getPosterUrl()`, not `getPoster()`.
        etPoster.setText(existing.getPosterUrl());

        // SPINNER — it has no `setValue`, it has `setSelection(index)`.
        // That is why a loop is needed: it looks for the INDEX of the matching value.
        // `getCount()` = the number of elements in `@array/movie_genre`.
        // `getItemAtPosition(index)` returns an `Object` (in practice, a `String`).
        // `break` at the first match: if the array had duplicates, the first wins.
        // If no match is found, the spinner stays on position 0 —
        //   silently, with no error.
        for(int index=0; index<spGenre.getCount(); index++)
        {
            if(spGenre.getItemAtPosition(index).equals(existing.getGenre()))
            {
                // `setSelection` moves the selection. It triggers no listener here,
                //   because we attached none on the Spinner.
                spGenre.setSelection(index);
                break;
            }
        }

        // RADIOGROUP — here too there is no `setValue`. `check(id)` receives an ID.
        // `getChildCount()` = 5 (the 5 MaterialRadioButtons).
        // `getChildAt(index)` returns a `View`; cast to `RadioButton` for `getText()`.
        for(int index=0; index<rgApproval.getChildCount(); index++)
        {
            RadioButton rb = (RadioButton) rgApproval.getChildAt(index);
            if(rb.getText().toString().equals(existing.getApproval()))
            {
                // `check(...)` receives the ID of the child, not the index. That is why
                //   it reads `getChildAt(index).getId()`.
                // Side effect: `check` automatically unchecks the previously checked button.
                rgApproval.check(rgApproval.getChildAt(index).getId());
                break;
            }
        }

        // WHAT THIS METHOD DOES NOT DO: it does not write into `releaseCal`.
        //   The text is displayed correctly, but the calendar stays on TODAY's date.
        //   See BUG 4, right after the list.
    }

    // ==================================================================================
    // initControls — 13 FINDVIEWBYID CALLS, IN THE ORDER OF THE FORM
    // ==================================================================================
    // WHAT IT DOES: binds each Java field to the view with the corresponding id from the XML.
    // HOW: 13 `findViewById` calls, in the visual order of the form.
    // RISK: each id must exist in `activity_item.xml`. A wrong id
    //   returns `null` and crashes on the first access, not here.
    // REF: `MainActivity.initControls` has the same role, with 2 views.
    // ==================================================================================
    private void initControls() {

        // The "Basics" card: the title and the date.
        etTitle = findViewById(R.id.etTitle);
        etRelease = findViewById(R.id.etRelease);

        // The genre — a Spinner wrapped in a MaterialCardView in the XML.
        spGenre = findViewById(R.id.spGenre);

        // The "Production" card: the budget and the duration.
        etBudget = findViewById(R.id.etBudget);
        sbDuration = findViewById(R.id.sbDuration);

        // The "Your verdict" card: rating, guidance, switch, check mark.
        rbRating = findViewById(R.id.rbRating);
        rgApproval = findViewById(R.id.rgApproval);

        // WATCH OUT: the id is `cbOnWatchList` (with a capital `O`), the field is
        //   `cbonWatchList` (with a lowercase `o`). They differ only by the capital letter.
        cbonWatchList = findViewById(R.id.cbOnWatchList);

        swWatched = findViewById(R.id.swWatched);

        // The "Poster" card.
        etPoster = findViewById(R.id.etPoster);

        // The save button. The id is `btnMovieAction`, the field `btMovieAction`.
        btMovieAction = findViewById(R.id.btnMovieAction);

        // The two dynamic labels. They are read only by the listeners below.
        tvDurationValue = findViewById(R.id.tvDurationValue);
        tvWatchedValue = findViewById(R.id.tvWatchedValue);
    }

    // ==================================================================================
    // initListeners — FOUR LISTENERS
    // ==================================================================================
    // WHAT IT DOES: attaches the behaviour of the four interactive widgets.
    // HOW: 1. etRelease -> opens the date dialog
    //      2. btMovieAction -> saveMovie
    //      3. swWatched -> updates the "Yes"/"No" label
    //      4. sbDuration -> updates the "N min." label
    // RISK: the switch listener and the SeekBar one are essential for `fillForm`;
    //   without them, the labels would stay out of sync on edit.
    // REF: the same anonymous classes as in `MainActivity.initListeners`.
    // ==================================================================================
    private void initListeners() {

        // 1. CLICK ON THE DATE FIELD -> dialog.
        // `EditText` is also editable, but here the click is intercepted and replaced
        //   with a dialog. The user does not type the date, they choose it.
        etRelease.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showReleaseDatePicker();
            }
        });

        // 2. CLICK ON THE BUTTON -> save.
        // The same listener does insert or update; the difference is the `position` field.
        btMovieAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveMovie();
            }
        });

        // 3. SWITCH -> label.
        // `CompoundButton.OnCheckedChangeListener` has a single method, with two
        //   arguments: the button and the new state.
        // `@NonNull` on `buttonView` is a contract annotation, not a check
        //   at runtime.
        swWatched.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull CompoundButton buttonView, boolean isChecked) {
                // Ternary operator: "Yes" if it is checked, "No" otherwise.
                // `setText` expects a `CharSequence`; both literals are `String`.
                tvWatchedValue.setText(isChecked ? "Yes" : "No");
            }
        });

        // 4. SEEKBAR -> label.
        // The interface has THREE methods; two remain empty, but MUST be written:
        //   otherwise the anonymous class does not compile.
        sbDuration.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            // Called on every change of value.
            // `progress` = the new value (0..300).
            // `fromUser` = true if the change comes from the finger, false if it comes
            //   from code (`setProgress`). Here it is IGNORED on purpose: the label
            //   must reflect the value in both cases.
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // `progress + " min."` — int concatenated with String. Java converts
                //   the int to text automatically. Result: "148 min.".
                tvDurationValue.setText(progress + " min.");
            }

            // Called when the finger touches the bar. We do not need it.
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            // Called when the finger lifts. We do not need it.
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    // ==================================================================================
    // saveMovie — WIDGETS -> MODEL -> INTENT -> RESULT
    // ==================================================================================
    // WHAT IT DOES: builds a new `Movie` from the current values of the form,
    //   puts it in an Intent and sends it back to MainActivity.
    // HOW: 1. `new Movie()` — the empty constructor
    //      2. 9 setters directly from the widgets
    //      3. loop over the RadioGroup for the 10th field
    //      4. Intent without a target, with MOVIE_EXTRA and, conditionally, POSITION_EXTRA
    //      5. setResult + finish
    // RISK: `Double.parseDouble` on empty text throws `NumberFormatException` — BUG 1.
    // REF: each line is the inverse of a line in `fillForm`.
    // ==================================================================================
    private void saveMovie() {

        // A NEW object, not the received one. Even in edit mode, the movie in the list
        //   is not modified directly: it is replaced with this one, through `movies.set`.
        Movie movie = new Movie();

        // `getText()` returns an `Editable`, not a `String`. `toString()` does the conversion.
        movie.setTitle(etTitle.getText().toString());

        // THE DATE: from `releaseCal`, NOT from the text in `etRelease`.
        // `getTime()` turns the calendar into a new `Date`.
        // Here you see BUG 4: in edit mode, `releaseCal` was not synchronized.
        movie.setRelease(releaseCal.getTime());

        // THE GENRE: `getSelectedItem()` returns an `Object`; `toString()` turns it into text.
        // If the spinner were empty, it would throw an NPE — here the list comes from
        //   `@array/movie_genre` and is not empty.
        movie.setGenre(spGenre.getSelectedItem().toString());

        // BUG 1: `Double.parseDouble("")` throws `NumberFormatException`.
        // ==============================================================================
        // `etBudget` starts EMPTY in insert mode. If the user presses "Save Movie"
        //   without writing a budget, the app CRASHES. There is no validation beforehand.
        // FIX: `String text = etBudget.getText().toString();`
        //             `double value = text.isEmpty() ? 0 : Double.parseDouble(text);`
        // ==============================================================================
        movie.setBudget(Double.parseDouble(etBudget.getText().toString()));

        // THE DURATION: directly from the progress of the bar. `int`, no conversion.
        movie.setDuration(sbDuration.getProgress());

        // THE RATING: `float`, exactly the type of the field.
        movie.setRating(rbRating.getRating());

        // THE SWITCH: `isChecked()` returns a `boolean`.
        movie.setWatched(swWatched.isChecked());

        // THE CHECK MARK: `isChecked()` returns a `boolean`.
        movie.setOnWatchlist(cbonWatchList.isChecked());

        // THE POSTER: the raw text, with no URL validation. It can also be empty.
        // Here you see the second naming difference: `setPosterUrl`, not `setPoster`.
        movie.setPosterUrl(etPoster.getText().toString());

        // THE GUIDANCE: the only field that has no direct getter on the widget.
        // `RadioGroup` has no `getSelectedItem`; it only has `getCheckedRadioButtonId()`,
        //   which would require an extra lookup. The loop is simpler.
        for(int index=0; index<rgApproval.getChildCount(); index++)
        {
            RadioButton rb = (RadioButton) rgApproval.getChildAt(index);
            if(rb.isChecked())
            {
                // The text of the button, not its ID. See field 8 in `Movie.java`.
                movie.setApproval(rb.getText().toString());
                break;
            }
        }

        // THE RESULT INTENT — without a target.
        // `new Intent()` starts nothing. It is only a container for `setResult`.
        Intent intent = new Intent();

        // The movie, under the same key used by MainActivity on read.
        intent.putExtra(MainActivity.MOVIE_EXTRA, movie);

        // THE POSITION — ONLY in edit mode.
        // In insert mode, the extra is missing, and MainActivity reads `-1` as the default.
        // Thus, the absence of the position is exactly the signal to "add".
        if (position != -1)
            intent.putExtra(MainActivity.POSITION_EXTRA, position);

        // Hands over the result. `RESULT_OK` is the constant inherited from `Activity` (= -1).
        // Without this call, MainActivity would receive `RESULT_CANCELED` and would ignore the movie.
        setResult(RESULT_OK, intent);

        // Closes the activity. Without `finish()`, the form would stay on screen,
        //   and the result would be delivered only on pressing the Back button.
        finish();
    }

    // ==================================================================================
    // showReleaseDatePicker — THE DATE DIALOG
    // ==================================================================================
    // WHAT IT DOES: opens a `DatePickerDialog` positioned on the current date from
    //   `releaseCal` and writes the result into the calendar and into the text.
    // HOW: 1. if the text exists, it tries to parse it into `releaseCal`
    //      2. opens the dialog with the year/month/day from `releaseCal`
    //      3. on confirm, it writes into `releaseCal` and formats the text
    // RISK: `parse` can throw `ParseException`; it is caught and IGNORED.
    // REF: the only place in the app that uses `DatePickerDialog`.
    // ==================================================================================
    private void showReleaseDatePicker() {

        // The current text in the field. It can be empty (insert) or a formatted date (edit).
        String current = etRelease.getText().toString();

        // If text exists, try to turn it into a date.
        if (!current.isEmpty()) {
            try {
                // `parse` returns `null` if the text cannot be fully interpreted.
                Date parsed = dateFormat.parse(current);
                if (parsed != null)
                    // Synchronizes the calendar with the text. This is the ONLY place
                    //   where `releaseCal` receives the date from the form — hence BUG 4.
                    releaseCal.setTime(parsed);
            } catch (ParseException ignored) { }
            // Empty `catch`, on purpose: invalid text does not block the dialog.
            //   The calendar stays on the previous value, and the dialog opens
            //   on it. The user corrects from the dialog.
        }

        // Builds the dialog.
        // `this` = the Context (the activity).
        // The second argument is the confirm listener, as an anonymous class.
        // The next three are the initial values: year, month, day.
        new DatePickerDialog(
                this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int day) {

                        // `month` is already 0-based, exactly as `Calendar.set` expects it.
                        //   Do NOT subtract 1. The confusion appears with `GregorianCalendar`, where
                        //   the `Calendar.JULY` constants hide the same convention.
                        releaseCal.set(year, month, day);

                        // Clears any error displayed on the field. `setError(null)`
                        //   hides the icon and the message under the `TextInputLayout`.
                        etRelease.setError(null);

                        // Writes the formatted text. Here `releaseCal` is already updated,
                        //   so the text and the calendar are synchronized.
                        etRelease.setText(dateFormat.format(releaseCal.getTime()));
                    }
                },

                // The initial values of the dialog wheels: from the calendar, not from the text.
                releaseCal.get(Calendar.YEAR),
                releaseCal.get(Calendar.MONTH),
                releaseCal.get(Calendar.DAY_OF_MONTH)

        // `show()` is mandatory. The constructor creates the dialog, but does not display it.
        ).show();
    }
}
