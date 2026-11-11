/*
 * ======================================================================================
 * FILE: MainActivity.java — THE SCREEN WITH THE LIST
 * ======================================================================================
 * WHAT IT IS: the start activity. It holds the list of movies, displays it with an `ArrayAdapter`
 *   in a `ListView` and starts `ItemActivity` for insert or edit.
 *
 * WHY IT EXISTS: it is the only place that owns the application STATE (the `movies` list).
 *   `ItemActivity` is just a form that produces a `Movie` and sends it back.
 *
 * WHAT IT KNOWS: the list, the adapter, the two extras, the result launcher.
 *   It does not know what the form looks like and it does not validate data.
 *
 * WHAT WOULD BREAK WITHOUT IT: the application would start directly in a form with no list,
 *   and the saved result would have nowhere to return to.
 * ======================================================================================
 */

package ro.ase.ie.g1097.activities;

// ======================================================================================
// IMPORTS — GROUPED BY SOURCE
// ======================================================================================
// android.*        — framework classes (Intent, Bundle, View, widgets)
// androidx.*       — Jetpack libraries (AppCompat, EdgeToEdge, insets, activity result)
// com.google.*     — Material Components (FloatingActionButton)
// java.util.*      — collections and dates
// ro.ase.ie.g1097  — the model and the generated R class
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
import android.widget.Toast;

// EdgeToEdge: enables drawing under the system bars (status + navigation).
import androidx.activity.EdgeToEdge;

// ActivityResult: the result returned by the secondary activity (code + Intent).
import androidx.activity.result.ActivityResult;

// ActivityResultCallback: the single-method interface, called on return.
import androidx.activity.result.ActivityResultCallback;

// ActivityResultLauncher: the object that starts the activity and holds the link to the callback.
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

// Date: the type stored in the model for the release date.
import java.util.Date;

// GregorianCalendar: the quick "year, month, day" constructor used in the demo data.
import java.util.GregorianCalendar;

// List: the list interface. The field is declared `List`, not `ArrayList`.
import java.util.List;

// The data model — the only dependency on `model`.
import ro.ase.ie.g1097.model.Movie;

// R: the class generated from resources. Here for `R.layout.activity_main` and `R.id.*`.
import ro.ase.ie.g1097.R;

/*
 * ======================================================================================
 * THEORY BLOCK: ACTIVITY, INTENT, ADAPTER
 * ======================================================================================
 *
 * 1. **Activity = a screen with a state.** `MainActivity` is a `Context` (it has access to
 *    resources, theme, system) and a `LifecycleOwner` (it has a lifecycle: onCreate ->
 *    onStart -> onResume -> onPause -> onStop -> onDestroy).
 *
 * 2. **Intent = a message.** An explicit `Intent` contains the target class and a `Bundle`
 *    of extras. Starting is done with `startActivity` (without a result) or with an
 *    `ActivityResultLauncher` (with a result).
 *
 * 3. **Adapter = the translator.** `ListView` does not know what a `Movie` is. `ArrayAdapter`
 *    does: for each position, it takes `movies.get(position)` and turns it into a
 *    UI row. Without an adapter, `ListView` would display a list of empty objects.
 *
 * 4. **The order in onCreate is a contract, not style.** Each step depends on the previous one:
 *    without `super`, AppCompat has no delegate; without `setContentView`, `findViewById`
 *    returns null; without an adapter, `setAdapter` receives null.
 *
 * VISUAL: the lifecycle during an insert
 *
 *   MainActivity:  onCreate -> onStart -> onResume -> onPause
 *                                                        |
 *                                                        v
 *   ItemActivity:                                   onCreate -> onStart -> onResume
 *                                                        |
 *                                                   (the user saves)
 *                                                        |
 *                                                   setResult + finish
 *                                                        |
 *   MainActivity:  onStart -> onResume -> [launcher callback] -> list redrawn
 *
 *   MainActivity is NOT destroyed while ItemActivity is on top. It is only
 *   stopped. That is why the `movies` list survives and why the callback can
 *   modify it directly.
 * ======================================================================================
 */
public class MainActivity extends AppCompatActivity {

    // ==================================================================================
    // THE EXTRA CONSTANTS
    // ==================================================================================
    // WHY CONSTANTS AND NOT LITERALS: the key "movie" is written in two files.
    //   If in one place you write "movie" and in another "Movie", `getSerializableExtra`
    //   returns `null` WITH NO ERROR AT ALL. The compiler does not help you with Strings.
    //
    // WHY `protected`: `ItemActivity` is in the same package, so `package-private` would
    //   work too. `protected` documents the intention: "it is used from
    //   subclasses / from nearby code".
    //
    // WHY `static`: the value belongs to the class, not to the instance. `MainActivity.MOVIE_EXTRA`
    //   can be written without a MainActivity object existing.
    //
    // WHY `final`: the key must never change at runtime.
    // ==================================================================================
    protected static final String POSITION_EXTRA = "position";
    protected static final String MOVIE_EXTRA = "movie";

    // ==================================================================================
    // FIELD 1: fabInsert
    // ==================================================================================
    // TYPE: FloatingActionButton — the "+" button.
    // WHY A FIELD: it is bound in `initControls` and used in `initListeners`.
    //   A local variable in `initControls` would die at the end of the method.
    // ==================================================================================
    private FloatingActionButton fabInsert;

    // ==================================================================================
    // FIELD 2: listView
    // ==================================================================================
    // TYPE: ListView — the widget that displays the rows.
    // WHY A FIELD: bound in `initControls`, a listener is attached to it in `initListeners`.
    // ==================================================================================
    private ListView listView;

    // ==================================================================================
    // FIELD 3: launcher
    // ==================================================================================
    // TYPE: ActivityResultLauncher<Intent> — the object returned by `registerForActivityResult`.
    // WHY A FIELD: it is created in `onCreate` and used in `initListeners`, in two
    //   places (the FAB and the row click).
    // ORDER RISK: `initListeners()` runs BEFORE this field is assigned.
    //   It works only because the listeners execute on CLICK, not at registration.
    // ==================================================================================
    private ActivityResultLauncher<Intent> launcher;

    // ==================================================================================
    // FIELD 4: movies
    // ==================================================================================
    // TYPE: `final List<Movie>` initialised with `ArrayList`.
    // WHY `List` and not `ArrayList`: the declared type is the interface. The implementation
    //   can be changed (e.g. `LinkedList`) without modifying the rest of the code.
    // WHY `final`: the REFERENCE never changes. You can do `movies.add(...)`,
    //   `movies.set(...)`, but not `movies = new ArrayList<>()`.
    // WHY `new ArrayList<>()` here: the list must exist before `onCreate`,
    //   because `ArrayAdapter` receives the reference to it.
    // ==================================================================================
    private final List<Movie> movies = new ArrayList<>();

    // ==================================================================================
    // FIELD 5: movieAdapter
    // ==================================================================================
    // TYPE: ArrayAdapter<Movie> — the adapter that links the list to the ListView.
    // WHY IT IS NOT `final`: it is assigned in `onCreate`, after `addSampleMovies()`.
    // WHY A FIELD: `notifyDataSetChanged()` is called from inside the callback,
    //   where you need the same instance.
    // ==================================================================================
    private ArrayAdapter<Movie> movieAdapter;

    // ==================================================================================
    // onCreate — THE SCREEN'S REAL CONSTRUCTOR
    // ==================================================================================
    // The signature is imposed by the framework: `protected void onCreate(Bundle)`.
    // `savedInstanceState` is `null` on first creation and a `Bundle` on recreation
    //   (screen rotation, theme change). Here it is NOT used: the list is lost
    //   on rotation. This is a known limitation of this commit.
    // ==================================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // STEP 1: start the parent class's initialisation chain.
        // WHAT IT DOES: AppCompat installs its delegate, loads the theme, creates
        //   the ActionBar and prepares the `FragmentManager`.
        // WHAT BREAKS IF IT IS MISSING: `SuperNotCalledException` at `onStart`, or
        //   `NullPointerException` in `setContentView` because the delegate does not exist.
        // WHAT BREAKS IF IT IS TOO LATE (after setContentView): AppCompat can no longer
        //   apply the theme to the window; you may see a flash of the wrong colour.
        super.onCreate(savedInstanceState);

        // STEP 2: enable edge-to-edge mode.
        // WHAT IT DOES: sets the windows to draw under the system bars as well, so
        //   that the content can occupy the whole screen. The bars become transparent.
        // WHY BEFORE setContentView: the window configuration must be done
        //   before the first view is attached, otherwise you may see a recolouring.
        // CONSEQUENCE: the content goes UNDER the clock and under the navigation bar. That is why
        //   step 4 applies padding.
        EdgeToEdge.enable(this);

        // STEP 3: fill the window with the view tree from XML.
        // WHAT IT DOES: inflates `res/layout/activity_main.xml` and attaches it to the window.
        // WHY BEFORE findViewById: before this call, `findViewById` has
        //   nowhere to search and returns `null` for any ID.
        // WHAT BREAKS IF IT IS MISSING: white screen; any `findViewById` -> null -> NPE.
        setContentView(R.layout.activity_main);

        // STEP 4: apply padding equal to the system bars on the root.
        // ==============================================================================
        // VISUAL: the safe area
        //
        //   +--------------------------------------------------+  ^
        //   |  clock  battery  notifications                   |  | status bar (~24dp)
        //   +--------------------------------------------------+  v
        //   |                                                  |  ^
        //   |                                                  |  |
        //   |            CONTENT (padding applied)             |  | screen
        //   |                                                  |  |
        //   |                                                  |  |
        //   +--------------------------------------------------+  v
        //   |  back  home  recents                             |  ^  navigation bar (~48dp)
        //   +--------------------------------------------------+  v
        //
        //   Without padding: the first row of the list goes under the clock, and the FAB goes
        //   under the navigation bar and becomes impossible to tap.
        //   With padding on R.id.main: ALL children are pushed into the safe area.
        // ==============================================================================
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {

            // Extracts the insets for "systemBars": status bar + navigation bar
            // + caption bar. It does not include the keyboard (IME), which would be `Type.ime()`.
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            // Applies padding on all four sides, in pixels.
            // WHY on the root: padding on a child would push only that child;
            //   the others would stay under the bars.
            // SIDE EFFECT: the padding is REAPPLIED on every insets change
            //   (rotation, navigation bar appearing/disappearing). `setPadding` does not add,
            //   it replaces — so it does not accumulate.
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);

            // `return insets` (not `CONSUMED`): the insets keep flowing to the
            // children. If you returned `WindowInsetsCompat.CONSUMED`, no child
            // would receive the information any more and each would have to apply its own padding.
            return insets;
        });

        // STEP 5: fill the list with 3 demo movies.
        // WHY BEFORE creating the adapter: the adapter receives the REFERENCE to the list.
        //   By filling the list first, the adapter already sees 3 elements at construction and
        //   you do not need `notifyDataSetChanged()`.
        // If you called `addSampleMovies()` after `new ArrayAdapter(...)`, it would work
        //   then too (same reference), but you would need a `notifyDataSetChanged()`
        //   for the ListView to redraw.
        addSampleMovies();

        // STEP 6: create the adapter.
        // Parameters:
        //   1. `this` — the Context (MainActivity is a Context).
        //   2. `android.R.layout.simple_list_item_1` — a layout that contains ONE TextView.
        //   3. `movies` — the data list, held by REFERENCE, not copied.
        // WHY `android.R.layout` and not `R.layout`: it is a layout from the framework,
        //   not from the project.
        // WHY `<>` (diamond): the compiler infers `ArrayAdapter<Movie>` from `movies`.
        movieAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, movies);

        // STEP 7: bind the views to the fields and attach the adapter.
        // WHY AFTER creating the adapter: `initControls` does `listView.setAdapter(movieAdapter)`.
        //   If `movieAdapter` were null, `setAdapter(null)` would leave the list empty.
        initControls();

        // STEP 8: attach the listeners.
        // WHY AFTER `initControls`: the listeners need `fabInsert` and `listView`,
        //   which are assigned there. Before, they would be null -> NPE at registration.
        // NOTE: `launcher` is still null at this moment. See step 9.
        initListeners();

        // STEP 9: register the callback for the result.
        // ==============================================================================
        // WHY HERE AND NOT INSIDE A LISTENER:
        //   `registerForActivityResult` must be called BEFORE the activity
        //   reaches the STARTED state. Otherwise you get:
        //   "LifecycleOwner ... is attempting to register while current state is RESUMED.
        //    LifecycleOwners must call register before they are STARTED."
        //   That is why the registration sits in `onCreate`, not in `onClick`.
        //
        // THE FLOW:
        //   [onCreate] registerForActivityResult(contract, callback)
        //        |
        //        v
        //   [FAB click] launcher.launch(intent)
        //        |
        //        v
        //   ItemActivity opens; MainActivity -> onPause
        //        |
        //        v
        //   ItemActivity: setResult(RESULT_OK, intent) + finish()
        //        |
        //        v
        //   MainActivity returns: onStart -> onResume
        //        |
        //        v
        //   callback.onActivityResult(result)   <-- the list is modified here
        // ==============================================================================
        launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),

                // The callback is an anonymous class. It can also be written as a lambda, but
                // the source uses the explicit form — useful in the exam for clarity.
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult o) {

                        // MANDATORY CHECK: did a good result arrive?
                        // If the user presses Back, `ItemActivity` closes with
                        // RESULT_CANCELED, and `o.getData()` is null.
                        // Without this `if`, the next `intent.getSerializableExtra`
                        // would throw NullPointerException.
                        if(o.getResultCode() == RESULT_OK)
                        {
                            // Extracts the result Intent. It can be null if
                            // the activity called `setResult(RESULT_OK)` without an Intent —
                            // that does not happen here, but it is a theoretical risk.
                            Intent intent = o.getData();

                            // Reads the serialised object. The cast is needed because
                            // `getSerializableExtra` returns `Serializable`.
                            // RISK: if the object were not a `Movie`, the cast would throw
                            //   `ClassCastException` at runtime.
                            Movie movie = (Movie) intent.getSerializableExtra(MOVIE_EXTRA);

                            // Reads the position. The default value `-1` is the KEY
                            // of the whole protocol: -1 means "there is no position",
                            // that is, insert.
                            int position = intent.getIntExtra(POSITION_EXTRA, -1);

                            // INSERT MODE: ItemActivity did not send a position.
                            if(position == -1)
                            {
                                // Adds at the end of the list.
                                movies.add(movie);

                                // Tells the adapter that the data has changed.
                                // WITHOUT THIS LINE: the movie is in the list, but
                                // the ListView does not redraw and the user
                                // sees nothing until the next recreation.
                                movieAdapter.notifyDataSetChanged();
                            }
                            // EDIT MODE: ItemActivity sent the position.
                            else
                            {
                                // Replaces the element at that position.
                                // `set` does not change the size of the list.
                                movies.set(position, movie);

                                // Here too it is mandatory: `set` notifies nobody.
                                movieAdapter.notifyDataSetChanged();
                            }
                        }

                        // If resultCode != RESULT_OK, we do nothing. The list stays
                        // untouched, exactly as before the form was opened.
                    }
                });
    }

    // ==================================================================================
    // initListeners — WHAT HAPPENS ON INTERACTION
    // ==================================================================================
    // Two actions: the FAB (insert) and the row click (edit).
    // Both start the SAME activity, with different extras.
    // ==================================================================================
    private void initListeners() {

        // FAB -> INSERT
        // Anonymous class `View.OnClickListener` with a single method `onClick`.
        fabInsert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // `MainActivity.this` AND NOT `this`.
                // ==========================================================================
                // WHY: inside the anonymous class, `this` is the LISTENER INSTANCE,
                //   not the activity. `new Intent(this, ItemActivity.class)` would look for a
                //   constructor `Intent(View.OnClickListener, Class)` — which does not exist —
                //   and would NOT compile.
                // `MainActivity.this` is the qualified reference to the outer instance,
                //   which IS a `Context`.
                // ==========================================================================
                Intent intent = new Intent(MainActivity.this, ItemActivity.class);

                // Starts the activity THROUGH THE LAUNCHER, not through `startActivity`.
                // WHY: `startActivity` starts the screen, but has nowhere to return
                //   the result to. `launcher.launch` knows who receives the answer.
                // WITHOUT EXTRAS: ItemActivity sees `existing == null` -> insert mode.
                launcher.launch(intent);
            }
        });

        // ROW CLICK -> EDIT
        // `AdapterView.OnItemClickListener` has a single method with 4 parameters.
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                // The parameters:
                //   `parent`   — the ListView (not used here)
                //   `view`     — the pressed row (not used here)
                //   `position` — the index in the adapter; THIS is what interests us
                //   `id`       — the row id; for ArrayAdapter it equals the position

                // The same kind of Intent: the same activity, different content.
                Intent intent = new Intent(MainActivity.this, ItemActivity.class);

                // Sends the selected movie. `putExtra(String, Serializable)` is
                // the overload chosen because `Movie implements Serializable`.
                // THE OBJECT IS SERIALISED — a COPY arrives in ItemActivity.
                intent.putExtra(MOVIE_EXTRA, movies.get(position));

                // Sends the position too. It is the only thing that distinguishes edit from insert.
                // `int` -> `putExtra(String, int)`.
                intent.putExtra(POSITION_EXTRA, position);

                // Starts for a result, exactly as on insert.
                launcher.launch(intent);
            }
        });
    }

    // ==================================================================================
    // initControls — THE XML <-> CODE LINK
    // ==================================================================================
    // Three lines, three responsibilities: two `findViewById` and one adapter attachment.
    // ==================================================================================
    private void initControls() {

        // `findViewById` searches the tree inflated at `setContentView`.
        // In modern Java, the method is generic: the type is inferred from the variable on the left.
        //   The old equivalent: `(FloatingActionButton) findViewById(R.id.floatingActionButton)`.
        // If the ID does not exist in the layout, it returns `null` WITH NO error — NPE at the first access.
        fabInsert = findViewById(R.id.floatingActionButton);

        // The same for the list. The ID `listView` exists in `activity_main.xml`.
        listView = findViewById(R.id.listView);

        // Attaches the adapter. From this moment on, the ListView knows what to display.
        // WHY THE LAST LINE: it needs `listView` (already assigned) and
        //   `movieAdapter` (assigned in onCreate before this method is called).
        listView.setAdapter(movieAdapter);
    }

    // ==================================================================================
    // addSampleMovies — THE DEMO DATA
    // ==================================================================================
    // Three movies built with the 10-parameter constructor. This is where, in practice, the
    // positional trap described in 22.1 becomes visible: every call has exactly 10 arguments, in
    // the order of the signature.
    //
    // WHY IT EXISTS: so that you have something to see on first launch, without entering data by hand.
    // WHY IN CODE AND NOT IN RESOURCES: they are objects, not texts; an XML array would need
    //   parsing and type conversions.
    // ==================================================================================
    private void addSampleMovies() {

        // MOVIE 1: Inception
        // The arguments, in order:
        //   1. "Inception"                                          -> String title
        //   2. new GregorianCalendar(2010, Calendar.JULY, 16).getTime() -> Date release
        //   3. "Sci-Fi"                                             -> String genre
        //   4. 160000000                                            -> double budget
        //   5. 148                                                  -> int duration
        //   6. 4.5f                                                 -> float rating
        //   7. true                                                 -> boolean watched
        //   8. "PG-13"                                              -> String approval
        //   9. false                                                -> boolean onWatchlist
        //  10. "https://example.com/posters/inception.jpg"          -> String poster
        movies.add(new Movie(
                "Inception",

                // `GregorianCalendar(year, month, day)`:
                //   - the year is ABSOLUTE (2010), not relative;
                //   - the month is 0-INDEXED: `Calendar.JULY` = 6, not 7. That is why
                //     the constants are used, not the numbers — `new GregorianCalendar(2010, 7, 16)`
                //     would mean 16 AUGUST;
                //   - the day is 1-indexed, as in real life.
                // `.getTime()` turns the Calendar into a `Date` (milliseconds).
                //   `GregorianCalendar` IS a `Calendar`, but is NOT a `Date`.
                new GregorianCalendar(2010, Calendar.JULY, 16).getTime(),

                // The genre — plain text, it does not have to exist in `@array/movie_genre`.
                "Sci-Fi",

                // The budget: an `int` literal (160,000,000) widened automatically to `double`.
                // It needs no suffix; `160000000.0` would be equivalent.
                160000000,

                // The duration: an `int` literal, in minutes.
                148,

                // The rating: a `float` literal. The `f` suffix is MANDATORY.
                // Without it, `4.5` would be a `double` and would not match the `float` parameter
                // without an explicit cast.
                4.5f,

                // `watched` — a movie that has been seen.
                true,

                // `approval` — the text of the radio button, not the ID.
                "PG-13",

                // `onWatchlist` — it is not on the rewatch list.
                false,

                // `poster` — a fictitious URL. It is not downloaded anywhere in this commit.
                "https://example.com/posters/inception.jpg"));

        // MOVIE 2: Toy Story
        // The same structure, different values. `Calendar.NOVEMBER` = 10 (0-indexed).
        // `4f` is also a `float` literal, just without decimals.
        // WARNING: the genre "Animation" does NOT exist in `@array/movie_genre` (Drama, Action,
        //   Comedy, Horror, Romance, Thriller, Sci-Fi, Adventure, Fantasy, Mystery).
        //   The consequence appears on edit: `fillForm` finds no match, the spinner
        //   stays on "Drama", and saving changes the movie's genre. See 22.3.
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

        // MOVIE 3: The Matrix
        // `Calendar.MARCH` = 2 (0-indexed). `5f` = maximum rating.
        // `watched = false` and `onWatchlist = true` — the combination "I have not seen it,
        // but I want to". It is the only movie of the three with this combination,
        // useful for checking the two independent controls.
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
