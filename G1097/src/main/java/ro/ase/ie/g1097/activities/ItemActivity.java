/*
 * ======================================================================================
 * FIȘIER: ItemActivity.java — FORMULARUL DE INSERT / EDIT
 * ======================================================================================
 * CE ESTE: o activitate cu 13 view-uri, un Calendar și un formatter de dată.
 *   Construiește un `Movie` din valorile din formular și îl trimite înapoi.
 *
 * DE CE EXISTĂ: `MainActivity` nu are voie să creeze obiecte de model dintr-un formular.
 *   Separarea înseamnă că formularul poate fi schimbat (alt layout, alte câmpuri) fără
 *   să atingi lista.
 *
 * CE ȘTIE: valorile curente ale celor 10 câmpuri, modul (insert sau edit) și poziția.
 *   Nu știe ce se întâmplă cu filmul după `finish()`.
 *
 * CE S-AR RUPE FĂRĂ EL: aplicația ar avea doar lista demo, fără posibilitatea de a
 *   adăuga sau modifica un film.
 * ======================================================================================
 */

package ro.ase.ie.g1097.activities;

// ======================================================================================
// IMPORTURI — CE ADUCE FIECARE
// ======================================================================================
// android.app.DatePickerDialog   — dialogul de alegere a datei
// android.content.Intent         — citirea extra-urilor la intrare și construirea
//                                  Intent-ului de rezultat
// android.os.Bundle              — semnătura lui onCreate
// android.view.View              — parametrul din listener-ii de click
// android.widget.*               — cele 13 widget-uri ale formularului
// androidx.activity.EdgeToEdge   — desenare sub barele de sistem
// androidx.annotation.NonNull    — contractul lui OnCheckedChangeListener
// androidx.core.content.IntentCompat — citirea tipată a unui Serializable (API 33+)
// androidx.core.view.*           — insets
// com.google.android.material.*  — MaterialSwitch, Snackbar
// java.text.*                    — parsare și formatare de dată
// java.util.*                    — Calendar, Date, Locale
// ro.ase.ie.g1097.*              — R, binding, model
// ======================================================================================

// Dialogul nativ cu calendar. Se deschide la click pe câmpul de dată.
import android.app.DatePickerDialog;

// Intent: la intrare, aduce filmul de editat; la ieșire, duce filmul salvat.
import android.content.Intent;

// Bundle: starea salvată de sistem. Neutilizat în corp, doar în semnătură.
import android.os.Bundle;

// View: tipul parametrului din `OnClickListener`.
import android.view.View;

// Button: tipul butonului de salvare. În XML este `MaterialButton`, care extinde Button.
import android.widget.Button;

// CheckBox: tipul declarat al câmpului, deși obiectul real este `MaterialCheckBox`.
import android.widget.CheckBox;

// CompoundButton: interfața comună pentru Switch și CheckBox; apare în listener.
import android.widget.CompoundButton;

// DatePicker: primul parametru al callback-ului `OnDateSetListener`.
import android.widget.DatePicker;

// EditText: tipul declarat pentru cele 4 câmpuri text.
import android.widget.EditText;

// RadioButton: folosit la bucla peste copiii lui RadioGroup.
import android.widget.RadioButton;

// RadioGroup: containerul celor 5 butoane de ghidaj.
import android.widget.RadioGroup;

// RatingBar: stelele.
import android.widget.RatingBar;

// SeekBar: slider-ul de durată.
import android.widget.SeekBar;

// Spinner: lista derulantă de genuri.
import android.widget.Spinner;

// Switch: importat, dar câmpul este declarat `MaterialSwitch`.
import android.widget.Switch;

// TextView: etichetele dinamice pentru durată și watched.
import android.widget.TextView;

// Toast: importat în sursă, dar NEUTILIZAT în acest fișier.
import android.widget.Toast;

// EdgeToEdge: conținut sub barele de sistem.
import androidx.activity.EdgeToEdge;

// NonNull: adnotarea din semnătura lui onCheckedChanged.
import androidx.annotation.NonNull;

// AppCompatActivity: clasa de bază.
import androidx.appcompat.app.AppCompatActivity;

// IntentCompat: variantă compatibilă cu API 33 pentru citirea unui Serializable tipat.
import androidx.core.content.IntentCompat;

// Insets: valorile barelor de sistem.
import androidx.core.graphics.Insets;

// ViewCompat: listener-ul de insets compatibil.
import androidx.core.view.ViewCompat;

// WindowInsetsCompat: tipurile de insets.
import androidx.core.view.WindowInsetsCompat;

// MaterialSwitch: switch-ul Material (nu cel din framework).
import com.google.android.material.materialswitch.MaterialSwitch;

// Snackbar: importat, dar NEUTILIZAT. Rămășiță de la o versiune anterioară.
import com.google.android.material.snackbar.Snackbar;

// ParseException: excepția aruncată de `SimpleDateFormat.parse`.
import java.text.ParseException;

// SimpleDateFormat: formatarea și parsarea „dd/MM/yyyy".
import java.text.SimpleDateFormat;

// Calendar: sursa de adevăr pentru dată.
import java.util.Calendar;

// Date: tipul din model.
import java.util.Date;

// Locale: formatul de dată depinde de limba dispozitivului.
import java.util.Locale;

// R: ID-urile din layout.
import ro.ase.ie.g1097.R;

// ActivityItemBinding: generat de View Binding. Importat, dar NEUTILIZAT —
//   sursa folosește `findViewById`. Rămășiță de la o variantă cu binding.
import ro.ase.ie.g1097.databinding.ActivityItemBinding;

// Modelul.
import ro.ase.ie.g1097.model.Movie;

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
 * 4. **Rezultatul este un Intent.** Activitatea nu întoarce un obiect prin return;
 *    îl pune într-un `Intent` și îl predă prin `setResult`.
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
    // onCreate — TREI FAZE: INFRASTRUCTURĂ, CONTROALE, MOD
    // ==================================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Faza 0: lanțul AppCompat. Fără el, tema și delegate-ul nu există.
        super.onCreate(savedInstanceState);

        // Edge-to-edge: conținutul poate ocupa tot ecranul, inclusiv sub bare.
        // Aici rădăcina este un `ScrollView` cu `android:id="@+id/main"`.
        EdgeToEdge.enable(this);

        // Inflatează `activity_item.xml`. Toate cele 13 `findViewById` depind de el.
        setContentView(R.layout.activity_item);

        // Insets pe rădăcina ScrollView. Identic cu MainActivity, dar aici padding-ul
        // este aplicat pe un container care DERULEAZĂ: conținutul se derulează în
        // interiorul zonei sigure, nu sub ea.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Faza 1: leagă cele 13 view-uri de câmpuri.
        // DE CE PRIMA: `fillForm` are nevoie de ele, iar `initListeners` la fel.
        initControls();

        // Faza 2: atașează listener-ii.
        // DE CE ÎNAINTE DE `fillForm`: `fillForm` apelează `setChecked` și `setProgress`.
        //   Aceste apeluri DECLANȘEAZĂ listener-ii, care actualizează etichetele
        //   `tvWatchedValue` și `tvDurationValue`. Dacă listener-ii ar fi atașați după
        //   `fillForm`, etichetele ar rămâne cu valorile implicite din XML.
        initListeners();

        // Faza 3: citește Intent-ul de intrare.
        Intent intent = getIntent();

        // Citire TIPATĂ a serializable-ului.
        // ==============================================================================
        // DE CE `IntentCompat` ȘI NU `intent.getSerializableExtra(...)`:
        //   Metoda veche este depreciată din API 33. `IntentCompat.getSerializableExtra`
        //   primește și clasa așteptată, deci face verificarea tipului înainte de cast
        //   și funcționează identic pe toate versiunile.
        // DE CE `MainActivity.MOVIE_EXTRA`: aceeași constantă ca la trimitere. O greșeală
        //   de literă aici ar produce `null` fără nicio eroare.
        // VALOAREA: `null` în modul insert (FAB-ul nu pune niciun extra).
        // ==============================================================================
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
            // ATENȚIE: tema aplicației este `Theme.Material3.DayNight.NoActionBar`
            //   (vezi `themes.xml`), iar layout-ul nu conține niciun `Toolbar`.
            //   Fără ActionBar, titlul nu se vede nicăieri pe ecran — schimbarea
            //   este reală, dar invizibilă. Feedback-ul vizibil pentru utilizator
            //   este textul butonului, setat pe linia următoare.
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
    // 10 câmpuri, în ordinea din model. Fiecare linie este inversul unei linii din
    // `saveMovie`.
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
        //   `Movie` este construit cu constructorul gol și salvat fără dată, sau dacă
        //   datele vin dintr-o sursă externă.
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

        // `setChecked` pe switch DECLANȘEAZĂ `OnCheckedChangeListener`.
        // Efectul: `tvWatchedValue` primește „Yes" sau „No".
        // DE ACEASTA linie depinde ordinea din `onCreate`: listener-ul trebuie să
        //   fie deja atașat când `fillForm` rulează.
        swWatched.setChecked(existing.isWatched());

        // `RatingBar.setRating(float)` — parametrul este `float`, exact tipul din model.
        // Fără listener: stelele se desenează direct.
        rbRating.setRating(existing.getRating());

        // `setText(null)` este legal: câmpul devine gol. Nu aruncă excepție.
        etPoster.setText(existing.getPoster());

        // ==============================================================================
        // BUCLA PENTRU SPINNER — potrivire pe TEXT
        // ==============================================================================
        // `Spinner` nu are `setSelectionByValue`. Are doar `setSelection(int)`.
        //   Deci trebuie să cauți indexul corespunzător valorii din model.
        // `getCount()` = numărul de elemente din `@array/movie_genre`.
        // `getItemAtPosition(index)` întoarce `Object`; aici, un `String`.
        // `equals` (nu `==`): compară conținutul, nu referințele.
        // RISC CONCRET: „Animation" (genul lui Toy Story) nu există în
        //   `@array/movie_genre`. Bucla nu găsește nimic, spinner-ul rămâne pe
        //   poziția 0 („Drama"), iar la salvare filmul își schimbă genul FĂRĂ
        //   să anunțe nimeni. Este cel mai probabil loc în care vei pierde puncte
        //   la o demonstrație live, pentru că nu apare nicio eroare.
        // ==============================================================================
        for(int index=0; index<spGenre.getCount(); index++)
        {
            if(spGenre.getItemAtPosition(index).equals(existing.getGenre()))
            {
                // Setează selecția și oprește bucla. Fără `break`, ar continua
                //   degeaba — dar rezultatul ar fi identic.
                spGenre.setSelection(index);
                break;
            }
        }

        // ==============================================================================
        // BUCLA PENTRU RADIOGROUP — potrivire pe TEXTUL BUTONULUI
        // ==============================================================================
        // `getChildCount()` = 5 (butoanele din XML).
        // `getChildAt(index)` întoarce `View`; cast la `RadioButton` pentru `getText()`.
        // `check(id)` — RadioGroup vrea ID-ul, nu indexul. De aceea `getChildAt(index).getId()`.
        // DE CE este generică: adaugi un al 6-lea buton în XML și codul îl vede
        //   fără nicio modificare.
        // RISC: potrivirea se face pe text. Redenumirea „PG-13" -> „PG13" în XML rupe
        //   editarea pentru filmele salvate cu vechea etichetă.
        // ==============================================================================
        for(int index=0; index<rgApproval.getChildCount(); index++)
        {
            RadioButton rb = (RadioButton) rgApproval.getChildAt(index);
            if(rb.getText().toString().equals(existing.getApproval()))
            {
                // `check` dezactivează automat orice alt buton din grup.
                rgApproval.check(rgApproval.getChildAt(index).getId());
                break;
            }
        }
    }

    // ==================================================================================
    // initControls — CELE 13 findViewById
    // ==================================================================================
    // DE CE TOATE ÎNTR-O METODĂ: dacă un ID este greșit, `findViewById` întoarce
    //   `null` fără eroare, iar NPE-ul apare abia la primul acces — undeva departe.
    //   Grupate aici, poți verifica vizual că fiecare linie are un ID existent.
    //
    // ORDINEA: nu contează pentru compilator. Este ordinea câmpurilor din clasă,
    //   adică ordinea din formular, de sus în jos.
    // ==================================================================================
    private void initControls() {

        // 1. Titlu — TextInputEditText etTitle (cardul „Basics").
        etTitle = findViewById(R.id.etTitle);

        // 2. Data — TextInputEditText etRelease (cardul „Basics").
        etRelease = findViewById(R.id.etRelease);

        // 3. Gen — Spinner spGenre (cardul „Basics").
        spGenre = findViewById(R.id.spGenre);

        // 4. Buget — TextInputEditText etBudget (cardul „Production").
        etBudget = findViewById(R.id.etBudget);

        // 5. Durată — SeekBar sbDuration (cardul „Production").
        sbDuration = findViewById(R.id.sbDuration);

        // 6. Rating — RatingBar rbRating (cardul „Your verdict").
        rbRating = findViewById(R.id.rbRating);

        // 7. Ghidaj — RadioGroup rgApproval (cardul „Your verdict").
        rgApproval = findViewById(R.id.rgApproval);

        // 8. Watchlist — ID `cbOnWatchList` (cu „On"), câmp `cbonWatchList` (fără „O").
        cbonWatchList = findViewById(R.id.cbOnWatchList);

        // 9. Switch — MaterialSwitch swWatched.
        swWatched = findViewById(R.id.swWatched);

        // 10. Poster — TextInputEditText etPoster (cardul „Poster").
        etPoster = findViewById(R.id.etPoster);

        // 11. Buton — ID `btnMovieAction`, câmp `btMovieAction`.
        btMovieAction = findViewById(R.id.btnMovieAction);

        // 12. Eticheta de durată — TextView tvDurationValue.
        tvDurationValue = findViewById(R.id.tvDurationValue);

        // 13. Eticheta de switch — TextView tvWatchedValue.
        tvWatchedValue = findViewById(R.id.tvWatchedValue);
    }

    // ==================================================================================
    // initListeners — PATRU LISTENERI
    // ==================================================================================
    // 1. click pe etRelease   -> deschide dialogul de dată
    // 2. click pe buton       -> saveMovie()
    // 3. switch schimbat      -> actualizează „Yes"/„No"
    // 4. seekbar mișcat       -> actualizează „NNN min."
    // ==================================================================================
    private void initListeners() {

        // LISTENER 1: click pe câmpul de dată.
        // ==============================================================================
        // DE CE `setOnClickListener` PE UN `EditText`: un EditText primește focus la
        //   atingere, nu click. Totuși, `setOnClickListener` funcționează: la prima
        //   atingere primește focus ȘI declanșează click-ul.
        // RISC: dacă utilizatorul apasă a doua oară (câmpul are deja focus), click-ul
        //   se declanșează din nou — dialogul se redeschide. Este comportamentul dorit.
        // RISC MAI MARE: câmpul are `inputType="date"`, deci utilizatorul POATE tasta.
        //   Textul tastat nu este citit în `releaseCal` decât la deschiderea dialogului.
        //   Dacă tastează și apasă direct Save, se salvează data veche din `releaseCal`.
        // ==============================================================================
        etRelease.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showReleaseDatePicker();
            }
        });

        // LISTENER 2: click pe butonul de salvare.
        // Nu există validare înainte de `saveMovie()`. Vezi BUG 1.
        btMovieAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveMovie();
            }
        });

        // LISTENER 3: switch-ul „Recommended".
        // ==============================================================================
        // `CompoundButton.OnCheckedChangeListener` are o singură metodă, cu doi parametri:
        //   `buttonView` — switch-ul care s-a schimbat (nefolosit aici)
        //   `isChecked`  — noua stare
        // `@NonNull` este adnotarea din semnătura interfeței; o copiem.
        // Operatorul ternar alege textul într-o singură expresie.
        // SE DECLANȘEAZĂ ȘI PROGRAMATIC: `fillForm` -> `swWatched.setChecked(...)`
        //   -> listener -> eticheta se actualizează. De aceea ordinea din onCreate contează.
        // ==============================================================================
        swWatched.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull CompoundButton buttonView, boolean isChecked) {
                tvWatchedValue.setText(isChecked ? "Yes" : "No");
            }
        });

        // LISTENER 4: slider-ul de durată.
        // ==============================================================================
        // `SeekBar.OnSeekBarChangeListener` are TREI metode. Toate trebuie implementate,
        //   chiar dacă două sunt goale — altfel clasa anonimă nu compilează.
        //
        // `onProgressChanged(seekBar, progress, fromUser)`:
        //   `progress`  — noua valoare (0..300)
        //   `fromUser`  — `true` dacă utilizatorul a tras de slider, `false` dacă
        //                 valoarea a fost setată din cod (`setProgress` în `fillForm`)
        //
        // DE CE `fromUser` ESTE IGNORAT AICI: vrem ca eticheta să reflecte MEREU
        //   valoarea reală, indiferent de cine a schimbat-o. Dacă ai filtra pe
        //   `fromUser`, la editare eticheta ar rămâne „0 min" până la prima atingere.
        // ==============================================================================
        sbDuration.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                // Concatenare: `int + String` -> String. Rezultat: „148 min.".
                // Punctul este în text, nu în valoare.
                tvDurationValue.setText(progress + " min.");
            }

            // Metoda 2: de la atingere la mișcare. Nu avem nevoie de ea.
            // Corp gol = intenționat, nu uitat. Interfața o cere.
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            // Metoda 3: la ridicarea degetului. Nu avem nevoie de ea.
            // Aici s-ar putea face un „commit" al valorii sau o haptică.
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    // ==================================================================================
    // saveMovie — WIDGET-URI -> MODEL -> INTENT
    // ==================================================================================
    // Cinci pași, în ordine:
    //   1. creează obiectul gol
    //   2. citește 9 câmpuri direct din widget-uri
    //   3. citește al 10-lea câmp (approval) prin buclă
    //   4. pune obiectul în Intent, plus poziția dacă există
    //   5. setResult + finish
    // ==================================================================================
    private void saveMovie() {

        // PASUL 1: obiect gol. Funcționează doar pentru că `Movie` are constructor
        // fără parametri scris explicit (vezi 22.1).
        Movie movie = new Movie();

        // PASUL 2: cele 9 câmpuri simple.
        // Fiecare linie este inversul unei linii din `fillForm`.
        // `getText()` întoarce `Editable`; `toString()` îl face `String`.
        movie.setTitle(etTitle.getText().toString());

        // `releaseCal.getTime()` — NU `etRelease.getText()`.
        // ==============================================================================
        // DE CE: calendarul este sursa de adevăr. Textul poate fi gol, poate fi tastat
        //   greșit, poate fi în alt format. Calendarul a fost actualizat doar de
        //   dialog, cu valori garantat valide.
        // `getTime()` construiește un `Date` NOU la fiecare apel, dar cu aceleași
        //   milisecunde.
        // ==============================================================================
        movie.setRelease(releaseCal.getTime());

        // `getSelectedItem()` întoarce `Object`; `toString()` îl face `String`.
        // RISC: dacă spinner-ul ar fi gol, ar fi `null` -> NPE. Aici `entries` din XML
        //   garantează cel puțin un element.
        movie.setGenre(spGenre.getSelectedItem().toString());

        // BUG 1: `Double.parseDouble("")` aruncă `NumberFormatException`.
        // ==============================================================================
        // CÂND SE ÎNTÂMPLĂ: la insert, câmpul de buget este gol. Utilizatorul apasă
        //   „Save Movie" fără să completeze bugetul -> aplicația se închide.
        // DE CE NU SE VEDE ÎN DEMO: cele 3 filme au buget completat, iar la editare
        //   câmpul este umplut de `fillForm`.
        // REPARAȚIE: `String budgetText = etBudget.getText().toString();
        //              double budget = budgetText.isEmpty() ? 0 : Double.parseDouble(budgetText);`
        // RISC SUPLIMENTAR: pe tastaturi care produc virgulă zecimală („1,5"),
        //   `parseDouble` aruncă aceeași excepție — `Double.parseDouble` acceptă doar
        //   punctul, indiferent de `Locale`.
        // ==============================================================================
        movie.setBudget(Double.parseDouble(etBudget.getText().toString()));

        // `getProgress()` întoarce `int`, exact tipul câmpului.
        movie.setDuration(sbDuration.getProgress());

        // `getRating()` întoarce `float`, exact tipul câmpului.
        movie.setRating(rbRating.getRating());

        // `isChecked()` întoarce `boolean`.
        movie.setWatched(swWatched.isChecked());

        // A doua bifă, independentă de prima.
        movie.setOnWatchlist(cbonWatchList.isChecked());

        // Posterul poate fi gol — `""`, nu `null`. Modelul acceptă ambele.
        movie.setPoster(etPoster.getText().toString());

        // PASUL 3: al 10-lea câmp — ghidajul.
        // ==============================================================================
        // RadioGroup nu are „getSelectedItem". Are „getCheckedRadioButtonId()", dar
        //   sursa preferă bucla, ca să poată lua TEXTUL, nu ID-ul.
        // `getChildCount()` = 5. `getChildAt` -> `View` -> cast la `RadioButton`.
        // `isChecked()` — exact un buton este bifat (RadioGroup garantează asta).
        // `break` după primul găsit: restul sunt oricum nebifate.
        // Dacă niciun buton nu ar fi bifat, `approval` ar rămâne `null` — dar XML-ul
        //   are `android:checked="true"` pe primul, deci nu se întâmplă.
        // ==============================================================================
        for(int index=0; index<rgApproval.getChildCount(); index++)
        {
            RadioButton rb = (RadioButton) rgApproval.getChildAt(index);
            if(rb.isChecked())
            {
                movie.setApproval(rb.getText().toString());
                break;
            }
        }

        // PASUL 4: construiește Intent-ul de REZULTAT.
        // ==============================================================================
        // `new Intent()` FĂRĂ parametri: nu este un Intent de navigare, ci un
        //   container de date. Nu are componentă țintă; este citit de launcher-ul
        //   din MainActivity.
        // ==============================================================================
        Intent intent = new Intent();

        // `putExtra(String, Serializable)` — `Movie` implementează interfața, deci
        //   suprascrierea este găsită. Obiectul este serializat aici.
        intent.putExtra(MainActivity.MOVIE_EXTRA, movie);

        // Poziția se trimite DOAR în modul edit.
        // ==============================================================================
        // În insert, cheia lipsește. MainActivity citește cu `getIntExtra(..., -1)`
        //   și primește exact sentinel-ul. Absența cheii ESTE informația „insert".
        // Dacă ai trimite mereu poziția (cu -1), ar funcționa identic — dar protocolul
        //   ar fi mai puțin clar.
        // ==============================================================================
        if (position != -1)
            intent.putExtra(MainActivity.POSITION_EXTRA, position);

        // PASUL 5a: predă rezultatul.
        // `RESULT_OK` este constanta `Activity.RESULT_OK` (-1). Fără acest apel,
        //   activitatea s-ar închide cu `RESULT_CANCELED` și MainActivity ar ignora-o.
        setResult(RESULT_OK, intent);

        // PASUL 5b: închide activitatea.
        // DE CE DUPĂ `setResult`: `finish()` programează închiderea și predarea
        //   rezultatului. Invers, rezultatul ar putea pleca înainte să fie setat.
        // `finish()` NU distruge imediat activitatea: ea trece prin onPause -> onStop
        //   -> onDestroy, iar MainActivity revine în prim-plan.
        finish();
    }

    // ==================================================================================
    // showReleaseDatePicker — DIALOGUL DE DATĂ
    // ==================================================================================
    // Fluxul:
    //   1. citește textul curent din câmp
    //   2. dacă nu e gol, îl parsează și actualizează `releaseCal`
    //   3. deschide dialogul poziționat pe valorile din `releaseCal`
    //   4. la confirmare, scrie în `releaseCal` și în text
    // ==================================================================================
    private void showReleaseDatePicker() {

        // Textul curent, ca String. Poate fi „" (câmp gol) sau „16/07/2010".
        String current = etRelease.getText().toString();

        // Doar dacă există text încercăm parsarea. Un string gol ar arunca
        // `ParseException` și ar fi prins mai jos — dar verificarea este mai ieftină.
        if (!current.isEmpty()) {
            try {

                // `parse` întoarce `Date` sau aruncă `ParseException`.
                // Poate întoarce și `null` dacă textul este gol — de aici verificarea.
                Date parsed = dateFormat.parse(current);

                // `setTime(Date)` copiază milisecundele în Calendar.
                if (parsed != null)
                    releaseCal.setTime(parsed);

            // `catch` GOL, cu nume `ignored`.
            // ==========================================================================
            // DE CE ESTE ACCEPTABIL: dacă textul nu este o dată validă, comportamentul
            //   dorit este „pornim de la data anterioară". Nu există nimic de raportat
            //   și nimic de reparat — dialogul se deschide oricum.
            // CÂND NU ar fi acceptabil: dacă eroarea ar trebui arătată utilizatorului
            //   sau dacă ar ascunde o problemă de logică.
            // ==========================================================================
            } catch (ParseException ignored) { }
        }

        // Construiește dialogul. Parametri:
        //   1. `this` — Context-ul
        //   2. listener-ul de confirmare (clasă anonimă)
        //   3. anul inițial
        //   4. luna inițială — 0-INDEXATĂ, exact ce așteaptă `Calendar`
        //   5. ziua inițială — 1-indexată
        // `.show()` — fără el, dialogul este construit dar nu apare.
        new DatePickerDialog(
                this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int day) {

                        // Scrie în Calendar. ATENȚIE: `set(an, lună, zi)` NU resetează
                        // ora. Ora rămâne cea din `Calendar.getInstance()` — adică
                        // momentul în care a fost creat formularul.
                        // Consecință: două filme salvate în aceeași zi au Date-uri
                        // diferite la nivel de milisecunde. Pentru afișare (dd/MM/yyyy)
                        // nu contează.
                        releaseCal.set(year, month, day);

                        // Șterge un eventual mesaj de eroare de pe câmp.
                        // `setError` nu este apelat nicăieri în acest fișier, deci
                        // linia este defensivă — utilă dacă mai târziu se adaugă validare.
                        etRelease.setError(null);

                        // Scrie textul formatat în câmp. Aici se vede legătura:
                        // Calendar -> String -> EditText. La salvare, drumul este invers:
                        // EditText -> (ignorat) ; Calendar -> Date -> model.
                        etRelease.setText(dateFormat.format(releaseCal.getTime()));
                    }
                },
                // Valorile inițiale ale dialogului, citite din Calendar.
                // `Calendar.MONTH` este deja 0-indexat, deci se passează direct.
                releaseCal.get(Calendar.YEAR),
                releaseCal.get(Calendar.MONTH),
                releaseCal.get(Calendar.DAY_OF_MONTH)
        ).show();
    }
}
