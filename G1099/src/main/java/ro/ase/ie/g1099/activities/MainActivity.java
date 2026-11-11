/*
 * ======================================================================================
 * FILE: MainActivity.java — THE LIST SCREEN
 * ======================================================================================
 * WHAT IT IS: the start activity. It holds the list of movies, displays it with an `ArrayAdapter`
 *   in a `ListView` and starts `ItemActivity` for insert or edit.
 *
 * WHY IT EXISTS: it is the only place that holds the STATE of the app (the `movies` list).
 *   `ItemActivity` is just a form that produces a `Movie` and sends it back.
 *
 * WHAT IT DEPENDS ON: `ListView`, `ArrayAdapter`, `Intent`, the Activity Result API
 *   and the `Movie` model. It does not depend on any form widget.
 *
 * WHO DEPENDS ON IT: `ItemActivity`, which reads `MainActivity.MOVIE_EXTRA` and
 *   `MainActivity.POSITION_EXTRA` — the two constants are the contract between the screens.
 *
 * WHAT IT DOES NOT DO: it validates no data, it draws no custom rows, it saves
 *   nothing to disk. The list lives as long as the process.
 *
 * WHAT BREAKS WITHOUT IT: the app would start directly in a form without a list,
 *   and the saved result would have nowhere to return to.
 * ======================================================================================
 */

// THE PACKAGE: `activities` — the UI layer. The model sits in `model`, not here.
package ro.ase.ie.g1099.activities;

// ======================================================================================
// IMPORTS — GROUPED BY SOURCE
// ======================================================================================
// android.*        — framework classes (Intent, Bundle, View, widgets)
// androidx.*       — Jetpack libraries (AppCompat, EdgeToEdge, insets, activity result)
// com.google.*     — Material Components (FloatingActionButton)
// java.util.*      — collections and dates
// ro.ase.ie.g1099  — the model and the generated R class
//
// READING RULE: every import must have a consumer in the file.
//   Two do not have one — `Toast` and `Date`. They are marked below. See 31.0.6.
// ======================================================================================

// Intent: the object that describes "which activity you start" and what data it carries.
import android.content.Intent;

// Bundle: the state bag saved by the system (used here only in the onCreate signature).
import android.os.Bundle;

// View: the base class for any UI element. It appears in the listener signatures.
import android.view.View;

// AdapterView: the base class for ListView/Spinner. Here only for OnItemClickListener.
import android.widget.AdapterView;

// ArrayAdapter: the bridge between `List<Movie>` and the rows in the ListView.
import android.widget.ArrayAdapter;

// ListView: the widget that displays the scrollable list.
import android.widget.ListView;

// Toast: short messages. Imported in the source, but UNUSED in this file.
//   There is no `Toast.makeText(...)` anywhere in the file. Dead import.
import android.widget.Toast;

// EdgeToEdge: enables drawing under the system bars (status + navigation).
import androidx.activity.EdgeToEdge;

// ActivityResult: the result returned by the secondary activity (code + Intent).
import androidx.activity.result.ActivityResult;

// ActivityResultCallback: the single-method interface, called on return.
import androidx.activity.result.ActivityResultCallback;

// ActivityResultLauncher: the object that starts the activity and keeps the link to the callback.
import androidx.activity.result.ActivityResultLauncher;

// ActivityResultContracts: the predefined contracts (here, StartActivityForResult).
import androidx.activity.result.contract.ActivityResultContracts;

// AppCompatActivity: the base activity with support for Material themes and ActionBar.
import androidx.appcompat.app.AppCompatActivity;

// Insets: the pixel values of the bars that cover the content.
import androidx.core.graphics.Insets;

// ViewCompat: the compatible version of the View APIs (insets on any API).
import androidx.core.view.ViewCompat;

// WindowInsetsCompat: the description of the insets, independent of the Android version.
import androidx.core.view.WindowInsetsCompat;

// FloatingActionButton: the round button in the bottom-right corner.
import com.google.android.material.floatingactionbutton.FloatingActionButton;

// ArrayList: the concrete implementation of the list.
import java.util.ArrayList;

// Calendar: the date fields (YEAR, MONTH, DAY_OF_MONTH) and the month constants.
import java.util.Calendar;

// Date: the type stored in the model for the release date.
//   WATCH OUT: the name `Date` is written nowhere in the body of the file. `getTime()`
//   returns a `Date`, but the type is inferred, not declared. Dead import.
import java.util.Date;

// GregorianCalendar: the (year, month, day) constructor used for the demo movies.
import java.util.GregorianCalendar;

// List: the DECLARED type of the list. The interface, not the implementation.
import java.util.List;

// R: the class generated from resources. Without it, `R.layout.activity_main` does not exist.
import ro.ase.ie.g1099.R;

// Movie: the data model.
import ro.ase.ie.g1099.model.Movie;

/*
 * ======================================================================================
 * THEORY BLOCK: WHO HOLDS THE STATE
 * ======================================================================================
 *
 * 1. **A single source of truth.** `movies` is `final` and lives as long as the activity.
 *    `ItemActivity` is not allowed to modify the list directly: it receives a COPY of a
 *    movie and sends back a NEW `Movie`. MainActivity decides what to do with it.
 *
 * 2. **The adapter holds the REFERENCE, not a copy.** `new ArrayAdapter<>(..., movies)`
 *    receives the list, not its content. That is why `movies.add(...)` is visible in the adapter
 *    as soon as you call `notifyDataSetChanged()`. If you wrote
 *    `new ArrayList<>(movies)`, the changes would no longer be visible.
 *
 * 3. **The Activity Result API replaces `startActivityForResult`.** There is no longer an
 *    `onActivityResult` overridden in the activity. There is a `launcher` with a
 *    callback registered once, in `onCreate`.
 *
 * 4. **The order in `onCreate` is semantic.** `initListeners()` is called BEFORE
 *    `launcher` is assigned. It works because the listener reads
 *    the FIELD `launcher` at the moment of the click, not at the moment of attachment.
 *
 * VISUAL: two entries, a single exit
 *
 *   FAB pressed  ---------------------+
 *                                     |
 *   row pressed ---+                  |
 *                  |                  v
 *                  |        new Intent(MainActivity.this, ItemActivity.class)
 *                  |                  |
 *                  |        (FAB: no extras  -> insert)
 *                  |        (row: MOVIE + POSITION -> edit)
 *                  |                  |
 *                  +------------------+--> launcher.launch(intent)
 *                                             |
 *                                             v
 *                                     ItemActivity
 *                                             |
 *                                   setResult(RESULT_OK, intent)
 *                                             |
 *                                             v
 *                              callback: RESULT_OK?
 *                                  position == -1 -> movies.add(movie)
 *                                  position >= 0  -> movies.set(position, movie)
 *                                             |
 *                                             v
 *                                  movieAdapter.notifyDataSetChanged()
 * ======================================================================================
 */
public class MainActivity extends AppCompatActivity {

    // ==================================================================================
    // THE TWO CONSTANTS — THE CONTRACT BETWEEN THE TWO SCREENS
    // ==================================================================================
    // The KEY under which the movie travels in the Intent. "movie" is an arbitrary String;
    //   what matters is that it is the SAME value when sending and when reading.
    // WHY `protected` and not `private`: `ItemActivity` is in the same package
    //   (`ro.ase.ie.g1099.activities`) and reads them as `MainActivity.MOVIE_EXTRA`.
    //   `protected` also covers the subclasses, but here the package is the real reason.
    // WHY `static final`: they are CLASS constants, not instance ones. They do not depend on
    //   any MainActivity object.
    // RISK: a typo in one of the two places produces `null`
    //   on read, with no exception and no compilation error.
    // ==================================================================================
    protected static final String POSITION_EXTRA = "position";
    protected static final String MOVIE_EXTRA = "movie";

    // The "+" button. The type is the Material class, not `ImageButton`.
    //   It is looked up in `initControls` and used in `initListeners`.
    private FloatingActionButton fabInsert;

    // The scrollable list. Looked up in `initControls`, it receives the adapter there too.
    private ListView listView;

    // The result launcher. It is NOT initialized here: it receives its value only
    //   at the end of `onCreate`, after `registerForActivityResult`.
    // WHY a field and not a local variable: the listeners in `initListeners`
    //   must be able to read it at the moment of the click, that is, long after `onCreate`.
    private ActivityResultLauncher<Intent> launcher;

    // THE LIST — the only real state of the app.
    // `final` means the REFERENCE does not change: you cannot write `movies = ...`.
    //   The content, however, changes freely: `add`, `set`, `clear` are allowed.
    // WHY `List` and not `ArrayList` in the declaration: the declared type is the interface,
    //   the implementation is hidden. You swap `ArrayList` for `LinkedList` without
    //   touching the rest of the file.
    // WHY `new ArrayList<>()` here: the list exists before `onCreate`, so
    //   `addSampleMovies()` can fill it without null checks.
    private final List<Movie> movies = new ArrayList<>();

    // THE ADAPTER. It holds the REFERENCE to `movies`, not a copy.
    // It is not initialized here because it needs a `Context` (`this`), and
    //   `this` is usable only after `super.onCreate`.
    private ArrayAdapter<Movie> movieAdapter;

    // ==================================================================================
    // onCreate — NINE STEPS, IN ORDER
    // ==================================================================================
    // WHAT IT DOES: builds the screen, loads the demo data, creates the adapter, attaches
    //   the listeners and registers the result launcher.
    // HOW: 1. infrastructure (super, edge-to-edge, layout, insets)
    //      2. data (addSampleMovies)
    //      3. adapter
    //      4. controls (findViewById + setAdapter)
    //      5. listeners
    //      6. launcher
    // RISK: `registerForActivityResult` must be called BEFORE the activity is
    //   STARTED. Here it is in `onCreate`, so it is legal. Moved to `onResume`,
    //   it would throw `IllegalStateException`.
    // REF: the same insets template appears in `ItemActivity.onCreate`.
    // ==================================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Edge-to-edge: the content is drawn under both the status bar and the
        // navigation bar. Without it, the app would have its own band at the top.
        EdgeToEdge.enable(this);

        // Inflates `activity_main.xml` into the view tree. From this moment,
        //   `findViewById` can find `R.id.main`, `R.id.listView` and the FAB.
        setContentView(R.layout.activity_main);

        // Insets: measures the system bars and turns them into padding on the container.
        // `(v, insets) -> { ... }` is a lambda expression for
        //   `OnApplyWindowInsetsListener` — the interface has a single method.
        // WHY `R.id.main`: the root FrameLayout in `activity_main.xml`.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            // `systemBars()` = the status bar + the navigation bar, together.
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Padding instead of margin: the margin would be ignored by the FrameLayout parent.
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            // `return insets` = "I have consumed nothing"; the event can go further.
            return insets;
        });

        // Step 2: fill the list with the 3 demo movies.
        // WHY BEFORE creating the adapter: so the adapter starts with 3
        //   elements already. If it came after, it would work the same, but it would require an
        //   extra `notifyDataSetChanged()`.
        addSampleMovies();

        // Step 3: create the adapter.
        // `android.R.layout.simple_list_item_1` = a single TextView per row, from
        //   the framework. It is not a layout of the project — hence the `android.` prefix.
        // The third argument is the LIST, not a copy of it. See the theory block.
        movieAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, movies);

        // Step 4: bind the references to the views and install the adapter.
        initControls();

        // Step 5: attach the listeners.
        // WATCH OUT: `launcher` is still `null` at this moment. It is fine —
        //   the listeners read it only on click, and by then `onCreate` has finished.
        initListeners();

        // Step 6: register the launcher.
        // `StartActivityForResult` is the contract that starts an activity and
        //   waits for a result — the modern equivalent of `startActivityForResult`.
        // The callback is an anonymous class, not a lambda expression, because it has
        //   two levels of indentation and a single method with a long body.
        launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult o) {

                        // THE FILTER: the result only counts if the secondary activity
                        //   called `setResult(RESULT_OK, ...)`. `RESULT_CANCELED`
                        //   (the Back button) takes the false branch and does nothing.
                        if(o.getResultCode() == RESULT_OK)
                        {
                            // `getData()` = the Intent built in `ItemActivity.saveMovie`.
                            // RISK: if an activity returned RESULT_OK without data,
                            //   `intent` would be `null` and the next line would throw an NPE.
                            //   It does not happen here: `saveMovie` always puts an Intent.
                            Intent intent = o.getData();

                            // Reads the movie. The method is the OLD one, with an explicit cast —
                            //   not `IntentCompat`, as `ItemActivity` uses.
                            //   It is deprecated since API 33, but it works.
                            // THE OBJECT IS A deserialized COPY, not the original.
                            Movie movie = (Movie) intent.getSerializableExtra(MOVIE_EXTRA);

                            // Reads the position. `-1` is the default value when
                            //   the extra is missing — that is, exactly in insert mode.
                            int position = intent.getIntExtra(POSITION_EXTRA, -1);

                            // THE INSERT BRANCH: there is no position.
                            if(position == -1)
                            {
                                // Adds at the end of the list.
                                movies.add(movie);

                                // Notifies the adapter. Without this call, the ListView does NOT
                                //   know that the list has changed and redraws nothing.
                                movieAdapter.notifyDataSetChanged();
                            }
                            // THE EDIT BRANCH: there is a position.
                            else
                            {
                                // REPLACES the element at the position. Not `add` — it would
                                //   duplicate the movie. Not `remove` + `add` — it would change the order.
                                movies.set(position, movie);

                                // The same notification. Both branches need it.
                                movieAdapter.notifyDataSetChanged();
                            }
                        }
                    }
                });
    }

    // ==================================================================================
    // initListeners — WHAT HAPPENS ON PRESS
    // ==================================================================================
    // WHAT IT DOES: attaches two listeners — one on the FAB, one on the rows of the list.
    // HOW: 1. FAB -> Intent without extras -> insert
    //      2. row -> Intent with MOVIE + POSITION -> edit
    // RISK: the method is called before `launcher` is assigned. It works
    //   only because the listeners read the field on click, not on attachment.
    // REF: the same anonymous listener pattern appears four times in `ItemActivity`.
    // ==================================================================================
    private void initListeners() {

        // Listener on the FAB. The anonymous class implements `View.OnClickListener`.
        fabInsert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // `MainActivity.this` — NOT `this`. Inside the anonymous class,
                //   `this` is the listener, not the activity. The `Context` required by
                //   the Intent constructor is the activity.
                // The second argument is the class of the activity to start.
                Intent intent = new Intent(MainActivity.this, ItemActivity.class);

                // WITHOUT extras. The absence of MOVIE_EXTRA is exactly the signal for
                //   "insert mode" that `ItemActivity.onCreate` reads.
                launcher.launch(intent);
            }
        });

        // Listener on the rows of the list. `AdapterView.OnItemClickListener` has four
        //   parameters: the parent, the row view, the position and the id.
        // `id` is the fourth and is IGNORED here: the identity is the position.
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                // The same Intent to the same activity.
                Intent intent = new Intent(MainActivity.this, ItemActivity.class);

                // HERE is the difference from the FAB: the movie leaves for the form.
                //   `movies.get(position)` is the object in the list; `putExtra` SERIALIZES it.
                //   The form will receive a copy.
                intent.putExtra(MOVIE_EXTRA, movies.get(position));

                // The position leaves separately, as an `int`. It is not part of the model.
                intent.putExtra(POSITION_EXTRA, position);

                // Starts the activity through the launcher, so the result reaches the callback.
                launcher.launch(intent);
            }
        });
    }

    // ==================================================================================
    // initControls — FINDVIEWBYID AND SETADAPTER
    // ==================================================================================
    // WHAT IT DOES: binds the two fields to the views in the XML and installs the adapter.
    // HOW: 1. FAB by id  2. ListView by id  3. setAdapter
    // RISK: if an id does not exist in the layout, `findViewById` returns `null` and
    //   the next call throws an NPE. Here both ids exist in `activity_main.xml`.
    // REF: `ItemActivity.initControls` does the same thing for 13 views.
    // ==================================================================================
    private void initControls() {

        // `R.id.floatingActionButton` — the id from the XML, not the name of the field.
        fabInsert = findViewById(R.id.floatingActionButton);

        // `R.id.listView` — the id from the XML.
        listView = findViewById(R.id.listView);

        // Installs the adapter. From this moment, `android:entries="@array/items"`
        //   in the XML is IGNORED: `setAdapter` replaces it completely.
        listView.setAdapter(movieAdapter);
    }

    // ==================================================================================
    // addSampleMovies — THE THREE DEMO MOVIES
    // ==================================================================================
    // WHAT IT DOES: adds three `Movie` objects to the list, using the constructor with 10 parameters.
    // HOW: three `movies.add(new Movie(...))` calls, each with 10 positional
    //   arguments, in the order of the signature.
    // RISK: the order of the arguments is POSITIONAL. "Sci-Fi" must be the
    //   third, not the second — otherwise it lands in `release` and does not compile, or
    //   it lands in `genre` when the types coincide and compiles wrongly.
    // REF: the constructor is annotated in 31.2; the position table is there.
    // ==================================================================================
    private void addSampleMovies() {

        // MOVIE 1 — Inception.
        // `new GregorianCalendar(year, month, day)` — WATCH OUT: the month is 0-based.
        //   `Calendar.JULY` is 6, not 7. `getTime()` turns the calendar into a `Date`.
        // Arguments 4..10: budget (int widened to double), duration, rating (`4.5f`),
        //   watched, approval, onWatchlist, posterUrl.
        movies.add(new Movie(
                "Inception",
                new GregorianCalendar(2010, Calendar.JULY, 16).getTime(),
                "Sci-Fi",
                160000000,
                148,
                4.5f,
                true,
                "PG-13",
                false,
                "https://example.com/posters/inception.jpg"));

        // MOVIE 2 — Toy Story. `4f` is a float literal: without `f`, it would be a double
        //   and would not fit in the `float rating` parameter.
        movies.add(new Movie(
                "Toy Story",
                new GregorianCalendar(1995, Calendar.NOVEMBER, 22).getTime(),
                "Animation",
                30000000,
                81,
                4f,
                true,
                "G",
                false,
                "https://example.com/posters/toy-story.jpg"));

        // MOVIE 3 — The Matrix. `Calendar.MARCH` is 2 (0-based).
        //   `watched = false`, `onWatchlist = true` — the only combination that differs
        //   from the first two and that shows the two booleans are independent.
        movies.add(new Movie(
                "The Matrix",
                new GregorianCalendar(1999, Calendar.MARCH, 31).getTime(),
                "Action",
                63000000,
                136,
                5f,
                false,
                "R",
                true,
                "https://example.com/posters/the-matrix.jpg"));
    }
}
