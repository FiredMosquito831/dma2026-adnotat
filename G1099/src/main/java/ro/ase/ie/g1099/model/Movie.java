/*
 * ======================================================================================
 * FILE: Movie.java — THE DATA MODEL OF THE APP
 * ======================================================================================
 * WHAT THIS FILE IS:
 *   A POJO class (Plain Old Java Object): 10 private fields, one no-argument
 *   constructor, one constructor with 10 parameters, one `toString()` override and
 *   20 getters/setters. Zero business logic, zero references to Android.
 *
 * WHY IT EXISTS:
 *   So it can carry the data of a movie from one screen to another without depending on the UI.
 *   `MainActivity` holds `List<Movie>`; `ItemActivity` builds a `Movie`.
 *
 * WHAT IT DEPENDS ON:
 *   Only on `java.io.Serializable` and `java.util.Date`. Nothing from `android.*`.
 *   That is why the class can be tested on the JVM, without an emulator.
 *
 * WHO DEPENDS ON IT:
 *   `MainActivity` (the list, the adapter, the 3 demo movies), `ItemActivity`
 *   (`fillForm` reads, `saveMovie` writes), `ArrayAdapter` (calls `toString()`).
 *
 * WHAT IT DOES NOT DO:
 *   It validates nothing. It does not know what an `EditText`, an `Intent` or a `ListView` is.
 *   It has no `id`: the identity of the movie in the list is the POSITION, not a field.
 *
 * WHAT BREAKS WITHOUT IT:
 *   The whole data flow. `Intent.putExtra` would have nothing to serialize, `ArrayAdapter`
 *   would have nothing to display, and `ItemActivity` would have to send 10 separate
 *   extras and recompose them manually in `MainActivity`.
 * ======================================================================================
 */

// THE PACKAGE: `model` — the data layer, separate from `activities`.
// WHY IT MATTERS: `ItemActivity` imports `ro.ase.ie.g1099.model.Movie`, never the other way round.
//   The model does not know who uses it. It is the only architecture rule in the app.
package ro.ase.ie.g1099.model;

// ======================================================================================
// IMPORT 1: java.io.Serializable
// ======================================================================================
// WHAT IT IS: a MARKER interface — it has no method to implement.
// WHY HERE: `Intent.putExtra(String, Serializable)` is the simplest way to
//   send a whole object to another activity. Without `implements Serializable`,
//   the call `intent.putExtra(MOVIE_EXTRA, movie)` finds no matching
//   override and the file DOES NOT COMPILE.
// WHAT HAPPENS AT RUNTIME: the JVM serializes the object graph into bytes, puts it
//   in the `Bundle`, and the receiving activity gets a deserialized COPY.
// RISK: without a declared `serialVersionUID`, the JVM computes one from the class structure.
//   If you add a field between writing and reading (different processes), you get
//   `InvalidClassException`. Here the writing and the reading happen in the SAME process,
//   a few milliseconds apart, so the risk is theoretical.
// ======================================================================================
import java.io.Serializable;

// ======================================================================================
// IMPORT 2: java.util.Date
// ======================================================================================
// WHAT IT IS: a moment in time, stored internally as a number of milliseconds since 1 January 1970.
// WHY HERE: the release date must be a DATE, not a text. As a `Date` you can
//   sort, compare and format with `SimpleDateFormat`.
// WHY NOT `String`: you would store "16/07/2010" and lose the ability to compute.
//   `fillForm` would have to parse on every display, and `saveMovie` would have to
//   format on every save — two places where you can get the format wrong.
// WHY NOT `java.time.LocalDate`: it is newer and cleaner, but `Date` is
//   `Serializable` out of the box and is the type used throughout the project.
// RISK: `Date` is MUTABLE. `getRelease()` returns the internal reference, not a copy,
//   so a careless caller can modify the date in the object without going through the setter.
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
 * 2. **Serializability** — being `Serializable` and without references to `Context`,
 *    the object can cross the boundary between two activities. A `Movie` that held
 *    an `EditText` could not be serialized: `View` is not `Serializable`.
 *
 * 3. **Testability** — you can write a simple JVM test that creates a `Movie` and
 *    checks `getTitle()`. No emulator, no Android. This happens because
 *    the file contains no `import android.*`.
 *
 * 4. **No `id` field** — the identity of the movie in the list is the POSITION, not an ID.
 *    From this comes the whole `POSITION_EXTRA` mechanism in the other two files.
 *    It is a didactic simplification: a real database would have a stable `id`.
 *
 * 5. **Ten fields, not nine** — the poster is the tenth. It does not appear in `toString()`
 *    (see 31.0.4), but it exists in the model, it has a getter and a setter, and it is read by
 *    `ItemActivity` on edit. A field can exist without being displayed.
 *
 * VISUAL: the object that crosses the boundary between activities
 *
 *   [MainActivity]                    [Intent / Bundle]                [ItemActivity]
 *   movies: List<Movie>
 *     [0] Movie@A  ------------------+
 *                                    |
 *                      putExtra("movie", Movie@A)
 *                                    |
 *                          serialize -> bytes
 *                                    |
 *                          Bundle { "movie" -> bytes }
 *                                    |
 *                          startActivityForResult
 *                                    |
 *                          deserialize -> Movie@A'
 *                                    |
 *                          (NEW object, new address)
 *
 *   Movie@A stays in the list in MainActivity. A change to Movie@A' is not visible
 *   in the list. That is why MainActivity does `movies.set(position, movie)` at the end.
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
    //   on every display, and a mistyped text would reach the model as it is.
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
    //   in `arrays.xml` would retroactively change the genre of all the saved movies.
    // ==================================================================================
    private String genre; //Spinner

    // ==================================================================================
    // FIELD 4: budget
    // ==================================================================================
    // TYPE: double — a number with decimals, precision of ~15–16 significant digits.
    // SOURCE WIDGET: `etBudget` (inputType=numberDecimal) -> `Double.parseDouble(...)`.
    // WHY double and not float: `float` has ~7 significant digits. For 160 000 000
    //   both are exact, but 160 000 001 would not be exactly representable in `float`.
    //   In addition, `Double.parseDouble` returns a `double` — a `float` field would require a cast.
    // WHAT BREAKS IF IT IS WRONG: if it were `int`, the decimals would be truncated;
    //   if it were `String`, you could not compare budgets.
    // ==================================================================================
    private double budget; //EditText number

    // ==================================================================================
    // FIELD 5: duration
    // ==================================================================================
    // TYPE: int — a whole number of minutes, 0..300.
    // SOURCE WIDGET: `sbDuration` (SeekBar) -> `getProgress()`.
    // WHY int: `SeekBar.getProgress()` returns an `int`. Any other type would require a cast.
    // WHAT BREAKS IF IT IS WRONG: if it were `String`, `setProgress` would require parsing
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
    // WATCH OUT: the type is `float`, not `android.media.Rating`. See 31.0.5 — that
    //   import is dead, and the widget is `RatingBar`.
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
    // WHAT BREAKS IF IT IS WRONG: if it were `String` ("Yes"/"No"), every `if` would require
    //   a text comparison, and a lowercase "yes" would be treated as false.
    // NOTE: the comment in the source says "Switch", but the real type of the widget
    //   is `MaterialSwitch`. Comments do not update themselves.
    // ==================================================================================
    private boolean watched; //Switch

    // ==================================================================================
    // FIELD 8: approval
    // ==================================================================================
    // TYPE: String — the TEXT of the selected radio button ("G", "PG", "PG-13", "R", "NC-17").
    // SOURCE WIDGET: `rgApproval` (RadioGroup) -> loop over the children + `getText()`.
    // WHY the text and not the resource ID: the text survives the reordering of the buttons
    //   in XML. The ID is a value generated in `R.java`, stable only as long as
    //   you do not rename the button.
    // RISK: if you change the label in XML ("PG13" instead of "PG-13"), `fillForm`
    //   no longer finds a match and checks nothing — silently.
    // WHAT BREAKS IF IT IS WRONG: if you stored the index (0..4), adding a button
    //   at the beginning of the group would change the meaning of all the saved values.
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
    // FIELD 10: posterUrl
    // ==================================================================================
    // TYPE: String — a URL (or any text) to the movie poster.
    // SOURCE WIDGET: `etPoster` (inputType=textUri) -> `getText().toString()`.
    // WHY it has NO explicit `= null` (the difference from G1097): `String` is
    //   `null` by default anyway. The absence of the initializer does not change the behaviour, only
    //   the documentation note. See 31.0.2.
    // WHY it is called `posterUrl` and not `poster`: the suffix declares that the value is
    //   a LINK, not an image. The getter is called, consequently, `getPosterUrl()`.
    // RISK: `null` reaches `etPoster.setText(null)` on edit — legal, but it displays
    //   an empty field. If you called `.length()` on it, you would get a `NullPointerException`.
    // ==================================================================================
    private String posterUrl;

    // ==================================================================================
    // THE NO-ARGUMENT CONSTRUCTOR — THE FIRST IN THE FILE
    // ==================================================================================
    // WHY IT IS MANDATORY TO WRITE IT EXPLICITLY:
    //   Java rule: if you define ANY constructor with parameters, the compiler
    //   NO LONGER generates the no-argument constructor automatically.
    //   `saveMovie()` in ItemActivity does `Movie movie = new Movie();`.
    //   Without this constructor, the file DOES NOT COMPILE.
    //
    // WHY THE BODY IS EMPTY: all the fields receive the default values
    //   (null / 0 / 0.0 / false). The object is filled immediately with setters.
    //
    // WHY IT IS FIRST (the difference from G1097, where it is last):
    //   the position does not matter to the compiler, but here the reader sees immediately that
    //   `new Movie()` is legal. See 31.0.3.
    // ==================================================================================
    public Movie() {
    }

    // ==================================================================================
    // THE CONSTRUCTOR WITH 10 PARAMETERS
    // ==================================================================================
    // WHY IT EXISTS: `addSampleMovies()` in MainActivity creates the 3 demo movies
    //   in a single expression. Without it, there would be 10 setter calls per movie.
    //
    // THE TRAP: the order of the parameters is POSITIONAL. The names in the signature do not help
    //   at the call — the compiler looks only at the types and the position.
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
    //     10      String    posterUrl      etPoster (textUri)
    //
    //   If you swap positions 1 and 3 (both String) or 7 and 9 (both boolean),
    //   the code COMPILES. The movie is created. Only that `title` receives the genre and
    //   `genre` receives the title. The compiler cannot help you here.
    // ==================================================================================
    public Movie(String title, Date release, String genre, double budget, int duration, float rating, boolean watched, String approval, boolean onWatchlist, String posterUrl) {

        // `this.title` = the class field; `title` = the parameter.
        // Without `this.`, the line would be `title = title` — self-assignment, the field stays null.
        this.title = title;

        // The same pattern for each of the 10 fields. All 10 lines
        // are structurally identical; if one is missing, the field keeps its default
        // value (null / 0 / 0.0 / false) and the bug appears only on display.
        this.release = release;
        this.genre = genre;
        this.budget = budget;
        this.duration = duration;
        this.rating = rating;
        this.watched = watched;
        this.approval = approval;
        this.onWatchlist = onWatchlist;

        // The last field: here you also see the naming difference from G1097.
        // `this.posterUrl` is the field; `posterUrl` is the parameter. Both have the same name.
        this.posterUrl = posterUrl;
    }

    // ==================================================================================
    // toString() — THE DISPLAY CONTRACT IN THE LIST
    // ==================================================================================
    // WHY IT IS MANDATORY HERE:
    //   `ArrayAdapter` with `android.R.layout.simple_list_item_1` receives a `TextView`
    //   and fills it with `item.toString()`. Without the override, `Object.toString()` would
    //   return `ro.ase.ie.g1099.model.Movie@a1b2c3d` — the memory address.
    //
    // THE EXACT FLOW:
    //   ListView.getView(position) -> ArrayAdapter.getView(...) -> TextView.setText(
    //   movies.get(position).toString())
    //
    // WHY `@Override`: the compiler checks the signature. If you write `tostring()`
    //   with a lowercase letter, without the annotation you get no error — only your method
    //   is never called.
    //
    // THE DIFFERENCE FROM G1097: the poster does NOT appear here. Nine fields, not ten.
    //   See 31.0.4 — a field can exist in the model without being displayed.
    // ==================================================================================
    @Override
    public String toString() {

        // String concatenation with `+`. Each `+` creates a new object in memory.
        // For 9 fields, the difference is negligible; for large loops you would use
        // `StringBuilder`.
        return "Movie{" +

                // The text fields are wrapped in single quotes, so the boundaries of the
                // value are visible: `title='Inception'`. Without them, `title=''` and `genre=''`
                // would be indistinguishable from an empty field.
                "title='" + title + '\'' +

                // `Date` is not wrapped in single quotes: its `toString()` already produces
                // a readable format (`Sat Jul 16 00:00:00 GMT+03:00 2010`).
                ", release=" + release +

                // The comma at the beginning, not at the end: the last field does not need
                // a separator, and `}` is glued on correctly.
                ", genre='" + genre + '\'' +
                ", budget=" + budget +
                ", duration=" + duration +
                ", rating=" + rating +
                ", watched=" + watched +
                ", approval='" + approval + '\'' +
                ", onWatchlist=" + onWatchlist +

                // The closing brace of the object, as text.
                // THE POSTER WOULD BE DISPLAYED HERE, in G1097: ", poster='" + poster + '\''.
                // In G1099 it is missing, and the row in the list ends after onWatchlist.
                '}';
    }

    // ==================================================================================
    // GETTERS AND SETTERS — 20 METHODS, ALL MECHANICAL
    // ==================================================================================
    // WHY THEY EXIST: the fields are `private`. Code from other packages (and even from
    //   `activities`) cannot read or write them directly. The getters and setters
    //   are the only interface.
    //
    // WHY SO MANY: 10 fields x 2 = 20. They are generated automatically by IntelliJ
    //   (Alt+Insert -> Getter and Setter). They are not written by hand in the exam.
    //
    // THE NAMING CONVENTION:
    //   - `getX()` for objects and primitives, EXCEPT for boolean;
    //   - `isX()` for boolean — the JavaBeans convention.
    // ==================================================================================

    // GETTER: returns the reference to the String. String is immutable, so there is no
    // risk of modification from the outside.
    public String getTitle() {
        return title;
    }

    // SETTER: `this.title` = the field, `title` = the parameter. The same trap as in
    // the constructor with 10 parameters.
    public void setTitle(String title) {
        this.title = title;
    }

    // GETTER for Date. WATCH OUT: it returns the REFERENCE, not a copy.
    // RISK: `movie.getRelease().setTime(0)` would modify the date in the object without the setter.
    //   A correct variant would be `return new Date(release.getTime());`.
    public Date getRelease() {
        return release;
    }

    // SETTER for Date. It does not validate `null` — a `Movie` with a null date is accepted
    // by the model and crashes only in `fillForm`, at `dateFormat.format(null)`.
    public void setRelease(Date release) {
        this.release = release;
    }

    // GETTER for the genre. The value comes from the Spinner, but the model does not know that.
    public String getGenre() {
        return genre;
    }

    // SETTER for the genre. It does not check whether the value exists in `@array/movie_genre`.
    // Validation is the responsibility of the UI, not of the model.
    public void setGenre(String genre) {
        this.genre = genre;
    }

    // GETTER for the budget. `double` is copied by value — there are no aliasing
    // problems as with Date.
    public double getBudget() {
        return budget;
    }

    // SETTER for the budget. It does not reject negative values: a budget of -5 would be accepted.
    // Validation should be done in `saveMovie`, before the call.
    public void setBudget(double budget) {
        this.budget = budget;
    }

    // GETTER for the duration, in minutes.
    public int getDuration() {
        return duration;
    }

    // SETTER for the duration. It does not limit to 0..300 — the limit exists only in XML
    // (`android:max="300"`), that is, only in the UI.
    public void setDuration(int duration) {
        this.duration = duration;
    }

    // GETTER for the rating. `float` — the score in half stars.
    public float getRating() {
        return rating;
    }

    // SETTER for the rating. The parameter is `float`, exactly the type required by
    // `RatingBar.setRating(float)`.
    public void setRating(float rating) {
        this.rating = rating;
    }

    // ==================================================================================
    // WHY `isWatched()` AND NOT `getWatched()`
    // ==================================================================================
    // 1. THE JAVABEANS CONVENTION: for `boolean`, the getter is called `isX()`, not `getX()`.
    //    Reflection (and the frameworks that read properties) look for the `is` prefix.
    // 2. CONSISTENCY: `isOnWatchlist()` follows the same rule. If one were
    //    `getWatched()` and the other `isOnWatchlist()`, the code would be hard to read.
    // 3. THE CODE DEPENDS ON THE NAME: `fillForm` calls `existing.isWatched()`. If you
    //    wrote `getWatched()`, the call does not compile — and it must be changed in two places.
    // ==================================================================================
    public boolean isWatched() {
        return watched;
    }

    // The setter remains `setWatched` — the `set` prefix is the same for all types.
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

    // GETTER for the "Watchlist" check mark. Prefix `is` — again the JavaBeans convention.
    public boolean isOnWatchlist() {
        return onWatchlist;
    }

    // SETTER for the check mark.
    public void setOnWatchlist(boolean onWatchlist) {
        this.onWatchlist = onWatchlist;
    }

    // GETTER for the poster URL. It can return `null`.
    // THE NAME: `getPosterUrl`, not `getPoster` — the difference from G1097.
    //   `ItemActivity.fillForm` calls exactly this name.
    public String getPosterUrl() {
        return posterUrl;
    }

    // SETTER for the poster. It accepts `null` without complaining.
    // `ItemActivity.saveMovie` calls exactly this name.
    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }
}
