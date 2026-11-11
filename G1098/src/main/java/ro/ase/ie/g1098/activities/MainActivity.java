/*
 * ======================================================================================
 * FILE: MainActivity.java — THE SCREEN WITH THE LIST
 * ======================================================================================
 * WHAT IT IS: the start activity. It holds the movie list, displays it with an `ArrayAdapter`
 *   in a `ListView` and starts `ItemActivity` for insert or edit.
 *
 * WHY IT EXISTS: it is the only place that owns the app's STATE (the `movies` list).
 *   `ItemActivity` is just a form that produces a `Movie` and sends it back.
 *
 * WHAT IT KNOWS: the list, the adapter, the two extras, the result launcher.
 *   It does not know what the form looks like and it does not validate data.
 *
 * WHAT IT DOES NOT DO: it saves nothing to disk. The list lives as long as the process lives.
 *   When the screen rotates, the activity is recreated and `addSampleMovies()` runs
 *   again — the added movies are lost. Persistence comes in the next lab.
 *
 * WHAT BREAKS WITHOUT IT: the app would start straight into a form with no list,
 *   and the saved result would have nowhere to return to.
 * ======================================================================================
 */

// ======================================================================================
// THE PACKAGE
// ======================================================================================
// `activities` holds the screens. The model sits in `model`, and `R` in the root of the package.
// ======================================================================================
package ro.ase.ie.g1098.activities;

// ======================================================================================
// IMPORTS — GROUPED BY SOURCE
// ======================================================================================
// android.*        — framework classes (Intent, Bundle, View, widgets)
// androidx.*       — Jetpack libraries (AppCompat, EdgeToEdge, insets, activity result)
// com.google.*     — Material Components (FloatingActionButton)
// java.util.*      — collections and dates
// ro.ase.ie.g1098  — the model and the generated R class
//
// HYGIENE NOTE: the only unused import in the file is `android.widget.Toast`.
//   G1097 has one more: `java.util.Date`. Both are harmless at runtime — an import
//   does not reach the bytecode — but it suggests a dependency that does not exist.
// ======================================================================================

// Intent: the object that describes "which activity you start" and what data it carries.
import android.content.Intent;

// Bundle: the saved-state bag the system keeps (used here only in the onCreate signature).
import android.os.Bundle;

// View: the base class for any UI element. It appears in the listeners' signatures.
import android.view.View;

// AdapterView: the base class for ListView/Spinner. Here only for OnItemClickListener.
import android.widget.AdapterView;

// ArrayAdapter: the bridge between `List<Movie>` and the rows in the ListView.
import android.widget.ArrayAdapter;

// ListView: the widget that displays the scrollable list.
import android.widget.ListView;

// Toast: short messages. Imported in the source, but UNUSED in this file.
//   It is the only dead import of group G1098 in MainActivity.
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

// AppCompatActivity: the base activity with support for Material themes and the ActionBar.
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

// GregorianCalendar: the date constructor used in addSampleMovies.
//   `new GregorianCalendar(year, month, day).getTime()` produces a `Date` directly.
import java.util.GregorianCalendar;

// List: the declared type of the `movies` field (the interface, not the implementation).
import java.util.List;

// R: the build-generated class that maps the names from XML to integers.
import ro.ase.ie.g1098.R;

// Movie: the data model.
import ro.ase.ie.g1098.model.Movie;

/*
 * ======================================================================================
 * THEORY BLOCK: WHO HOLDS THE STATE AND WHO DISPLAYS IT
 * ======================================================================================
 *
 * 1. **A single owner of the list.** `movies` is `final` and is the only list
 *    in the app. `ItemActivity` has no list; it receives a `Movie` and returns a
 *    `Movie`. The "single source of truth" rule prevents lists that contradict each other.
 *
 * 2. **The adapter does not copy the list.** `new ArrayAdapter<>(this, layout, movies)` receives
 *    the REFERENCE to the list. The adapter reads from it on every `getView`. That is why
 *    `movies.add(...)` is seen immediately in the adapter — but the ListView does not redraw
 *    until after `notifyDataSetChanged()`.
 *
 * 3. **The order in onCreate is a contract.** `addSampleMovies()` must run
 *    before the adapter is created, so the list has content on the first bind.
 *    `initControls()` must run before `initListeners()`, so that
 *    `fabInsert` and `listView` are already bound when listeners are put on them.
 *
 * 4. **The launcher is registered in onCreate, not in a listener.** `registerForActivityResult`
 *    must be called before the activity reaches the STARTED state. Called from an
 *    `onClick`, it would throw `IllegalStateException`.
 *
 * THE LIFECYCLE, IN SHORT:
 *
 *   onCreate  -> super, EdgeToEdge, setContentView, insets, data, adapter,
 *                controls, listeners, launcher
 *   onStart   -> the activity becomes visible
 *   onResume  -> receives input
 *   onPause   -> loses input (ItemActivity appears on top of it)
 *   onStop    -> it is no longer visible
 *   onDestroy -> destroyed (rotation, back, process killed)
 *
 *   On returning from ItemActivity: onStart -> onResume -> the launcher's callback.
 * ======================================================================================
 */
public class MainActivity extends AppCompatActivity {

    // ==================================================================================
    // THE EXTRA KEYS — THE CONTRACT BETWEEN THE TWO ACTIVITIES
    // ==================================================================================
    // `protected static final`: `static` because it does not depend on an instance, `protected`
    //   so that it is visible from ItemActivity (the same `activities` package), `final`
    //   because the value never changes.
    // THE VALUE is an arbitrary String: "position" and "movie". The convention is that it be
    //   unique in the app; otherwise two extras with the same key overwrite each other.
    // ==================================================================================
    protected static final String POSITION_EXTRA = "position";
    protected static final String MOVIE_EXTRA = "movie";

    // The "+" button. The concrete type is FloatingActionButton, not View: we need
    // only setOnClickListener, which exists on any View, but the concrete type
    // documents what widget it is.
    private FloatingActionButton fabInsert;

    // The scrollable list.
    private ListView listView;

    // The result launcher. It is assigned in onCreate, AFTER initListeners().
    // `ActivityResultLauncher<Intent>`: starts an Intent and returns an ActivityResult.
    private ActivityResultLauncher<Intent> launcher;

    // THE LIST — the app's state. `final` means the reference does not change;
    // the content, yes. `List` is the interface; `ArrayList` is the implementation.
    // Initialization at the declaration guarantees the list exists before any method.
    private final List<Movie> movies = new ArrayList<>();

    // The adapter. It is created in onCreate, after the list already has the 3 demo movies.
    private ArrayAdapter<Movie> movieAdapter;

    /*
     * ==============================================================================
     * THEORY BLOCK: ACTIVITY RESULT API
     * ==============================================================================
     *
     * 1. Before AndroidX Activity 1.2, the result was taken with:
     *      startActivityForResult(intent, REQUEST_CODE);
     *      onActivityResult(requestCode, resultCode, data)
     *    Both are DEPRECATED. The problem: the request code was a global `int`, easy
     *    to duplicate, and the `onActivityResult` method grew with a `switch` for
     *    every activity started.
     *
     * 2. The new API binds a CALLBACK to a CONTRACT, once:
     *      launcher = registerForActivityResult(contract, callback)
     *      launcher.launch(intent)
     *
     * 3. `StartActivityForResult` is the generic contract: it receives an `Intent` and
     *    returns an `ActivityResult` (code + Intent).
     *
     * 4. The registration must be done in `onCreate`, before the STARTED state.
     *    A `launcher` registered too late throws `IllegalStateException`.
     *
     * 5. The callback runs on the UI thread, after `onResume`, when the activity is
     *    visible again. That is why `notifyDataSetChanged()` is safe there.
     * ==============================================================================
     */

    // ==================================================================================
    // onCreate — THE PLACE WHERE THE SCREEN IS BUILT
    // ==================================================================================
    // WHAT IT DOES: binds the layout, populates the list, creates the adapter, binds the widgets,
    //   installs the listeners and registers the result launcher.
    // HOW: in the order below. The order is part of the lesson.
    // RISK: if `initListeners()` ran before `initControls()`, `fabInsert`
    //   would be `null` and `setOnClickListener` would throw `NullPointerException`.
    // REF: the same skeleton in all the groups; chapter 22, section 22.2.
    // ==================================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // 1. The chain to the superclass. MANDATORY first: AppCompatActivity does its
        //    internal initialization (theme, fragment manager, saved state) here.
        //    If you move it lower, you get exceptions that are hard to diagnose.
        super.onCreate(savedInstanceState);

        // 2. Edge-to-edge: the content is drawn under the status bar and under the
        //    navigation bar. Without step 4, the first rows would sit under the clock.
        EdgeToEdge.enable(this);

        // 3. The layout. `R.layout.activity_main` is the ID generated for the file
        //    `res/layout/activity_main.xml`. From here on, `findViewById` can
        //    find the widgets.
        setContentView(R.layout.activity_main);

        // 4. Insets: we measure the system bars and turn them into padding on the root.
        //    `findViewById(R.id.main)` is the FrameLayout from the XML — that is why the ID
        //    `main` is mandatory there.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {

            // 4a. How much the system bars take, in pixels.
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            // 4b. Padding on all four sides. This way the content starts under
            //     the status bar and ends above the navigation bar.
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);

            // 4c. `return insets` = "I consumed nothing"; the children receive it further.
            return insets;
        });

        // 5. The demo data. It must run BEFORE the adapter is created,
        //    so the list has content on the first bind.
        addSampleMovies();

        // 6. The adapter. `android.R.layout.simple_list_item_1` is a layout from
        //    the framework: a single TextView. `this` is the Context required by the constructor.
        //    The third argument is the list — the adapter keeps the REFERENCE, not a copy.
        movieAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, movies);

        // 7. Binding the widgets to the fields.
        initControls();

        // 8. Installing the listeners. It runs after initControls, so the widgets
        //    are already non-null.
        initListeners();

        // 9. The result launcher. It is registered ONCE, in onCreate.
        //    `StartActivityForResult` is the contract that says: "I start an activity
        //    and I want a result back".
        launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),

                // The callback: an anonymous class that implements a single method.
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult o) {

                        // 9a. We check the code. `RESULT_OK` means that ItemActivity
                        //     called `setResult(RESULT_OK, intent)`. If the user
                        //     pressed Back, the code is `RESULT_CANCELED` and we do nothing.
                        if(o.getResultCode() == RESULT_OK)
                        {
                            // 9b. The result Intent. It can theoretically be `null`, but
                            //     ItemActivity always sends it when the code is OK.
                            Intent intent = o.getData();

                            // 9c. We take the object out. `getSerializableExtra` returns
                            //     `Serializable`, so the cast to `Movie` is mandatory.
                            //     The object is A DESERIALIZED COPY, not the original.
                            Movie movie = (Movie) intent.getSerializableExtra(MOVIE_EXTRA);

                            // 9d. The position. A missing extra means insert, that is, -1.
                            int position = intent.getIntExtra(POSITION_EXTRA, -1);

                            // 9e. The INSERT branch: the position is missing.
                            if(position == -1)
                            {
                                // We add at the end of the list.
                                movies.add(movie);

                                // We notify the adapter. Without this call, the ListView does not
                                // know the list has a new element and does not redraw.
                                movieAdapter.notifyDataSetChanged();
                            }
                            // 9f. The EDIT branch: the position exists.
                            else
                            {
                                // REPLACEMENT, not addition. `set` keeps the order.
                                movies.set(position, movie);

                                // Here too a notification is needed.
                                movieAdapter.notifyDataSetChanged();
                            }
                        }
                    }
                });
    }

    // ==================================================================================
    // initListeners — WHAT HAPPENS ON A PRESS
    // ==================================================================================
    // WHAT IT DOES: installs two listeners — one on the FAB (insert), one on the list row
    //   (edit).
    // HOW: `setOnClickListener` on the FAB; `setOnItemClickListener` on the ListView.
    // RISK: the listeners are anonymous classes. Inside them, `this` is NOT
    //   MainActivity, but the listener object. That is why you write `MainActivity.this`.
    // REF: chapter 22, section 22.2.
    // ==================================================================================
    private void initListeners() {

        // 1. FAB -> empty form (insert).
        fabInsert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // EXPLICIT Intent: (Context, Class). The second argument is the class
                // of the activity to start. An implicit Intent would look for
                // an app that knows how to do something, not our class.
                Intent intent = new Intent(MainActivity.this, ItemActivity.class);

                // Starting through the launcher, not through `startActivityForResult` (deprecated).
                // The launcher knows which callback to call on return.
                launcher.launch(intent);
            }
        });

        // 2. Click on a row -> filled form (edit).
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                // 2a. The same explicit Intent.
                Intent intent = new Intent(MainActivity.this, ItemActivity.class);

                // 2b. The object that travels. `movies.get(position)` is the movie
                //     on the pressed row. Serialization happens automatically.
                intent.putExtra(MOVIE_EXTRA, movies.get(position));

                // 2c. The position, so ItemActivity knows what to replace on return.
                intent.putExtra(POSITION_EXTRA, position);

                // 2d. Start.
                launcher.launch(intent);
            }
        });
    }

    // ==================================================================================
    // initControls — FINDVIEWBYID
    // ==================================================================================
    // WHAT IT DOES: binds the two widgets to fields and installs the adapter on the list.
    // HOW: `findViewById` with the ID from the XML; then `setAdapter`.
    // RISK: `findViewById` returns `null` if the ID does not exist in the layout that was set.
    //   A wrong ID gives no compile error — it gives a `NullPointerException` at runtime.
    // REF: `R.id.floatingActionButton` and `R.id.listView` from `activity_main.xml`.
    // ==================================================================================
    private void initControls() {

        // The ID in the XML is `floatingActionButton`, and the field is called `fabInsert`.
        // The names do not have to match; only the ID in `R.id` matters.
        fabInsert = findViewById(R.id.floatingActionButton);

        // The list.
        listView = findViewById(R.id.listView);

        // The adapter is installed on the list. From this moment, the ListView asks
        // the adapter what to display on each row.
        listView.setAdapter(movieAdapter);
    }

    // ==================================================================================
    // addSampleMovies — THE DEMO DATA
    // ==================================================================================
    // WHAT IT DOES: adds three movies to the list, so the screen is not empty on first launch.
    // HOW: three calls of the 10-parameter constructor.
    // RISK: the order of the arguments is POSITIONAL. Here there are 3 Strings and 2 booleans,
    //   so swapping them compiles. Check the table in 30.2.
    // REF: the `Movie(...)` constructor from `Movie.java`.
    // ==================================================================================
    private void addSampleMovies() {

        // MOVIE 1 — Inception.
        // `new GregorianCalendar(year, month, day)`: the month is 0-based! `Calendar.JULY` = 6,
        // not 7. `.getTime()` turns Calendar into Date, the type required by the constructor.
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

        // MOVIE 2 — Toy Story.
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

        // MOVIE 3 — The Matrix.
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
