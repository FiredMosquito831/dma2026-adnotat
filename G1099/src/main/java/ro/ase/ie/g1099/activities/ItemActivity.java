/*
 * ======================================================================================
 * FIȘIER: ItemActivity.java — FORMULARUL DE INSERT ȘI EDIT
 * ======================================================================================
 * CE ESTE: o singură activitate care servește DOUĂ moduri. Dacă primește un `Movie`
 *   în Intent, este editare; dacă nu primește nimic, este inserare.
 *
 * DE CE EXISTĂ: este ecranul care transformă widget-uri în model. Toate cele 10
 *   câmpuri ale lui `Movie` sunt produse aici, din `getText()`, `getProgress()`,
 *   `getRating()`, `isChecked()` și `getSelectedItem()`.
 *
 * DE CE DEPINDE: de `activity_item.xml` (13 view-uri), de `Movie`, de constantele
 *   `MainActivity.MOVIE_EXTRA` / `POSITION_EXTRA` și de Activity Result API
 *   (prin `setResult`).
 *
 * CINE DEPINDE DE EL: `MainActivity`, care primește Intent-ul de rezultat în callback.
 *
 * CE NU FACE: nu ține lista, nu știe ce poziție are filmul în listă (o primește),
 *   nu salvează nimic pe disc. Nu validează câmpurile înainte de salvare.
 *
 * CE S-AR RUPE FĂRĂ EL: aplicația ar avea listă, dar niciun mod de a adăuga sau
 *   modifica un film. FAB-ul ar porni o activitate inexistentă.
 *
 * ATENȚIE LA IMPORTURI: trei dintre ele sunt MOARTE — `android.media.Rating`,
 *   `android.widget.Switch` și `Snackbar`. Sunt marcate pe rând mai jos. Vezi 31.0.5.
 * ======================================================================================
 */

// PACHETUL: același `activities` ca MainActivity — de aceea constantele `protected`
//   ale lui MainActivity sunt vizibile aici fără import.
package ro.ase.ie.g1099.activities;

// DatePickerDialog: dialogul cu roți pentru an/lună/zi.
import android.app.DatePickerDialog;

// Intent: obiectul de rezultat, construit la salvare.
import android.content.Intent;

// Rating: clasa de rating pentru conținut MEDIA, din API 21.
//   IMPORT MORT. Nu este folosită nicăieri în fișier — apare exact o dată, aici.
//   Widget-ul din formular este `RatingBar`, cu totul altă clasă. Vezi 31.0.5.
import android.media.Rating;

// Bundle: doar în semnătura lui onCreate.
import android.os.Bundle;

// View: tipul parametrului din OnClickListener.
import android.view.View;

// Button: butonul de salvare (`btMovieAction`).
import android.widget.Button;

// CheckBox: bifa „Watchlist" (`cbonWatchList`).
import android.widget.CheckBox;

// CompoundButton: interfața comună pentru Switch și CheckBox; apare în semnătura
//   listener-ului de switch.
import android.widget.CompoundButton;

// DatePicker: tipul view-ului din callback-ul dialogului de dată.
import android.widget.DatePicker;

// EditText: titlul, data, bugetul și posterul.
import android.widget.EditText;

// RadioButton: cele 5 butoane de ghidaj parental.
import android.widget.RadioButton;

// RadioGroup: containerul celor 5 butoane.
import android.widget.RadioGroup;

// RatingBar: bara de stele. ACESTA este widget-ul de rating, nu `android.media.Rating`.
import android.widget.RatingBar;

// SeekBar: bara de durată.
import android.widget.SeekBar;

// Spinner: lista derulantă de genuri.
import android.widget.Spinner;

// Switch: widget-ul clasic de comutare.
//   IMPORT MORT. Câmpul `swWatched` este declarat `MaterialSwitch`, iar
//   `MaterialSwitch` extinde `SwitchCompat`, nu `Switch`. Numele `Switch` nu apare
//   nicăieri în afara acestei linii. Vezi 31.0.6.
import android.widget.Switch;

// TextView: cele două etichete dinamice (`tvDurationValue`, `tvWatchedValue`).
import android.widget.TextView;

// EdgeToEdge: desenare sub barele de sistem.
import androidx.activity.EdgeToEdge;

// NonNull: adnotare pe parametrul `buttonView` din listener-ul de switch.
//   Este o promisiune că parametrul nu este null; compilatorul nu o verifică,
//   dar lint-ul o folosește.
import androidx.annotation.NonNull;

// AppCompatActivity: clasa de bază a activității.
import androidx.appcompat.app.AppCompatActivity;

// IntentCompat: varianta tipată a lui `getSerializableExtra`, cu `Class<T>`.
//   Spre deosebire de MainActivity, care folosește metoda veche cu cast,
//   aici se folosește varianta modernă.
import androidx.core.content.IntentCompat;

// Insets / ViewCompat / WindowInsetsCompat: aceleași trei clase ca în MainActivity,
//   pentru padding-ul de sub barele de sistem.
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// MaterialSwitch: switch-ul Material 3. Extinde SwitchCompat -> CompoundButton.
import com.google.android.material.materialswitch.MaterialSwitch;

// Snackbar: bară scurtă de mesaj, în partea de jos a ecranului.
//   IMPORT MORT. Nu există niciun `Snackbar.make(...)` în fișier.
import com.google.android.material.snackbar.Snackbar;

// ParseException: excepția aruncată de `SimpleDateFormat.parse` pe text invalid.
import java.text.ParseException;

// SimpleDateFormat: formatter-ul „dd/MM/yyyy" pentru câmpul de dată.
import java.text.SimpleDateFormat;

// Calendar: sursa de adevăr pentru dată.
import java.util.Calendar;

// Date: tipul întors de `dateFormat.parse(...)`.
import java.util.Date;

// Locale: formatul regional pentru formatter.
import java.util.Locale;

// R: resursele generate.
import ro.ase.ie.g1099.R;

// Movie: modelul construit la salvare și citit la umplere.
import ro.ase.ie.g1099.model.Movie;

/*
 * ======================================================================================
 * BLOC DE TEORIE: FORMULARUL CA MAȘINĂ DE STARE
 * ======================================================================================
 *
 * 1. **Două moduri, o singură activitate.** `ItemActivity` nu are două layout-uri și
 *    două clase. Are un câmp `position` care este `-1` (insert) sau `>= 0` (edit).
 *    Tot comportamentul diferit derivă din acest câmp.
 *
 * 2. **Sursa de adevăr nu este întotdeauna widget-ul.** Pentru 9 câmpuri, widget-ul
 *    este sursa (citești direct din el la salvare). Pentru dată, sursa este
 *    `releaseCal`, iar `EditText`-ul este doar o AFIȘARE.
 *
 * 3. **Listener-ii sunt și ei sursă de adevăr pentru etichete.** `tvDurationValue`
 *    și `tvWatchedValue` nu sunt citite la salvare; sunt actualizate de listener-ii
 *    widget-urilor. Dacă un listener nu se declanșează, eticheta rămâne în urmă.
 *
 * 4. **Rezultatul este un Intent.** Activitatea nu întoarce un obiect prin `return`;
 *    îl pune într-un `Intent` și îl predă prin `setResult`.
 *
 * 5. **Ordinea din `onCreate` este critică.** `initListeners()` rulează ÎNAINTE de
 *    `fillForm()`. Motivul: `setChecked` și `setProgress` declanșează listener-ii,
 *    care actualizează etichetele. Invers, etichetele ar rămâne cu valorile din XML.
 *
 * VIZUAL: fluxul de date în formular
 *
 *   [etTitle] -------------------------+
 *   [etRelease] --(doar afișare)--+    |
 *                                 |    |
 *   [releaseCal] -----------------+----+---> saveMovie() ---> Movie ---> Intent
 *   [spGenre] -------------------------+                                    |
 *   [etBudget] ------------------------+                                    v
 *   [sbDuration] ----------------------+                            setResult(RESULT_OK)
 *   [rbRating] ------------------------+                                    |
 *   [swWatched] -----------------------+                                    v
 *   [cbonWatchList] -------------------+                                  finish()
 *   [rgApproval] --(buclă)--+----------+
 *   [etPoster] ------------------------+
 *
 * VIZUAL: cele două moduri
 *
 *   Intent FĂRĂ MOVIE_EXTRA          Intent CU MOVIE_EXTRA
 *          |                                  |
 *          v                                  v
 *   existing == null                   existing != null
 *          |                                  |
 *   position rămâne -1                 position = getIntExtra(..., -1)
 *   formular gol                       fillForm(existing)
 *   buton: "Save Movie"                buton: "Update Movie"
 *          |                                  |
 *          +---------------+------------------+
 *                          v
 *                     saveMovie()
 *                          |
 *          position == -1 -> Intent FĂRĂ POSITION_EXTRA
 *          position >= 0  -> Intent CU POSITION_EXTRA
 * ======================================================================================
 */
public class ItemActivity extends AppCompatActivity {

    // ==================================================================================
    // CELE 13 REFERINȚE LA VIEW-URI
    // ==================================================================================
    // DE CE 13: formularul are 10 câmpuri de date + 2 etichete dinamice + 1 buton.
    //   Cele 7 `TextView`-uri de etichetă fixă (`tvTile`, `tvGenre`, `tvBudget`,
    //   `tvDuration`, `tvRating`, `tvWatched`, `tvGuidance`) NU apar aici: nu sunt
    //   citite și nu sunt modificate din cod, deci nu au nevoie de referință.
    //
    // DE CE TOATE SUNT `private`: sunt detalii de implementare ale ecranului.
    // ==================================================================================

    // Câmpul 1: titlul. Obiectul real din XML este `TextInputEditText`, dar tipul
    // declarat este `EditText` — clasa părinte. Este suficient pentru `getText()`.
    private EditText etTitle;

    // Câmpul 2: data, ca TEXT. Valoarea reală este în `releaseCal` (vezi mai jos).
    private EditText etRelease;

    // Câmpul 3: genul. `Spinner` — lista derulantă din `@array/movie_genre`.
    private Spinner spGenre;

    // Câmpul 4: bugetul. `inputType="numberDecimal"` în XML.
    private EditText etBudget;

    // Câmpul 5: durata. `SeekBar` cu max 300.
    private SeekBar sbDuration;

    // Câmpul 6: rating-ul. `RatingBar` cu 5 stele și pas 0.5.
    // ATENȚIE: tipul este `RatingBar`, NU `android.media.Rating`. Importul acela
    //   este mort — vezi 31.0.5.
    private RatingBar rbRating;

    // Câmpul 7: ghidajul parental. `RadioGroup` cu 5 butoane.
    private RadioGroup rgApproval;

    // Câmpul 8: bifa „Watchlist".
    // ATENȚIE LA NUME: câmpul este `cbonWatchList`, ID-ul este `cbOnWatchList`.
    //   Un `o` lipsă într-o parte, un `O` în plus în cealaltă. Nu este typo de corectat:
    //   așa este sursa. `findViewById(R.id.cbonWatchList)` NU ar compila.
    private CheckBox cbonWatchList;

    // Câmpul 9: switch-ul „Recommended". Tipul real este `MaterialSwitch`, care
    // extinde `SwitchCompat` -> `CompoundButton`. Declarat `MaterialSwitch` pentru
    // a avea acces la toate metodele lui.
    // DE ACEA importul `android.widget.Switch` este mort: nu este tipul acestui câmp.
    private MaterialSwitch swWatched;

    // Câmpul 10: URL-ul posterului.
    private EditText etPoster;

    // Câmpul 11: butonul de salvare.
    // ATENȚIE LA NUME: câmpul este `btMovieAction`, ID-ul este `btnMovieAction`.
    private Button btMovieAction;

    // Câmpul 12: eticheta care afișează durata („148 min."). NU este citită la
    // salvare — este doar afișare, actualizată de listener-ul SeekBar-ului.
    private TextView tvDurationValue;

    // Câmpul 13: eticheta care afișează „Yes"/„No" pentru switch.
    private TextView tvWatchedValue;

    // ==================================================================================
    // STAREA FORMULARULUI
    // ==================================================================================

    // MODUL: -1 = insert, >= 0 = edit.
    // DE CE `-1` și nu `0`: `0` este o poziție validă în listă. Sentinel-ul trebuie
    //   să fie o valoare IMPOSIBILĂ ca poziție.
    // CINE ÎL SCHIMBĂ: doar `onCreate`, când găsește un film în Intent.
    private int position = -1;   // -1 = insert mode

    // SURSA DE ADEVĂR PENTRU DATĂ.
    // `Calendar.getInstance()` = data și ora CURENTĂ. De aceea formularul pornește
    //   cu data de azi, nu cu o dată goală.
    // DE CE NU `null`: `saveMovie` apelează `releaseCal.getTime()` necondiționat.
    //   Un `null` ar arunca NPE la prima salvare.
    // DE CE `Calendar` și nu `Date`: `Calendar` are câmpuri (YEAR, MONTH, DAY_OF_MONTH)
    //   pe care `DatePickerDialog` le citește și le scrie direct.
    // RISC: în modul edit, `fillForm` NU actualizează acest calendar — vezi BUG 4.
    private Calendar releaseCal = Calendar.getInstance();

    // FORMATTER-UL DE DATĂ.
    // „dd/MM/yyyy" = zi cu 2 cifre / lună cu 2 cifre / an cu 4 cifre.
    //   ATENȚIE: `MM` = lună, `mm` = minute. Inversarea este o greșeală clasică.
    // `Locale.getDefault()` = formatul limbii dispozitivului. Aici pattern-ul este
    //   explicit, deci Locale influențează doar simbolurile și ordinea internă.
    // `final`: referința nu se schimbă. Obiectul, însă, este MUTABIL.
    // RISC: `SimpleDateFormat` nu este thread-safe. Aici este folosit exclusiv pe
    //   UI thread, deci nu există concurență.
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    // ==================================================================================
    // onCreate — INFRASTRUCTURĂ, CONTROALE, LISTENER-I, MOD
    // ==================================================================================
    // CE FACE: construiește ecranul, atașează listener-ii, apoi decide dacă umple
    //   formularul cu un film existent sau îl lasă gol.
    // CUM: 1. infrastructură (super, edge-to-edge, layout, insets)
    //      2. initControls
    //      3. initListeners
    //      4. citește Intent-ul
    //      5. dacă există film -> position, fillForm, titlu, text buton
    // RISC: ordinea 2-3-5 este obligatorie. `fillForm` are nevoie de view-uri (2)
    //   și trebuie să declanșeze listener-ii (3) pentru etichete.
    // REF: `MainActivity.onCreate` are aceeași primă fază de infrastructură.
    // ==================================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Edge-to-edge: același apel ca în MainActivity.
        EdgeToEdge.enable(this);

        // Umflă `activity_item.xml`. Arborele are un ScrollView rădăcină cu id `main`.
        setContentView(R.layout.activity_item);

        // Insets pe containerul rădăcină. Identic cu MainActivity, cu o singură
        //   diferență de context: aici `R.id.main` este ScrollView-ul, nu FrameLayout-ul.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Faza 1: leagă cele 13 câmpuri de view-urile din XML.
        // DE CE PRIMA: `fillForm` are nevoie de ele, iar `initListeners` la fel.
        initControls();

        // Faza 2: atașează listener-ii.
        // DE CE ÎNAINTE DE `fillForm`: `fillForm` apelează `setChecked` și `setProgress`.
        //   Aceste apeluri DECLANȘEAZĂ listener-ii, care actualizează etichetele
        //   `tvWatchedValue` și `tvDurationValue`. Dacă listener-ii ar fi atașați după
        //   `fillForm`, etichetele ar rămâne cu valorile implicite din XML.
        initListeners();

        // Faza 3: citește Intent-ul de intrare.
        // `getIntent()` întoarce Intent-ul care a pornit activitatea. Nu este null:
        //   orice activitate este pornită cu un Intent, chiar și unul gol.
        Intent intent = getIntent();

        // Citire TIPATĂ a serializable-ului.
        // DE CE `IntentCompat` ȘI NU `intent.getSerializableExtra(...)`:
        //   Metoda veche este depreciată din API 33. `IntentCompat.getSerializableExtra`
        //   primește și clasa așteptată, deci face verificarea tipului înainte de cast
        //   și funcționează identic pe toate versiunile.
        // DE CE `MainActivity.MOVIE_EXTRA`: aceeași constantă ca la trimitere. O greșeală
        //   de literă aici ar produce `null` fără nicio eroare.
        // VALOAREA: `null` în modul insert (FAB-ul nu pune niciun extra).
        Movie existing = IntentCompat.getSerializableExtra(intent, MainActivity.MOVIE_EXTRA, Movie.class);

        // MOD EDIT: există un film în Intent.
        if (existing != null) {

            // Citește poziția. Aici `-1` ar însemna „film trimis fără poziție" —
            // un caz care nu se întâmplă în fluxul normal, dar ar transforma salvarea
            // într-un insert (duplicat).
            position = intent.getIntExtra(MainActivity.POSITION_EXTRA, -1);

            // Umple formularul cu valorile filmului primit.
            // OBIECTUL ESTE O COPIE: modificările din formular NU ajung în lista din
            // MainActivity decât prin Intent-ul de rezultat.
            fillForm(existing);

            // Schimbă titlul ecranului. `setTitle` este metoda Activity-ului,
            //   NU setter-ul modelului — nu există conflict, destinatarul diferă.
            // Dacă tema nu are ActionBar, titlul nu se vede nicăieri pe ecran;
            //   feedback-ul vizibil pentru utilizator este textul butonului, mai jos.
            setTitle("Edit movie");

            // Schimbă textul butonului. Același buton face insert sau update;
            //   eticheta este singura diferență vizibilă pentru utilizator.
            btMovieAction.setText("Update Movie");
        }

        // MOD INSERT: `existing == null`. Nu se întâmplă nimic.
        //   `position` rămâne -1, formularul rămâne cu valorile implicite din XML:
        //   titlu gol, dată = azi, gen = primul din listă, buget gol, durată 0,
        //   rating 0, switch oprit, bifă oprită, „G" bifat, poster gol.
    }

    // ==================================================================================
    // fillForm — MODEL -> WIDGET-URI
    // ==================================================================================
    // CE FACE: copiază cele 10 valori din `Movie` în cele 10 widget-uri.
    // CUM: 1. câmpurile simple, în ordinea din model
    //      2. Spinner: buclă peste poziții, potrivire pe text
    //      3. RadioGroup: buclă peste copii, potrivire pe text
    // RISC: nu actualizează `releaseCal`. Vezi BUG 4 — data se pierde la update.
    // REF: fiecare linie este inversul unei linii din `saveMovie`.
    // ==================================================================================
    private void fillForm(Movie existing) {

        // Text: `getText().toString()` la salvare, `setText(...)` la umplere.
        // `setText` așteaptă `CharSequence`; `String` este un `CharSequence`.
        etTitle.setText(existing.getTitle());

        // BUG 2: `existing.getRelease()` poate fi `null`.
        // ==============================================================================
        // `SimpleDateFormat.format(null)` aruncă `NullPointerException`.
        // CÂND SE ÎNTÂMPLĂ: când filmul editat are `release == null`. În fluxul demo
        //   toate cele 3 filme au dată, deci bug-ul nu se vede. Devine real dacă un
        //   `Movie` este construit cu constructorul gol și salvat fără dată.
        // REPARAȚIE: `if (existing.getRelease() != null) { ... } else { etRelease.setText(""); }`
        // ==============================================================================
        etRelease.setText(dateFormat.format(existing.getRelease()));

        // BUG 3: `String.valueOf(double)` produce notație științifică pentru valori mari.
        // ==============================================================================
        // `String.valueOf(160000000.0)` -> "1.6E8".
        // Utilizatorul vede „1.6E8" în câmpul de buget. `Double.parseDouble("1.6E8")`
        //   funcționează, deci valoarea se salvează corect — dar afișarea este urâtă
        //   și pare o eroare.
        // REPARAȚIE: `String.format(Locale.getDefault(), "%.0f", existing.getBudget())`
        //   sau un `DecimalFormat`.
        // ==============================================================================
        etBudget.setText(String.valueOf(existing.getBudget()));

        // `SeekBar.setProgress(int)` — valoarea este limitată automat la 0..max.
        // Dacă durata din model ar fi 500, bara s-ar opri la 300.
        // EFECT SECUNDAR: dacă valoarea se SCHIMBĂ, se declanșează `onProgressChanged`
        //   și eticheta `tvDurationValue` se actualizează. Dacă valoarea este deja 0
        //   (implicită), listener-ul NU se declanșează — dar eticheta din XML este
        //   deja „0 min", deci rezultatul este corect.
        sbDuration.setProgress(existing.getDuration());

        // `CheckBox.setChecked(boolean)` — nu are listener, deci nicio reacție în lanț.
        cbonWatchList.setChecked(existing.isOnWatchlist());

        // `MaterialSwitch.setChecked(boolean)` — DECLANȘEAZĂ listener-ul de switch,
        //   care scrie „Yes"/„No" în `tvWatchedValue`. De aceea listener-ii trebuie
        //   atașați înainte de `fillForm`.
        swWatched.setChecked(existing.isWatched());

        // `RatingBar.setRating(float)` — parametrul este `float`, exact tipul câmpului
        //   din model. O valoare de 4.5 aprinde 4 stele și jumătate.
        rbRating.setRating(existing.getRating());

        // `setText(null)` este legal: câmpul devine gol. Nu aruncă excepție.
        // Aici se vede diferența de nume față de G1097: `getPosterUrl()`, nu `getPoster()`.
        etPoster.setText(existing.getPosterUrl());

        // SPINNER — nu are `setValue`, are `setSelection(index)`.
        // De aceea este nevoie de o buclă: caută INDEXUL valorii care corespunde.
        // `getCount()` = numărul de elemente din `@array/movie_genre`.
        // `getItemAtPosition(index)` întoarce un `Object` (în practică, `String`).
        // `break` la prima potrivire: dacă array-ul ar avea duplicate, câștigă primul.
        // Dacă nu se găsește nicio potrivire, spinner-ul rămâne pe poziția 0 —
        //   silențios, fără eroare.
        for(int index=0; index<spGenre.getCount(); index++)
        {
            if(spGenre.getItemAtPosition(index).equals(existing.getGenre()))
            {
                // `setSelection` mută selecția. Nu declanșează niciun listener aici,
                //   pentru că nu am atașat niciunul pe Spinner.
                spGenre.setSelection(index);
                break;
            }
        }

        // RADIOGROUP — nici aici nu există `setValue`. `check(id)` primește un ID.
        // `getChildCount()` = 5 (cele 5 MaterialRadioButton).
        // `getChildAt(index)` întoarce `View`; cast la `RadioButton` pentru `getText()`.
        for(int index=0; index<rgApproval.getChildCount(); index++)
        {
            RadioButton rb = (RadioButton) rgApproval.getChildAt(index);
            if(rb.getText().toString().equals(existing.getApproval()))
            {
                // `check(...)` primește ID-ul copilului, nu indexul. De aceea se
                //   citește `getChildAt(index).getId()`.
                // Efect secundar: `check` dezbină automat butonul bifat anterior.
                rgApproval.check(rgApproval.getChildAt(index).getId());
                break;
            }
        }

        // CE NU FACE ACEASTĂ METODĂ: nu scrie în `releaseCal`.
        //   Textul este afișat corect, dar calendarul rămâne pe data de AZI.
        //   Vezi BUG 4, imediat după listă.
    }

    // ==================================================================================
    // initControls — 13 FINDVIEWBYID, ÎN ORDINEA DIN FORMULAR
    // ==================================================================================
    // CE FACE: leagă fiecare câmp Java de view-ul cu id-ul corespunzător din XML.
    // CUM: 13 apeluri `findViewById`, în ordinea vizuală a formularului.
    // RISC: fiecare id trebuie să existe în `activity_item.xml`. Un id greșit
    //   întoarce `null` și crapă la primul acces, nu aici.
    // REF: `MainActivity.initControls` are același rol, cu 2 view-uri.
    // ==================================================================================
    private void initControls() {

        // Cardul „Basics": titlul și data.
        etTitle = findViewById(R.id.etTitle);
        etRelease = findViewById(R.id.etRelease);

        // Genul — Spinner învelit într-un MaterialCardView în XML.
        spGenre = findViewById(R.id.spGenre);

        // Cardul „Production": bugetul și durata.
        etBudget = findViewById(R.id.etBudget);
        sbDuration = findViewById(R.id.sbDuration);

        // Cardul „Your verdict": rating, ghidaj, switch, bifă.
        rbRating = findViewById(R.id.rbRating);
        rgApproval = findViewById(R.id.rgApproval);

        // ATENȚIE: id-ul este `cbOnWatchList` (cu `O` mare), câmpul este
        //   `cbonWatchList` (cu `o` mic). Diferă doar prin majusculă.
        cbonWatchList = findViewById(R.id.cbOnWatchList);

        swWatched = findViewById(R.id.swWatched);

        // Cardul „Poster".
        etPoster = findViewById(R.id.etPoster);

        // Butonul de salvare. Id-ul este `btnMovieAction`, câmpul `btMovieAction`.
        btMovieAction = findViewById(R.id.btnMovieAction);

        // Cele două etichete dinamice. Sunt citite doar de listener-ii de mai jos.
        tvDurationValue = findViewById(R.id.tvDurationValue);
        tvWatchedValue = findViewById(R.id.tvWatchedValue);
    }

    // ==================================================================================
    // initListeners — PATRU LISTENER-I
    // ==================================================================================
    // CE FACE: atașează comportamentul celor patru widget-uri interactive.
    // CUM: 1. etRelease -> deschide dialogul de dată
    //      2. btMovieAction -> saveMovie
    //      3. swWatched -> actualizează eticheta „Yes"/„No"
    //      4. sbDuration -> actualizează eticheta „N min."
    // RISC: listener-ul de switch și cel de SeekBar sunt esențiale pentru `fillForm`;
    //   fără ele, etichetele ar rămâne desincronizate la editare.
    // REF: aceleași clase anonime ca în `MainActivity.initListeners`.
    // ==================================================================================
    private void initListeners() {

        // 1. CLICK PE CÂMPUL DE DATĂ -> dialog.
        // `EditText` este și editabil, dar aici click-ul este interceptat și înlocuit
        //   cu un dialog. Utilizatorul nu tastează data, o alege.
        etRelease.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showReleaseDatePicker();
            }
        });

        // 2. CLICK PE BUTON -> salvare.
        // Același listener face insert sau update; diferența este câmpul `position`.
        btMovieAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveMovie();
            }
        });

        // 3. SWITCH -> etichetă.
        // `CompoundButton.OnCheckedChangeListener` are un singur metodă, cu două
        //   argumente: butonul și noua stare.
        // `@NonNull` pe `buttonView` este o adnotare de contract, nu o verificare
        //   la runtime.
        swWatched.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull CompoundButton buttonView, boolean isChecked) {
                // Operator ternar: „Yes" dacă este bifat, „No" altfel.
                // `setText` așteaptă `CharSequence`; ambii literali sunt `String`.
                tvWatchedValue.setText(isChecked ? "Yes" : "No");
            }
        });

        // 4. SEEKBAR -> etichetă.
        // Interfața are TREI metode; două rămân goale, dar TREBUIE scrise:
        //   altfel clasa anonimă nu compilează.
        sbDuration.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            // Apelat la fiecare schimbare de valoare.
            // `progress` = noua valoare (0..300).
            // `fromUser` = true dacă schimbarea vine de la deget, false dacă vine
            //   din cod (`setProgress`). Aici este IGNORAT intenționat: eticheta
            //   trebuie să reflecte valoarea în ambele cazuri.
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // `progress + " min."` — concatenare int cu String. Java convertește
                //   automat int-ul în text. Rezultat: „148 min.".
                tvDurationValue.setText(progress + " min.");
            }

            // Apelat când degetul atinge bara. Nu avem nevoie de el.
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            // Apelat când degetul ridică. Nu avem nevoie de el.
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    // ==================================================================================
    // saveMovie — WIDGET-URI -> MODEL -> INTENT -> REZULTAT
    // ==================================================================================
    // CE FACE: construiește un `Movie` nou din valorile curente ale formularului,
    //   îl pune într-un Intent și îl trimite înapoi lui MainActivity.
    // CUM: 1. `new Movie()` — constructorul gol
    //      2. 9 setter-e directe din widget-uri
    //      3. buclă pe RadioGroup pentru al 10-lea câmp
    //      4. Intent fără target, cu MOVIE_EXTRA și, condiționat, POSITION_EXTRA
    //      5. setResult + finish
    // RISC: `Double.parseDouble` pe text gol aruncă `NumberFormatException` — BUG 1.
    // REF: fiecare linie este inversul unei linii din `fillForm`.
    // ==================================================================================
    private void saveMovie() {

        // Obiect NOU, nu obiectul primit. Chiar și în modul edit, filmul din listă
        //   nu este modificat direct: este înlocuit cu acesta, prin `movies.set`.
        Movie movie = new Movie();

        // `getText()` întoarce un `Editable`, nu un `String`. `toString()` face conversia.
        movie.setTitle(etTitle.getText().toString());

        // DATA: din `releaseCal`, NU din textul din `etRelease`.
        // `getTime()` transformă calendarul într-un `Date` nou.
        // Aici se vede BUG 4: în modul edit, `releaseCal` nu a fost sincronizat.
        movie.setRelease(releaseCal.getTime());

        // GENUL: `getSelectedItem()` întoarce `Object`; `toString()` îl face text.
        // Dacă spinner-ul ar fi gol, ar arunca NPE — aici lista vine din
        //   `@array/movie_genre` și nu este goală.
        movie.setGenre(spGenre.getSelectedItem().toString());

        // BUG 1: `Double.parseDouble("")` aruncă `NumberFormatException`.
        // ==============================================================================
        // `etBudget` pornește GOL în modul insert. Dacă utilizatorul apasă „Save Movie"
        //   fără să scrie un buget, aplicația CRAPĂ. Nu există nicio validare înainte.
        // REPARAȚIE: `String text = etBudget.getText().toString();`
        //             `double value = text.isEmpty() ? 0 : Double.parseDouble(text);`
        // ==============================================================================
        movie.setBudget(Double.parseDouble(etBudget.getText().toString()));

        // DURATA: direct din progresul barei. `int`, fără conversie.
        movie.setDuration(sbDuration.getProgress());

        // RATING: `float`, exact tipul câmpului.
        movie.setRating(rbRating.getRating());

        // SWITCH: `isChecked()` întoarce `boolean`.
        movie.setWatched(swWatched.isChecked());

        // BIFĂ: `isChecked()` întoarce `boolean`.
        movie.setOnWatchlist(cbonWatchList.isChecked());

        // POSTER: textul brut, fără validare de URL. Poate fi și gol.
        // Aici se vede a doua diferență de nume: `setPosterUrl`, nu `setPoster`.
        movie.setPosterUrl(etPoster.getText().toString());

        // GHIDAJUL: singurul câmp care nu are un getter direct pe widget.
        // `RadioGroup` nu are `getSelectedItem`; are doar `getCheckedRadioButtonId()`,
        //   care ar cere o căutare suplimentară. Bucla este mai simplă.
        for(int index=0; index<rgApproval.getChildCount(); index++)
        {
            RadioButton rb = (RadioButton) rgApproval.getChildAt(index);
            if(rb.isChecked())
            {
                // Textul butonului, nu ID-ul lui. Vezi câmpul 8 din `Movie.java`.
                movie.setApproval(rb.getText().toString());
                break;
            }
        }

        // INTENT DE REZULTAT — fără target.
        // `new Intent()` nu pornește nimic. Este doar un container pentru `setResult`.
        Intent intent = new Intent();

        // Filmul, sub aceeași cheie folosită de MainActivity la citire.
        intent.putExtra(MainActivity.MOVIE_EXTRA, movie);

        // POZIȚIA — DOAR în modul edit.
        // În modul insert, extra-ul lipsește, iar MainActivity citește `-1` ca implicit.
        // Astfel, absența poziției este chiar semnalul de „adaugă".
        if (position != -1)
            intent.putExtra(MainActivity.POSITION_EXTRA, position);

        // Predă rezultatul. `RESULT_OK` este constanta moștenită din `Activity` (= -1).
        // Fără acest apel, MainActivity ar primi `RESULT_CANCELED` și ar ignora filmul.
        setResult(RESULT_OK, intent);

        // Închide activitatea. Fără `finish()`, formularul ar rămâne pe ecran,
        //   iar rezultatul ar fi livrat abia la apăsarea butonului Back.
        finish();
    }

    // ==================================================================================
    // showReleaseDatePicker — DIALOGUL DE DATĂ
    // ==================================================================================
    // CE FACE: deschide un `DatePickerDialog` poziționat pe data curentă din
    //   `releaseCal` și scrie rezultatul în calendar și în text.
    // CUM: 1. dacă textul există, încearcă să îl parseze în `releaseCal`
    //      2. deschide dialogul cu anul/luna/ziua din `releaseCal`
    //      3. la confirmare, scrie în `releaseCal` și formatează textul
    // RISC: `parse` poate arunca `ParseException`; este prinsă și IGNORATĂ.
    // REF: singurul loc din aplicație care folosește `DatePickerDialog`.
    // ==================================================================================
    private void showReleaseDatePicker() {

        // Textul curent din câmp. Poate fi gol (insert) sau o dată formatată (edit).
        String current = etRelease.getText().toString();

        // Dacă există text, încearcă să îl transformi în dată.
        if (!current.isEmpty()) {
            try {
                // `parse` întoarce `null` dacă textul nu poate fi interpretat complet.
                Date parsed = dateFormat.parse(current);
                if (parsed != null)
                    // Sincronizează calendarul cu textul. Acesta este SINGURUL loc
                    //   în care `releaseCal` primește data din formular — de aici BUG 4.
                    releaseCal.setTime(parsed);
            } catch (ParseException ignored) { }
            // `catch` gol, intenționat: un text invalid nu blochează dialogul.
            //   Calendarul rămâne pe valoarea anterioară, iar dialogul se deschide
            //   pe ea. Utilizatorul corectează din dialog.
        }

        // Construiește dialogul.
        // `this` = Context-ul (activitatea).
        // Al doilea argument este listener-ul de confirmare, ca clasă anonimă.
        // Următoarele trei sunt valorile inițiale: an, lună, zi.
        new DatePickerDialog(
                this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int day) {

                        // `month` este deja 0-based, exact cum îl așteaptă `Calendar.set`.
                        //   NU se scade 1. Confuzia apare la `GregorianCalendar`, unde
                        //   constantele `Calendar.JULY` ascund aceeași convenție.
                        releaseCal.set(year, month, day);

                        // Șterge eventuala eroare afișată pe câmp. `setError(null)`
                        //   ascunde iconița și mesajul de sub `TextInputLayout`.
                        etRelease.setError(null);

                        // Scrie textul formatat. Aici `releaseCal` este deja actualizat,
                        //   deci textul și calendarul sunt sincronizate.
                        etRelease.setText(dateFormat.format(releaseCal.getTime()));
                    }
                },

                // Valorile inițiale ale roților dialogului: din calendar, nu din text.
                releaseCal.get(Calendar.YEAR),
                releaseCal.get(Calendar.MONTH),
                releaseCal.get(Calendar.DAY_OF_MONTH)

        // `show()` este obligatoriu. Constructorul creează dialogul, dar nu îl afișează.
        ).show();
    }
}
