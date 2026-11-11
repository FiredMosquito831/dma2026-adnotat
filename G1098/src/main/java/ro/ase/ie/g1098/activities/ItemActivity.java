/*
 * ======================================================================================
 * FILE: ItemActivity.java — THE INSERT AND EDIT FORM
 * ======================================================================================
 * WHAT IT IS: the second activity. It shows a form with 13 controls, reads the values
 *   and packs them into a `Movie`, which it sends back to MainActivity.
 *
 * WHY IT EXISTS: MainActivity holds the list, but has no room on the screen for a form.
 *   Splitting into two activities is the classic "list + detail" pattern.
 *
 * WHAT IT KNOWS: the 13 widgets, a `Calendar` that is the source of truth for the date,
 *   a `SimpleDateFormat`, the position in the list (-1 = insert) and the extra keys.
 *
 * WHAT IT DOES NOT DO: it does not hold the list, it does not know what is before or after
 *   the edited movie, it does not save anything to disk. It produces a `Movie` and hands it over.
 *
 * WHAT BREAKS WITHOUT IT: the "+" button would open a non-existent activity, and
 *   a click on a row would throw `ActivityNotFoundException`.
 * ======================================================================================
 */

// ======================================================================================
// THE PACKAGE
// ======================================================================================
// The same package as MainActivity: `activities`. That is why `MainActivity.MOVIE_EXTRA`
// is visible here without an import — `protected` + the same package.
// ======================================================================================
package ro.ase.ie.g1098.activities;

// ======================================================================================
// IMPORTS — ALL USED
// ======================================================================================
// The difference from G1097: there were four dead imports there (`Switch`, `Toast`,
// `Snackbar`, `ActivityItemBinding`). Here every import corresponds to a name written
// in the file. The check is mechanical: look for the name in the body of the class.
// ======================================================================================

// DatePickerDialog: the date picker dialog.
import android.app.DatePickerDialog;

// Intent: the object we put the result in.
import android.content.Intent;

// Bundle: only in the onCreate signature.
import android.os.Bundle;

// View: the type of the onClick parameter and the base class of the widgets.
import android.view.View;

// Button: the "Save Movie" / "Update Movie" button.
import android.widget.Button;

// CheckBox: the "Watchlist" check box. The declared type is CheckBox, although the widget in the XML
//   is MaterialCheckBox — which extends CheckBox.
import android.widget.CheckBox;

// CompoundButton: the interface for switch/checkbox; `OnCheckedChangeListener` lives here.
import android.widget.CompoundButton;

// DatePicker: the parameter in onDateSet.
import android.widget.DatePicker;

// EditText: title, date, budget, poster.
import android.widget.EditText;

// RadioButton: the buttons in the guidance group.
import android.widget.RadioButton;

// RadioGroup: the container that guarantees the single selection.
import android.widget.RadioGroup;

// RatingBar: the stars.
import android.widget.RatingBar;

// SeekBar: the duration slider.
import android.widget.SeekBar;

// Spinner: the genre list.
import android.widget.Spinner;

// TextView: the two dynamic labels (the duration and the switch's value).
import android.widget.TextView;

// EdgeToEdge: drawing under the system bars.
import androidx.activity.EdgeToEdge;

// NonNull: an annotation on the onCheckedChanged parameter. It does not change behaviour;
//   it documents that Android guarantees a non-null object.
import androidx.annotation.NonNull;

// AppCompatActivity: the base class.
import androidx.appcompat.app.AppCompatActivity;

// IntentCompat: a compatible variant of `getSerializableExtra`, with an explicit type.
import androidx.core.content.IntentCompat;

// Insets / ViewCompat / WindowInsetsCompat: the same as in MainActivity.
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// MaterialSwitch: the Material switch. NOT `android.widget.Switch` — that is another class.
import com.google.android.material.materialswitch.MaterialSwitch;

// ParseException: the exception thrown by `SimpleDateFormat.parse`.
import java.text.ParseException;

// SimpleDateFormat: the Date <-> String conversion.
import java.text.SimpleDateFormat;

// Calendar: the date fields and the source of truth for release.
import java.util.Calendar;

// Date: the type returned by `releaseCal.getTime()` and by `dateFormat.parse(...)`.
import java.util.Date;

// Locale: the regional format for dates.
import java.util.Locale;

// R and Movie.
import ro.ase.ie.g1098.R;
import ro.ase.ie.g1098.model.Movie;

/*
 * ======================================================================================
 * THEORY BLOCK: THE FORM AS A STATE MACHINE
 * ======================================================================================
 *
 * 1. **Two modes, a single activity.** `position == -1` means INSERT;
 *    `position >= 0` means EDIT. There are no two separate activities, because
 *    the form is identical; only the initial values and the button label differ.
 *
 * 2. **The widgets are the source of truth for 9 fields, Calendar for one.**
 *    The title is read from `etTitle`, the budget from `etBudget` etc. The date is NOT read
 *    from `etRelease`: the text is only a DISPLAY, the real value sits in `releaseCal`.
 *
 *        [etRelease]  --(display)-->  "16/07/2010"
 *        [releaseCal] --(value)-->  Date  --> saveMovie() --> Movie
 *
 * 3. **The result goes back through an Intent.** `setResult(RESULT_OK, intent)` +
 *    `finish()` is the mandatory pair. Without `finish()`, the activity stays
 *    open; without `setResult`, MainActivity receives `RESULT_CANCELED`.
 *
 * 4. **Validation is missing.** The form checks nothing: an empty budget crashes,
 *    a null date crashes, an unmarked guidance leaves `approval` null. They are real bugs,
 *    marked below with `BUG:`.
 *
 * WHAT HAPPENS ON EACH PRESS:
 *
 *   etRelease click      -> showReleaseDatePicker() -> dialog -> releaseCal + text
 *   btnMovieAction click -> saveMovie() -> Intent -> setResult -> finish
 *   swWatched toggle     -> tvWatchedValue.setText("Yes"/"No")
 *   sbDuration drag      -> tvDurationValue.setText(progress + " min.")
 * ======================================================================================
 */
public class ItemActivity extends AppCompatActivity {

    // ==================================================================================
    // THE 13 WIDGETS — fields, bound in initControls()
    // ==================================================================================
    // They are all `private`: nobody from outside is allowed to touch them. All are
    //   bound with `findViewById` in the same method, so an ID mistake is
    //   visible in a single place.
    // ==================================================================================

    // 1. The title — TextInputEditText.
    private EditText etTitle;

    // 2. The date — TextInputEditText, but it is NOT read from it (see releaseCal).
    private EditText etRelease;

    // 3. The genre — Spinner with `@array/movie_genre`.
    private Spinner spGenre;

    // 4. The budget — TextInputEditText with inputType=numberDecimal.
    private EditText etBudget;

    // 5. The duration — SeekBar 0..300.
    private SeekBar sbDuration;

    // 6. Rating — RatingBar with 5 stars and a step of 0.5.
    private RatingBar rbRating;

    // 7. The guidance — RadioGroup with 5 buttons.
    private RadioGroup rgApproval;

    // 8. The Watchlist check box — ID `cbOnWatchList`, field `cbonWatchList`.
    private CheckBox cbonWatchList;

    // 9. The switch — MaterialSwitch, NOT android.widget.Switch.
    private MaterialSwitch swWatched;

    // 10. The poster — TextInputEditText with inputType=textUri.
    private EditText etPoster;

    // 11. The action button — "Save Movie" or "Update Movie".
    private Button btMovieAction;

    // 12. The label that shows the current duration.
    private TextView tvDurationValue;

    // 13. The label that shows "Yes"/"No" for the switch.
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
    // "dd/MM/yyyy" = day with 2 digits / month with 2 digits / year with 4 digits.
    //   WATCH OUT: `MM` = month, `mm` = minutes. Swapping them is a classic mistake.
    // `Locale.getDefault()` = the device language's format.
    // `final`: the reference does not change. The object, however, is MUTABLE.
    // RISK: `SimpleDateFormat` is not thread-safe. Here it is used exclusively on
    //   the UI thread, so there is no concurrency.
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    // ==================================================================================
    // onCreate — BUILDING THE FORM
    // ==================================================================================
    // WHAT IT DOES: binds the layout, binds the widgets, installs the listeners and then
    //   checks whether it was started for edit.
    // HOW: initControls -> initListeners -> reading the Intent -> fillForm.
    // RISK: the order matters. `fillForm` touches widgets, so it must run
    //   after `initControls`. The listeners must be installed before `fillForm`,
    //   otherwise `setChecked` would trigger the listener? No: the listener is installed
    //   before, and `setChecked` DOES TRIGGER IT — that is why the "Yes"/"No" label
    //   updates correctly on edit.
    // REF: chapter 22, section 22.3.
    // ==================================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // 1. The chain to the superclass, mandatory first.
        super.onCreate(savedInstanceState);

        // 2. Edge-to-edge.
        EdgeToEdge.enable(this);

        // 3. The form's layout.
        setContentView(R.layout.activity_item);

        // 4. Insets on the root. `R.id.main` is the ScrollView from the XML.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 5. The widgets, before anything touches them.
        initControls();

        // 6. The listeners, before fillForm (so setChecked updates the label).
        initListeners();

        // 7. The Intent that started the activity. It can contain a movie (edit)
        //    or it can be empty (insert).
        Intent intent = getIntent();

        // 8. Reading the movie. `IntentCompat.getSerializableExtra` is the
        //    typed variant: the third argument is the class, so no cast is needed any more.
        //    The result is A DESERIALIZED COPY, not the original from the list.
        Movie existing = IntentCompat.getSerializableExtra(intent, MainActivity.MOVIE_EXTRA, Movie.class);

        // 9. If there is a movie, we are in EDIT mode.
        if (existing != null) {

            // 9a. The position in the list. If the extra is missing, it stays -1 — but
            //     in edit mode it is always sent by MainActivity.
            position = intent.getIntExtra(MainActivity.POSITION_EXTRA, -1);

            // 9b. We fill the form with the movie's values.
            fillForm(existing);

            // 9c. The screen title (the top bar).
            setTitle("Edit movie");

            // 9d. The button label.
            btMovieAction.setText("Update Movie");
        }
    }

    // ==================================================================================
    // fillForm — MODEL -> WIDGETS
    // ==================================================================================
    // WHAT IT DOES: copies the 10 fields of a `Movie` into the 13 controls.
    // HOW: 9 direct settings + 2 matching loops (Spinner, RadioGroup).
    // RISK: `existing.getRelease()` can be `null` -> `dateFormat.format(null)`
    //   throws an NPE. `getPosterUrl()` can be `null` -> `setText(null)` is legal.
    // REF: this method's pair is `saveMovie` — every setting here has
    //   an inverse equivalent there.
    // ==================================================================================
    private void fillForm(Movie existing) {

        // 1. The title: String -> EditText.
        etTitle.setText(existing.getTitle());

        // 2. The date: Date -> formatted String. WATCH OUT: `releaseCal` is NOT updated
        //    here. See BUG 3 below.
        etRelease.setText(dateFormat.format(existing.getRelease()));

        // 3. The budget: double -> String. `String.valueOf` avoids `"" + budget`.
        etBudget.setText(String.valueOf(existing.getBudget()));

        // 4. The duration: int -> progress on the SeekBar.
        sbDuration.setProgress(existing.getDuration());

        // 5. The Watchlist check box.
        cbonWatchList.setChecked(existing.isOnWatchlist());

        // 6. The switch. `setChecked` triggers the listener installed in onCreate,
        //    so `tvWatchedValue` receives "Yes"/"No" with no extra code.
        swWatched.setChecked(existing.isWatched());

        // 7. The rating: float -> stars.
        rbRating.setRating(existing.getRating());

        // 8. The poster: String -> EditText. It can be `null`; `setText(null)` shows empty.
        etPoster.setText(existing.getPosterUrl());

        // 9. The genre: a search in the Spinner by TEXT.
        //    `getCount()` = the number of elements in `@array/movie_genre`.
        for(int index=0; index<spGenre.getCount(); index++)
        {
            // `getItemAtPosition(index)` returns `Object`; `.equals` compares the texts.
            if(spGenre.getItemAtPosition(index).equals(existing.getGenre()))
            {
                // We found the genre: we select it and leave the loop.
                spGenre.setSelection(index);

                // Without `break`, we would keep searching and could select another element
                // if the list contained duplicates.
                break;
            }
        }

        // 10. The guidance: a search in the RadioGroup by the button's TEXT.
        //     `getChildCount()` = 5. `getChildAt(index)` returns `View`.
        for(int index=0; index<rgApproval.getChildCount(); index++)
        {
            // Cast to RadioButton so the text can be read.
            RadioButton rb = (RadioButton) rgApproval.getChildAt(index);

            // Match by text: "PG-13" in the model == "PG-13" in the XML.
            if(rb.getText().toString().equals(existing.getApproval()))
            {
                // `check` wants the ID, not the index. `check` automatically unchecks the rest.
                rgApproval.check(rgApproval.getChildAt(index).getId());

                // Only one button can be selected; we leave.
                break;
            }
        }
    }

    /*
     * ==============================================================================
     * THEORY BLOCK: findViewById vs ViewBinding
     * ==============================================================================
     *
     * 1. `findViewById(R.id.x)` searches the view tree at RUNTIME and returns
     *    `View`. The cast to the concrete type is implicit through the generic, but it is not
     *    checked by the compiler: a wrong ID or a wrong type only gives an error at
     *    runtime.
     *
     * 2. ViewBinding generates an `ActivityItemBinding` class with a field for each
     *    ID. `binding.etTitle` is checked at COMPILE time. Besides, there is no longer any need
     *    for 13 lines of `findViewById`.
     *
     * 3. Why it is not used here: this lab teaches the classic method.
     *    ViewBinding appears in DMA05, and the course treats it separately.
     *
     * 4. Mixing is allowed, but it costs: if you use binding for 12 fields
     *    and findViewById for the 13th, you have two mechanisms to maintain.
     * ==============================================================================
     */

    // ==================================================================================
    // initControls — THE 13 findViewById CALLS
    // ==================================================================================
    // WHAT IT DOES: binds each field to the widget with the corresponding ID.
    // HOW: `findViewById(R.id.x)` for each one.
    // RISK: a wrong ID returns `null` WITHOUT a compile error. The NPE shows up at
    //   the first access, somewhere far away. That is why they are all grouped here.
    // REF: the IDs from `activity_item.xml`.
    // ==================================================================================
    private void initControls() {

        // 1. Title.
        etTitle = findViewById(R.id.etTitle);

        // 2. Date.
        etRelease = findViewById(R.id.etRelease);

        // 3. The genre.
        spGenre = findViewById(R.id.spGenre);

        // 4. The budget.
        etBudget = findViewById(R.id.etBudget);

        // 5. The duration.
        sbDuration = findViewById(R.id.sbDuration);

        // 6. The rating.
        rbRating = findViewById(R.id.rbRating);

        // 7. The guidance.
        rgApproval = findViewById(R.id.rgApproval);

        // 8. The check box. The ID has a capital "On", the field has a lowercase "on": `cbOnWatchList` vs
        //    `cbonWatchList`. A typo here gives an NPE in fillForm.
        cbonWatchList = findViewById(R.id.cbOnWatchList);

        // 9. The switch.
        swWatched = findViewById(R.id.swWatched);

        // 10. The poster.
        etPoster = findViewById(R.id.etPoster);

        // 11. The button. The ID is `btnMovieAction`, the field `btMovieAction`.
        btMovieAction = findViewById(R.id.btnMovieAction);

        // 12. The duration label.
        tvDurationValue = findViewById(R.id.tvDurationValue);

        // 13. The switch label.
        tvWatchedValue = findViewById(R.id.tvWatchedValue);
    }

    // ==================================================================================
    // initListeners — THE 4 LISTENERS
    // ==================================================================================
    // WHAT IT DOES: binds the events to methods.
    // HOW: click on the date, click on the button, switch change, slider movement.
    // RISK: the listeners are anonymous classes; `this` inside is NOT the activity.
    // REF: `saveMovie()` and `showReleaseDatePicker()` are the methods called.
    // ==================================================================================
    private void initListeners() {

        // 1. Click on the date field -> dialog.
        //    `EditText` has no dedicated "click" event; `OnClickListener` works
        //    because EditText is a View.
        etRelease.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showReleaseDatePicker();
            }
        });

        // 2. Click on the button -> save.
        btMovieAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveMovie();
            }
        });

        // 3. Switch -> the "Yes"/"No" label.
        //    `OnCheckedChangeListener` is also called when the change comes from CODE
        //    (`setChecked` in fillForm), not only from the user.
        swWatched.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull CompoundButton buttonView, boolean isChecked) {
                // Ternary: a single expression for both states.
                tvWatchedValue.setText(isChecked ? "Yes" : "No");
            }
        });

        // 4. Slider -> the duration label.
        sbDuration.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            // 4a. On every progress change — including when it comes from code.
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // `progress` is int; concatenation with a String converts it automatically.
                tvDurationValue.setText(progress + " min.");
            }

            // 4b. When the slider is touched. We need nothing here.
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            // 4c. When the finger is lifted. We need nothing here.
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    // ==================================================================================
    // saveMovie — WIDGETS -> MODEL -> INTENT
    // ==================================================================================
    // WHAT IT DOES: builds a `Movie` from the form values, puts it in an
    //   Intent and sends it back to MainActivity.
    // HOW: 1) new Movie(); 2) 9 setters; 3) a loop for the guidance; 4) putExtra;
    //   5) setResult + finish.
    // RISK: `Double.parseDouble("")` throws `NumberFormatException`; `releaseCal`
    //   can contain an old date (see BUG 3).
    // REF: the inverse pair of `fillForm`.
    // ==================================================================================
    private void saveMovie() {

        // 1. An empty object. Here you can see why the no-argument constructor is mandatory.
        Movie movie = new Movie();

        // 2. The title, straight from the text.
        movie.setTitle(etTitle.getText().toString());

        // 3. The date — from the CALENDAR, not from the text.
        movie.setRelease(releaseCal.getTime());

        // 4. The genre — the text selected in the Spinner.
        movie.setGenre(spGenre.getSelectedItem().toString());

        // 5. The budget — parsing. BUG 1: if the user leaves the field empty,
        //    `Double.parseDouble("")` throws `NumberFormatException` and the app closes.
        movie.setBudget(Double.parseDouble(etBudget.getText().toString()));

        // 6. The duration — straight from the progress.
        movie.setDuration(sbDuration.getProgress());

        // 7. The rating.
        movie.setRating(rbRating.getRating());

        // 8. The switch.
        movie.setWatched(swWatched.isChecked());

        // 9. The check box.
        movie.setOnWatchlist(cbonWatchList.isChecked());

        // 10. The poster. It can be an empty string if the user wrote nothing.
        movie.setPosterUrl(etPoster.getText().toString());

        // 11. The guidance: we walk the group and take the text of the checked button.
        for(int index=0; index<rgApproval.getChildCount(); index++)
        {
            RadioButton rb = (RadioButton) rgApproval.getChildAt(index);

            // `isChecked()` — only one button can be checked in a RadioGroup.
            if(rb.isChecked())
            {
                movie.setApproval(rb.getText().toString());

                // We found the button; we stop searching.
                break;
            }
        }

        // 12. An Intent WITHOUT a target. We do not start an activity; we only carry data
        //     back. `new Intent()` is enough for `setResult`.
        Intent intent = new Intent();

        // 13. The object, serialized automatically.
        intent.putExtra(MainActivity.MOVIE_EXTRA, movie);

        // 14. The position, ONLY in edit mode. In insert, the key is missing, and
        //     `getIntExtra(..., -1)` in MainActivity returns -1.
        if (position != -1)
            intent.putExtra(MainActivity.POSITION_EXTRA, position);

        // 15. The result. Without this call, MainActivity would receive RESULT_CANCELED.
        setResult(RESULT_OK, intent);

        // 16. Closing the activity. Without it, the form stays on the screen.
        finish();
    }

    // ==================================================================================
    // showReleaseDatePicker — THE DATE DIALOG
    // ==================================================================================
    // WHAT IT DOES: opens a `DatePickerDialog` positioned on the date in `releaseCal`.
    // HOW: 1) it tries to parse the current text; 2) it opens the dialog; 3) on confirmation,
    //   it writes into the calendar and into the text.
    // RISK: manually typed text only reaches the calendar if the dialog is opened.
    // REF: the only place that writes into `releaseCal` after initialization.
    // ==================================================================================
    private void showReleaseDatePicker() {

        // 1. The current text in the field.
        String current = etRelease.getText().toString();

        // 2. If it is not empty, it tries to turn it into a Date.
        if (!current.isEmpty()) {
            try {
                // `parse` returns `null` if the text cannot be interpreted.
                Date parsed = dateFormat.parse(current);

                // If we have a valid date, we put it in the calendar.
                if (parsed != null)
                    releaseCal.setTime(parsed);

            // 3. Invalid text: we ignore it. The dialog opens on the old date.
            } catch (ParseException ignored) { }
        }

        // 4. The dialog. The constructor requires: Context, listener, year, month, day.
        new DatePickerDialog(
                this,

                // 4a. The confirmation listener.
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int day) {

                        // We write into the calendar. `month` is 0-based, exactly as Calendar wants it.
                        releaseCal.set(year, month, day);

                        // We clear any error shown on the field.
                        etRelease.setError(null);

                        // We update the displayed text from the calendar.
                        etRelease.setText(dateFormat.format(releaseCal.getTime()));
                    }
                },

                // 4b. The initial values: from the calendar, not from the text.
                releaseCal.get(Calendar.YEAR),
                releaseCal.get(Calendar.MONTH),
                releaseCal.get(Calendar.DAY_OF_MONTH)

        // 5. `show()` — without it, the dialog is built but does not appear.
        ).show();
    }
}
