/*
 * ======================================================================================
 * FILE: MainActivity.java — THE LIST SCREEN, VERSION 2
 * ======================================================================================
 * WHAT THIS FILE IS:
 *   The start activity. It holds the movie list in an `ArrayList<Movie>`, displays it
 *   with a `RecyclerView` through an adapter of its own (`MovieAdapter`) and starts
 *   `ItemActivity` — for insert, from the FAB, and for edit, from a click on a row.
 *
 * WHY IT EXISTS:
 *   It is the only place that holds the STATE of the app: the `movies` list. All the other
 *   screens receive a `Movie` through an `Intent` and return a `Movie` through the result.
 *   Without this class there is nowhere for the result to come back to.
 *
 * WHAT IT DEPENDS ON:
 *   - `Movie` (the model), `MovieAdapter` and `IMovieListener` (the `adapter` package),
 *   - `R.layout.activity_main` and four IDs: `main`, `rvMovies`,
 *     `floatingActionButton`, `btnVlad`,
 *   - the `ItemActivity` and `PlaceCiocanul` classes, started through an `Intent`.
 *
 * WHO DEPENDS ON IT:
 *   `ItemActivity` reads `MainActivity.MOVIE_EXTRA` and `MainActivity.POSITION_EXTRA`
 *   (it sees them because they are `protected static` and the file is in the SAME package).
 *   `AndroidManifest.xml` declares it with the LAUNCHER intent-filter.
 *
 * WHAT IT DOES NOT DO:
 *   It does not validate data, does not keep a “selected” position, does not save anything to disk.
 *   The list lives in memory and disappears with the process.
 *
 * WHAT BREAKS WITHOUT IT:
 *   The app would have no entry point: without the LAUNCHER activity, the icon
 *   starts nothing. And without `movies` here, the form would have somewhere to send
 *   the result to, but nobody would receive it.
 * ======================================================================================
 */

// ======================================================================================
// THE PACKAGE AND THE IMPORTS
// ======================================================================================
package com.example.group1098.activities;

// Intent: the object that describes “which activity you start” and what data it carries.
//   Here it appears three times: at the FAB, at the second FAB and in the result callback.
import android.content.Intent;

// Bundle: the state bag saved by the system. It appears only in the signature of `onCreate`.
import android.os.Bundle;

// View: the base class of any UI element. The type of the parameter in `onClick`.
import android.view.View;

// AdapterView: the base class for ListView/Spinner, used by
//   `AdapterView.OnItemClickListener`.
// RISK: UNUSED IMPORT in version 2. Its only reference remains in the commented
//   block from `initListeners`. The code compiles, but Android Studio shows it grey.
import android.widget.AdapterView;

// ArrayAdapter: the bridge between `List<Movie>` and the ListView rows.
// RISK: UNUSED IMPORT — likewise, it survives only in comments.
import android.widget.ArrayAdapter;

// ListView: the old version's widget. It does not even exist in the layout any more.
// RISK: UNUSED IMPORT — a leftover of the migration.
import android.widget.ListView;

// Toast: short messages on screen. It is not used anywhere in this file —
//   it was not in the old version either (chapter 22 flagged it the same way).
import android.widget.Toast;

// EdgeToEdge: drawing under the system bars (status + navigation).
import androidx.activity.EdgeToEdge;

// ActivityResult: the result returned by the secondary activity: code + Intent.
import androidx.activity.result.ActivityResult;

// ActivityResultCallback: the single-method interface, called on return.
import androidx.activity.result.ActivityResultCallback;

// ActivityResultLauncher: the object that starts the activity and keeps the link
//   to the callback. It is the `launcher` field.
import androidx.activity.result.ActivityResultLauncher;

// ActivityResultContracts: the predefined contracts. Here `StartActivityForResult`,
//   that is, exactly the old `startActivityForResult`, wrapped in a modern way.
import androidx.activity.result.contract.ActivityResultContracts;

// AppCompatActivity: the base activity, with a Material theme and AppCompat support.
import androidx.appcompat.app.AppCompatActivity;

// Insets: the pixel values of the bars that cover the content.
import androidx.core.graphics.Insets;

// ViewCompat: the compatible version of the View APIs (insets on any API).
import androidx.core.view.ViewCompat;

// WindowInsetsCompat: the description of the insets, independent of the Android version.
import androidx.core.view.WindowInsetsCompat;

// LinearLayoutManager: the only layout manager used here. It decides the ARRANGEMENT
//   of the rows in a vertical list — `RecyclerView` has no default one.
import androidx.recyclerview.widget.LinearLayoutManager;

// RecyclerView: the widget that replaces ListView. The container, not the adapter.
import androidx.recyclerview.widget.RecyclerView;

// IMovieListener: the click contract. MainActivity IMPLEMENTS it.
import com.example.group1098.adapter.IMovieListener;

// MovieAdapter: the adapter of your own, written in the `adapter` package.
import com.example.group1098.adapter.MovieAdapter;

// FloatingActionButton: the round button. There are TWO on this screen.
import com.google.android.material.floatingactionbutton.FloatingActionButton;

// ArrayList: the concrete implementation of the movie list.
import java.util.ArrayList;

// Calendar: the month constants (`Calendar.JULY`, `NOVEMBER`, `MARCH`).
import java.util.Calendar;

// GregorianCalendar: the quick “year, month, day” constructor in the demo data.
import java.util.GregorianCalendar;

// List: the list interface.
// RISK: UNUSED IMPORT — the field is declared `ArrayList<Movie>`, not `List<Movie>`.
import java.util.List;

// R: the class generated from resources. Here for `R.layout.activity_main` and `R.id.*`.
import com.example.group1098.R;

// Movie: the data model. The only dependency on the `model` package.
import com.example.group1098.model.Movie;

/*
 * ======================================================================================
 * THEORY BLOCK: ACTIVITY, RECYCLERVIEW, CALLBACK
 * ======================================================================================
 *
 * 1. **Activity = a screen with a state.** `MainActivity` is a `Context` (it has access to
 *    resources and to the system) and a `LifecycleOwner` (it has a lifecycle: onCreate ->
 *    onStart -> onResume -> onPause -> onStop -> onDestroy).
 *
 * 2. **RecyclerView = an opinion-free container.** It does not know what it displays, it does not
 *    know what a row looks like and it does not know in which direction it scrolls. All it knows is to ask
 *    the adapter: “how many rows are there?” and “give me the row at position N”.
 *
 * 3. **Adapter = the translator.** For each visible row, it creates (once)
 *    the view and then fills it with data (every time). The two moments are two
 *    different methods: `onCreateViewHolder` and `onBindViewHolder`.
 *
 * 4. **Callback = the return of the click.** A row is an inert view. For the click to reach
 *    the activity, the adapter receives an object that implements `IMovieListener`;
 *    here that object is `MainActivity` ITSELF (`new MovieAdapter(movies, this)`).
 *
 * 5. **Launcher = the contract with the result.** `registerForActivityResult` is called
 *    ONCE, in `onCreate`, before the activity becomes visible. The result arrives
 *    in `onActivityResult`, with a code (`RESULT_OK` / `RESULT_CANCELED`) and an `Intent`.
 *
 * THE VISUAL FLOW OF EACH ROW:
 *
 *   movies (ArrayList<Movie>)            <- the data, owned by the activity
 *        |
 *        v
 *   MovieAdapter                         <- getItemCount() + onBindViewHolder()
 *        |
 *        v
 *   RecyclerView  <--- LinearLayoutManager   <- WHERE the rows live
 *        |
 *        v
 *   movie_item.xml  ->  one card per movie
 *
 * WHO HOLDS WHAT:
 *
 *   MainActivity  ---- owns ------>  ArrayList<Movie> movies
 *        |                                  ^
 *        | creates                          | the same reference
 *        v                                  |
 *   MovieAdapter  ----------------------> movies
 *        |
 *        +---- forwards the click to -----> IMovieListener ( = MainActivity )
 *
 * WHY `implements IMovieListener` IS MANDATORY:
 *   `new MovieAdapter(movies, this)` requires the second argument to be an
 *   `IMovieListener`. Without `implements`, the line DOES NOT COMPILE — and that is good:
 *   the compiler checks exactly the contract you want.
 * ======================================================================================
 */

// The start activity. `implements IMovieListener` makes it the recipient of the clicks.
public class MainActivity extends AppCompatActivity implements IMovieListener {

    // ==================================================================================
    // THE TWO KEYS OF THE COMMUNICATION WITH ItemActivity
    // ==================================================================================
    // `static final` = class constant: the same value for all instances,
    //   set once, at compile time.
    // `protected` = visible in subclasses AND in the whole package. ItemActivity is in
    //   `com.example.group1098.activities`, so it sees the constants without an import.
    // WHY NOT `private`: ItemActivity would receive them as the literal text “movie” / “position”;
    //   a typing mistake would give a non-existent extra, with no error at all.
    // RISK: if you change the “movie” value here, ItemActivity sees it automatically — but
    //   any `getStringExtra("movie")` written by hand in another file would break silently.
    // ==================================================================================
    protected static final String POSITION_EXTRA = "position";

    // The second key: the serialised movie. Same convention.
    protected static final String MOVIE_EXTRA = "movie";

    // The “+” FAB, from the bottom-right corner. It opens ItemActivity in insert mode.
    private FloatingActionButton fabInsert;

    // The old version's code, KEPT ON PURPOSE:
    // private ListView listView;
    // WHY IT STAYED: so the reader sees exactly what was replaced and with what. In the
    //   shipped code the comment has no effect; you can read it as a “before”.
    // REF: chapter 22, section 22.2 — the version in which the line was active.

    // The SUCCESSOR of `listView`: version 2's list container.
    private RecyclerView rvMovies;

    // The result launcher. A single object serves BOTH screens
    //   (`ItemActivity` and `PlaceCiocanul`), because both are started the same way.
    // RISK: it is assigned ONLY at the end of `onCreate`, but it is used in
    //   listeners created earlier. See the explanation of step 8 in `onCreate`.
    private ActivityResultLauncher<Intent> launcher;

    // THE LIST — the screen's only data source.
    // `final` means the REFERENCE never changes; the content does.
    //   That is why `movies.add(...)` is legal, and `movies = new ArrayList<>()` is not.
    // WHY `final` helps here: the adapter receives THIS reference; if you
    //   replaced the list with another, the adapter would stay on the old one, with no error.
    private final ArrayList<Movie> movies = new ArrayList<>();

    // The old version's code, kept commented out:
    //private ArrayAdapter<Movie> movieAdapter;
    // Note the spacing: the original had no space after `//`. It is the exact record
    //   of a line commented with Ctrl+/ in Android Studio, not rewritten by hand.
    // REF: chapter 22 — `ArrayAdapter` with `simple_list_item_1`.

    // The adapter of your own. The type is the class written in the `adapter` package, not a system one.
    private MovieAdapter movieAdapter;

    // The second FAB, added in version 2. Bottom-left; it opens the test screen.
    // The name (`btnVlad`) is the one from the XML (`@+id/btnVlad`) — it matches exactly.
    private FloatingActionButton btnVlad;

    // ==================================================================================
    // METHOD: onCreate — THE CONSTRUCTION OF THE SCREEN
    // ==================================================================================
    // WHAT IT DOES: it creates the screen, loads the demo data, builds the adapter, binds
    //   the views to the code, installs the two button listeners and registers
    //   the result launcher.
    // HOW: in the order below — and the order is the only part that is hard to fix.
    // RISK: each step depends on the previous one. Move `initControls()` after
    //   `initListeners()` and you get a `NullPointerException` on `fabInsert`,
    //   because the listener would bind to a field that is still `null`.
    // REF: chapter 22, 22.2 — the same scheme, with ListView instead of RecyclerView.
    // ==================================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // 1. The mandatory chain of the base class: without it, AppCompat does not
        //    install its delegate and the theme is not applied.
        super.onCreate(savedInstanceState);

        // 2. The windows draw under the system bars (status bar + navigation).
        EdgeToEdge.enable(this);

        // 3. The view tree is inflated from the XML. From this moment on,
        //    `findViewById` has something to look for.
        setContentView(R.layout.activity_main);

        // 4. The compensation of the bars: the `R.id.main` container receives padding equal to
        //    the height of the bars, so the first row does not sit under the status bar.
        //    The lambda receives `insets` and returns exactly what it received — the API contract.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {

            // 4a. The “system bars” insets: status bar on top, navigation at the bottom.
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            // 4b. Padding on all four sides. `left/right` also cover the RTL case,
            //     because they are already computed by the system in the right direction.
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);

            // 4c. The received object is returned: otherwise the system would consider the insets
            //     consumed and other views would no longer receive them.
            return insets;
        });

        // 5. The demo data: three movies, added BEFORE the adapter is created.
        //    (Technically, the order would not hurt: the adapter holds the reference to the list, not a copy.)
        addSampleMovies();

        // The old version's code, kept commented out — the ready-made adapter:
        // movieAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, movies);
        // WHAT IT DID: it bound the list to a standard row, which displayed `movie.toString()`,
        //   that is, the whole object, on a single line of text.
        // WHAT REPLACES IT: the line below, with the adapter of your own.

        // 6. The adapter of your own. `this` is the second parameter because MainActivity
        //    implements `IMovieListener` — it is the recipient of the clicks.
        //    The REFERENCE to the list is passed: the adapter sees any later change.
        movieAdapter = new MovieAdapter(movies, this);

        // 7a. The binding of the views: at this moment `rvMovies`, `fabInsert` and `btnVlad`
        //     receive the real objects. `movieAdapter` is already built, so
        //     `setAdapter` has something to receive.
        initControls();

        // 7b. The button listeners. They need the views from 7a — hence
        //     this order and not the reverse.
        initListeners();

        // 7c. The second listener, in a separate method: the `btnVlad` FAB.
        initSecondListener();

        // 8. The result launcher. `registerForActivityResult` must be called
        //    before the activity is STARTED; `onCreate` is the canonical place.
        //    The `StartActivityForResult` contract is the modern equivalent of the old
        //    `startActivityForResult(intent, REQUEST_CODE)`.
        launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),

                // The callback: an anonymous class that implements a single method.
                new ActivityResultCallback<ActivityResult>() {

                    // It is called on the return from ItemActivity or PlaceCiocanul.
                    @Override
                    public void onActivityResult(ActivityResult o) {

                        // The gate: work is done only when the secondary screen has said
                        //   “I have a result”. `RESULT_CANCELED` (the Back button) does not
                        //   change anything in the list.
                        if(o.getResultCode() == RESULT_OK)
                        {
                            // The result Intent. It can be `null` if the activity
                            //   called `setResult(RESULT_OK)` without an Intent.
                            // RISK: `null` is not checked here. Today it is safe, because
                            //   ItemActivity always puts an Intent (chapter 22, 22.3),
                            //   and PlaceCiocanul never calls `setResult`.
                            Intent intent = o.getData();

                            // The travelling movie: a DESERIALISED COPY, not the original.
                            //   The cast is necessary: `getSerializableExtra` returns
                            //   `Serializable`, the common type of all the objects put in.
                            Movie movie = (Movie) intent.getSerializableExtra(MOVIE_EXTRA);

                            // The position: -1 = “does not exist”, that is, insert mode.
                            //   The default value -1 is precisely the app's convention.
                            int position = intent.getIntExtra(POSITION_EXTRA, -1);

                            // Branch 1: no position -> a NEW movie.
                            if(position == -1)
                            {
                                // Addition at the end of the list.
                                movies.add(movie);

                                // The notification to the RecyclerView. Without it, the list stays
                                //   visually unchanged: the adapter does not observe the ArrayList.
                                // REF: chapter 23 — the fine-grained variant, `notifyItemInserted`.
                                movieAdapter.notifyDataSetChanged();
                            }

                            // Branch 2: valid position -> REPLACEMENT, not addition.
                            else
                            {
                                // `set` overwrites the element at the given position.
                                // RISK: if the list had changed in the meantime (deletion,
                                //   reordering), the position could point to another movie.
                                //   Here the list does not change outside these two branches.
                                movies.set(position, movie);

                                // The same notification: a row has changed.
                                movieAdapter.notifyDataSetChanged();
                            }
                        }
                    }
                });
    }

    // ==================================================================================
    // METHOD: initListeners — the INSERT FAB (and the old code, commented out)
    // ==================================================================================
    // WHAT IT DOES: it binds `fabInsert` to starting ItemActivity without extras. A single
    //   active listener; on top of that, it keeps version 1's list listener commented out.
    // HOW: 1) listener on the FAB; 2) build an explicit Intent; 3) launch.
    // RISK: `launcher` is still `null` at the moment the listener is created —
    //   but it is read only on click, when `onCreate` finished long ago.
    // REF: chapter 22, 22.2 — the same method, without the commented block.
    // ==================================================================================
    private void initListeners() {

        // Listener on the FAB. An anonymous class, not a lambda: the style of the project (and of the exam).
        fabInsert.setOnClickListener(new View.OnClickListener() {

            // It runs on every press.
            @Override
            public void onClick(View v) {

                // EXPLICIT Intent: the source (`MainActivity.this`) and the destination
                //   (`ItemActivity.class`). No extras: ItemActivity sees that it has no
                //   `MOVIE_EXTRA` and enters insert mode.
                // NOTE: the class is written with the full package name, although
                //   ItemActivity is in the SAME package. It is redundant, but legal.
                Intent intent = new Intent(MainActivity.this, com.example.group1098.activities.ItemActivity.class);

                // Starting through the launcher: the result will reach the callback from
                //   step 8 of `onCreate`.
                launcher.launch(intent);
            }
        });

        // ==============================================================================
        // THE OLD VERSION'S CODE — THE ROW CLICK, KEPT COMMENTED OUT
        // ==============================================================================
        // This is the most valuable didactic block in the file: it shows EXACTLY what was
        // lost when ListView was replaced by RecyclerView and where it was moved.
        //
        //   BEFORE (ListView):                 NOW (RecyclerView):
        //   listView.setOnItemClickListener    listener in MovieAdapter (on itemView)
        //   onItemClick(..., position, id)     onMovieClick(Movie, position)
        //   movies.get(position)               movies.get(position)
        //   putExtra(MOVIE_EXTRA, ...)         putExtra(MOVIE_EXTRA, ...)
        //   putExtra(POSITION_EXTRA, ...)      putExtra(POSITION_EXTRA, ...)
        //   launcher.launch(intent)            launcher.launch(intent)
        //
        // The last four lines are IDENTICAL to `onMovieClick` at the end of the file.
        // The difference is only WHO calls them: ListView before, the adapter now.
        /* listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(MainActivity.this, com.example.group1098.activities.ItemActivity.class);
                intent.putExtra(MOVIE_EXTRA, movies.get(position));
                intent.putExtra(POSITION_EXTRA, position);
                launcher.launch(intent);
            }
        });*/
        // WHY THE MOVE WAS NECESSARY: a RecyclerView row is not a list item
        //   observed by the widget, but an ordinary view. There is no “onItemClick”;
        //   the click is bound manually, on `itemView`, in the `ViewHolder` (38.2).
        // RISK: if you left both active (the uncommented block + `onMovieClick`),
        //   the click would start ItemActivity twice — two overlapping activities.
    }

    // ==================================================================================
    // METHOD: initSecondListener — THE SECOND FAB
    // ==================================================================================
    // WHAT IT DOES: it binds `btnVlad` to starting the `PlaceCiocanul` screen.
    // HOW: 1) listener; 2) explicit Intent; 3) the SAME launcher.
    // RISK: the same launcher also receives the result of this screen. PlaceCiocanul does not
    //   call `setResult`, so the returned code is `RESULT_CANCELED` and the callback
    //   does nothing — exactly the desired behaviour.
    // REF: `initListeners`, with a single difference: the destination class.
    // ==================================================================================
    private void initSecondListener() {

        // Listener on the second FAB (bottom-left, the hammer icon).
        btnVlad.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                // The variable is called `intent2` — enough not to be confused
                //   with `intent` from the method above, although they are in different methods.
                Intent intent2 = new Intent(MainActivity.this, com.example.group1098.activities.PlaceCiocanul.class);

                // The same launcher: a single result contract for two screens.
                launcher.launch(intent2);
            }
        });
    }

    // ==================================================================================
    // METHOD: initControls — FINDVIEWBYID AND THE CONFIGURATION OF THE LIST
    // ==================================================================================
    // WHAT IT DOES: it binds the Java fields to the views in the XML and configures the RecyclerView
    //   (layout manager + adapter).
    // HOW: 1) FAB; 2) RecyclerView; 3) layout manager; 4) adapter; 5) the second FAB.
    // RISK: `setLayoutManager` must be called BEFORE `setAdapter`. Without a layout
    //   manager, RecyclerView draws nothing and writes to Logcat:
    //   “No layout manager attached; skipping layout”.
    // REF: chapter 22, 22.2 — the same method, with `ListView.setAdapter` instead.
    // ==================================================================================
    private void initControls() {

        // 1. The insert FAB, by the ID from `activity_main.xml`.
        fabInsert = findViewById(R.id.floatingActionButton);

        // The old version's code, kept commented out — the binding of the ListView:
        // listView = findViewById(R.id.listView);
        // listView.setAdapter(movieAdapter);
        // NOTE: both lines are commented out, but `R.id.listView` does not even
        //   exist in the layout any more. If you uncommented them, the file WOULD NOT COMPILE.

        // 2. The RecyclerView. The `rvMovies` ID exists in version 2's layout.
        rvMovies = findViewById(R.id.rvMovies);

        // 3. Layout manager: MANDATORY, before the adapter. `this` is the Context.
        //    LinearLayoutManager = vertical list, one row under another.
        //    Other variants: GridLayoutManager (grid), StaggeredGridLayoutManager.
        rvMovies.setLayoutManager(new LinearLayoutManager(this));

        // 4. The adapter built in `onCreate`. From this moment on, the RecyclerView knows
        //    how many rows the list has and how to build them.
        rvMovies.setAdapter(movieAdapter);

        // 5. The second FAB. Looked up AFTER the adapter is bound — the order between
        //    views does not matter, they all exist from `setContentView`.
        btnVlad = findViewById(R.id.btnVlad);
    }

    // ==================================================================================
    // METHOD: addSampleMovies — THE DEMO DATA
    // ==================================================================================
    // WHAT IT DOES: it adds three movies built with the 10-parameter constructor to the list.
    // HOW: three `new Movie(...)` calls, each with the same order of arguments.
    // RISK: the constructor is POSITIONAL. Swapping two arguments of the SAME type
    //   (String with String, boolean with boolean) compiles and produces wrong data.
    // REF: chapter 22, 22.1 — the table of positions in `Movie`'s constructor.
    // ==================================================================================
    private void addSampleMovies() {

        // Movie 1. The arguments, by position:
        //   1 title="Inception", 2 release=16 July 2010, 3 genre="Sci-Fi",
        //   4 budget=160000000, 5 duration=148, 6 rating=4.5f,
        //   7 watched=true, 8 approval="PG-13", 9 onWatchlist=false, 10 posterUrl=...
        movies.add(new Movie(

                // 1. The title.
                "Inception",

                // 2. The date. `GregorianCalendar(year, month, day)` — the month is 0-based:
                //    `Calendar.JULY` is 6, not 7. `.getTime()` turns the calendar
                //    into a `Date`, the type required by the model.
                new GregorianCalendar(2010, Calendar.JULY, 16).getTime(),

                // 3. The genre — free text, not an enum.
                "Sci-Fi",

                // 4. The budget — `double`, hence no suffix.
                160000000,

                // 5. The duration in minutes — `int`.
                148,

                // 6. The rating — `float`: the `f` suffix is MANDATORY. Without it,
                //    the literal `4.5` is a `double` and the call does not compile.
                4.5f,

                // 7. “I have watched the movie” — boolean.
                true,

                // 8. The parental guidance — the radio button's text.
                "PG-13",

                // 9. “On the rewatch list” — the second boolean, independent of the first.
                false,

                // 10. The poster URL. Here it is an example URL: the app
                //     downloads no image, so the value never reaches the screen.
                "https://example.com/posters/inception.jpg"));

        // Movie 2. The same structure, other values. `Calendar.NOVEMBER` = 10 (0-based).
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

        // Movie 3. `Calendar.MARCH` = 2 (0-based); rating 5f — the maximum score;
        //   `watched=false`, `onWatchlist=true`: the movie is set aside, not watched.
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
    // METHOD: onMovieClick — THE IMPLEMENTATION OF THE CLICK CONTRACT
    // ==================================================================================
    // WHAT IT DOES: it receives the movie and the position from the adapter, then starts ItemActivity
    //   in edit mode, with both extras.
    // HOW: 1) explicit Intent; 2) put the movie; 3) put the position; 4) launch.
    // RISK: the method uses `movies.get(position)`, NOT the `movie` parameter. The two
    //   are consistent here (the adapter reads from the same list). If the adapter
    //   received another list, the parameter would be the correct source.
    // REF: the commented block in `initListeners` — this is `onItemClick` moved.
    // ==================================================================================
    @Override
    public void onMovieClick(Movie movie, int position) {

        // 1. The same explicit Intent as at the FAB, but with extras.
        Intent intent = new Intent(MainActivity.this, com.example.group1098.activities.ItemActivity.class);

        // 2. The movie, serialised into the Bundle. `putExtra(String, Serializable)`.
        //    It is taken from the list by position — the screen's source of truth.
        intent.putExtra(MOVIE_EXTRA, movies.get(position));

        // 3. The position: its presence is the “edit mode” signal for ItemActivity.
        intent.putExtra(POSITION_EXTRA, position);

        // 4. The start. The result (the modified movie) arrives in the callback from `onCreate`
        //    and goes down the `movies.set(position, movie)` branch.
        launcher.launch(intent);
    }
}
