/*
 * ======================================================================================
 * FILE: Movie.java — THE DATA MODEL
 * ======================================================================================
 * WHAT THIS FILE IS:
 *   A POJO class (Plain Old Java Object): 10 private fields, one constructor with 10
 *   parameters, one empty constructor and 20 getters/setters. Zero business logic.
 *
 * WHY IT EXISTS:
 *   To carry a movie's data from one screen to another without depending on the UI.
 *   `MainActivity` holds a list of `Movie`; `ItemActivity` builds a `Movie`.
 *
 * WHAT IT KNOWS:
 *   Only the values: title, date, genre, budget, duration, rating, watched, approval,
 *   onWatchlist, poster. It does not know what an `EditText`, an `Intent` or a `ListView` is.
 *
 * WHAT WOULD BREAK WITHOUT IT:
 *   The entire data flow. `Intent.putExtra` would have nothing to serialise, `ArrayAdapter`
 *   would have nothing to display, and `ItemActivity` would have to send 10 separate extras
 *   and reassemble them by hand in `MainActivity`.
 * ======================================================================================
 */

package ro.ase.ie.g1097.model;

// ======================================================================================
// IMPORT 1: java.io.Serializable
// ======================================================================================
// WHAT IT IS: a MARKER interface — it has no method to implement.
// WHY HERE: `Intent.putExtra(String, Serializable)` is the simplest way to
//   send a whole object to another activity. Without `implements Serializable`,
//   the call `intent.putExtra(MOVIE_EXTRA, movie)` finds no matching overload
//   and DOES NOT COMPILE.
// WHAT HAPPENS AT RUNTIME: the JVM serialises the object graph into bytes, puts it
//   in the `Bundle`, and the receiving activity gets a DESERIALISED COPY.
// RISK: without a declared `serialVersionUID`, the JVM computes one from the class structure.
//   If you add a field between writing and reading (different processes), you get
//   `InvalidClassException`. Here the write and the read happen in the SAME process,
//   a few milliseconds apart, so the risk is theoretical.
// ======================================================================================
import java.io.Serializable;

// ======================================================================================
// IMPORT 2: java.util.Date
// ======================================================================================
// WHAT IT IS: a moment in time, stored internally as a number of milliseconds since 1 January 1970.
// WHY HERE: the release date must be a DATE, not a text. As a `Date` you can
//   sort, compare and format it with `SimpleDateFormat`.
// WHY NOT `String`: you would store "16/07/2010" and lose the ability to compute.
//   `fillForm` would have to parse on every display, and `saveMovie` would have to
//   format on every save — two places where you can get the format wrong.
// WHY NOT `java.time.LocalDate`: it is newer and cleaner, but `Date` is
//   `Serializable` out of the box and is the type required by the assignment statement.
// RISK: `Date` is MUTABLE. `getRelease()` returns the internal reference, not a copy,
//   so a careless caller can change the date in the object without going through the setter.
// ======================================================================================
import java.util.Date;

/*
 * ======================================================================================
 * THEORY BLOCK: WHY A SEPARATE MODEL (POJO)
 * ======================================================================================
 *
 * 1. **Single Responsibility** — the class has a single reason to change: the
 *    structure of a movie's data changes. It does not change when a layout, a button
 *    or a colour changes. If you put `getText().toString()` here, the class would depend on
 *    `android.widget.EditText` and would have two reasons to change.
 *
 * 2. **Serializability** — being `Serializable` and holding no references to a `Context`,
 *    the object can cross the boundary between two activities. A `Movie` that held
 *    an `EditText` could not be serialised: `View` is not `Serializable`.
 *
 * 3. **Testability** — you can write a simple JVM test that creates a `Movie` and
 *    checks `getTitle()`. No emulator, no Android. That happens because
 *    the file contains no `import android.*` at all.
 *
 * 4. **No `id` field** — a movie's identity in the list is its POSITION, not an ID.
 *    The whole `POSITION_EXTRA` mechanism in the other two files comes from this.
 *    It is a didactic simplification: a real database would have a stable `id`.
 *
 * VISUAL: how the object travels between activities
 *
 *   [MainActivity]                      [Intent]                     [ItemActivity]
 *   movies.get(position)                                                  |
 *         |                                                               |
 *         +-- putExtra("movie", obj) --+                                  |
 *                                      |                                  |
 *                          serialize (JVM -> bytes)                      |
 *                                      |                                  |
 *                                      +-- Bundle -- startActivity --> --+
 *                                                                         |
 *                                                       getSerializableExtra(...)
 *                                                                         |
 *                                                          A COPY, not the original
 *
 *   The original stays in MainActivity's list. Changes to the copy are NOT visible
 *   in the list. That is why MainActivity calls `movies.set(position, movie)` at the end.
 * ======================================================================================
 */
public class Movie implements Serializable {

    // ==================================================================================
    // FIELD 1: title
    // ==================================================================================
    // TYPE: String — free text, of arbitrary length.
    // SOURCE WIDGET: `etTitle` (TextInputEditText) -> `getText().toString()`.
    // WHY String: the title is text, it has no numeric or date operations.
    // WHAT BREAKS IF IT IS WRONG: if it were `int`, "Inception" would not fit;
    //   if it were `char[]`, `ArrayAdapter` would display the array's address.
    // ==================================================================================
    private String title; //EditText

    // ==================================================================================
    // FIELD 2: release
    // ==================================================================================
    // TYPE: Date — a moment in time (here, only the day matters).
    // SOURCE WIDGET: `etRelease` + `DatePickerDialog`; it reaches the model from
    //   `releaseCal.getTime()`, NOT from the text in the EditText.
    // WHY Date: it allows formatting with `SimpleDateFormat` and comparison.
    // WHAT BREAKS IF IT IS WRONG: if it were `String`, `fillForm` would have to parse
    //   on every display, and a badly typed text would reach the model as it is.
    // ==================================================================================
    private Date release; //EditText date

    // ==================================================================================
    // FIELD 3: genre
    // ==================================================================================
    // TYPE: String — the text selected from the list ("Sci-Fi", "Animation", "Action").
    // SOURCE WIDGET: `spGenre` (Spinner) -> `getSelectedItem().toString()`.
    // WHY String and not `enum`: the genres come from `@array/movie_genre`, that is, from XML.
    //   With an `enum`, adding a genre would require changing the Java code.
    // WHAT BREAKS IF IT IS WRONG: if you stored the position (`int`), changing the order
    //   in `arrays.xml` would retroactively change the genre of every saved movie.
    // ==================================================================================
    private String genre; //Spinner

    // ==================================================================================
    // FIELD 4: budget
    // ==================================================================================
    // TYPE: double — a number with decimals, precision of ~15–16 significant digits.
    // SOURCE WIDGET: `etBudget` (inputType=numberDecimal) -> `Double.parseDouble(...)`.
    // WHY double and not float: `float` has ~7 significant digits. For 160,000,000
    //   both are exact, but 160,000,001 would not be exactly representable in `float`.
    //   On top of that, `Double.parseDouble` returns `double` — a `float` field would need a cast.
    // WHAT BREAKS IF IT IS WRONG: if it were `int`, the decimals would be truncated;
    //   if it were `String`, you could not compare budgets.
    // ==================================================================================
    private double budget; //EditText number

    // ==================================================================================
    // FIELD 5: duration
    // ==================================================================================
    // TYPE: int — a whole number of minutes, 0..300.
    // SOURCE WIDGET: `sbDuration` (SeekBar) -> `getProgress()`.
    // WHY int: `SeekBar.getProgress()` returns `int`. Any other type would need a cast.
    // WHAT BREAKS IF IT IS WRONG: if it were `String`, `setProgress` would need parsing
    //   and you could save "abc" as a duration.
    // ==================================================================================
    private int duration; //SeekBar

    // ==================================================================================
    // FIELD 6: rating
    // ==================================================================================
    // TYPE: float — the score in half stars (0.0, 0.5, ..., 5.0).
    // SOURCE WIDGET: `rbRating` (RatingBar) -> `getRating()`.
    // WHY float: `RatingBar.getRating()` and `setRating(float)` work with `float`.
    //   A `double` field would make `setRating(existing.getRating())` NOT compile
    //   without an explicit cast.
    // WHAT BREAKS IF IT IS WRONG: if it were `int`, you would lose the half stars;
    //   if it were `String` ("4.5"), the comparisons would be lexicographic.
    // ==================================================================================
    private float rating; //RatingBar

    // ==================================================================================
    // FIELD 7: watched
    // ==================================================================================
    // TYPE: boolean — true/false.
    // SOURCE WIDGET: `swWatched` (MaterialSwitch) -> `isChecked()`.
    // WHY boolean: the state of a switch is binary. `isChecked()` returns exactly that.
    // WHAT BREAKS IF IT IS WRONG: if it were `String` ("Yes"/"No"), every `if` would need
    //   a text comparison, and a lowercase "yes" would be treated as false.
    // ==================================================================================
    private boolean watched; //Switch

    // ==================================================================================
    // FIELD 8: approval
    // ==================================================================================
    // TYPE: String — the TEXT of the selected radio button ("G", "PG", "PG-13", "R", "NC-17").
    // SOURCE WIDGET: `rgApproval` (RadioGroup) -> loop over the children + `getText()`.
    // WHY text and not the resource ID: the text survives reordering the buttons
    //   in XML. The ID is a value generated in `R.java`, stable only as long as
    //   you do not rename the button.
    // RISK: if you change the label in XML ("PG13" instead of "PG-13"), `fillForm`
    //   no longer finds a match and checks nothing — silently.
    // WHAT BREAKS IF IT IS WRONG: if you stored the index (0..4), adding a button
    //   at the start of the group would change the meaning of every saved value.
    // ==================================================================================
    private String approval; //RadioGroup | RadioButtons

    // ==================================================================================
    // FIELD 9: onWatchlist
    // ==================================================================================
    // TYPE: boolean — true/false.
    // SOURCE WIDGET: `cbonWatchList` (MaterialCheckBox) -> `isChecked()`.
    // WHY boolean: a check mark is binary. Independent of `watched`: you can have seen
    //   the movie AND keep it on the rewatch list.
    // WHAT BREAKS IF IT IS WRONG: if you reused `watched` for both, the two
    //   controls would contradict each other on save.
    // ==================================================================================
    private boolean onWatchlist; //Checkbox

    // ==================================================================================
    // FIELD 10: poster
    // ==================================================================================
    // TYPE: String — a URL (or any text) to the movie's poster.
    // SOURCE WIDGET: `etPoster` (inputType=textUri) -> `getText().toString()`.
    // WHY `= null` explicitly: a `String` is `null` by default anyway. The line is
    //   DOCUMENTATION: "this field may be missing". A movie without a poster is valid.
    // RISK: `null` reaches `etPoster.setText(null)` on edit — legal, but it shows
    //   an empty field. If you called `.length()` on it, you would get `NullPointerException`.
    // ==================================================================================
    private String poster = null;

    // ==================================================================================
    // THE CONSTRUCTOR WITH 10 PARAMETERS
    // ==================================================================================
    // WHY IT EXISTS: `addSampleMovies()` in MainActivity creates the 3 demo movies
    //   in a single expression. Without it, there would be 10 setter calls per movie.
    //
    // THE TRAP: the order of the parameters is POSITIONAL. The names in the signature do not help
    //   at the call site — the compiler only looks at types and position.
    //
    //   POSITION  TYPE      NAME           WIDGET THAT PRODUCES THE VALUE
    //   ---------------------------------------------------------------------------
    //      1      String    title          TextInputEditText etTitle
    //      2      Date      release        etRelease + DatePickerDialog
    //      3      String    genre          Spinner spGenre
    //      4      double    budget         etBudget (numberDecimal)
    //      5      int       duration       SeekBar sbDuration
    //      6      float     rating         RatingBar rbRating
    //      7      boolean   watched        MaterialSwitch swWatched
    //      8      String    approval       RadioGroup rgApproval
    //      9      boolean   onWatchlist    MaterialCheckBox cbOnWatchList
    //     10      String    poster         etPoster (textUri)
    //
    //   If you swap positions 1 and 3 (both String) or 7 and 9 (both boolean),
    //   the code COMPILES. The movie is created. It is just that `title` receives the genre and
    //   `genre` receives the title. The compiler cannot help you here.
    // ==================================================================================
    public Movie(String title, Date release, String genre, double budget, int duration, float rating, boolean watched, String approval, boolean onWatchlist, String poster) {

        // `this.title` = the class field; `title` = the parameter.
        // Without `this.`, the line would be `title = title` — self-assignment, the field stays null.
        this.title = title;

        // The same pattern for each of the 10 fields. All 10 lines
        // are structurally identical; if one is missing, the field keeps its
        // default value (null / 0 / 0.0 / false) and the bug only shows up at display time.
        this.release = release;
        this.genre = genre;
        this.budget = budget;
        this.duration = duration;
        this.rating = rating;
        this.watched = watched;
        this.approval = approval;
        this.onWatchlist = onWatchlist;
        this.poster = poster;
    }

    // ==================================================================================
    // toString() — THE DISPLAY CONTRACT IN THE LIST
    // ==================================================================================
    // WHY IT IS MANDATORY HERE:
    //   `ArrayAdapter` with `android.R.layout.simple_list_item_1` receives a `TextView`
    //   and fills it with `item.toString()`. Without the override, `Object.toString()` would
    //   return `ro.ase.ie.g1097.model.Movie@a1b2c3d` — the address in memory.
    //
    // THE EXACT FLOW:
    //   ListView.getView(position) -> ArrayAdapter.getView(...) -> TextView.setText(
    //   movies.get(position).toString())
    //
    // WHY `@Override`: the compiler checks the signature. If you write `tostring()`
    //   in lowercase, without the annotation you get no error at all — your method
    //   is simply never called.
    //
    // THE FORMAT: generated by IntelliJ with Alt+Insert -> toString(). `'\''` is an
    //   escaped apostrophe, so that it does not close the character literal.
    //
    // WHAT YOU SEE ON SCREEN: all 10 fields, on a single line, including the poster
    //   URL. Ugly, but correct. It is fixed in the next chapter with RecyclerView
    //   and `movie_item.xml`.
    // ==================================================================================
    @Override
    public String toString() {

        // String concatenation with `+`. Every `+` creates a new object in memory.
        // For 10 fields the difference is negligible; for large loops you would use
        // `StringBuilder`.
        return "Movie{" +

                // The text fields are wrapped in apostrophes, so that the limits of the
                // value are visible: `title='Inception'`. Without them, `title=''` and `genre=''`
                // would be indistinguishable from an empty field.
                "title='" + title + '\'' +

                // `Date` is not wrapped in apostrophes: its `toString()` already produces
                // a readable format (`Sat Jul 16 00:00:00 GMT+03:00 2010`).
                ", release=" + release +

                // The comma at the start, not at the end: the last field does not need
                // a separator, and `}` is glued on correctly.
                ", genre='" + genre + '\'' +
                ", budget=" + budget +
                ", duration=" + duration +
                ", rating=" + rating +
                ", watched=" + watched +
                ", approval='" + approval + '\'' +
                ", onWatchlist=" + onWatchlist +
                ", poster='" + poster + '\'' +

                // The closing brace of the object, as text.
                '}';
    }

    // ==================================================================================
    // GETTERS AND SETTERS — 20 METHODS, ALL MECHANICAL
    // ==================================================================================
    // WHY THEY EXIST: the fields are `private`. Code in other packages (and even in
    //   `activities`) cannot read or write them directly. The getters and setters
    //   are the only interface.
    //
    // WHY SO MANY: 10 fields x 2 = 20. They are generated automatically by IntelliJ
    //   (Alt+Insert -> Getter and Setter). You do not write them by hand in the exam.
    //
    // THE NAMING CONVENTION:
    //   - `getX()` for objects and primitives, EXCEPT for boolean;
    //   - `isX()` for boolean — the JavaBeans convention.
    // ==================================================================================

    // GETTER: returns the reference to the String. String is immutable, so there is no
    // risk of modification from outside.
    public String getTitle() {
        return title;
    }

    // SETTER: `this.title` = the field, `title` = the parameter. The same trap as in
    // the constructor with 10 parameters.
    public void setTitle(String title) {
        this.title = title;
    }

    // GETTER for Date. WARNING: it returns the REFERENCE, not a copy.
    // RISK: `movie.getRelease().setTime(0)` would change the date in the object without a setter.
    //   A correct variant would be `return new Date(release.getTime());`.
    public Date getRelease() {
        return release;
    }

    // SETTER for Date. It does not validate `null` — a `Movie` with a null date is accepted
    // by the model and crashes only in `fillForm` (see BUG 2).
    public void setRelease(Date release) {
        this.release = release;
    }

    // GETTER for genre. The value comes from the Spinner, but the model does not know that.
    public String getGenre() {
        return genre;
    }

    // SETTER for genre. It does not check whether the value exists in `@array/movie_genre`.
    // Validation is the UI's responsibility, not the model's.
    public void setGenre(String genre) {
        this.genre = genre;
    }

    // GETTER for budget. `double` is copied by value — there are no aliasing
    // problems as with Date.
    public double getBudget() {
        return budget;
    }

    // SETTER for budget. It does not reject negative values: a budget of -5 would be accepted.
    // Validation should be done in `saveMovie`, before the call.
    public void setBudget(double budget) {
        this.budget = budget;
    }

    // GETTER for duration, in minutes.
    public int getDuration() {
        return duration;
    }

    // SETTER for duration. It does not clamp to 0..300 — the limit exists only in XML
    // (`android:max="300"`), that is, only in the UI.
    public void setDuration(int duration) {
        this.duration = duration;
    }

    // GETTER for rating. `float` — the score in half stars.
    public float getRating() {
        return rating;
    }

    // SETTER for rating. The parameter is `float`, exactly the type required by
    // `RatingBar.setRating(float)`.
    public void setRating(float rating) {
        this.rating = rating;
    }

    // ==================================================================================
    // WHY `isWatched()` AND NOT `getWatched()`
    // ==================================================================================
    // 1. THE JAVABEANS CONVENTION: for `boolean`, the getter is called `isX()`, not `getX()`.
    //    Reflection (and frameworks that read properties) look for the `is` prefix.
    // 2. CONSISTENCY: `isOnWatchlist()` follows the same rule. If one were
    //    `getWatched()` and the other `isOnWatchlist()`, the code would be hard to read.
    // 3. THE CODE DEPENDS ON THE NAME: `fillForm` calls `existing.isWatched()`. If you had
    //    written `getWatched()`, the call would not compile — and it has to be changed in two places.
    // ==================================================================================
    public boolean isWatched() {
        return watched;
    }

    // The setter stays `setWatched` — the `set` prefix is the same for all types.
    public void setWatched(boolean watched) {
        this.watched = watched;
    }

    // GETTER for the parental guidance ("PG-13" etc.).
    public String getApproval() {
        return approval;
    }

    // SETTER for the guidance. The value is the text of the radio button, not its ID.
    public void setApproval(String approval) {
        this.approval = approval;
    }

    // GETTER for the "Watchlist" check mark. `is` prefix — again the JavaBeans convention.
    public boolean isOnWatchlist() {
        return onWatchlist;
    }

    // SETTER for the check mark.
    public void setOnWatchlist(boolean onWatchlist) {
        this.onWatchlist = onWatchlist;
    }

    // GETTER for the poster URL. It may return `null`.
    public String getPoster() {
        return poster;
    }

    // SETTER for the poster. It accepts `null` without complaining.
    public void setPoster(String poster) {
        this.poster = poster;
    }

    // ==================================================================================
    // THE CONSTRUCTOR WITHOUT PARAMETERS — THE LAST LINE OF THE FILE
    // ==================================================================================
    // WHY IT MUST BE WRITTEN EXPLICITLY:
    //   The Java rule: if you define ANY constructor with parameters, the compiler
    //   NO LONGER generates the no-argument constructor automatically.
    //   `saveMovie()` in ItemActivity does `Movie movie = new Movie();`.
    //   Without this constructor, the file DOES NOT COMPILE.
    //
    // WHY THE BODY IS EMPTY: all fields receive their default values
    //   (null / 0 / 0.0 / false). The object is filled immediately with setters.
    //
    // WHY IT IS AT THE END: the position does not matter to the compiler. It is at the end
    //   because that is how IntelliJ generated it and because the reader sees it after
    //   all the methods it depends on.
    // ==================================================================================
    public Movie() {
    }
}
