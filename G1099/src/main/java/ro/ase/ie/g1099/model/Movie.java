/*
 * ======================================================================================
 * FIȘIER: Movie.java — MODELUL DE DATE AL APLICAȚIEI
 * ======================================================================================
 * CE ESTE ACEST FIȘIER:
 *   O clasă POJO (Plain Old Java Object): 10 câmpuri private, un constructor fără
 *   parametri, un constructor cu 10 parametri, o suprascriere de `toString()` și
 *   20 de getter-e/setter-e. Zero logică de business, zero referințe la Android.
 *
 * DE CE EXISTĂ:
 *   Ca să transporte datele unui film dintr-un ecran în altul fără să depindă de UI.
 *   `MainActivity` ține `List<Movie>`; `ItemActivity` construiește un `Movie`.
 *
 * DE CE DEPINDE:
 *   Doar de `java.io.Serializable` și `java.util.Date`. Nimic din `android.*`.
 *   De aceea clasa poate fi testată pe JVM, fără emulator.
 *
 * CINE DEPINDE DE EL:
 *   `MainActivity` (lista, adapterul, cele 3 filme demo), `ItemActivity`
 *   (`fillForm` citește, `saveMovie` scrie), `ArrayAdapter` (apelează `toString()`).
 *
 * CE NU FACE:
 *   Nu validează nimic. Nu știe ce este un `EditText`, un `Intent` sau un `ListView`.
 *   Nu are `id`: identitatea filmului în listă este POZIȚIA, nu un câmp.
 *
 * CE S-AR RUPE FĂRĂ EL:
 *   Tot fluxul de date. `Intent.putExtra` nu ar avea ce serializa, `ArrayAdapter`
 *   nu ar avea ce afișa, iar `ItemActivity` ar trebui să trimită 10 extra-uri
 *   separate și să le recompună manual în `MainActivity`.
 * ======================================================================================
 */

// PACHETUL: `model` — stratul de date, separat de `activities`.
// DE CE CONTEAZĂ: `ItemActivity` importă `ro.ase.ie.g1099.model.Movie`, niciodată invers.
//   Modelul nu știe cine îl folosește. Este singura regulă de arhitectură din aplicație.
package ro.ase.ie.g1099.model;

// ======================================================================================
// IMPORT 1: java.io.Serializable
// ======================================================================================
// CE ESTE: o interfață MARKER — nu are nicio metodă de implementat.
// DE CE AICI: `Intent.putExtra(String, Serializable)` este cea mai simplă cale de a
//   trimite un obiect întreg către altă activitate. Fără `implements Serializable`,
//   apelul `intent.putExtra(MOVIE_EXTRA, movie)` nu găsește nicio suprascriere
//   potrivită și fișierul NU COMPILEAZĂ.
// CE SE ÎNTÂMPLĂ LA RUNTIME: JVM-ul serializează graful de obiecte în bytes, îl pune
//   în `Bundle`, iar activitatea destinatară primește o COPIE deserializată.
// RISC: fără `serialVersionUID` declarat, JVM-ul calculează unul din structura clasei.
//   Dacă adaugi un câmp între scriere și citire (procese diferite), primești
//   `InvalidClassException`. Aici scrierea și citirea se întâmplă în ACELAȘI proces,
//   la câteva milisecunde distanță, deci riscul este teoretic.
// ======================================================================================
import java.io.Serializable;

// ======================================================================================
// IMPORT 2: java.util.Date
// ======================================================================================
// CE ESTE: un moment în timp, stocat intern ca număr de milisecunde din 1 ianuarie 1970.
// DE CE AICI: data de lansare trebuie să fie o DATĂ, nu un text. Ca `Date` poți
//   sorta, compara și formata cu `SimpleDateFormat`.
// DE CE NU `String`: ai stoca „16/07/2010" și ai pierde capacitatea de a calcula.
//   `fillForm` ar trebui să parseze la fiecare afișare, iar `saveMovie` ar trebui
//   să formateze la fiecare salvare — două locuri în care poți greși formatul.
// DE CE NU `java.time.LocalDate`: este mai nou și mai curat, dar `Date` este
//   `Serializable` din fabrică și este tipul folosit în tot proiectul.
// RISC: `Date` este MUTABIL. `getRelease()` întoarce referința internă, nu o copie,
//   deci un apelant neatent poate modifica data din obiect fără să treacă prin setter.
// ======================================================================================
import java.util.Date;

/*
 * ======================================================================================
 * BLOC DE TEORIE: DE CE UN MODEL SEPARAT (POJO)
 * ======================================================================================
 *
 * 1. **Single Responsibility** — clasa are un singur motiv să se schimbe: se modifică
 *    structura datelor unui film. Nu se schimbă când se schimbă un layout, un buton
 *    sau o culoare. Dacă ai pune `getText().toString()` aici, clasa ar depinde de
 *    `android.widget.EditText` și ar avea două motive să se schimbe.
 *
 * 2. **Serializability** — fiind `Serializable` și fără referințe către `Context`,
 *    obiectul poate traversa granița dintre două activități. Un `Movie` care ar ține
 *    un `EditText` nu ar putea fi serializat: `View` nu este `Serializable`.
 *
 * 3. **Testability** — poți scrie un test JVM simplu care creează un `Movie` și
 *    verifică `getTitle()`. Fără emulator, fără Android. Asta se întâmplă pentru că
 *    în fișier nu există niciun `import android.*`.
 *
 * 4. **Fără câmp `id`** — identitatea filmului în listă este POZIȚIA, nu un ID.
 *    De aici vine tot mecanismul `POSITION_EXTRA` din celelalte două fișiere.
 *    Este o simplificare didactică: o bază de date reală ar avea un `id` stabil.
 *
 * 5. **Zece câmpuri, nu nouă** — posterul este al zecelea. El nu apare în `toString()`
 *    (vezi 31.0.4), dar există în model, are getter și setter, și este citit de
 *    `ItemActivity` la editare. Un câmp poate exista fără să fie afișat.
 *
 * VIZUAL: obiectul care traversează granița dintre activități
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
 *                          (obiect NOU, adresă nouă)
 *
 *   Movie@A rămâne în lista din MainActivity. Modificarea lui Movie@A' nu se vede
 *   în listă. De aceea MainActivity face `movies.set(position, movie)` la final.
 * ======================================================================================
 */
public class Movie implements Serializable {

    // ==================================================================================
    // CÂMPUL 1: title
    // ==================================================================================
    // TIP: String — text liber, de lungime arbitrară.
    // WIDGET SURSĂ: `etTitle` (TextInputEditText) -> `getText().toString()`.
    // DE CE String: titlul este text, nu are operații numerice sau de dată.
    // CE SE RUPE DACĂ E GREȘIT: dacă ar fi `int`, „Inception" nu ar încăpea;
    //   dacă ar fi `char[]`, `ArrayAdapter` ar afișa adresa array-ului.
    // ==================================================================================
    private String title; //EditText

    // ==================================================================================
    // CÂMPUL 2: release
    // ==================================================================================
    // TIP: Date — un moment în timp (aici, doar ziua contează).
    // WIDGET SURSĂ: `etRelease` + `DatePickerDialog`; în model ajunge din
    //   `releaseCal.getTime()`, NU din textul din EditText.
    // DE CE Date: permite formatare cu `SimpleDateFormat` și comparare.
    // CE SE RUPE DACĂ E GREȘIT: dacă ar fi `String`, `fillForm` ar trebui să parseze
    //   la fiecare afișare, iar un text tastat greșit ar ajunge în model ca atare.
    // ==================================================================================
    private Date release; //EditText date

    // ==================================================================================
    // CÂMPUL 3: genre
    // ==================================================================================
    // TIP: String — textul selectat din listă („Sci-Fi", „Animation", „Action").
    // WIDGET SURSĂ: `spGenre` (Spinner) -> `getSelectedItem().toString()`.
    // DE CE String și nu `enum`: genurile vin din `@array/movie_genre`, adică din XML.
    //   Cu `enum`, adăugarea unui gen ar cere modificarea codului Java.
    // CE SE RUPE DACĂ E GREȘIT: dacă ai stoca poziția (`int`), schimbarea ordinii
    //   din `arrays.xml` ar schimba retroactiv genul tuturor filmelor salvate.
    // ==================================================================================
    private String genre; //Spinner

    // ==================================================================================
    // CÂMPUL 4: budget
    // ==================================================================================
    // TIP: double — număr cu zecimale, precizie de ~15–16 cifre semnificative.
    // WIDGET SURSĂ: `etBudget` (inputType=numberDecimal) -> `Double.parseDouble(...)`.
    // DE CE double și nu float: `float` are ~7 cifre semnificative. Pentru 160 000 000
    //   ambele sunt exacte, dar 160 000 001 nu ar fi reprezentabil exact în `float`.
    //   În plus, `Double.parseDouble` întoarce `double` — un câmp `float` ar cere cast.
    // CE SE RUPE DACĂ E GREȘIT: dacă ar fi `int`, zecimalele ar fi trunchiate;
    //   dacă ar fi `String`, nu ai putea compara bugete.
    // ==================================================================================
    private double budget; //EditText number

    // ==================================================================================
    // CÂMPUL 5: duration
    // ==================================================================================
    // TIP: int — număr întreg de minute, 0..300.
    // WIDGET SURSĂ: `sbDuration` (SeekBar) -> `getProgress()`.
    // DE CE int: `SeekBar.getProgress()` întoarce `int`. Orice alt tip ar cere cast.
    // CE SE RUPE DACĂ E GREȘIT: dacă ar fi `String`, `setProgress` ar cere parsare
    //   și ai putea salva „abc" ca durată.
    // ==================================================================================
    private int duration; //SeekBar

    // ==================================================================================
    // CÂMPUL 6: rating
    // ==================================================================================
    // TIP: float — nota cu jumătăți de stea (0.0, 0.5, ..., 5.0).
    // WIDGET SURSĂ: `rbRating` (RatingBar) -> `getRating()`.
    // DE CE float: `RatingBar.getRating()` și `setRating(float)` lucrează cu `float`.
    //   Un câmp `double` ar face `setRating(existing.getRating())` să NU compileze
    //   fără cast explicit.
    // ATENȚIE: tipul este `float`, nu `android.media.Rating`. Vezi 31.0.5 — importul
    //   acela este mort, iar widget-ul este `RatingBar`.
    // CE SE RUPE DACĂ E GREȘIT: dacă ar fi `int`, ai pierde jumătățile de stea;
    //   dacă ar fi `String` („4.5"), comparațiile ar fi lexicografice.
    // ==================================================================================
    private float rating; //RatingBar

    // ==================================================================================
    // CÂMPUL 7: watched
    // ==================================================================================
    // TIP: boolean — true/false.
    // WIDGET SURSĂ: `swWatched` (MaterialSwitch) -> `isChecked()`.
    // DE CE boolean: starea unui switch este binară. `isChecked()` întoarce exact asta.
    // CE SE RUPE DACĂ E GREȘIT: dacă ar fi `String` („Yes"/„No"), orice `if` ar cere
    //   comparație de text, iar „yes" minuscul ar fi tratat ca false.
    // OBSERVAȚIE: comentariul din sursă spune „Switch", dar tipul real al widget-ului
    //   este `MaterialSwitch`. Comentariile nu se actualizează singure.
    // ==================================================================================
    private boolean watched; //Switch

    // ==================================================================================
    // CÂMPUL 8: approval
    // ==================================================================================
    // TIP: String — TEXTUL butonului radio selectat („G", „PG", „PG-13", „R", „NC-17").
    // WIDGET SURSĂ: `rgApproval` (RadioGroup) -> buclă peste copii + `getText()`.
    // DE CE text și nu ID-ul resursei: textul supraviețuiește reordonării butoanelor
    //   în XML. ID-ul este o valoare generată în `R.java`, stabilă doar cât timp
    //   nu redenumești butonul.
    // RISC: dacă schimbi eticheta în XML („PG13" în loc de „PG-13"), `fillForm`
    //   nu mai găsește potrivirea și nu bifează nimic — silențios.
    // CE SE RUPE DACĂ E GREȘIT: dacă ai stoca indexul (0..4), adăugarea unui buton
    //   la începutul grupului ar schimba sensul tuturor valorilor salvate.
    // ==================================================================================
    private String approval; //RadioGroup | RadioButtons

    // ==================================================================================
    // CÂMPUL 9: onWatchlist
    // ==================================================================================
    // TIP: boolean — true/false.
    // WIDGET SURSĂ: `cbonWatchList` (MaterialCheckBox) -> `isChecked()`.
    // DE CE boolean: o bifă este binară. Independentă de `watched`: poți să fi văzut
    //   filmul ȘI să îl ții pe lista de revăzut.
    // CE SE RUPE DACĂ E GREȘIT: dacă ai refolosi `watched` pentru amândouă, cele două
    //   controale s-ar contrazice la salvare.
    // ==================================================================================
    private boolean onWatchlist; //Checkbox

    // ==================================================================================
    // CÂMPUL 10: posterUrl
    // ==================================================================================
    // TIP: String — un URL (sau orice text) către afișul filmului.
    // WIDGET SURSĂ: `etPoster` (inputType=textUri) -> `getText().toString()`.
    // DE CE NU are `= null` explicit (diferența față de G1097): `String` este oricum
    //   `null` implicit. Absența inițializatorului nu schimbă comportamentul, doar
    //   nota de documentație. Vezi 31.0.2.
    // DE CE se numește `posterUrl` și nu `poster`: sufixul declară că valoarea este
    //   un LINK, nu o imagine. Getter-ul se numește, în consecință, `getPosterUrl()`.
    // RISC: `null` ajunge în `etPoster.setText(null)` la editare — legal, dar afișează
    //   câmp gol. Dacă ai apela `.length()` pe el, ai lua `NullPointerException`.
    // ==================================================================================
    private String posterUrl;

    // ==================================================================================
    // CONSTRUCTORUL FĂRĂ PARAMETRI — PRIMUL ÎN FIȘIER
    // ==================================================================================
    // DE CE ESTE OBLIGATORIU SĂ FIE SCRIS EXPLICIT:
    //   Regula Java: dacă definești ORICE constructor cu parametri, compilatorul
    //   NU mai generează automat constructorul fără parametri.
    //   `saveMovie()` din ItemActivity face `Movie movie = new Movie();`.
    //   Fără acest constructor, fișierul NU COMPILEAZĂ.
    //
    // DE CE CORPUL ESTE GOL: toate câmpurile primesc valorile implicite
    //   (null / 0 / 0.0 / false). Obiectul este umplut imediat cu setter-e.
    //
    // DE CE ESTE PRIMUL (diferența față de G1097, unde este ultimul):
    //   poziția nu contează pentru compilator, dar aici cititorul vede imediat că
    //   `new Movie()` este legal. Vezi 31.0.3.
    // ==================================================================================
    public Movie() {
    }

    // ==================================================================================
    // CONSTRUCTORUL CU 10 PARAMETRI
    // ==================================================================================
    // DE CE EXISTĂ: `addSampleMovies()` din MainActivity creează cele 3 filme demo
    //   într-o singură expresie. Fără el, ar fi 10 apeluri de setter pe fiecare film.
    //
    // CAPCANA: ordinea parametrilor este POZIȚIONALĂ. Numele din semnătură nu ajută
    //   la apel — compilatorul se uită doar la tipuri și la poziție.
    //
    //   POZIȚIA   TIP       NUME           WIDGET CARE PRODUCE VALOAREA
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
    //   Dacă inversezi pozițiile 1 și 3 (ambele String) sau 7 și 9 (ambele boolean),
    //   codul COMPILEAZĂ. Filmul se creează. Doar că `title` primește genul și
    //   `genre` primește titlul. Compilatorul nu te poate ajuta aici.
    // ==================================================================================
    public Movie(String title, Date release, String genre, double budget, int duration, float rating, boolean watched, String approval, boolean onWatchlist, String posterUrl) {

        // `this.title` = câmpul clasei; `title` = parametrul.
        // Fără `this.`, linia ar fi `title = title` — auto-atribuire, câmpul rămâne null.
        this.title = title;

        // Același model pentru fiecare dintre cele 10 câmpuri. Toate cele 10 linii
        // sunt identice structural; dacă una lipsește, câmpul rămâne cu valoarea
        // implicită (null / 0 / 0.0 / false) și bug-ul apare abia la afișare.
        this.release = release;
        this.genre = genre;
        this.budget = budget;
        this.duration = duration;
        this.rating = rating;
        this.watched = watched;
        this.approval = approval;
        this.onWatchlist = onWatchlist;

        // Ultimul câmp: aici se vede și diferența de nume față de G1097.
        // `this.posterUrl` este câmpul; `posterUrl` este parametrul. Ambele au același nume.
        this.posterUrl = posterUrl;
    }

    // ==================================================================================
    // toString() — CONTRACTUL DE AFIȘARE ÎN LISTĂ
    // ==================================================================================
    // DE CE ESTE OBLIGATORIU AICI:
    //   `ArrayAdapter` cu `android.R.layout.simple_list_item_1` primește un `TextView`
    //   și îl umple cu `item.toString()`. Fără suprascriere, `Object.toString()` ar
    //   întoarce `ro.ase.ie.g1099.model.Movie@a1b2c3d` — adresa din memorie.
    //
    // FLUXUL EXACT:
    //   ListView.getView(position) -> ArrayAdapter.getView(...) -> TextView.setText(
    //   movies.get(position).toString())
    //
    // DE CE `@Override`: compilatorul verifică semnătura. Dacă scrii `tostring()`
    //   cu literă mică, fără adnotare nu primești nicio eroare — doar metoda ta
    //   nu este apelată niciodată.
    //
    // DIFERENȚA FAȚĂ DE G1097: posterul NU apare aici. Nouă câmpuri, nu zece.
    //   Vezi 31.0.4 — un câmp poate exista în model fără să fie afișat.
    // ==================================================================================
    @Override
    public String toString() {

        // Concatenare de String-uri cu `+`. Fiecare `+` creează un obiect nou în memorie.
        // Pentru 9 câmpuri, diferența este neglijabilă; pentru bucle mari s-ar folosi
        // `StringBuilder`.
        return "Movie{" +

                // Câmpurile text sunt încadrate în apostrofuri, ca să se vadă limitele
                // valorii: `title='Inception'`. Fără ele, `title=''` și `genre=''`
                // ar fi indistinguibile de un câmp gol.
                "title='" + title + '\'' +

                // `Date` nu este încadrat în apostrofuri: `toString()`-ul lui produce
                // deja un format lizibil (`Sat Jul 16 00:00:00 GMT+03:00 2010`).
                ", release=" + release +

                // Virgula de la început, nu de la sfârșit: ultimul câmp nu are nevoie
                // de separator, iar `}` se lipește corect.
                ", genre='" + genre + '\'' +
                ", budget=" + budget +
                ", duration=" + duration +
                ", rating=" + rating +
                ", watched=" + watched +
                ", approval='" + approval + '\'' +
                ", onWatchlist=" + onWatchlist +

                // Acolada de închidere a obiectului, ca text.
                // AICI S-AR FI AFIȘAT POSTERUL, în G1097: ", poster='" + poster + '\''.
                // În G1099 lipsește, iar rândul din listă se termină după onWatchlist.
                '}';
    }

    // ==================================================================================
    // GETTERI ȘI SETTERI — 20 DE METODE, TOATE MECANICE
    // ==================================================================================
    // DE CE EXISTĂ: câmpurile sunt `private`. Codul din alte pachete (și chiar din
    //   `activities`) nu le poate citi sau scrie direct. Getter-ele și setter-ele
    //   sunt singura interfață.
    //
    // DE CE ATÂT DE MULTE: 10 câmpuri x 2 = 20. Sunt generate automat de IntelliJ
    //   (Alt+Insert -> Getter and Setter). Nu se scriu de mână la examen.
    //
    // CONVENȚIA DE NUME:
    //   - `getX()` pentru obiecte și primitive, CU EXCEPȚIA lui boolean;
    //   - `isX()` pentru boolean — convenția JavaBeans.
    // ==================================================================================

    // GETTER: întoarce referința la String. String este imutabil, deci nu există
    // risc de modificare din exterior.
    public String getTitle() {
        return title;
    }

    // SETTER: `this.title` = câmpul, `title` = parametrul. Aceeași capcană ca în
    // constructorul cu 10 parametri.
    public void setTitle(String title) {
        this.title = title;
    }

    // GETTER pentru Date. ATENȚIE: întoarce REFERINȚA, nu o copie.
    // RISC: `movie.getRelease().setTime(0)` ar modifica data din obiect fără setter.
    //   O variantă corectă ar fi `return new Date(release.getTime());`.
    public Date getRelease() {
        return release;
    }

    // SETTER pentru Date. Nu validează `null` — un `Movie` cu dată null este acceptat
    // de model și crapă abia în `fillForm`, la `dateFormat.format(null)`.
    public void setRelease(Date release) {
        this.release = release;
    }

    // GETTER pentru gen. Valoarea provine din Spinner, dar modelul nu știe asta.
    public String getGenre() {
        return genre;
    }

    // SETTER pentru gen. Nu verifică dacă valoarea există în `@array/movie_genre`.
    // Validarea este responsabilitatea UI-ului, nu a modelului.
    public void setGenre(String genre) {
        this.genre = genre;
    }

    // GETTER pentru buget. `double` se copiază prin valoare — nu există probleme
    // de aliasing ca la Date.
    public double getBudget() {
        return budget;
    }

    // SETTER pentru buget. Nu respinge valori negative: un buget -5 ar fi acceptat.
    // Validarea ar trebui făcută în `saveMovie`, înainte de apel.
    public void setBudget(double budget) {
        this.budget = budget;
    }

    // GETTER pentru durată, în minute.
    public int getDuration() {
        return duration;
    }

    // SETTER pentru durată. Nu limitează la 0..300 — limita există doar în XML
    // (`android:max="300"`), adică doar în UI.
    public void setDuration(int duration) {
        this.duration = duration;
    }

    // GETTER pentru rating. `float` — nota cu jumătăți de stea.
    public float getRating() {
        return rating;
    }

    // SETTER pentru rating. Parametrul este `float`, exact tipul cerut de
    // `RatingBar.setRating(float)`.
    public void setRating(float rating) {
        this.rating = rating;
    }

    // ==================================================================================
    // DE CE `isWatched()` ȘI NU `getWatched()`
    // ==================================================================================
    // 1. CONVENȚIA JAVABEANS: pentru `boolean`, getter-ul se numește `isX()`, nu `getX()`.
    //    Reflectarea (și framework-urile care citesc proprietăți) caută prefixul `is`.
    // 2. CONSISTENȚA: `isOnWatchlist()` respectă aceeași regulă. Dacă unul ar fi
    //    `getWatched()` și celălalt `isOnWatchlist()`, codul ar fi greu de citit.
    // 3. CODUL DEPINDE DE NUME: `fillForm` apelează `existing.isWatched()`. Dacă ai
    //    scris `getWatched()`, apelul nu compilează — și trebuie schimbat în două locuri.
    // ==================================================================================
    public boolean isWatched() {
        return watched;
    }

    // Setter-ul rămâne `setWatched` — prefixul `set` este același pentru toate tipurile.
    public void setWatched(boolean watched) {
        this.watched = watched;
    }

    // GETTER pentru ghidajul parental („PG-13" etc.).
    public String getApproval() {
        return approval;
    }

    // SETTER pentru ghidaj. Valoarea este textul butonului radio, nu ID-ul lui.
    public void setApproval(String approval) {
        this.approval = approval;
    }

    // GETTER pentru bifa „Watchlist". Prefix `is` — din nou convenția JavaBeans.
    public boolean isOnWatchlist() {
        return onWatchlist;
    }

    // SETTER pentru bifă.
    public void setOnWatchlist(boolean onWatchlist) {
        this.onWatchlist = onWatchlist;
    }

    // GETTER pentru URL-ul posterului. Poate întoarce `null`.
    // NUMELE: `getPosterUrl`, nu `getPoster` — diferența față de G1097.
    //   `ItemActivity.fillForm` apelează exact acest nume.
    public String getPosterUrl() {
        return posterUrl;
    }

    // SETTER pentru poster. Acceptă `null` fără să se plângă.
    // `ItemActivity.saveMovie` apelează exact acest nume.
    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }
}
