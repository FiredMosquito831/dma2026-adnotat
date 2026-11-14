/*
 * ======================================================================================
 * FILE: MovieAdapter.java — THE LIST ADAPTER
 * ======================================================================================
 * WHAT THIS FILE IS:
 *   The bridge between `ArrayList<Movie>` (data) and `RecyclerView` (UI). It extends
 *   `RecyclerView.Adapter<MovieAdapter.MovieViewHolder>` and contains two classes:
 *   the adapter itself and the `ViewHolder` that holds the row's references.
 *
 * WHY IT EXISTS:
 *   `RecyclerView` does not know how to display a `Movie`. It only knows how to ask:
 *   “how many elements do you have?” (`getItemCount`), “give me a row view” (`onCreateViewHolder`)
 *   and “fill this view with the element at position N” (`onBindViewHolder`).
 *
 * WHAT IT DEPENDS ON:
 *   - `Movie` (the model) and `IMovieListener` (the click contract),
 *   - `R.layout.movie_item` (the row layout) and nine IDs from it,
 *   - `Chip` from Material Components, `RatingBar` and `ImageView` from the framework.
 *
 * WHO DEPENDS ON IT:
 *   `MainActivity` builds it (`new MovieAdapter(movies, this)`) and passes it
 *   to `rvMovies.setAdapter(...)`.
 *
 * WHAT IT DOES NOT DO:
 *   It does not own the list (it receives it by reference), does not decide the arrangement of the rows
 *   (the `LayoutManager` does), does not know what happens on click — it only notifies
 *   the listener.
 *
 * WHAT BREAKS WITHOUT IT:
 *   `rvMovies.setAdapter(...)` would have no argument; the list would stay empty on screen,
 *   although `movies` would have three elements.
 * ======================================================================================
 */

// The `adapter` package: separate from `activities` and from `model`. The adapter depends on both,
//   but neither of them depends on the adapter except MainActivity.
package com.example.group1098.adapter;

// LayoutInflater: the only way to turn an XML into a view tree.
import android.view.LayoutInflater;

// View: the type of the root of an inflated row and the type of the parameter in `onClick`.
import android.view.View;

// ViewGroup: the parent in which the row will sit (the RecyclerView itself).
//   The `Context` for the inflate is taken from it.
import android.view.ViewGroup;

// ImageView: the poster. Declared in the holder, but NOT FILLED in `onBindViewHolder`.
import android.widget.ImageView;

// RatingBar: the stars. Declared in the holder, but NOT FILLED — it stays at the XML value.
import android.widget.RatingBar;

// TextView: the card's four texts (title, subtitle, rating value, facts).
import android.widget.TextView;

// NonNull: a contract annotation. It does not change the bytecode; it tells lint (and
//   the reader) that the parameter is not allowed to be null, and that the return is not null.
import androidx.annotation.NonNull;

// RecyclerView: the base class. `RecyclerView.Adapter` and `RecyclerView.ViewHolder`
//   are the inner classes we extend here.
import androidx.recyclerview.widget.RecyclerView;

// R: `R.layout.movie_item` and the nine IDs looked up with `findViewById`.
import com.example.group1098.R;

// Movie: the element displayed.
import com.example.group1098.model.Movie;

// Chip: the rounded label from Material Components. Three of them per card.
import com.google.android.material.chip.Chip;

// SimpleDateFormat: turns `Date` into a year, for the “genre. year” subtitle.
import java.text.SimpleDateFormat;

// ArrayList: the type of the list received through the constructor.
import java.util.ArrayList;

/*
 * ======================================================================================
 * THEORY BLOCK: THE RecyclerView.Adapter CONTRACT
 * ======================================================================================
 *
 * 1. **Three mandatory methods, nothing else:**
 *      - `getItemCount()`        -> how many rows exist in total;
 *      - `onCreateViewHolder()`  -> builds an EMPTY row view;
 *      - `onBindViewHolder()`    -> fills an EXISTING view with data.
 *
 * 2. **Why two methods and not one.** `onCreateViewHolder` is expensive: it reads the XML
 *    and builds the view tree. It is called once for each
 *    row that fits on screen. `onBindViewHolder` is cheap: only `setText`.
 *    It is called every time a row reappears on scroll. If you had 10,000
 *    movies and 8 visible rows, ~10 views are created and they are bound thousands of times.
 *
 * 3. **Recycling.** When a row leaves the screen, its view is not thrown away: it goes
 *    into a pool. When a new row is needed, a view is taken out of the pool and
 *    re-bound — with no inflate. Hence the name “RecyclerView”.
 *
 *    screen:  [0][1][2][3][4][5][6][7]      (8 visible)
 *    you scroll down:
 *       row 0 leaves  ->  its view goes into the pool
 *       row 8 enters <-  it receives the recycled view, re-bound with its 8's data
 *
 * 4. **ViewHolder = the findViewById cache.** Without it, every bind would look up
 *    eight views through the tree again. The `ViewHolder` holds them once, in `final` fields.
 *    `RecyclerView` guarantees that the `ViewHolder` object travels together with its
 *    view, so `holder.title` is always the current row's TextView.
 *
 * 5. **The position is not yours.** `position` comes as a parameter at bind, but at click it no
 *    longer exists: it is requested with `getAbsoluteAdapterPosition()`. In the meantime, the list can
 *    change, and a position saved in a field would become wrong.
 *
 * 6. **`viewType`.** Here it is ignored: all the rows look the same. If you had two
 *    types of cards (watched movie / new movie), `getItemViewType(position)` would decide,
 *    and `onCreateViewHolder` would receive a different `viewType` and would inflate another XML.
 * ======================================================================================
 */

// The adapter. The generic in `<...>` is the TYPE OF THE HOLDER, not the type of the element.
//   That is why `RecyclerView.Adapter<MovieAdapter.MovieViewHolder>`: the inner class
//   is referred to by its full name, because it appears in the signature of the class that contains it.
public class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.MovieViewHolder> {

    // ==================================================================================
    // THE TWO REFERENCES
    // ==================================================================================
    // The list is NOT copied: the adapter holds the reference received from the activity.
    //   The good consequence: `movies.add(...)` in MainActivity is seen here immediately,
    //   without having to rebuild the adapter.
    // The dangerous consequence: if MainActivity replaced the list with another
    //   (`movies = new ArrayList<>()`), the adapter would stay on the old one.
    // `final` = the reference is not reassigned. The content can change freely.
    // No access modifier = visible in the `adapter` package.
    final ArrayList<Movie> movies;

    // The click listener: who receives the event. Here, MainActivity.
    // RISK: `null` is not checked. If you built the adapter with `null`, everything would
    //   work until the first click, when you would get a NullPointerException.
    final IMovieListener listener;

    // ==================================================================================
    // THE CONSTRUCTOR — ONLY TWO ASSIGNMENTS
    // ==================================================================================
    // WHAT IT DOES: it receives the list and the listener and holds them for the adapter's whole life.
    // HOW: two assignments, no logic.
    // RISK: the list is not cloned. If you wanted a copy, you would need `new ArrayList<>(movies)`
    //   — but then the changes in the activity would no longer be seen in the list.
    // REF: `new MovieAdapter(movies, this)` in MainActivity, 38.1.
    // ==================================================================================
    public MovieAdapter(ArrayList<Movie> movies, IMovieListener listener) {

        // `this.movies` = the field; `movies` = the parameter. The same trap as in `Movie`.
        this.movies = movies;

        // The same pattern for the listener.
        this.listener = listener;
    }

    // ==================================================================================
    // METHOD: onCreateViewHolder — THE CONSTRUCTION OF AN EMPTY ROW
    // ==================================================================================
    // WHAT IT DOES: it inflates `movie_item.xml` into a new view and wraps it in a holder.
    // HOW: 1) take the inflater from the parent's context; 2) inflate the layout with `false`;
    //   3) return a new `MovieViewHolder`.
    // RISK: the third argument must be `false`. With `true`, the view would be glued
    //   to the parent immediately, and the RecyclerView would try to add it again -> crash.
    // REF: chapter 23, the exercise about `ViewHolder` — the same code, step by step.
    // ==================================================================================
    @NonNull
    @Override
    public MovieViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        // 1. The inflater is taken FROM THE PARENT'S CONTEXT, not from the activity: the adapter
        //    does not have and must not have a reference to an Activity. `parent.getContext()`
        //    is the activity hosting the RecyclerView, so the theme is the right one.
        // 2. `inflate(R.layout.movie_item, parent, false)`:
        //      - first argument: the row's XML;
        //      - second: the parent, from which the correct `LayoutParams` are taken;
        //      - third, `false`: do NOT attach now. The RecyclerView decides when.
        //    “Do not attach” is the correct answer in 99% of adapter cases.
        // 3. The result is a card view, still with no data in it.
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.movie_item, parent, false);

        // 4. It is wrapped in the holder. From this moment on, `holder.title` etc. are valid:
        //    the holder's constructor does all the `findViewById` calls.
        return new MovieViewHolder(view);
    }

    // ==================================================================================
    // METHOD: onBindViewHolder — FILLING THE ROW WITH DATA
    // ==================================================================================
    // WHAT IT DOES: it takes the movie at the given position and writes the title and subtitle into the holder.
    // HOW: 1) a year formatter; 2) the movie; 3) the title; 4) “genre. year”.
    // RISK: only TWO fields out of nine are filled. The rest keep the XML values
    //   — see the BUG below.
    // REF: chapter 22, 22.6 — `movie_item.xml`, the card you see now.
    // ==================================================================================
    @Override
    public void onBindViewHolder(@NonNull MovieViewHolder holder, int position) {

        // 1. A formatter for the year. It is created ON EVERY BIND — a new object for
        //    each row, every time the row reappears on scroll.
        // RISK: `SimpleDateFormat` is expensive to construct. The correct way would be a field
        //   `private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy");`
        //   built once. Here, with three movies, it is not noticeable.
        // WARNING: `"yyyy"` is the calendar year. `"YYYY"` would be the week-year
        //   (ISO week-year) and would give 2027 for a few days of December 2026.
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy");

        // 2. The movie at the requested position. `position` is given by the RecyclerView and is
        //    guaranteed to be in the range 0..getItemCount()-1 during a bind.
        Movie movie = movies.get(position);

        // 3. The title. `setText` accepts a CharSequence; `getTitle()` returns a String.
        holder.title.setText(movie.getTitle());

        // 4. The subtitle: “genre. year”. Three observations, all verifiable:
        //    - `movie.getGenre()` is already a `String`; `.toString()` on a String
        //      returns the SAME object — it is redundant, but harmless.
        //      RISK: if `genre` were null, the call would throw a NullPointerException.
        //      DANGER!: the exception erupts in onBindViewHolder, during scrolling,
        //        not at start-up — a crash that appears only when the list reaches that
        //        movie. Here `addSampleMovies` and the form always put a genre, so it does not
        //        happen; but the model allows `null` through the empty constructor.
        //    - the separator is “. ” (dot and space), not “ · ” as the
        //      comment in `movie_item.xml` suggests. The visible result: “Sci-Fi. 2010”.
        //    - `sdf.format(movie.getRelease())` requires a non-null `Date`. If the movie
        //      had a null date (possible through the empty constructor), it would throw an NPE.
        holder.subtitle.setText(movie.getGenre().toString() + ". " + sdf.format(movie.getRelease()));

        // NOTE: the method ends here. There is no `super.onBindViewHolder` call.
        //   It is not mandatory — the base class does nothing in it.
    }

    // ==================================================================================
    // METHOD: getItemCount — HOW MANY ROWS THERE ARE
    // ==================================================================================
    // WHAT IT DOES: it returns the number of elements in the list.
    // HOW: `movies.size()`, read on every call — hence always up to date.
    // RISK: if you returned a constant value or a manually updated field,
    //   the RecyclerView would request binds for non-existent positions -> IndexOutOfBounds.
    // REF: `notifyDataSetChanged()` in MainActivity — the notification that makes the RecyclerView
    //   ask this method again.
    // ==================================================================================
    @Override
    public int getItemCount() {

        // The source of truth is the list, not a parallel counter.
        return movies.size();
    }

    /*
     * ==================================================================================
     * BUG: THE CARD DISPLAYS STATIC DATA FOR ALL THE MOVIES
     * ==================================================================================
     * `MovieViewHolder` looks up nine views, but `onBindViewHolder` writes only into two:
     * `title` and `subtitle`. The other seven stay exactly as they were inflated from XML:
     *
     *   from XML (the same value on EVERY row)        from the model (ignored)
     *   --------------------------------------------  ------------------------
     *   movieFacts      = "142 min  ·  $160M"          duration, budget
     *   movieRatingValue= "4.5"                        rating
     *   movieRating     = 0 stars (no `rating=` in XML)  rating
     *   chipApproval    = "Approved"                   approval
     *   chipWatched     = "Watched"                    watched
     *   chipWatchlist   = "Watchlist"                  onWatchlist
     *   moviePoster     = the gallery icon             posterUrl
     *
     * Consequence: Inception (148 min, $160M, PG-13) and Toy Story (81 min, $30M, G)
     * appear with the SAME facts and the SAME labels. The data is in the model, but never
     * reaches the screen. It is not a syntax error: it compiles and runs.
     * ==================================================================================
     */

    // ==================================================================================
    // THE INNER CLASS: MovieViewHolder
    // ==================================================================================
    // WHAT IT DOES: it holds the references of the card's nine views and binds the click.
    // HOW: `findViewById` once, in the constructor; the click on `itemView`.
    // RISK: it is a NON-static inner class — it holds an implicit reference to the adapter.
    //   Here that is convenient (it sees `movies` and `listener` directly), but it means the
    //   holder cannot live without the adapter.
    // REF: `RecyclerView.ViewHolder`, which holds `itemView` — the whole row's view.
    // ==================================================================================
    class MovieViewHolder extends RecyclerView.ViewHolder {

        // ==============================================================================
        // THE NINE FIELDS — THE findViewById CACHE
        // ==============================================================================
        // All `final`: they are assigned once, in the constructor, and never change again.
        //   A new row = a new holder = a new set of references.
        // The nine correspond to the nine IDs in `movie_item.xml` that the
        //   code uses. In the layout there are two more IDs that the holder does NOT look up:
        //   `movieStatusGroup` (the ChipGroup) and `chipCiocan` (the fourth chip).
        // ==============================================================================

        // The poster. Looked up, but never filled — see the BUG above.
        final ImageView poster;

        // The title — FILLED in `onBindViewHolder`.
        final TextView title;

        // The “genre. year” subtitle — FILLED in `onBindViewHolder`.
        final TextView subtitle;

        // The stars. Never set: `android:rating` does not exist in XML, so 0 stars.
        final RatingBar rating;

        // The numeric value of the rating. It stays “4.5” from XML for any movie.
        final TextView ratingValue;

        // “duration · budget”. It stays “142 min · $160M” from XML.
        final TextView facts;

        // The three status labels. They remain the texts from XML.
        final Chip chipApproval;
        final Chip chipWatched;
        final Chip chipWatchList;

        // ==============================================================================
        // THE HOLDER'S CONSTRUCTOR
        // ==============================================================================
        // WHAT IT DOES: it binds each field to its view in the received row and installs
        //   the click listener on the whole row.
        // HOW: `super(itemView)` mandatory, then nine `findViewById`, then a listener.
        // RISK: `findViewById` on `itemView`, NOT on the activity. If you looked in the
        //   activity, you would find the first view with that ID on the whole screen — not necessarily
        //   from the current row.
        // REF: chapter 22, 22.6 — the IDs looked up here are exactly the ones in the layout.
        // ==============================================================================
        public MovieViewHolder(@NonNull View itemView) {

            // `super(itemView)` is mandatory: the base class keeps the row's view
            //   and uses it for positioning and for animations.
            super(itemView);

            // Each `findViewById` returns a `View`; assigning it to a field of a
            //   concrete type does the cast automatically (generics), without a hand-written `(TextView)`.
            //   If the ID were missing from the layout, it would return `null` — no error at
            //   compile time, just an NPE at the first bind.
            poster = itemView.findViewById(R.id.moviePoster);
            title = itemView.findViewById(R.id.movieTitle);
            subtitle = itemView.findViewById(R.id.movieSubtitle);
            rating = itemView.findViewById(R.id.movieRating);
            ratingValue = itemView.findViewById(R.id.movieRatingValue);
            facts = itemView.findViewById(R.id.movieFacts);
            chipApproval = itemView.findViewById(R.id.chipApproval);
            chipWatched = itemView.findViewById(R.id.chipWatched);
            chipWatchList = itemView.findViewById(R.id.chipWatchlist);

            // ==========================================================================
            // THE ROW CLICK
            // ==========================================================================
            // This is the whole mechanism that replaces `setOnItemClickListener`.
            // It binds on `itemView` — the whole card, not a specific text.
            // WHY in the constructor and not in bind: the listener is the same for the whole
            //   life of the view; binding it on every bind would allocate an object for nothing.
            // ==========================================================================
            itemView.setOnClickListener(new View.OnClickListener() {

                // It runs on press. `v` is the card.
                @Override
                public void onClick(View v) {

                    // The position is requested NOW, not held in a field.
                    // `getAbsoluteAdapterPosition()` returns the position in the
                    //   adapter's list, or `NO_POSITION` (-1) if the row was removed
                    //   from the list in the meantime (deletion animation, changed list).
                    int pos = getAbsoluteAdapterPosition();

                    // The guard: valid position? Without it, `movies.get(-1)` would throw
                    //   IndexOutOfBoundsException. Here no elements are deleted, but
                    //   the guard is mandatory as a pattern.
                    if (pos != RecyclerView.NO_POSITION) {

                        // The movie at that position.
                        Movie movie = movies.get(pos);

                        // The notification to the activity. `listener` is MainActivity.
                        //   From this call you reach `onMovieClick`, which starts
                        //   ItemActivity with two extras (38.1).
                        listener.onMovieClick(movie, pos);
                    }
                }
            });
        }
    }
}
