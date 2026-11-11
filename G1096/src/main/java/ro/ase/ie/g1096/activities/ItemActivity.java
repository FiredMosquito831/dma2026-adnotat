/*
 * ======================================================================================
 * FIȘIER: ItemActivity.java — FORMULARUL DE ADAUGĂ / EDITEZĂ
 * ======================================================================================
 * CE ESTE: un ecran cu 13 controale, care citește valorile din ele, construiește un
 *   `Movie` și îl trimite înapoi în `MainActivity` printr-un `Intent` de rezultat.
 *
 * DE CE EXISTĂ: este SINGURUL loc care știe să transforme widget-uri în model.
 *   `MainActivity` nu atinge niciun `EditText`; primește un `Movie` gata făcut.
 *
 * DE CE DEPINDE:
 *   - `activity_item.xml` — pentru cele 13 ID-uri (`etTitle` ... `btnMovieAction`).
 *   - `MainActivity` — pentru constantele `MOVIE_EXTRA` și `POSITION_EXTRA`.
 *   - `Movie` — tipul pe care îl construiește.
 *   - `IntentCompat` — pentru citirea tipată a extra-ului serializabil.
 *
 * CINE DEPINDE DE EL:
 *   `MainActivity` (îl pornește prin `launcher`), `AndroidManifest.xml` (îl declară).
 *
 * CE NU FACE:
 *   - Nu validează nimic: un buget gol produce `NumberFormatException`, nu un mesaj.
 *   - Nu persistă nimic: nu scrie în fișier, în `SharedPreferences` sau în bază de date.
 *   - Nu încarcă posterul: `etPoster` este doar text.
 *   - Nu știe ce se întâmplă cu `Movie`-ul după `finish()`.
 *
 * CE S-AR RUPE FĂRĂ EL:
 *   Aplicația ar avea listă, dar niciun mod de a adăuga sau modifica un film.
 *   FAB-ul ar porni o activitate inexistentă -> `ActivityNotFoundException`.
 * ======================================================================================
 */

// Același pachet ca `MainActivity` — de aceea poate folosi constantele ei
// fără import și fără calificare completă.
package ro.ase.ie.g1096.activities;

// ======================================================================================
// IMPORTURI — 24 DE NUME, DINTRE CARE UNUL MORT
// ======================================================================================
// android.widget.*   — cele 13 tipuri de widget-uri + interfețele lor de listener
// androidx.*         — AppCompat, EdgeToEdge, insets, IntentCompat, @NonNull
// com.google.*       — MaterialSwitch (singurul widget Material din cod)
// java.text.*        — SimpleDateFormat și excepția ei
// java.util.*        — Calendar, Date, Locale
//
// MORT: `android.widget.Switch`. Câmpul este `MaterialSwitch`, care extinde
//   `SwitchCompat`, NU `android.widget.Switch`. Vezi diferența 29.0.6.
// ======================================================================================

// DatePickerDialog: dialogul de calendar care apare la click pe câmpul de dată.
import android.app.DatePickerDialog;

// Intent: obiectul de rezultat, construit în `saveMovie()`.
import android.content.Intent;

// Bundle: doar în semnătura lui onCreate.
import android.os.Bundle;

// View: tipul parametrului din OnClickListener.
import android.view.View;

// Button: tipul câmpului `btMovieAction`.
import android.widget.Button;

// CheckBox: tipul câmpului `cbonWatchList`.
import android.widget.CheckBox;

// CompoundButton: clasa de bază pentru Switch/CheckBox; interfața de listener este aici.
import android.widget.CompoundButton;

// DatePicker: primul parametru al lui `onDateSet`.
import android.widget.DatePicker;

// EditText: tipul câmpurilor `etTitle`, `etRelease`, `etBudget`, `etPoster`.
import android.widget.EditText;

// RadioButton: tipul în care se face CAST fiecare copil al lui `rgApproval`.
import android.widget.RadioButton;

// RadioGroup: containerul care garantează selecție unică între butoanele radio.
import android.widget.RadioGroup;

// RatingBar: tipul câmpului `rbRating`.
import android.widget.RatingBar;

// SeekBar: tipul câmpului `sbDuration`.
import android.widget.SeekBar;

// Spinner: tipul câmpului `spGenre`.
import android.widget.Spinner;

// Switch: IMPORT MORT. Niciun nume `Switch` nu apare în fișier.
// DANGER! Dacă ai „repara" importul folosindu-l — `private Switch swWatched;` —
//   `findViewById(R.id.swWatched)` ar întoarce un `MaterialSwitch`, care NU este
//   subclasă a lui `android.widget.Switch`, și ai lua `ClassCastException` la runtime.
import android.widget.Switch;

// TextView: tipul câmpurilor `tvDurationValue` și `tvWatchedValue`.
import android.widget.TextView;

// EdgeToEdge: desenare sub barele de sistem.
import androidx.activity.EdgeToEdge;

// NonNull: adnotare pe parametrul lui `onCheckedChanged`. Doar documentație pentru
//   compilator și pentru lint; nu schimbă bytecode-ul.
import androidx.annotation.NonNull;

// AppCompatActivity: clasa de bază.
import androidx.appcompat.app.AppCompatActivity;

// IntentCompat: variantă compatibilă a citirii extra-urilor. `getSerializableExtra(
//   Intent, String, Class<T>)` este varianta tipată, care nu mai aruncă
//   ClassCastException și funcționează pe toate API-urile.
import androidx.core.content.IntentCompat;

// Insets / ViewCompat / WindowInsetsCompat: aceleași trei clase ca în MainActivity,
// pentru același listener de padding.
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// MaterialSwitch: widget-ul Material pentru comutator. Extinde SwitchCompat,
// care extinde CompoundButton — de aceea listener-ul este `CompoundButton.OnCheckedChangeListener`.
import com.google.android.material.materialswitch.MaterialSwitch;

// ParseException: excepția aruncată de `SimpleDateFormat.parse` pe text invalid.
import java.text.ParseException;

// SimpleDateFormat: formatează și parsează „dd/MM/yyyy".
import java.text.SimpleDateFormat;

// Calendar: tipul lui `releaseCal` și constantele YEAR / MONTH / DAY_OF_MONTH.
import java.util.Calendar;

// Date: tipul returnat de `dateFormat.parse(...)`.
import java.util.Date;

// Locale: formatarea depinde de localizare (nume de luni, separatori).
import java.util.Locale;

// Clasa R — pentru cele 13 ID-uri.
import ro.ase.ie.g1096.R;

// Modelul construit de formular.
import ro.ase.ie.g1096.model.Movie;

/*
 * ======================================================================================
 * BLOC DE TEORIE: FORMULARUL CA FABRICĂ ȘI CELE DOUĂ MODURI
 * ======================================================================================
 *
 * 1. UN SINGUR ECRAN, DOUĂ MODURI
 *    Nu există `AddActivity` și `EditActivity`. Există un `ItemActivity` care se
 *    comportă diferit în funcție de ce primește în `Intent`:
 *
 *      Intent FĂRĂ MOVIE_EXTRA   -> mod INSERT: formular gol, buton „Save Movie"
 *      Intent CU    MOVIE_EXTRA  -> mod EDIT:   formular plin, buton „Update Movie"
 *
 *    Discriminantul este `existing != null`, nu un flag boolean separat. Mai puțin
 *    cod, mai puține locuri în care flag-ul poate fi greșit.
 *
 * 2. DIRECȚIA DATELOR
 *    MainActivity --(Intent cu Movie)--> ItemActivity --(formular)--> Movie nou
 *    ItemActivity --(setResult cu Movie nou)--> MainActivity
 *    Datele curg într-un singur sens, o singură dată. Nu există observatori,
 *    nu există callback-uri intermediare.
 *
 * 3. `Intent`-UL DE REZULTAT NU ESTE PORNIT NICĂIODATĂ
 *    `new Intent()` fără target nu descrie o navigație. Este doar un container
 *    pentru `putExtra`, pe care `setResult` îl trimite înapoi apelantului.
 *    Confuzia clasică: „de ce nu are `IntentActivity.class`?" — pentru că nu
 *    pornește nimic.
 *
 * 4. CINE ESTE SURSA DE ADEVĂR PENTRU DATĂ
 *    NU textul din `etRelease`. Ci `releaseCal`, un `Calendar`. Textul este doar
 *    o reprezentare; calendarul este valoarea. `saveMovie()` citește din calendar.
 *    Consecința: dacă uiți să sincronizezi calendarul cu textul, salvezi altă dată.
 *    Aici EXISTĂ această uitare — vezi BUG 2.
 *
 * 5. CICLUL DE VIAȚĂ AL UNUI FORMULAR
 *    onCreate -> (utilizatorul completează) -> saveMovie -> setResult -> finish
 *    -> onPause -> onStop -> onDestroy. Nu există salvare automată, nu există
 *    „draft". Dacă apeși Back, tot ce ai tastat se pierde, fără avertisment.
 * ======================================================================================
 */

// Aceeași clasă de bază ca MainActivity.
public class ItemActivity extends AppCompatActivity {

    // ==================================================================================
    // CELE 13 CÂMPURI DE WIDGET — ORDINEA URMEAZĂ FORMULARUL, NU XML-UL
    // ==================================================================================
    // Sunt declarate în ordinea în care apar pe ecran: titlu, dată, gen, buget,
    // durată, rating, ghidaj, watchlist, switch, poster, buton, două etichete.
    // Tipul fiecăruia este exact clasa din XML — `EditText`, nu `TextInputEditText`,
    // pentru că `TextInputEditText` este subclasă a lui `EditText`.
    // ==================================================================================

    // Titlul filmului. `TextInputEditText` din XML este un `EditText`, deci merge.
    private EditText etTitle;

    // Data lansării. NU se tastează: click-ul deschide `DatePickerDialog`.
    private EditText etRelease;

    // Genul, ales din `@array/movie_genre`.
    private Spinner spGenre;

    // Bugetul, tastat pe tastatură numerică cu zecimale.
    private EditText etBudget;

    // Durata, aleasă cu slider-ul 0..300.
    private SeekBar sbDuration;

    // Nota, 0..5 stele cu pas de 0.5.
    private RatingBar rbRating;

    // Grupul de butoane radio pentru ghidajul parental.
    private RadioGroup rgApproval;

    // Bifa „Watchlist". Numele câmpului are un `b` în plus față de ID: `cbonWatchList`.
    private CheckBox cbonWatchList;

    // Comutatorul „Recommended". TIPUL ESTE MATERIAL, nu `android.widget.Switch`.
    private MaterialSwitch swWatched;

    // URL-ul posterului, tastat cu tastatură de tip URI.
    private EditText etPoster;

    // Butonul de salvare. Textul lui se schimbă în modul edit.
    private Button btMovieAction;

    // Eticheta „148 min." de lângă slider — se actualizează la fiecare mișcare.
    private TextView tvDurationValue;

    // Eticheta „Yes"/„No" de lângă switch — se actualizează la fiecare comutare.
    private TextView tvWatchedValue;

    // ==================================================================================
    // STARE INTERNĂ — TREI CÂMPURI CARE NU SUNT WIDGET-URI
    // ==================================================================================
    // `position` = modul. -1 înseamnă insert; orice altă valoare înseamnă edit.
    //   Este SINGURUL flag de mod din clasă. Nu există `boolean isEdit`.
    //
    // `releaseCal` = sursa de adevăr pentru dată. Inițializat cu „acum", ca dialogul
    //   să pornească de undeva chiar dacă utilizatorul nu a ales nimic.
    //
    // `dateFormat` = formatatorul comun pentru afișare și parsare. `final` pentru că
    //   referința nu se schimbă; `SimpleDateFormat` este mutabilă intern, dar aici
    //   este folosită doar pe thread-ul principal.
    // ==================================================================================
    private int position = -1;   // -1 = insert mode
    private Calendar releaseCal = Calendar.getInstance();
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    // ==================================================================================
    // onCreate — CONSTRUIEȘTE FORMULARUL, APOI ÎL UMPLE DACĂ E CAZUL
    // ==================================================================================
    // CE FACE: umflă layout-ul, leagă cele 13 view-uri, atașează cei 4 listeneri,
    //   apoi citește `Intent`-ul și decide dacă formularul pornește gol sau plin.
    // CUM: 1. super 2. EdgeToEdge 3. setContentView 4. insets 5. initControls
    //   6. initListeners 7. getIntent 8. citire extra 9. dacă există, fillForm + texte.
    // RISC: ordinea 5-6-9 este esențială. `fillForm` apelează `setChecked` și
    //   `setProgress`, care declanșează listenerii. Dacă listenerii nu ar fi atașați
    //   înainte, etichetele `tvWatchedValue` și `tvDurationValue` ar rămâne pe
    //   valorile din XML („No", „0 min").
    // REF: aceeași ordine ca în MainActivity.onCreate, plus pasul de citire a Intent-ului.
    // ==================================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // 1. Lanțul clasei de bază. Obligatoriu primul.
        super.onCreate(savedInstanceState);

        // 2. Desenare sub barele de sistem.
        EdgeToEdge.enable(this);

        // 3. Umflă `activity_item.xml`. Din acest moment `findViewById` găsește view-urile.
        setContentView(R.layout.activity_item);

        // 4. Padding pentru barele de sistem, pe rădăcina `R.id.main` (ScrollView-ul).
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 5. Leagă cele 13 câmpuri de view-uri.
        initControls();

        // 6. Atașează cei 4 listeneri. TREBUIE înainte de `fillForm`, pentru că
        //    `fillForm` va declanșa `onCheckedChanged` și `onProgressChanged`.
        initListeners();

        // 7. Ia Intent-ul care a pornit activitatea.
        //    - La insert: Intent gol, construit de FAB.
        //    - La edit: Intent cu MOVIE_EXTRA și POSITION_EXTRA, construit de click-ul pe rând.
        Intent intent = getIntent();

        // 8. Citește obiectul serializat, în varianta TIPATĂ.
        //    `IntentCompat.getSerializableExtra(intent, key, Movie.class)` întoarce
        //    direct `Movie` sau `null`, fără cast manual și fără metoda deprecată.
        //    ATENȚIE: obiectul primit este O COPIE deserializată, nu originalul din listă.
        Movie existing = IntentCompat.getSerializableExtra(intent, MainActivity.MOVIE_EXTRA, Movie.class);

        // 9. Discriminantul de mod: există obiect sau nu.
        if (existing != null) {

            // 9a. Reține poziția pentru `saveMovie`. Dacă lipsește, rămâne -1 și
            //     `saveMovie` ar trimite un film nou în loc să înlocuiască.
            position = intent.getIntExtra(MainActivity.POSITION_EXTRA, -1);

            // 9b. Umple cele 13 controale cu valorile filmului.
            fillForm(existing);

            // 9c. `setTitle` este metoda ACTIVITĂȚII (schimbă titlul din ActionBar),
            //     NU `Movie.setTitle`. Aceeași nume, altă clasă — capcană de citire.
            setTitle("Edit movie");

            // 9d. Schimbă textul butonului. Utilizatorul vede că salvează o modificare.
            btMovieAction.setText("Update Movie");
        }
        // Dacă `existing` este null, nu se face nimic: formularul rămâne gol,
        // butonul rămâne „Save Movie" din XML, iar `position` rămâne -1.
    }

    // ==================================================================================
    // fillForm — MODEL -> WIDGET-URI
    // ==================================================================================
    // CE FACE: copiază cele 10 valori ale unui `Movie` în cele 13 controale.
    // CUM: 8 setter-e directe, apoi 2 bucle — una pe Spinner (după TEXT), una pe
    //   RadioGroup (după TEXT).
    // RISC: cele două bucle eșuează SILENȚIOS dacă textul nu se potrivește. Spinner-ul
    //   rămâne pe prima poziție, grupul radio rămâne pe butonul bifat implicit.
    // REF: perechea acestei metode este `saveMovie()` — acolo merge invers, widget -> model.
    // ==================================================================================
    private void fillForm(Movie existing) {

        // Text simplu. `setText` acceptă `null` și afișează câmp gol.
        etTitle.setText(existing.getTitle());

        // Data: `Date` -> text cu `SimpleDateFormat`. ATENȚIE: această linie NU
        // actualizează `releaseCal`. Vezi BUG 2 mai jos.
        etRelease.setText(dateFormat.format(existing.getRelease()));

        // Buget: `double` -> text. `String.valueOf(double)` produce „1.6E8" pentru
        // 160 000 000, nu „160000000". Vezi BUG 3.
        etBudget.setText(String.valueOf(existing.getBudget()));

        // Durata: `int` -> progres pe slider. Valoarea este limitată automat la 0..300.
        // Această linie DECLANȘEAZĂ `onProgressChanged`, care actualizează eticheta.
        sbDuration.setProgress(existing.getDuration());

        // Bifa: `boolean` -> `setChecked`. Declanșează listener-ul doar dacă valoarea
        // se SCHIMBĂ față de starea curentă.
        cbonWatchList.setChecked(existing.isOnWatchlist());

        // Switch-ul: `boolean` -> `setChecked`. Declanșează `onCheckedChanged`,
        // care scrie „Yes"/„No" în `tvWatchedValue`.
        swWatched.setChecked(existing.isWatched());

        // Nota: `float` -> `setRating(float)`. Tipurile se potrivesc exact.
        rbRating.setRating(existing.getRating());

        // Posterul: `String` -> text. Poate fi `null`; `setText(null)` este legal.
        etPoster.setText(existing.getPosterUrl());

        // BUCLA 1 — Spinner-ul nu are „setValue(String)". Are doar „setSelection(index)".
        // Deci căutăm indexul după text, comparând fiecare element cu genul filmului.
        for(int index=0; index<spGenre.getCount(); index++)
        {
            // `getItemAtPosition` întoarce elementul din `@array/movie_genre`.
            // `.equals` pe obiectul din adapter; dacă ar fi null, ar arunca NPE.
            if(spGenre.getItemAtPosition(index).equals(existing.getGenre()))
            {
                // Am găsit genul — selectează-l.
                spGenre.setSelection(index);

                // Ieși din buclă: prima potrivire este suficientă.
                break;
            }
            // Dacă nu se găsește nicio potrivire, spinner-ul rămâne pe poziția 0.
            // RISC: niciun mesaj, nicio eroare. Doar genul afișat este altul.
        }

        // BUCLA 2 — RadioGroup-ul nu are „select by text" nici el.
        // Iterăm peste COPIII grupului, nu peste un array separat.
        for(int index=0; index<rgApproval.getChildCount(); index++)
        {
            // CAST la RadioButton. Funcționează pentru că toți copiii din XML sunt
            // `MaterialRadioButton`, care extinde `RadioButton`.
            // RISC: dacă cineva adaugă un `View` separator în RadioGroup, acest cast
            //   aruncă `ClassCastException` la editare.
            RadioButton rb = (RadioButton) rgApproval.getChildAt(index);

            // Compară TEXTUL butonului („G", „PG", „PG-13", „R", „NC-17") cu valoarea.
            if(rb.getText().toString().equals(existing.getApproval()))
            {
                // `check(id)` bifează butonul și îl debifează automat pe celălalt —
                // aceasta este garanția pe care o dă RadioGroup.
                rgApproval.check(rgApproval.getChildAt(index).getId());

                // Ieși din buclă.
                break;
            }
        }
    }

    // ==================================================================================
    // initControls — 13 FINDVIEWBYID, UNUL PENTRU FIECARE ID DIN XML
    // ==================================================================================
    // CE FACE: leagă fiecare câmp Java de view-ul cu ID-ul corespunzător.
    // CUM: 13 atribuiri mecanice. Ordinea urmează ordinea câmpurilor.
    // RISC: un ID greșit -> `findViewById` întoarce null -> `NullPointerException`
    //   la prima utilizare, nu aici. Cea mai greu de depanat clasă de bug-uri.
    // REF: `MainActivity.initControls()` are doar 3 linii; aici sunt 13.
    // ==================================================================================
    private void initControls() {

        // Fiecare linie are aceeași formă: `câmp = findViewById(R.id.id_din_xml);`
        // `findViewById` este generic din API 26: tipul se deduce din câmpul din stânga.
        etTitle = findViewById(R.id.etTitle);
        etRelease = findViewById(R.id.etRelease);
        spGenre = findViewById(R.id.spGenre);
        etBudget = findViewById(R.id.etBudget);
        sbDuration = findViewById(R.id.sbDuration);
        rbRating = findViewById(R.id.rbRating);
        rgApproval = findViewById(R.id.rgApproval);

        // ATENȚIE LA NUME: câmpul este `cbonWatchList`, ID-ul este `cbOnWatchList`.
        // Un `b` în plus în câmp, un `O` mare în ID. Ambele sunt corecte.
        cbonWatchList = findViewById(R.id.cbOnWatchList);

        swWatched = findViewById(R.id.swWatched);
        etPoster = findViewById(R.id.etPoster);

        // Butonul: câmpul `btMovieAction`, ID-ul `btnMovieAction`.
        btMovieAction = findViewById(R.id.btnMovieAction);

        // Cele două etichete care se actualizează din listeneri.
        tvDurationValue = findViewById(R.id.tvDurationValue);
        tvWatchedValue = findViewById(R.id.tvWatchedValue);
    }

    // ==================================================================================
    // initListeners — PATRU COMPORTAMENTE
    // ==================================================================================
    // CE FACE: atașează: click pe dată -> dialog; click pe buton -> salvare;
    //   schimbare switch -> etichetă; mișcare slider -> etichetă.
    // CUM: 4 clase anonime, dintre care una implementează o interfață cu 3 metode.
    // RISC: ordinea contează față de `fillForm` — vezi onCreate, pasul 6.
    // REF: `MainActivity.initListeners()` are 2 listeneri; aici sunt 4.
    // ==================================================================================
    private void initListeners() {

        // LISTENER 1 — click pe câmpul de dată.
        // Un `EditText` nu are „onDateClick"; folosim click-ul generic.
        etRelease.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Deschide dialogul de calendar. Metoda este mai jos.
                showReleaseDatePicker();
            }
        });

        // LISTENER 2 — click pe butonul de salvare.
        btMovieAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Construiește Movie-ul, îl pune în Intent, setează rezultatul, închide.
                saveMovie();
            }
        });

        // LISTENER 3 — schimbarea switch-ului.
        // Interfața este `CompoundButton.OnCheckedChangeListener`, nu una proprie
        // lui MaterialSwitch: MaterialSwitch este un CompoundButton.
        swWatched.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull CompoundButton buttonView, boolean isChecked) {
                // Operator ternar: o singură expresie pentru ambele cazuri.
                // `isChecked` este valoarea NOUĂ, nu cea veche.
                tvWatchedValue.setText(isChecked ? "Yes" : "No");
            }
        });

        // LISTENER 4 — slider-ul de durată.
        // Interfața are TREI metode. Toate trebuie implementate; două rămân goale.
        // Nu există metode default într-o interfață Java clasică.
        sbDuration.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // `progress` = valoarea nouă, 0..300.
                // `fromUser` = true dacă vine din degetul utilizatorului, false dacă
                //   vine din `setProgress(...)`. Aici este IGNORAT intenționat:
                //   vrem ca eticheta să fie corectă și când `fillForm` apelează
                //   `setProgress`, nu doar când trage utilizatorul.
                tvDurationValue.setText(progress + " min.");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Gol: nu ne interesează momentul în care începe tragerea.
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Gol: nu ne interesează momentul în care se termină tragerea.
                // Aici s-ar putea salva valoarea sau afișa un mesaj, dacă ar fi nevoie.
            }
        });
    }

    // ==================================================================================
    // saveMovie — WIDGET-URI -> MODEL -> INTENT -> REZULTAT
    // ==================================================================================
    // CE FACE: citește cele 13 controale, construiește un `Movie`, îl pune într-un
    //   `Intent` de rezultat împreună cu poziția (dacă există) și închide ecranul.
    // CUM: 1. new Movie() 2. nouă setter-e 3. buclă pe RadioGroup 4. Intent
    //   5. putExtra 6. poziție condiționată 7. setResult 8. finish.
    // RISC: `Double.parseDouble` pe text gol aruncă `NumberFormatException`.
    //   Vezi BUG 1 — este singurul crash reproductibil din aplicație.
    // REF: perechea lui `fillForm`; consumatorul este callback-ul din MainActivity.
    // ==================================================================================
    private void saveMovie() {

        // 1. Obiect gol, umplut imediat cu setter-e. De aceea constructorul fără
        //    parametri este obligatoriu în model.
        Movie movie = new Movie();

        // 2. Cele nouă setter-e care corespund unui widget fiecare.
        //    Ordinea nu contează; doar perechile widget -> setter contează.
        movie.setTitle(etTitle.getText().toString());

        // DATA NU VINE DIN TEXT, ci din calendar. Textul poate fi invalid; calendarul nu.
        // `getTime()` transformă Calendar -> Date.
        movie.setRelease(releaseCal.getTime());

        // Genul: textul selectat în Spinner. `getSelectedItem()` nu este null cât timp
        // spinner-ul are cel puțin un element (valorile din `@array/movie_genre`).
        movie.setGenre(spGenre.getSelectedItem().toString());

        // BUG 1: dacă `etBudget` este gol, `Double.parseDouble("")` aruncă
        // `NumberFormatException`, iar aplicația se închide. Nu există try/catch
        // și nu există verificare `isEmpty()`.
        // Aceeași linie refuză și „1,5" (virgulă zecimală, locale românesc), dar
        // acceptă „1.6E8" (notație științifică).
        movie.setBudget(Double.parseDouble(etBudget.getText().toString()));

        // Durata: direct din progres, fără conversie.
        movie.setDuration(sbDuration.getProgress());

        // Nota: `getRating()` întoarce `float`, exact tipul câmpului.
        movie.setRating(rbRating.getRating());

        // Switch-ul: `isChecked()` -> boolean.
        movie.setWatched(swWatched.isChecked());

        // Bifa: `isChecked()` -> boolean. Independentă de switch.
        movie.setOnWatchlist(cbonWatchList.isChecked());

        // Posterul: text liber. Poate fi gol — modelul acceptă.
        movie.setPosterUrl(etPoster.getText().toString());

        // 3. Ghidajul parental: nu are getter direct pe RadioGroup, deci căutăm
        //    butonul bifat. Aceeași buclă ca în `fillForm`, dar cu `isChecked()`.
        for(int index=0; index<rgApproval.getChildCount(); index++)
        {
            RadioButton rb = (RadioButton) rgApproval.getChildAt(index);

            // Exact un buton este bifat (garanție RadioGroup). Dacă niciunul nu ar fi
            // bifat, `approval` ar rămâne null — dar XML-ul bifează `rbGeneral` implicit.
            if(rb.isChecked())
            {
                // Valoarea salvată este TEXTUL butonului, nu ID-ul.
                movie.setApproval(rb.getText().toString());
                break;
            }
        }

        // 4. Intent FĂRĂ target. Nu pornește nicio activitate: este containerul
        //    rezultatului, citit de callback-ul din MainActivity.
        Intent intent = new Intent();

        // 5. Pune filmul. `putExtra(String, Serializable)` serializează obiectul.
        intent.putExtra(MainActivity.MOVIE_EXTRA, movie);

        // 6. Poziția se pune DOAR în modul edit. În modul insert, absența ei este
        //    exact semnalul pe care `MainActivity` îl citește ca „adaugă la sfârșit".
        //    Fără acolade: `if`-ul controlează o singură instrucțiune.
        if (position != -1)
            intent.putExtra(MainActivity.POSITION_EXTRA, position);

        // 7. Setează codul de rezultat. `RESULT_OK` este -1 — aceeași valoare ca
        //    „poziția lipsește", dar complet nelegată. Aici este cod de rezultat,
        //    acolo este index de listă. Coincidență numerică, nu conflict.
        setResult(RESULT_OK, intent);

        // 8. Închide activitatea. Fără `finish()`, `setResult` nu ajunge niciodată
        //    la apelant, pentru că activitatea ar rămâne deschisă.
        finish();
    }

    // ==================================================================================
    // showReleaseDatePicker — DIALOGUL DE CALENDAR
    // ==================================================================================
    // CE FACE: deschide un `DatePickerDialog` poziționat pe data curentă din
    //   `releaseCal`, iar la confirmare scrie data în calendar ȘI în text.
    // CUM: 1. parsează textul existent (dacă e valid) 2. construiește dialogul
    //   3. la confirmare, actualizează calendarul 4. formatează înapoi în EditText.
    // RISC: dacă textul din câmp a fost tastat manual și este invalid, parse-ul
    //   eșuează silențios și dialogul pornește de la data veche din calendar.
    // REF: `SimpleDateFormat` este folosit și în `fillForm`; aici în direcția inversă.
    // ==================================================================================
    private void showReleaseDatePicker() {

        // Textul curent din câmp. Poate fi gol (formular nou) sau o dată formatată
        // de `fillForm` (mod edit) sau text tastat manual de utilizator.
        String current = etRelease.getText().toString();

        // Parsăm doar dacă există ceva de parsat.
        if (!current.isEmpty()) {
            try {
                // `parse` întoarce `Date` sau aruncă `ParseException`.
                Date parsed = dateFormat.parse(current);

                // `parse` poate întoarce null teoretic; verificăm înainte de folosire.
                if (parsed != null)
                    // Sincronizează calendarul cu textul. Aici se întâmplă legătura
                    // text -> Calendar, care lipsește din `fillForm` (BUG 2).
                    releaseCal.setTime(parsed);
            } catch (ParseException ignored) { }
            // Excepția este IGNORATĂ intenționat: text invalid nu trebuie să blocheze
            // dialogul. Se pornește de la data veche din calendar.
            // RISC: utilizatorul nu află niciodată că textul lui a fost ignorat.
        }

        // Construiește dialogul. Parametrii:
        //   1. `this` = Context
        //   2. listener-ul de confirmare (clasă anonimă)
        //   3-5. anul, luna, ziua de start — LUNA ESTE 0-BASED (`Calendar.MONTH`)
        // `.show()` la final: dialogul nu apare până nu este apelată.
        new DatePickerDialog(
                this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int day) {

                        // `month` vine tot 0-based din DatePicker, deci se potrivește
                        // direct cu `Calendar.set(year, month, day)`. Fără conversie.
                        releaseCal.set(year, month, day);

                        // Curăță orice eroare afișată anterior pe câmp. Aici nimic nu
                        // setează vreodată o eroare, dar apelul este defensiv.
                        etRelease.setError(null);

                        // Scrie data înapoi în câmp, în același format „dd/MM/yyyy".
                        etRelease.setText(dateFormat.format(releaseCal.getTime()));
                    }
                },
                // Valorile de start: exact starea curentă a calendarului.
                releaseCal.get(Calendar.YEAR),
                releaseCal.get(Calendar.MONTH),
                releaseCal.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    // ==================================================================================
    // CELE TREI BUG-URI REALE DIN ACEST FIȘIER
    // ==================================================================================
    // BUG 1 — BUGET GOL -> CRASH
    //   `Double.parseDouble("")` aruncă `NumberFormatException`. Utilizatorul apasă
    //   „Save Movie" fără să completeze bugetul, aplicația se închide.
    //   Reproducibil 100%. Nu există validare, nu există try/catch.
    //
    // BUG 2 — DATA SE PIERDE LA EDITARE
    //   `fillForm` scrie data în `etRelease`, dar NU o scrie în `releaseCal`.
    //   `releaseCal` rămâne pe `Calendar.getInstance()`, adică „acum", din momentul
    //   creării activității. Dacă utilizatorul editează un film și NU atinge câmpul
    //   de dată, `saveMovie` salvează `releaseCal.getTime()` = data de AZI.
    //   Filmul „Inception" (2010) devine „azi", fără nicio eroare.
    //   Corect ar fi ca `fillForm` să facă și `releaseCal.setTime(existing.getRelease())`.
    //
    // BUG 3 — NOTAȚIE ȘTIINȚIFICĂ ÎN CÂMPUL DE BUGET
    //   `String.valueOf(160000000.0)` produce „1.6E8", nu „160000000".
    //   La editare, câmpul afișează „1.6E8". Parsarea înapoi funcționează, deci
    //   datele sunt corecte — doar afișarea este urâtă. Cosmetic, dar vizibil.
    //
    // RISC SUPLIMENTAR — GENUL CARE NU SE GĂSEȘTE
    //   Dacă `@array/movie_genre` se schimbă și valoarea salvată nu mai există,
    //   `fillForm` lasă spinner-ul pe poziția 0, fără mesaj. La salvare, genul
    //   filmului se schimbă în primul element din listă.
    // ==================================================================================
}
