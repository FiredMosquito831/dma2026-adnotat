/*
 * ======================================================================================
 * FILE: ItemActivity.java — THE INSERT / EDIT FORM
 * ======================================================================================
 * WHAT IT IS: an activity with 13 views, a Calendar and a date formatter.
 *   It builds a `Movie` from the values in the form and sends it back.
 *
 * WHY IT EXISTS: `MainActivity` is not allowed to create model objects from a form.
 *   The separation means the form can be changed (another layout, other fields) without
 *   touching the list.
 *
 * WHAT IT KNOWS: the current values of the 10 fields, the mode (insert or edit) and the position.
 *   It does not know what happens to the movie after `finish()`.
 *
 * WHAT WOULD BREAK WITHOUT IT: the application would only have the demo list, with no way
 *   to add or modify a movie.
 * ======================================================================================
 */

package ro.ase.ie.g1097.activities;

// ======================================================================================
// IMPORTS — WHAT EACH ONE BRINGS
// ======================================================================================
// android.app.DatePickerDialog   — the date-picking dialog
// android.content.Intent         — reading the extras on the way in and building
//                                  the result Intent
// android.os.Bundle              — the onCreate signature
// android.view.View              — the parameter in the click listeners
// android.widget.*               — the form's 13 widgets
// androidx.activity.EdgeToEdge   — drawing under the system bars
// androidx.annotation.NonNull    — the OnCheckedChangeListener contract
// androidx.core.content.IntentCompat — typed reading of a Serializable (API 33+)
// androidx.core.view.*           — insets
// com.google.android.material.*  — MaterialSwitch, Snackbar
// java.text.*                    — date parsing and formatting
// java.util.*                    — Calendar, Date, Locale
// ro.ase.ie.g1097.*              — R, binding, model
// ======================================================================================

// The native calendar dialog. It opens on a click on the date field.
import android.app.DatePickerDialog;

// Intent: on the way in, it brings the movie to edit; on the way out, it carries the saved movie.
import android.content.Intent;

// Bundle: the state saved by the system. Unused in the body, only in the signature.
import android.os.Bundle;

// View: the parameter type in `OnClickListener`.
import android.view.View;

// Button: the type of the save button. In XML it is a `MaterialButton`, which extends Button.
import android.widget.Button;

// CheckBox: the declared type of the field, although the real object is a `MaterialCheckBox`.
import android.widget.CheckBox;

// CompoundButton: the common interface for Switch and CheckBox; it appears in the listener.
import android.widget.CompoundButton;

// DatePicker: the first parameter of the `OnDateSetListener` callback.
import android.widget.DatePicker;

// EditText: the declared type for the 4 text fields.
import android.widget.EditText;

// RadioButton: used in the loop over the children of the RadioGroup.
import android.widget.RadioButton;

// RadioGroup: the container of the 5 guidance buttons.
import android.widget.RadioGroup;

// RatingBar: the stars.
import android.widget.RatingBar;

// SeekBar: the duration slider.
import android.widget.SeekBar;

// Spinner: the drop-down list of genres.
import android.widget.Spinner;

// Switch: imported, but the field is declared `MaterialSwitch`.
import android.widget.Switch;

// TextView: the dynamic labels for duration and watched.
import android.widget.TextView;

// Toast: imported in the source, but UNUSED in this file.
import android.widget.Toast;

// EdgeToEdge: content under the system bars.
import androidx.activity.EdgeToEdge;

// NonNull: the annotation from the onCheckedChanged signature.
import androidx.annotation.NonNull;

// AppCompatActivity: the base class.
import androidx.appcompat.app.AppCompatActivity;

// IntentCompat: an API 33 compatible variant for reading a typed Serializable.
import androidx.core.content.IntentCompat;

// Insets: the values of the system bars.
import androidx.core.graphics.Insets;

// ViewCompat: the compatible insets listener.
import androidx.core.view.ViewCompat;

// WindowInsetsCompat: the insets types.
import androidx.core.view.WindowInsetsCompat;

// MaterialSwitch: the Material switch (not the framework one).
import com.google.android.material.materialswitch.MaterialSwitch;

// Snackbar: imported, but UNUSED. Leftover from an earlier version.
import com.google.android.material.snackbar.Snackbar;

// ParseException: the exception thrown by `SimpleDateFormat.parse`.
import java.text.ParseException;

// SimpleDateFormat: formatting and parsing "dd/MM/yyyy".
import java.text.SimpleDateFormat;

// Calendar: the source of truth for the date.
import java.util.Calendar;

// Date: the type in the model.
import java.util.Date;

// Locale: the date format depends on the device language.
import java.util.Locale;

// R: the IDs from the layout.
import ro.ase.ie.g1097.R;

// ActivityItemBinding: generated by View Binding. Imported, but UNUSED —
//   the source uses `findViewById`. Leftover from a variant with binding.
import ro.ase.ie.g1097.databinding.ActivityItemBinding;

// The model.
import ro.ase.ie.g1097.model.Movie;

/*
 * ======================================================================================
 * THEORY BLOCK: THE FORM AS A STATE MACHINE
 * ======================================================================================
 *
 * 1. **Two modes, one activity.** `ItemActivity` does not have two layouts and
 *    two classes. It has a `position` field that is `-1` (insert) or `>= 0` (edit).
 *    All the different behaviour derives from this field.
 *
 * 2. **The source of truth is not always the widget.** For 9 fields, the widget
 *    is the source (you read directly from it on save). For the date, the source is
 *    `releaseCal`, and the `EditText` is only a DISPLAY.
 *
 * 3. **The listeners are also a source of truth for the labels.** `tvDurationValue`
 *    and `tvWatchedValue` are not read on save; they are updated by the widget
 *    listeners. If a listener does not fire, the label lags behind.
 *
 * 4. **The result is an Intent.** The activity does not return an object through a return value;
 *    it puts it in an `Intent` and hands it over through `setResult`.
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
 *   [rgApproval] --(loop)---+----------+
 *   [etPoster] ------------------------+
 * ======================================================================================
 */
public class ItemActivity extends AppCompatActivity {

    // ==================================================================================
    // THE 13 REFERENCES TO THE VIEWS
    // ==================================================================================
    // WHY 13: the form has 10 data fields + 2 dynamic labels + 1 button.
    //   The 7 fixed-label `TextView`s (`tvTile`, `tvGenre`, `tvBudget`,
    //   `tvDuration`, `tvRating`, `tvWatched`, `tvGuidance`) do NOT appear here: they are not
    //   read and they are not modified from code, so they need no reference.
    //
    // WHY THEY ARE ALL `private`: they are implementation details of the screen.
    // ==================================================================================

    // Field 1: the title. The real object in XML is a `TextInputEditText`, but the declared
    // type is `EditText` — the parent class. It is enough for `getText()`.
    private EditText etTitle;

    // Field 2: the date, as TEXT. The real value is in `releaseCal` (see below).
    private EditText etRelease;

    // Field 3: the genre. `Spinner` — the drop-down list from `@array/movie_genre`.
    private Spinner spGenre;

    // Field 4: the budget. `inputType="numberDecimal"` in XML.
    private EditText etBudget;

    // Field 5: the duration. `SeekBar` with max 300.
    private SeekBar sbDuration;

    // Field 6: the rating. `RatingBar` with 5 stars and a 0.5 step.
    private RatingBar rbRating;

    // Field 7: the parental guidance. `RadioGroup` with 5 buttons.
    private RadioGroup rgApproval;

    // Field 8: the "Watchlist" check mark.
    // WATCH THE NAME: the field is `cbonWatchList`, the ID is `cbOnWatchList`.
    //   One `o` missing on one side, one `O` extra on the other. It is not a typo to fix:
    //   that is how the source is. `findViewById(R.id.cbonWatchList)` would NOT compile.
    private CheckBox cbonWatchList;

    // Field 9: the "Recommended" switch. The real type is `MaterialSwitch`, which
    // extends `SwitchCompat` -> `CompoundButton`. Declared `MaterialSwitch` in order
    // to have access to all of its methods.
    private MaterialSwitch swWatched;

    // Field 10: the poster URL.
    private EditText etPoster;

    // Field 11: the save button.
    // WATCH THE NAME: the field is `btMovieAction`, the ID is `btnMovieAction`.
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
    private Calendar releaseCal = Calendar.getInstance();

    // THE DATE FORMATTER.
    // "dd/MM/yyyy" = two-digit day / two-digit month / four-digit year.
    //   WARNING: `MM` = month, `mm` = minutes. Swapping them is a classic mistake.
    // `Locale.getDefault()` = the device language's format. Here the pattern is
    //   explicit, so Locale only influences the symbols and the internal order.
    // `final`: the reference does not change. The object, however, is MUTABLE.
    // RISK: `SimpleDateFormat` is not thread-safe. Here it is used exclusively on
    //   the UI thread, so there is no concurrency.
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    // ==================================================================================
    // onCreate — THREE PHASES: INFRASTRUCTURE, CONTROLS, MODE
    // ==================================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Phase 0: the AppCompat chain. Without it, the theme and the delegate do not exist.
        super.onCreate(savedInstanceState);

        // Edge-to-edge: the content can occupy the whole screen, including under the bars.
        // Here the root is a `ScrollView` with `android:id="@+id/main"`.
        EdgeToEdge.enable(this);

        // Inflates `activity_item.xml`. All 13 `findViewById` calls depend on it.
        setContentView(R.layout.activity_item);

        // Insets on the ScrollView root. Identical to MainActivity, but here the padding
        // is applied to a container that SCROLLS: the content scrolls inside
        // the safe area, not under it.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Phase 1: bind the 13 views to the fields.
        // WHY FIRST: `fillForm` needs them, and so does `initListeners`.
        initControls();

        // Phase 2: attach the listeners.
        // WHY BEFORE `fillForm`: `fillForm` calls `setChecked` and `setProgress`.
        //   Those calls FIRE the listeners, which update the
        //   `tvWatchedValue` and `tvDurationValue` labels. If the listeners were attached after
        //   `fillForm`, the labels would keep the default values from XML.
        initListeners();

        // Phase 3: read the incoming Intent.
        Intent intent = getIntent();

        // TYPED reading of the serializable.
        // ==============================================================================
        // WHY `IntentCompat` AND NOT `intent.getSerializableExtra(...)`:
        //   The old method is deprecated since API 33. `IntentCompat.getSerializableExtra`
        //   also receives the expected class, so it checks the type before the cast
        //   and works identically on all versions.
        // WHY `MainActivity.MOVIE_EXTRA`: the same constant as when sending. A typo
        //   here would produce `null` with no error at all.
        // THE VALUE: `null` in insert mode (the FAB puts no extra).
        // ==============================================================================
        Movie existing = IntentCompat.getSerializableExtra(intent, MainActivity.MOVIE_EXTRA, Movie.class);

        // EDIT MODE: there is a movie in the Intent.
        if (existing != null) {

            // Reads the position. Here `-1` would mean "a movie sent without a position" —
            // a case that does not happen in the normal flow, but it would turn the save
            // into an insert (a duplicate).
            position = intent.getIntExtra(MainActivity.POSITION_EXTRA, -1);

            // Fills the form with the values of the received movie.
            // THE OBJECT IS A COPY: the changes in the form do NOT reach the list in
            // MainActivity except through the result Intent.
            fillForm(existing);

            // Changes the screen title. `setTitle` is the Activity method,
            //   NOT the model setter — there is no conflict, the receiver differs.
            // WARNING: the application theme is `Theme.Material3.DayNight.NoActionBar`
            //   (see `themes.xml`), and the layout contains no `Toolbar`.
            //   Without an ActionBar, the title is not visible anywhere on screen — the change
            //   is real, but invisible. The visible feedback for the user
            //   is the button text, set on the next line.
            setTitle("Edit movie");

            // Changes the button text. The same button does insert or update;
            //   the label is the only visible difference for the user.
            btMovieAction.setText("Update Movie");
        }

        // INSERT MODE: `existing == null`. Nothing happens.
        //   `position` stays -1, the form keeps the default values from XML:
        //   empty title, date = today, genre = the first in the list, empty budget, duration 0,
        //   rating 0, switch off, check mark off, "G" checked, empty poster.
    }

    // ==================================================================================
    // fillForm — MODEL -> WIDGETS
    // ==================================================================================
    // 10 fields, in the order of the model. Each line is the inverse of a line in
    // `saveMovie`.
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
        //   `Movie` is built with the empty constructor and saved without a date, or if
        //   the data comes from an external source.
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

        // `SeekBar.setProgress(int)` — the value is automatically clamped to 0..max.
        // If the duration in the model were 500, the bar would stop at 300.
        // SIDE EFFECT: if the value CHANGES, `onProgressChanged` fires
        //   and the `tvDurationValue` label updates. If the value is already 0
        //   (the default), the listener does NOT fire — but the label in XML is
        //   already "0 min", so the result is correct.
        sbDuration.setProgress(existing.getDuration());

        // `CheckBox.setChecked(boolean)` — it has no listener, so no chained reaction.
        cbonWatchList.setChecked(existing.isOnWatchlist());

        // `setChecked` on the switch FIRES `OnCheckedChangeListener`.
        // The effect: `tvWatchedValue` receives "Yes" or "No".
        // The order in `onCreate` depends on THIS line: the listener must
        //   already be attached when `fillForm` runs.
        swWatched.setChecked(existing.isWatched());

        // `RatingBar.setRating(float)` — the parameter is `float`, exactly the type in the model.
        // No listener: the stars are drawn directly.
        rbRating.setRating(existing.getRating());

        // `setText(null)` is legal: the field becomes empty. It does not throw.
        etPoster.setText(existing.getPoster());

        // ==============================================================================
        // THE LOOP FOR THE SPINNER — matching on TEXT
        // ==============================================================================
        // `Spinner` has no `setSelectionByValue`. It only has `setSelection(int)`.
        //   So you have to look for the index that corresponds to the value in the model.
        // `getCount()` = the number of elements in `@array/movie_genre`.
        // `getItemAtPosition(index)` returns `Object`; here, a `String`.
        // `equals` (not `==`): it compares content, not references.
        // CONCRETE RISK: "Animation" (Toy Story's genre) does not exist in
        //   `@array/movie_genre`. The loop finds nothing, the spinner stays on
        //   position 0 ("Drama"), and on save the movie changes its genre WITHOUT
        //   telling anyone. It is the most likely place where you will lose points
        //   in a live demonstration, because no error appears.
        // ==============================================================================
        for(int index=0; index<spGenre.getCount(); index++)
        {
            if(spGenre.getItemAtPosition(index).equals(existing.getGenre()))
            {
                // Sets the selection and stops the loop. Without `break`, it would keep going
                //   for nothing — but the result would be identical.
                spGenre.setSelection(index);
                break;
            }
        }

        // ==============================================================================
        // THE LOOP FOR THE RADIOGROUP — matching on the BUTTON TEXT
        // ==============================================================================
        // `getChildCount()` = 5 (the buttons from XML).
        // `getChildAt(index)` returns `View`; cast to `RadioButton` for `getText()`.
        // `check(id)` — RadioGroup wants the ID, not the index. Hence `getChildAt(index).getId()`.
        // WHY it is generic: you add a 6th button in XML and the code sees it
        //   with no modification at all.
        // RISK: the matching is done on text. Renaming "PG-13" -> "PG13" in XML breaks
        //   editing for movies saved with the old label.
        // ==============================================================================
        for(int index=0; index<rgApproval.getChildCount(); index++)
        {
            RadioButton rb = (RadioButton) rgApproval.getChildAt(index);
            if(rb.getText().toString().equals(existing.getApproval()))
            {
                // `check` automatically unchecks any other button in the group.
                rgApproval.check(rgApproval.getChildAt(index).getId());
                break;
            }
        }
    }

    // ==================================================================================
    // initControls — THE 13 findViewById CALLS
    // ==================================================================================
    // WHY ALL IN ONE METHOD: if an ID is wrong, `findViewById` returns
    //   `null` with no error, and the NPE only appears at the first access — somewhere far away.
    //   Grouped here, you can visually check that every line has an existing ID.
    //
    // THE ORDER: it does not matter to the compiler. It is the order of the fields in the class,
    //   that is, the order in the form, top to bottom.
    // ==================================================================================
    private void initControls() {

        // 1. Title — TextInputEditText etTitle (the "Basics" card).
        etTitle = findViewById(R.id.etTitle);

        // 2. Date — TextInputEditText etRelease (the "Basics" card).
        etRelease = findViewById(R.id.etRelease);

        // 3. Genre — Spinner spGenre (the "Basics" card).
        spGenre = findViewById(R.id.spGenre);

        // 4. Budget — TextInputEditText etBudget (the "Production" card).
        etBudget = findViewById(R.id.etBudget);

        // 5. Duration — SeekBar sbDuration (the "Production" card).
        sbDuration = findViewById(R.id.sbDuration);

        // 6. Rating — RatingBar rbRating (the "Your verdict" card).
        rbRating = findViewById(R.id.rbRating);

        // 7. Guidance — RadioGroup rgApproval (the "Your verdict" card).
        rgApproval = findViewById(R.id.rgApproval);

        // 8. Watchlist — ID `cbOnWatchList` (with "On"), field `cbonWatchList` (without "O").
        cbonWatchList = findViewById(R.id.cbOnWatchList);

        // 9. Switch — MaterialSwitch swWatched.
        swWatched = findViewById(R.id.swWatched);

        // 10. Poster — TextInputEditText etPoster (the "Poster" card).
        etPoster = findViewById(R.id.etPoster);

        // 11. Button — ID `btnMovieAction`, field `btMovieAction`.
        btMovieAction = findViewById(R.id.btnMovieAction);

        // 12. Duration label — TextView tvDurationValue.
        tvDurationValue = findViewById(R.id.tvDurationValue);

        // 13. Switch label — TextView tvWatchedValue.
        tvWatchedValue = findViewById(R.id.tvWatchedValue);
    }

    // ==================================================================================
    // initListeners — FOUR LISTENERS
    // ==================================================================================
    // 1. click on etRelease   -> opens the date dialog
    // 2. click on the button  -> saveMovie()
    // 3. switch changed       -> updates "Yes"/"No"
    // 4. seekbar moved        -> updates "NNN min."
    // ==================================================================================
    private void initListeners() {

        // LISTENER 1: click on the date field.
        // ==============================================================================
        // WHY `setOnClickListener` ON AN `EditText`: an EditText receives focus on
        //   touch, not click. Still, `setOnClickListener` works: on the first
        //   touch it receives focus AND fires the click.
        // RISK: if the user presses a second time (the field already has focus), the click
        //   fires again — the dialog reopens. That is the desired behaviour.
        // BIGGER RISK: the field has `inputType="date"`, so the user CAN type.
        //   The typed text is not read into `releaseCal` until the dialog is opened.
        //   If they type and press Save directly, the old date from `releaseCal` is saved.
        // ==============================================================================
        etRelease.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showReleaseDatePicker();
            }
        });

        // LISTENER 2: click on the save button.
        // There is no validation before `saveMovie()`. See BUG 1.
        btMovieAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveMovie();
            }
        });

        // LISTENER 3: the "Recommended" switch.
        // ==============================================================================
        // `CompoundButton.OnCheckedChangeListener` has a single method, with two parameters:
        //   `buttonView` — the switch that changed (unused here)
        //   `isChecked`  — the new state
        // `@NonNull` is the annotation from the interface signature; we copy it.
        // The ternary operator picks the text in a single expression.
        // IT ALSO FIRES PROGRAMMATICALLY: `fillForm` -> `swWatched.setChecked(...)`
        //   -> listener -> the label updates. That is why the order in onCreate matters.
        // ==============================================================================
        swWatched.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull CompoundButton buttonView, boolean isChecked) {
                tvWatchedValue.setText(isChecked ? "Yes" : "No");
            }
        });

        // LISTENER 4: the duration slider.
        // ==============================================================================
        // `SeekBar.OnSeekBarChangeListener` has THREE methods. All must be implemented,
        //   even if two are empty — otherwise the anonymous class does not compile.
        //
        // `onProgressChanged(seekBar, progress, fromUser)`:
        //   `progress`  — the new value (0..300)
        //   `fromUser`  — `true` if the user dragged the slider, `false` if
        //                 the value was set from code (`setProgress` in `fillForm`)
        //
        // WHY `fromUser` IS IGNORED HERE: we want the label to ALWAYS reflect
        //   the real value, regardless of who changed it. If you filtered on
        //   `fromUser`, on edit the label would stay at "0 min" until the first touch.
        // ==============================================================================
        sbDuration.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                // Concatenation: `int + String` -> String. Result: "148 min.".
                // The full stop is in the text, not in the value.
                tvDurationValue.setText(progress + " min.");
            }

            // Method 2: from touch to movement. We do not need it.
            // Empty body = intentional, not forgotten. The interface requires it.
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            // Method 3: when the finger is lifted. We do not need it.
            // This is where you could "commit" the value or trigger haptics.
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    // ==================================================================================
    // saveMovie — WIDGETS -> MODEL -> INTENT
    // ==================================================================================
    // Five steps, in order:
    //   1. create the empty object
    //   2. read 9 fields directly from the widgets
    //   3. read the 10th field (approval) through a loop
    //   4. put the object in the Intent, plus the position if it exists
    //   5. setResult + finish
    // ==================================================================================
    private void saveMovie() {

        // STEP 1: an empty object. It works only because `Movie` has an explicitly written
        // no-argument constructor (see 22.1).
        Movie movie = new Movie();

        // STEP 2: the 9 simple fields.
        // Each line is the inverse of a line in `fillForm`.
        // `getText()` returns `Editable`; `toString()` turns it into a `String`.
        movie.setTitle(etTitle.getText().toString());

        // `releaseCal.getTime()` — NOT `etRelease.getText()`.
        // ==============================================================================
        // WHY: the calendar is the source of truth. The text can be empty, can be typed
        //   wrongly, can be in another format. The calendar was updated only by
        //   the dialog, with guaranteed valid values.
        // `getTime()` builds a NEW `Date` on every call, but with the same
        //   milliseconds.
        // ==============================================================================
        movie.setRelease(releaseCal.getTime());

        // `getSelectedItem()` returns `Object`; `toString()` turns it into a `String`.
        // RISK: if the spinner were empty, it would be `null` -> NPE. Here `entries` in XML
        //   guarantees at least one element.
        movie.setGenre(spGenre.getSelectedItem().toString());

        // BUG 1: `Double.parseDouble("")` throws `NumberFormatException`.
        // ==============================================================================
        // WHEN IT HAPPENS: on insert, the budget field is empty. The user presses
        //   "Save Movie" without filling in the budget -> the application closes.
        // WHY IT IS NOT VISIBLE IN THE DEMO: the 3 movies have a budget filled in, and on edit
        //   the field is filled by `fillForm`.
        // FIX: `String budgetText = etBudget.getText().toString();
        //              double budget = budgetText.isEmpty() ? 0 : Double.parseDouble(budgetText);`
        // ADDITIONAL RISK: on keyboards that produce a decimal comma ("1,5"),
        //   `parseDouble` throws the same exception — `Double.parseDouble` accepts only
        //   the full stop, regardless of `Locale`.
        // ==============================================================================
        movie.setBudget(Double.parseDouble(etBudget.getText().toString()));

        // `getProgress()` returns `int`, exactly the type of the field.
        movie.setDuration(sbDuration.getProgress());

        // `getRating()` returns `float`, exactly the type of the field.
        movie.setRating(rbRating.getRating());

        // `isChecked()` returns `boolean`.
        movie.setWatched(swWatched.isChecked());

        // The second check mark, independent of the first.
        movie.setOnWatchlist(cbonWatchList.isChecked());

        // The poster can be empty — `""`, not `null`. The model accepts both.
        movie.setPoster(etPoster.getText().toString());

        // STEP 3: the 10th field — the guidance.
        // ==============================================================================
        // RadioGroup has no "getSelectedItem". It has "getCheckedRadioButtonId()", but
        //   the source prefers the loop, so that it can take the TEXT, not the ID.
        // `getChildCount()` = 5. `getChildAt` -> `View` -> cast to `RadioButton`.
        // `isChecked()` — exactly one button is checked (RadioGroup guarantees that).
        // `break` after the first one found: the rest are unchecked anyway.
        // If no button were checked, `approval` would stay `null` — but the XML
        //   has `android:checked="true"` on the first one, so it does not happen.
        // ==============================================================================
        for(int index=0; index<rgApproval.getChildCount(); index++)
        {
            RadioButton rb = (RadioButton) rgApproval.getChildAt(index);
            if(rb.isChecked())
            {
                movie.setApproval(rb.getText().toString());
                break;
            }
        }

        // STEP 4: build the RESULT Intent.
        // ==============================================================================
        // `new Intent()` WITH NO parameters: it is not a navigation Intent, but a
        //   data container. It has no target component; it is read by the launcher
        //   in MainActivity.
        // ==============================================================================
        Intent intent = new Intent();

        // `putExtra(String, Serializable)` — `Movie` implements the interface, so
        //   the overload is found. The object is serialised here.
        intent.putExtra(MainActivity.MOVIE_EXTRA, movie);

        // The position is sent ONLY in edit mode.
        // ==============================================================================
        // On insert, the key is missing. MainActivity reads with `getIntExtra(..., -1)`
        //   and receives exactly the sentinel. The absence of the key IS the "insert" information.
        // If you always sent the position (with -1), it would work identically — but the protocol
        //   would be less clear.
        // ==============================================================================
        if (position != -1)
            intent.putExtra(MainActivity.POSITION_EXTRA, position);

        // STEP 5a: hand over the result.
        // `RESULT_OK` is the constant `Activity.RESULT_OK` (-1). Without this call,
        //   the activity would close with `RESULT_CANCELED` and MainActivity would ignore it.
        setResult(RESULT_OK, intent);

        // STEP 5b: close the activity.
        // WHY AFTER `setResult`: `finish()` schedules the closing and the handing over
        //   of the result. The other way round, the result could leave before it is set.
        // `finish()` does NOT destroy the activity immediately: it goes through onPause -> onStop
        //   -> onDestroy, and MainActivity comes back to the foreground.
        finish();
    }

    // ==================================================================================
    // showReleaseDatePicker — THE DATE DIALOG
    // ==================================================================================
    // The flow:
    //   1. read the current text from the field
    //   2. if it is not empty, parse it and update `releaseCal`
    //   3. open the dialog positioned on the values in `releaseCal`
    //   4. on confirmation, write into `releaseCal` and into the text
    // ==================================================================================
    private void showReleaseDatePicker() {

        // The current text, as a String. It can be "" (empty field) or "16/07/2010".
        String current = etRelease.getText().toString();

        // Only if there is text do we attempt parsing. An empty string would throw
        // `ParseException` and would be caught below — but the check is cheaper.
        if (!current.isEmpty()) {
            try {

                // `parse` returns a `Date` or throws `ParseException`.
                // It can also return `null` if the text is empty — hence the check.
                Date parsed = dateFormat.parse(current);

                // `setTime(Date)` copies the milliseconds into the Calendar.
                if (parsed != null)
                    releaseCal.setTime(parsed);

            // An EMPTY `catch`, named `ignored`.
            // ==========================================================================
            // WHY IT IS ACCEPTABLE: if the text is not a valid date, the desired
            //   behaviour is "start from the previous date". There is nothing to report
            //   and nothing to fix — the dialog opens anyway.
            // WHEN it would NOT be acceptable: if the error had to be shown to the user
            //   or if it hid a logic problem.
            // ==========================================================================
            } catch (ParseException ignored) { }
        }

        // Builds the dialog. Parameters:
        //   1. `this` — the Context
        //   2. the confirmation listener (anonymous class)
        //   3. the initial year
        //   4. the initial month — 0-INDEXED, exactly what `Calendar` expects
        //   5. the initial day — 1-indexed
        // `.show()` — without it, the dialog is built but does not appear.
        new DatePickerDialog(
                this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int day) {

                        // Writes into the Calendar. WARNING: `set(year, month, day)` does NOT reset
                        // the time. The time stays the one from `Calendar.getInstance()` — that is,
                        // the moment the form was created.
                        // Consequence: two movies saved on the same day have Dates that
                        // differ at the millisecond level. For display (dd/MM/yyyy)
                        // it does not matter.
                        releaseCal.set(year, month, day);

                        // Clears any error message left on the field.
                        // `setError` is not called anywhere in this file, so
                        // the line is defensive — useful if validation is added later.
                        etRelease.setError(null);

                        // Writes the formatted text into the field. Here the link is visible:
                        // Calendar -> String -> EditText. On save, the path is the reverse:
                        // EditText -> (ignored) ; Calendar -> Date -> model.
                        etRelease.setText(dateFormat.format(releaseCal.getTime()));
                    }
                },
                // The initial values of the dialog, read from the Calendar.
                // `Calendar.MONTH` is already 0-indexed, so it is passed directly.
                releaseCal.get(Calendar.YEAR),
                releaseCal.get(Calendar.MONTH),
                releaseCal.get(Calendar.DAY_OF_MONTH)
        ).show();
    }
}
