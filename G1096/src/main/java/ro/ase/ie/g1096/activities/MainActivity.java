/*
 * ======================================================================================
 * FILE: MainActivity.java — THE SCREEN WITH THE LIST
 * ======================================================================================
 * WHAT IT IS: the start activity. It holds the list of movies, displays it with an
 *   `ArrayAdapter` in a `ListView` and starts `ItemActivity` for insert or edit.
 *
 * WHY IT EXISTS: it is the only place that owns the app's STATE (the `movies` list).
 *   `ItemActivity` is just a form that produces a `Movie` and sends it back.
 *   Without `MainActivity`, the saved result would have nowhere to return to.
 *
 * WHAT IT DEPENDS ON:
 *   - `activity_main.xml` — for `R.id.main`, `R.id.listView`, `R.id.floatingActionButton`.
 *   - `ItemActivity` — the class it starts through an `Intent`.
 *   - `Movie` — the type of the elements in the list.
 *   - `androidx.activity.result.*` — for `registerForActivityResult`.
 *
 * WHO DEPENDS ON IT:
 *   `AndroidManifest.xml` declares it as the LAUNCHER. `ItemActivity` reads its
 *   constants `MOVIE_EXTRA` and `POSITION_EXTRA`.
 *
 * WHAT IT DOES NOT DO:
 *   - It validates nothing: it accepts any `Movie` returned by the form.
 *   - It does not persist the list: on rotation or process death, the list is lost.
 *   - It has no row layout of its own: it uses `android.R.layout.simple_list_item_1`.
 *   - It does not load posters: the URL stays text in the model.
 *
 * WHAT BREAKS WITHOUT IT:
 *   The app would start directly in a form with no list. `MOVIE_EXTRA` and
 *   `POSITION_EXTRA` would not exist, so `ItemActivity` would not compile.
 * ======================================================================================
 */

// The `activities` package — the screens. Separate from `model`, so that it does not mix
// the data types with the UI ones.
package ro.ase.ie.g1096.activities;

// ======================================================================================
// IMPORTS — GROUPED BY SOURCE
// ======================================================================================
// android.*        — framework classes (Intent, Bundle, View, widgets)
// androidx.*       — Jetpack libraries (AppCompat, EdgeToEdge, insets, activity result)
// com.google.*     — Material Components (FloatingActionButton)
// java.util.*      — collections and dates
// ro.ase.ie.g1096  — the model and the generated R class
//
// IN G1096, TWO IMPORTS ARE DEAD: `java.math.BigDecimal` and
// `android.widget.Toast`. In G1097 the dead ones are `java.util.Date` and `Toast`.
// See difference 29.0.5.
// ======================================================================================

// Intent: the object that describes "which activity you start" and what data it carries.
import android.content.Intent;

// Bundle: the state bag saved by the system (used here only in the signature of onCreate).
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
// It is part of difference 29.0.5 — it appears dead in both groups.
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

// BigDecimal: decimal numbers with EXACT precision. Imported in the source, but UNUSED.
// THE DIFFERENCE FROM G1097: there this import does not exist; instead there is `java.util.Date`,
//   also unused. Here `Date` is not even imported — and it is not needed, because
//   the name `Date` never appears in the file; `getTime()` is passed straight on.
import java.math.BigDecimal;

// ArrayList: the concrete implementation of the list.
import java.util.ArrayList;

// Calendar: the date fields (YEAR, MONTH, DAY_OF_MONTH) and the month constants.
import java.util.Calendar;

// GregorianCalendar: the quick "year, month, day" constructor used in the demo data.
import java.util.GregorianCalendar;

// List: the list interface. The field is declared `List`, not `ArrayList`.
import java.util.List;

// The R class, generated at build time from all the files in `res/`. Without it, `R.id.listView`
// would not exist as a symbol.
import ro.ase.ie.g1096.R;

// The data model — the only dependency towards `model`.
import ro.ase.ie.g1096.model.Movie;

/*
 * ======================================================================================
 * THEORY BLOCK: WHO OWNS THE STATE AND WHO DISPLAYS IT
 * ======================================================================================
 *
 * 1. A SINGLE OWNER OF THE LIST
 *    `movies` is an instance field in `MainActivity`. `ItemActivity` has no
 *    reference to it. The form produces a `Movie` and sends it back through
 *    `setResult`; only `MainActivity` decides what to do with it (`add` or `set`).
 *    This is the separation that makes the flow easy to follow.
 *
 * 2. `final` DOES NOT MEAN IMMUTABLE
 *    `private final List<Movie> movies = new ArrayList<>();`
 *    `final` blocks REASSIGNING the reference (`movies = new ArrayList<>()` does not
 *    compile), but does NOT block changing the contents. `movies.add(...)` and
 *    `movies.set(...)` are perfectly legal. It is exactly what we want: the same list,
 *    always, with contents that change.
 *
 * 3. THE ADAPTER HOLDS THE REFERENCE, NOT A COPY
 *    `new ArrayAdapter<>(this, ..., movies)` receives the list by reference. That is why
 *    `movies.add(movie)` is visible to the adapter — but only after
 *    `notifyDataSetChanged()`. ListView does not observe the list by itself.
 *
 * 4. TWO MODES, A SINGLE FORM
 *    - FAB  -> `Intent` WITHOUT extras -> `ItemActivity` in INSERT mode.
 *    - Row  -> `Intent` WITH `MOVIE_EXTRA` and `POSITION_EXTRA` -> EDIT mode.
 *    `ItemActivity` decides the mode by `existing != null`, not by a separate flag.
 *
 * 5. THE LIFECYCLE OF THE LIST
 *    The list lives as long as the activity. On rotation, Android destroys and recreates
 *    the activity, and `movies` is reinitialized with the 3 demo movies. There is no
 *    `onSaveInstanceState`, so the added movies are lost. See RISK below.
 * ======================================================================================
 */

// `extends AppCompatActivity` — the AndroidX base activity. It provides `setContentView`,
// `findViewById`, `getIntent`, `setResult`, `finish`, `RESULT_OK`.
public class MainActivity extends AppCompatActivity {

    // ==================================================================================
    // THE PROTOCOL CONSTANTS BETWEEN THE TWO ACTIVITIES
    // ==================================================================================
    // `protected static final` = class constant, visible in subclasses and in the package.
    // `ItemActivity` is in the SAME package (`ro.ase.ie.g1096.activities`), so it would
    // work even without `protected`. It is declared this way out of habit.
    //
    // WATCH OUT — A FINE TRAP: a `static final String` with a literal value is a
    // COMPILE-TIME CONSTANT. The compiler INLINES it into every class that uses it.
    // If you change "movie" to "film" and recompile only MainActivity,
    // the previously compiled ItemActivity keeps "movie" in its bytecode. It is solved with
    // a clean build. It is not asked at the exam, but it explains why public constants
    // change rarely.
    // ==================================================================================
    protected static final String POSITION_EXTRA = "position";
    protected static final String MOVIE_EXTRA = "movie";

    // The round "+" button. The type is the Material class, not `ImageButton`.
    private FloatingActionButton fabInsert;

    // The scrolling list. It fills the whole screen; the FAB floats over it.
    private ListView listView;

    // The result launcher. It is NOT initialized here — it gets a value in `onCreate`,
    // AFTER `initListeners()`. See the explanation at `initListeners`.
    private ActivityResultLauncher<Intent> launcher;

    // The list of movies — the only state of the app.
    // `final` = the reference never changes; the contents do.
    // `new ArrayList<>()` = the diamond operator; the type is inferred from the declaration.
    private final List<Movie> movies = new ArrayList<>();

    // The adapter that binds the list to the ListView. The generic type is `Movie`, so
    // `getItem(position)` returns `Movie`, not `Object`.
    private ArrayAdapter<Movie> movieAdapter;

    // ==================================================================================
    // onCreate — THE ORDER MATTERS
    // ==================================================================================
    // WHAT IT DOES: builds the screen, populates the demo list, creates the adapter, binds
    //   the controls to the code, attaches the listeners and registers the launcher.
    // HOW: 1. super 2. EdgeToEdge 3. setContentView 4. insets 5. demo data
    //   6. adapter 7. findViewById 8. listeners 9. launcher.
    // RISK: each step depends on the previous one. `findViewById` before
    //   `setContentView` returns null; `setAdapter` before `new ArrayAdapter`
    //   would give a NullPointerException.
    // REF: exactly the same order in G1097 (chapter 22, section 22.2).
    // ==================================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // 1. Starts the initialization chain of the base class. MANDATORY first.
        //    Without it, `AppCompat` does not install its delegate and the theme is not applied.
        super.onCreate(savedInstanceState);

        // 2. Enables edge-to-edge drawing: the content goes under the status bar and
        //    under the navigation bar. The consequence: we must handle the padding ourselves,
        //    otherwise the first row of text sits under the clock.
        EdgeToEdge.enable(this);

        // 3. Inflates the view tree from `activity_main.xml`. From this moment
        //    `findViewById` can find something. Before this call, it returns null.
        setContentView(R.layout.activity_main);

        // 4. Fixes the padding for the system bars.
        //    `findViewById(R.id.main)` = the root FrameLayout from the XML.
        //    The lambda is `View.OnApplyWindowInsetsListener` (a functional interface).
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {

            // 4a. Extracts the dimensions of the system bars: status bar + navigation bar.
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            // 4b. Applies them as padding on the root container. The children squeeze
            //     into it automatically; we do not have to touch each one.
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);

            // 4c. Returns `insets` UNconsumed, so that it also reaches the children.
            //     If you returned `WindowInsetsCompat.CONSUMED`, nobody else
            //     would receive the information anymore.
            return insets;
        });

        // 5. Puts the 3 demo movies in the list. This is done BEFORE creating the adapter,
        //    so that the adapter counts 3 elements from the start. If you did it after,
        //    it would work the same (the adapter holds the reference), but you would need
        //    `notifyDataSetChanged()` to redraw.
        addSampleMovies();

        // 6. Creates the adapter.
        //    `this` = Context (the activity).
        //    `android.R.layout.simple_list_item_1` = the row layout from the FRAMEWORK,
        //      not from the app. It contains a single TextView with the id `android.R.id.text1`.
        //      That is why the row displays the model's `toString()`.
        //    `movies` = the data source, by reference.
        movieAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, movies);

        // 7. Binds the Java fields to the views from the XML.
        initControls();

        // 8. Attaches the listeners. Here `launcher` is still `null`, but it is OK:
        //    the listeners read the field only on CLICK, and by then step 9 has set it.
        initListeners();

        // 9. Registers the result contract. It MUST be called before
        //    the activity reaches the STARTED state, so `onCreate` is the right
        //    place. It returns an `ActivityResultLauncher<Intent>`.
        launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),

                // The callback: an anonymous class that implements the single-method interface.
                // It runs on the main thread, after the secondary activity
                // has closed and this one has come back to the foreground.
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult o) {

                        // FILTER 1: did it return something useful? `RESULT_OK` is -1.
                        // If the user pressed Back, the code is RESULT_CANCELED (0)
                        // and we do nothing — the list stays untouched.
                        if(o.getResultCode() == RESULT_OK)
                        {
                            // `getData()` = the Intent built by ItemActivity in `saveMovie()`.
                            // It can theoretically be null; here RESULT_OK guarantees it is not.
                            Intent intent = o.getData();

                            // Extracts the serialized object. WATCH OUT: it is A COPY.
                            // `getSerializableExtra(String)` is DEPRECATED since API 33;
                            // the modern variant is `IntentCompat.getSerializableExtra(...)`,
                            // already used in ItemActivity. Here the old variant remained —
                            // a real inconsistency between the two files.
                            Movie movie = (Movie) intent.getSerializableExtra(MOVIE_EXTRA);

                            // The second piece of information from the Intent: the position. Default -1 = "does not exist".
                            // -1 is the app's convention for "insert mode".
                            int position = intent.getIntExtra(POSITION_EXTRA, -1);

                            // BRANCH 1: the position is missing -> NEW movie.
                            if(position == -1)
                            {
                                // Adds at the end of the list.
                                movies.add(movie);

                                // Notifies the adapter. Without this call, ListView does not know
                                // that the list has one more element and does not redraw.
                                movieAdapter.notifyDataSetChanged();
                            }
                            // BRANCH 2: the position exists -> REPLACEMENT, not addition.
                            else
                            {
                                // `set` overwrites the element at the given position.
                                // If you used `add`, you would have a duplicate.
                                movies.set(position, movie);
                                movieAdapter.notifyDataSetChanged();
                            }
                        }
                    }
                });
    }

    // ==================================================================================
    // initListeners — WHAT HAPPENS ON TAP
    // ==================================================================================
    // WHAT IT DOES: attaches two behaviours — the FAB starts the empty form, a click
    //   on a row starts the filled form.
    // HOW: 1. `fabInsert.setOnClickListener` 2. `listView.setOnItemClickListener`.
    // RISK: `launcher` is still null when this method runs. It is safe because
    //   the listeners run only after `onCreate`, when `launcher` has a value.
    // REF: the same anonymous-class pattern as in `ItemActivity.initListeners()`.
    // ==================================================================================
    private void initListeners() {

        // An anonymous class that implements `View.OnClickListener`.
        // `new View.OnClickListener() { ... }` = "an object that only knows how to react
        // to a click". It has no name, it is not reused.
        fabInsert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // `MainActivity.this` = the activity's Context.
                // `this` alone would be the listener (the anonymous class), not an activity —
                // and the constructor `Intent(Context, Class)` would receive the wrong type.
                Intent intent = new Intent(MainActivity.this, ItemActivity.class);

                // NO extras! The absence of `MOVIE_EXTRA` is exactly the signal
                // that `ItemActivity` reads as "insert mode".
                launcher.launch(intent);
            }
        });

        // Listener for a click on a row. The interface has 4 parameters:
        //   parent   = the ListView
        //   view     = the pressed row (a TextView, here)
        //   position = the index in the list (0-based)
        //   id       = the id of the row; for ArrayAdapter it equals position
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                // The same target as the FAB — but this time we also carry data.
                Intent intent = new Intent(MainActivity.this, ItemActivity.class);

                // The object on the pressed row. It is serialized automatically on `launch`.
                // `movies.get(position)` returns the REFERENCE from the list; `putExtra`
                // serializes it into a copy. The original stays untouched.
                intent.putExtra(MOVIE_EXTRA, movies.get(position));

                // The position, so that `ItemActivity` knows which row to replace on save.
                intent.putExtra(POSITION_EXTRA, position);

                // Starts the activity through the launcher, not through `startActivity`.
                // The difference: the launcher guarantees that the result reaches the callback.
                launcher.launch(intent);
            }
        });
    }

    // ==================================================================================
    // initControls — FINDVIEWBYID, PURE AND SIMPLE
    // ==================================================================================
    // WHAT IT DOES: binds the two views to the Java fields and installs the adapter.
    // HOW: 1. FAB 2. ListView 3. setAdapter.
    // RISK: `findViewById` returns null if the ID does not exist in the current layout.
    //   Here all three IDs exist in `activity_main.xml`.
    // REF: in `ItemActivity`, the same method has 13 lines, not 3.
    // ==================================================================================
    private void initControls() {

        // The ID in the XML is `@+id/floatingActionButton`; the field is called `fabInsert`.
        // The names differ, the ID matters.
        fabInsert = findViewById(R.id.floatingActionButton);

        // The ID in the XML is `@+id/listView`.
        listView = findViewById(R.id.listView);

        // Installs the adapter. From this moment the ListView has something to draw.
        // `android:entries="@array/items"` from the XML is IGNORED from now on.
        listView.setAdapter(movieAdapter);
    }

    // ==================================================================================
    // addSampleMovies — THE DEMO DATA
    // ==================================================================================
    // WHAT IT DOES: adds 3 movies to the list, so that the screen does not start empty.
    // HOW: 3 calls of the 10-parameter constructor, each with a `new GregorianCalendar`.
    // RISK: the order of the arguments is POSITIONAL. Swapping two arguments of the
    //   same type (String/String or boolean/boolean) compiles and produces wrong data.
    // REF: the constructor is documented in 29.2; here you see its ONLY consumer.
    // ==================================================================================
    private void addSampleMovies() {

        // MOVIE 1 — "Inception".
        // `new Movie(` opens the call; each argument is on its own line, so that the
        // position is visible. The order is the one from the signature: title, release, genre, budget,
        // duration, rating, watched, approval, onWatchlist, posterUrl.
        movies.add(new Movie(
                "Inception",                                              // 1. title
                // 2. release: `GregorianCalendar(year, month, day)` — THE MONTH IS 0-BASED!
                //    `Calendar.JULY` = 6, not 7. If you wrote `new GregorianCalendar(2010, 7, 16)`
                //    you would get 16 AUGUST. `getTime()` turns the Calendar into a Date.
                new GregorianCalendar(2010, Calendar.JULY, 16).getTime(),
                "Sci-Fi",                                                 // 3. genre
                160000000,                                                // 4. budget (int -> double)
                148,                                                      // 5. duration (minutes)
                4.5f,                                                     // 6. rating (float!)
                true,                                                     // 7. watched
                "PG-13",                                                  // 8. approval
                false,                                                    // 9. onWatchlist
                "https://example.com/posters/inception.jpg"));            // 10. posterUrl

        // MOVIE 2 — "Toy Story".
        // The same structure. `4f` is a `float` literal; without the `f` it would be `double`
        // and the constructor would not find a match for the `float rating` parameter.
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

        // MOVIE 3 — "The Matrix".
        // The only one with `watched = false` and `onWatchlist = true`: the two booleans
        // are independent, and this movie demonstrates exactly that.
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

    // ==================================================================================
    // WHAT IS MISSING FROM THIS FILE — AND IS ASKED AT THE EXAM
    // ==================================================================================
    // - `onSaveInstanceState(Bundle)`: it does not exist. On rotation, the activity is
    //   recreated, `movies` is reinitialized, and the added movies DISAPPEAR.
    //   A REAL risk, not a theoretical one: turn the phone after adding a movie.
    // - `onResume()` / `onStart()`: they do not exist. No need; everything is done in `onCreate`.
    // - `onDestroy()`: it does not exist. There are no resources to release.
    // - Menu, `OptionsMenu`, `Toolbar`: they do not exist. The screen is just a list + FAB.
    // ==================================================================================
}
