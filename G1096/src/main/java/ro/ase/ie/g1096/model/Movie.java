/*
 * ======================================================================================
 * FILE: Movie.java — THE DATA MODEL OF THE APP
 * ======================================================================================
 * WHAT THIS FILE IS:
 *   A POJO class (Plain Old Java Object): 10 private fields, one no-argument
 *   constructor, one constructor with 10 parameters and 20 getters/setters.
 *   Zero business logic, zero references to Android.
 *
 * WHY IT EXISTS:
 *   It is the only type that crosses the boundary between the two activities.
 *   `MainActivity` holds `List<Movie>`; `ItemActivity` builds a `Movie` from the
 *   form and sends it back. The model is the binder between the two screens.
 *
 * WHAT IT DEPENDS ON:
 *   - `java.io.Serializable` — so the object can be put in an `Intent`.
 *   - `java.util.Date` — the type of the release date.
 *   Nothing from `android.*`. This is the property that makes it testable on the JVM.
 *
 * WHO DEPENDS ON IT:
 *   `MainActivity` (the list, the adapter, the extras), `ItemActivity` (`fillForm`,
 *   `saveMovie`), `ArrayAdapter` (through `toString()`).
 *
 * WHAT IT DOES NOT DO:
 *   - It validates nothing: it accepts an empty title, a negative budget, a null date.
 *   - It has no `id`: identity in the list is the POSITION.
 *   - It has no `equals()` / `hashCode()`: two movies with the same values are not
 *     "equal" for `List.remove()` or `List.contains()`.
 *   - It does not know what an `EditText`, a `Spinner` or an `Intent` is.
 *
 * WHAT BREAKS WITHOUT IT:
 *   `Intent.putExtra(MOVIE_EXTRA, movie)` would have no type; `ArrayAdapter` would
 *   have nothing to display; `ItemActivity` would have to send 10 separate extras, and
 *   `MainActivity` would recompose them by hand — 10 keys to keep in sync in two places.
 * ======================================================================================
 */

// The package = the path under `src/main/java`. Here `ro.ase.ie.g1096.model`.
// `model` is the subpackage that holds the data types; `activities` holds the screens.
// The separation is not cosmetic: `model` imports nothing from `activities`, so it
// can be tested and reused without Android.
package ro.ase.ie.g1096.model;

// ======================================================================================
// IMPORT 1: java.io.Serializable
// ======================================================================================
// WHAT IT IS: a MARKER interface — it has no method to implement.
// WHY HERE: `Intent.putExtra(String, Serializable)` is the simplest way to
//   send a whole object to another activity. Without `implements Serializable`,
//   the call `intent.putExtra(MOVIE_EXTRA, movie)` finds no matching overload
//   and DOES NOT COMPILE.
// WHAT HAPPENS AT RUNTIME: the JVM serializes the object graph into bytes, puts it
//   in the `Bundle`, and the receiving activity gets a deserialized COPY.
// RISK: with no `serialVersionUID` declared, the JVM computes one from the class structure.
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
//   `Serializable` out of the box and is the type used by the rest of the app.
// RISK: `Date` is MUTABLE. `getRelease()` returns the internal reference, not a copy,
//   so a careless caller can change the date in the object without going through the setter.
// ======================================================================================
import java.util.Date;

/*
 * ======================================================================================
 * THEORY BLOCK: WHY A SEPARATE MODEL (POJO) AND WHY `Serializable`
 * ======================================================================================
 *
 * 1. SINGLE RESPONSIBILITY
 *    The class has a single reason to change: the structure of a movie's data changes.
 *    It does not change when a layout, a button or a colour changes. If you put
 *    `getText().toString()` here, the class would depend on `android.widget.EditText` and
 *    would have two reasons to change.
 *
 * 2. SERIALIZABILITY
 *    `Intent.putExtra(String, Serializable)` is the simplest way to send a
 *    whole object to another activity. Without `implements Serializable`, the call
 *    `intent.putExtra(MOVIE_EXTRA, movie)` finds no matching overload and
 *    DOES NOT COMPILE. The interface has no methods: it only changes what the JVM can
 *    do with the object — turn it into bytes and back.
 *
 * 3. WHAT HAPPENS AT RUNTIME
 *    The JVM serializes the object graph into bytes, puts it in the `Bundle`, and the
 *    receiving activity gets a deserialized COPY. The original stays in the
 *    list in `MainActivity`.
 *
 *    [MainActivity]                      [Intent]                     [ItemActivity]
 *    movies.get(position)                                                  |
 *          |                                                               |
 *          +-- putExtra("movie", obj) --+                                  |
 *                                       |                                  |
 *                           serialize (JVM -> bytes)                      |
 *                                       |                                  |
 *                                       +-- Bundle -- startActivity --> --+
 *                                                                          |
 *                                                        getSerializableExtra(...)
 *                                                                          |
 *                                                           A COPY, not the original
 *
 *    The practical consequence: changing the copy is NOT visible in the list. That is why
 *    `MainActivity` does `movies.set(position, movie)` on return.
 *
 * 4. WHY NO `id` FIELD
 *    The identity of the movie in the list is the POSITION, not an ID. From this comes the whole
 *    `POSITION_EXTRA` mechanism in the other two files. It is a didactic simplification: a real
 *    database would have a stable `id`, and deleting a row would no longer move the
 *    identity of the others.
 *
 * 5. TESTABILITY
 *    In the file there is no `import android.*`. You can write a JVM test that creates
 *    a `Movie` and checks `getTitle()`. No emulator, no Android, no Robolectric.
 *
 * 6. THE NAMING CONVENTION FOR BOOLEANS
 *    `isWatched()` and `isOnWatchlist()`, not `getWatched()` and `getOnWatchlist()`.
 *    It is the JavaBeans convention: frameworks that read properties through reflection
 *    look for the `is` prefix for `boolean`. `fillForm` calls exactly these names.
 * ======================================================================================
 */

// `implements Serializable` = marker interface, with no method to implement.
// It only changes what the JVM can do with the object: turn it into bytes.
public class Movie implements Serializable {

    // ==================================================================================
    // FIELD 1: title
    // ==================================================================================
    // TYPE: String — free text, of arbitrary length.
    // SOURCE WIDGET: `etTitle` (TextInputEditText) -> `getText().toString()`.
    // WHY String: the title is text, it has no numeric or date operations.
    // WHAT BREAKS IF IT IS WRONG: if it were `int`, "Inception" would not fit;
    //   if it were `char[]`, `ArrayAdapter` would display the address of the array.
    // ==================================================================================
    private String title; //EditText

    // ==================================================================================
    // FIELD 2: release
    // ==================================================================================
    // TYPE: Date — a moment in time (here, only the day matters).
    // SOURCE WIDGET: `etRelease` + `DatePickerDialog`; in the model it arrives from
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
    //   in the resource list `@array/movie_genre` would retroactively change the genre
    //   of all saved movies.
    // ==================================================================================
    private String genre; //Spinner

    // ==================================================================================
    // FIELD 4: budget
    // ==================================================================================
    // TYPE: double — a number with decimals, precision of ~15–16 significant digits.
    // SOURCE WIDGET: `etBudget` (inputType=numberDecimal) -> `Double.parseDouble(...)`.
    // WHY double and not float: `float` has ~7 significant digits. For 160 000 000
    //   both are exact, but 160 000 001 would not be exactly representable in `float`.
    //   In addition, `Double.parseDouble` returns `double` — a `float` field would require a cast.
    // WHAT BREAKS IF IT IS WRONG: if it were `int`, the decimals would be truncated;
    //   if it were `String`, you could not compare budgets.
    // ==================================================================================
    private double budget; //EditText number

    // ==================================================================================
    // FIELD 5: duration
    // ==================================================================================
    // TYPE: int — a whole number of minutes, 0..300.
    // SOURCE WIDGET: `sbDuration` (SeekBar) -> `getProgress()`.
    // WHY int: `SeekBar.getProgress()` returns `int`. Any other type would require a cast.
    // WHAT BREAKS IF IT IS WRONG: if it were `String`, `setProgress` would require parsing
    //   and you could save "abc" as a duration.
    // ==================================================================================
    private int duration; //SeekBar

    // ==================================================================================
    // FIELD 6: rating
    // ==================================================================================
    // TYPE: float — the rating in half stars (0.0, 0.5, ..., 5.0).
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
    // WHAT BREAKS IF IT IS WRONG: if it were `String` ("Yes"/"No"), every `if` would require
    //   a text comparison, and a lowercase "yes" would be treated as false.
    // WATCH THE LABEL: in `activity_item.xml`, the switch is labelled
    //   "Recommended", not "Watched". The field says `watched`, the screen says something else.
    // ==================================================================================
    private boolean watched; //Switch

    // ==================================================================================
    // FIELD 8: approval
    // ==================================================================================
    // TYPE: String — the TEXT of the selected radio button ("G", "PG", "PG-13", "R", "NC-17").
    // SOURCE WIDGET: `rgApproval` (RadioGroup) -> a loop over the children + `getText()`.
    // WHY text and not the resource ID: the text survives reordering the buttons
    //   in XML. The ID is a value generated in `R.java`, stable only as long as
    //   you do not rename the button.
    // RISK: if you change the label in XML ("PG13" instead of "PG-13"), `fillForm`
    //   no longer finds the match and checks nothing — silently, with no error.
    // WHAT BREAKS IF IT IS WRONG: if you stored the index (0..4), adding a button
    //   at the start of the group would change the meaning of all saved values.
    // ==================================================================================
    private String approval; //RadioGroup | RadioButtons

    // ==================================================================================
    // FIELD 9: onWatchlist
    // ==================================================================================
    // TYPE: boolean — true/false.
    // SOURCE WIDGET: `cbonWatchList` (MaterialCheckBox) -> `isChecked()`.
    // WHY boolean: a checkbox is binary. Independent of `watched`: you can have seen
    //   the movie AND keep it on the rewatch list.
    // WHAT BREAKS IF IT IS WRONG: if you reused `watched` for both, the two
    //   controls would contradict each other on save — the last one read would win.
    // ==================================================================================
    private boolean onWatchlist; //Checkbox

    // ==================================================================================
    // FIELD 10: posterUrl — THE ONLY NAME DIFFERENCE FROM G1097
    // ==================================================================================
    // TYPE: String — a URL (or any text) pointing to the movie poster.
    // SOURCE WIDGET: `etPoster` (inputType=textUri) -> `getText().toString()`.
    // WHY `posterUrl` and not `poster`: the name says the value is a LINK, not a
    //   loaded image. In G1097 the field is called `poster` and has an explicit `= null`.
    // WHY NO `= null`: in Java an object field is `null` implicitly anyway. G1096
    //   does not write the initializer; the behaviour is identical, only the documentation differs.
    // RISK: `null` reaches `etPoster.setText(null)` on edit — legal, but it displays
    //   an empty field. If you called `.length()` on it, you would get `NullPointerException`.
    // NOTE: the app NEVER loads the image. There is no `ImageView`,
    //   no network library, no `INTERNET` permission in the manifest.
    //   The URL is stored as text and that is all.
    // ==================================================================================
    private String posterUrl;

    // ==================================================================================
    // THE NO-ARGUMENT CONSTRUCTOR — THE FIRST IN THE CLASS IN G1096
    // ==================================================================================
    // WHY IT MUST BE WRITTEN EXPLICITLY:
    //   The Java rule: if you define ANY constructor with parameters, the compiler
    //   no longer generates the no-argument constructor automatically.
    //   `saveMovie()` in ItemActivity does `Movie movie = new Movie();`.
    //   Without this constructor, the file DOES NOT COMPILE.
    //
    // WHY THE BODY IS EMPTY: all the fields get their default values
    //   (null / 0 / 0.0 / false). The object is filled immediately with setters.
    //
    // WHY IT IS FIRST HERE: the position does not matter to the compiler. In G1096 it sits
    //   immediately after the fields, so the reader sees first "the empty object", then
    //   "the full object". In G1097 the same constructor is the LAST method of the file.
    //   Both variants are correct; it is a style decision, not a rule.
    // ==================================================================================
    public Movie() {
    }

    // ==================================================================================
    // THE 10-PARAMETER CONSTRUCTOR
    // ==================================================================================
    // WHY IT EXISTS: `addSampleMovies()` in MainActivity creates the 3 demo movies
    //   in a single expression. Without it, there would be 10 setter calls per movie.
    //
    // THE TRAP: the order of the parameters is POSITIONAL. The names in the signature do not help
    //   at the call — the compiler looks only at types and position.
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
    //   the code COMPILES. The movie is created. Only that `title` gets the genre and
    //   `genre` gets the title. The compiler cannot help you here.
    //
    // FORMATTING: in G1096 the parameter list is broken over two lines, the second aligned
    //   under the first parameter. In G1097 it is on a single line. Semantically: identical.
    // ==================================================================================
    public Movie(String title, Date release, String genre, double budget, int duration, float rating, boolean watched, String approval, boolean onWatchlist,
                 String posterUrl) {

        // `this.title` = the field of the class; `title` = the parameter.
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
        this.posterUrl = posterUrl;
    }

    // ==================================================================================
    // toString() — THE DISPLAY CONTRACT IN THE LIST
    // ==================================================================================
    // WHY IT IS MANDATORY HERE:
    //   `ArrayAdapter` with `android.R.layout.simple_list_item_1` receives a `TextView`
    //   and fills it with `item.toString()`. Without the override, `Object.toString()` would
    //   return `ro.ase.ie.g1096.model.Movie@a1b2c3d` — the memory address.
    //
    // THE EXACT FLOW:
    //   ListView.getView(position) -> ArrayAdapter.getView(...) -> TextView.setText(
    //   movies.get(position).toString())
    //
    // WHY `@Override`: the compiler checks the signature. If you write `tostring()`
    //   with a lowercase letter, without the annotation you get no error — only your method
    //   is never called.
    //
    // THE DIFFERENCE FROM G1097: here `posterUrl` does NOT appear in the returned text.
    //   G1097 ends with `", poster='" + poster + '\''`. Here it ends with
    //   `onWatchlist`. Adding a field to the class does not add it here automatically.
    //
    // THE FORMAT: generated by IntelliJ with Alt+Insert -> toString(). `'\''` is an
    //   escaped apostrophe, so that it does not close the character literal.
    //
    // WHAT YOU SEE ON SCREEN: nine fields, on a single row. Ugly, but correct.
    //   It is fixed in the next lab with RecyclerView and a row layout.
    // ==================================================================================
    @Override
    public String toString() {

        // String concatenation with `+`. Each `+` creates a new object in memory.
        // For 9 fields, the difference is negligible; for large loops you would use
        // `StringBuilder`.
        return "Movie{" +

                // The text fields are wrapped in apostrophes, so that the boundaries of the
                // value are visible: `title='Inception'`. Without them, `title=''` and `genre=''`
                // would be indistinguishable from an empty field.
                "title='" + title + '\'' +

                // `Date` is not wrapped in apostrophes: its `toString()` already produces
                // a readable format (`Sat Jul 16 00:00:00 GMT+03:00 2010`).
                ", release=" + release +

                // The comma at the beginning, not at the end: the last field does not need
                // a separator, and `}` joins correctly.
                ", genre='" + genre + '\'' +
                ", budget=" + budget +
                ", duration=" + duration +
                ", rating=" + rating +
                ", watched=" + watched +
                ", approval='" + approval + '\'' +

                // THE LAST FIELD IN THE TEXT — and the last in the class that appears here.
                // `posterUrl` is missing. See difference 29.0.4.
                ", onWatchlist=" + onWatchlist +

                // The closing brace of the object, as text.
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
    //   (Alt+Insert -> Getter and Setter). You do not write them by hand at the exam.
    //
    // THE NAMING CONVENTION:
    //   - `getX()` for objects and primitives, EXCEPT for boolean;
    //   - `isX()` for boolean — the JavaBeans convention.
    //
    // THE ORDER: the fields appear in the same order as the declarations, except for
    //   `watched`, which skips over `approval` and `onWatchlist` — its getter comes
    //   immediately after `rating`, then `approval`, then `onWatchlist`, then `posterUrl`.
    // ==================================================================================

    // GETTER: returns the reference to the String. String is immutable, so there is no
    // risk of modification from outside.
    public String getTitle() {
        return title;
    }

    // SETTER: `this.title` = the field, `title` = the parameter. The same trap as in
    // the 10-parameter constructor.
    public void setTitle(String title) {
        this.title = title;
    }

    // GETTER for Date. WATCH OUT: it returns the REFERENCE, not a copy.
    // RISK: `movie.getRelease().setTime(0)` would change the date in the object without the setter.
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

    // GETTER for the rating. `float` — the rating in half stars.
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
    //    Reflection (and the frameworks that read properties) looks for the `is` prefix.
    // 2. CONSISTENCY: `isOnWatchlist()` follows the same rule. If one were
    //    `getWatched()` and the other `isOnWatchlist()`, the code would be hard to read.
    // 3. THE CODE DEPENDS ON THE NAME: `fillForm` calls `existing.isWatched()`. If you
    //    wrote `getWatched()`, the call would not compile — and it must be changed in two places.
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

    // GETTER for the "Watchlist" checkbox. Prefix `is` — again the JavaBeans convention.
    public boolean isOnWatchlist() {
        return onWatchlist;
    }

    // SETTER for the checkbox.
    public void setOnWatchlist(boolean onWatchlist) {
        this.onWatchlist = onWatchlist;
    }

    // ==================================================================================
    // GETTER AND SETTER FOR THE POSTER — THE NAME THAT GIVES THE GROUP AWAY
    // ==================================================================================
    // Here difference 29.0.1 is visible in its most concrete form: the methods are called
    // `getPosterUrl` / `setPosterUrl`, not `getPoster` / `setPoster`.
    // `ItemActivity` in G1096 calls these two. Copying the code from
    // chapter 22 (written for G1097) produces `cannot find symbol`.
    // ==================================================================================
    public String getPosterUrl() {
        return posterUrl;
    }

    // SETTER for the poster. It accepts `null` without complaining.
    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    // The closing brace of the class. After it there is nothing else in the file:
    // no inner class, no `enum`, no constant.
}
