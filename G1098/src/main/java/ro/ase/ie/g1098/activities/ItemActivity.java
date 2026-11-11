/*
 * ======================================================================================
 * FIȘIER: ItemActivity.java — FORMULARUL DE INSERT ȘI EDIT
 * ======================================================================================
 * CE ESTE: a doua activitate. Afișează un formular cu 13 controale, citește valorile
 *   și le împachetează într-un `Movie`, pe care îl trimite înapoi în MainActivity.
 *
 * DE CE EXISTĂ: MainActivity ține lista, dar nu are loc pe ecran pentru un formular.
 *   Separarea în două activități este modelul clasic „listă + detaliu".
 *
 * CE ȘTIE: cele 13 widget-uri, un `Calendar` care este sursa de adevăr pentru dată,
 *   un `SimpleDateFormat`, poziția din listă (-1 = insert) și cheile extra.
 *
 * CE NU FACE: nu ține lista, nu știe ce este înainte sau după filmul editat, nu
 *   salvează nimic pe disc. Produce un `Movie` și îl predă.
 *
 * CE S-AR RUPE FĂRĂ EL: butonul „+" ar deschide o activitate inexistentă, iar
 *   click-ul pe un rând ar arunca `ActivityNotFoundException`.
 * ======================================================================================
 */

// ======================================================================================
// PACHETUL
// ======================================================================================
// Același pachet ca MainActivity: `activities`. De aceea `MainActivity.MOVIE_EXTRA`
// este vizibil aici fără import — `protected` + același pachet.
// ======================================================================================
package ro.ase.ie.g1098.activities;

// ======================================================================================
// IMPORTURI — TOATE FOLOSITE
// ======================================================================================
// Diferența față de G1097: acolo existau patru importuri moarte (`Switch`, `Toast`,
// `Snackbar`, `ActivityItemBinding`). Aici fiecare import corespunde unui nume scris
// în fișier. Verificarea este mecanică: caută numele în corpul clasei.
// ======================================================================================

// DatePickerDialog: dialogul de alegere a datei.
import android.app.DatePickerDialog;

// Intent: obiectul în care punem rezultatul.
import android.content.Intent;

// Bundle: doar în semnătura lui onCreate.
import android.os.Bundle;

// View: tipul parametrului din onClick și clasa de bază a widget-urilor.
import android.view.View;

// Button: butonul „Save Movie" / „Update Movie".
import android.widget.Button;

// CheckBox: bifa „Watchlist". Tipul declarat este CheckBox, deși widget-ul din XML
//   este MaterialCheckBox — care extinde CheckBox.
import android.widget.CheckBox;

// CompoundButton: interfața pentru switch/checkbox; `OnCheckedChangeListener` stă aici.
import android.widget.CompoundButton;

// DatePicker: parametrul din onDateSet.
import android.widget.DatePicker;

// EditText: titlu, dată, buget, poster.
import android.widget.EditText;

// RadioButton: butoanele din grupul de ghidaj.
import android.widget.RadioButton;

// RadioGroup: containerul care garantează selecția unică.
import android.widget.RadioGroup;

// RatingBar: stelele.
import android.widget.RatingBar;

// SeekBar: slider-ul de durată.
import android.widget.SeekBar;

// Spinner: lista de genuri.
import android.widget.Spinner;

// TextView: cele două etichete dinamice (durata și valoarea switch-ului).
import android.widget.TextView;

// EdgeToEdge: desenare sub barele de sistem.
import androidx.activity.EdgeToEdge;

// NonNull: adnotare pe parametrul lui onCheckedChanged. Nu schimbă comportamentul;
//   documentează că Android garantează un obiect non-null.
import androidx.annotation.NonNull;

// AppCompatActivity: clasa de bază.
import androidx.appcompat.app.AppCompatActivity;

// IntentCompat: variantă compatibilă a `getSerializableExtra`, cu tip explicit.
import androidx.core.content.IntentCompat;

// Insets / ViewCompat / WindowInsetsCompat: aceleași ca în MainActivity.
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// MaterialSwitch: switch-ul Material. NU `android.widget.Switch` — aceea este altă clasă.
import com.google.android.material.materialswitch.MaterialSwitch;

// ParseException: excepția aruncată de `SimpleDateFormat.parse`.
import java.text.ParseException;

// SimpleDateFormat: conversia Date <-> String.
import java.text.SimpleDateFormat;

// Calendar: câmpurile de dată și sursa de adevăr pentru release.
import java.util.Calendar;

// Date: tipul întors de `releaseCal.getTime()` și de `dateFormat.parse(...)`.
import java.util.Date;

// Locale: formatul regional pentru date.
import java.util.Locale;

// R și Movie.
import ro.ase.ie.g1098.R;
import ro.ase.ie.g1098.model.Movie;

/*
 * ======================================================================================
 * BLOC DE TEORIE: FORMULARUL CA MAȘINĂ DE STARE
 * ======================================================================================
 *
 * 1. **Două moduri, o singură activitate.** `position == -1` înseamnă INSERT;
 *    `position >= 0` înseamnă EDIT. Nu există două activități separate, pentru că
 *    formularul este identic; diferă doar valorile inițiale și eticheta butonului.
 *
 * 2. **Widget-urile sunt sursa de adevăr pentru 9 câmpuri, Calendar pentru unul.**
 *    Titlul se citește din `etTitle`, bugetul din `etBudget` etc. Data NU se citește
 *    din `etRelease`: textul este doar o AFIȘARE, valoarea reală stă în `releaseCal`.
 *
 *        [etRelease]  --(afișare)-->  "16/07/2010"
 *        [releaseCal] --(valoare)-->  Date  --> saveMovie() --> Movie
 *
 * 3. **Rezultatul se întoarce prin Intent.** `setResult(RESULT_OK, intent)` +
 *    `finish()` este perechea obligatorie. Fără `finish()`, activitatea rămâne
 *    deschisă; fără `setResult`, MainActivity primește `RESULT_CANCELED`.
 *
 * 4. **Validarea lipsește.** Formularul nu verifică nimic: bugetul gol crapă,
 *    data null crapă, ghidajul nemarcat lasă `approval` null. Sunt bug-uri reale,
 *    marcate mai jos cu `BUG:`.
 *
 * CE SE ÎNTÂMPLĂ LA FIECARE APĂSARE:
 *
 *   etRelease click      -> showReleaseDatePicker() -> dialog -> releaseCal + text
 *   btnMovieAction click -> saveMovie() -> Intent -> setResult -> finish
 *   swWatched toggle     -> tvWatchedValue.setText("Yes"/"No")
 *   sbDuration drag      -> tvDurationValue.setText(progress + " min.")
 * ======================================================================================
 */
public class ItemActivity extends AppCompatActivity {

    // ==================================================================================
    // CELE 13 WIDGET-URI — câmpuri, legate în initControls()
    // ==================================================================================
    // Toate sunt `private`: nimeni din afară nu are voie să le atingă. Toate sunt
    //   legate cu `findViewById` în aceeași metodă, ca o greșeală de ID să fie
    //   vizibilă într-un singur loc.
    // ==================================================================================

    // 1. Titlul — TextInputEditText.
    private EditText etTitle;

    // 2. Data — TextInputEditText, dar NU se citește din el (vezi releaseCal).
    private EditText etRelease;

    // 3. Genul — Spinner cu `@array/movie_genre`.
    private Spinner spGenre;

    // 4. Bugetul — TextInputEditText cu inputType=numberDecimal.
    private EditText etBudget;

    // 5. Durata — SeekBar 0..300.
    private SeekBar sbDuration;

    // 6. Rating — RatingBar cu 5 stele și pas 0.5.
    private RatingBar rbRating;

    // 7. Ghidajul — RadioGroup cu 5 butoane.
    private RadioGroup rgApproval;

    // 8. Bifa Watchlist — ID `cbOnWatchList`, câmp `cbonWatchList`.
    private CheckBox cbonWatchList;

    // 9. Switch-ul — MaterialSwitch, NU android.widget.Switch.
    private MaterialSwitch swWatched;

    // 10. Posterul — TextInputEditText cu inputType=textUri.
    private EditText etPoster;

    // 11. Butonul de acțiune — „Save Movie" sau „Update Movie".
    private Button btMovieAction;

    // 12. Eticheta care arată durata curentă.
    private TextView tvDurationValue;

    // 13. Eticheta care arată „Yes"/„No" pentru switch.
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
    // `Locale.getDefault()` = formatul limbii dispozitivului.
    // `final`: referința nu se schimbă. Obiectul, însă, este MUTABIL.
    // RISC: `SimpleDateFormat` nu este thread-safe. Aici este folosit exclusiv pe
    //   UI thread, deci nu există concurență.
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    // ==================================================================================
    // onCreate — CONSTRUIREA FORMULARULUI
    // ==================================================================================
    // CE FACE: leagă layout-ul, leagă widget-urile, instalează listenerii și apoi
    //   verifică dacă a fost pornită pentru edit.
    // CUM: initControls -> initListeners -> citirea Intent-ului -> fillForm.
    // RISC: ordinea contează. `fillForm` atinge widget-uri, deci trebuie să ruleze
    //   după `initControls`. Listenerii trebuie instalați înainte de `fillForm`,
    //   altfel `setChecked` ar declanșa listener-ul? Nu: listener-ul se instalează
    //   înainte, iar `setChecked` ÎL DECLANȘEAZĂ — de aceea eticheta „Yes"/„No"
    //   se actualizează corect la editare.
    // REF: capitolul 22, secțiunea 22.3.
    // ==================================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // 1. Lanțul către superclasă, obligatoriu primul.
        super.onCreate(savedInstanceState);

        // 2. Edge-to-edge.
        EdgeToEdge.enable(this);

        // 3. Layout-ul formularului.
        setContentView(R.layout.activity_item);

        // 4. Insets pe rădăcină. `R.id.main` este ScrollView-ul din XML.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 5. Widget-urile, înainte de orice le atinge.
        initControls();

        // 6. Listenerii, înainte de fillForm (ca setChecked să actualizeze eticheta).
        initListeners();

        // 7. Intent-ul care a pornit activitatea. Poate conține un film (edit)
        //    sau poate fi gol (insert).
        Intent intent = getIntent();

        // 8. Citirea filmului. `IntentCompat.getSerializableExtra` este varianta
        //    tipată: al treilea argument este clasa, deci nu mai este nevoie de cast.
        //    Rezultatul este O COPIE deserializată, nu originalul din listă.
        Movie existing = IntentCompat.getSerializableExtra(intent, MainActivity.MOVIE_EXTRA, Movie.class);

        // 9. Dacă există un film, suntem în modul EDIT.
        if (existing != null) {

            // 9a. Poziția din listă. Dacă extra-ul lipsește, rămâne -1 — dar
            //     în modul edit el este mereu trimis de MainActivity.
            position = intent.getIntExtra(MainActivity.POSITION_EXTRA, -1);

            // 9b. Umplem formularul cu valorile filmului.
            fillForm(existing);

            // 9c. Titlul ecranului (bara de sus).
            setTitle("Edit movie");

            // 9d. Eticheta butonului.
            btMovieAction.setText("Update Movie");
        }
    }

    // ==================================================================================
    // fillForm — MODEL -> WIDGET-URI
    // ==================================================================================
    // CE FACE: copiază cele 10 câmpuri ale unui `Movie` în cele 13 controale.
    // CUM: 9 setări directe + 2 bucle de potrivire (Spinner, RadioGroup).
    // RISC: `existing.getRelease()` poate fi `null` -> `dateFormat.format(null)`
    //   aruncă NPE. `getPosterUrl()` poate fi `null` -> `setText(null)` este legal.
    // REF: perechea acestei metode este `saveMovie` — fiecare setare de aici are
    //   un echivalent invers acolo.
    // ==================================================================================
    private void fillForm(Movie existing) {

        // 1. Titlul: String -> EditText.
        etTitle.setText(existing.getTitle());

        // 2. Data: Date -> String formatat. ATENȚIE: `releaseCal` NU este actualizat
        //    aici. Vezi BUG 3 mai jos.
        etRelease.setText(dateFormat.format(existing.getRelease()));

        // 3. Bugetul: double -> String. `String.valueOf` evita `"" + budget`.
        etBudget.setText(String.valueOf(existing.getBudget()));

        // 4. Durata: int -> progres pe SeekBar.
        sbDuration.setProgress(existing.getDuration());

        // 5. Bifa Watchlist.
        cbonWatchList.setChecked(existing.isOnWatchlist());

        // 6. Switch-ul. `setChecked` declanșează listener-ul instalat în onCreate,
        //    deci `tvWatchedValue` primește „Yes"/„No" fără cod suplimentar.
        swWatched.setChecked(existing.isWatched());

        // 7. Rating-ul: float -> stele.
        rbRating.setRating(existing.getRating());

        // 8. Posterul: String -> EditText. Poate fi `null`; `setText(null)` afișează gol.
        etPoster.setText(existing.getPosterUrl());

        // 9. Genul: căutare în Spinner după TEXT.
        //    `getCount()` = numărul de elemente din `@array/movie_genre`.
        for(int index=0; index<spGenre.getCount(); index++)
        {
            // `getItemAtPosition(index)` întoarce `Object`; `.equals` compară textele.
            if(spGenre.getItemAtPosition(index).equals(existing.getGenre()))
            {
                // Am găsit genul: îl selectăm și ieșim din buclă.
                spGenre.setSelection(index);

                // Fără `break`, am continua să căutăm și am putea selecta alt element
                // dacă lista ar conține duplicate.
                break;
            }
        }

        // 10. Ghidajul: căutare în RadioGroup după TEXTUL butonului.
        //     `getChildCount()` = 5. `getChildAt(index)` întoarce `View`.
        for(int index=0; index<rgApproval.getChildCount(); index++)
        {
            // Cast la RadioButton pentru a putea citi textul.
            RadioButton rb = (RadioButton) rgApproval.getChildAt(index);

            // Potrivire pe text: „PG-13" din model == „PG-13" din XML.
            if(rb.getText().toString().equals(existing.getApproval()))
            {
                // `check` vrea ID-ul, nu indexul. `check` debifează automat restul.
                rgApproval.check(rgApproval.getChildAt(index).getId());

                // Un singur buton poate fi selectat; ieșim.
                break;
            }
        }
    }

    /*
     * ==============================================================================
     * BLOC DE TEORIE: findViewById vs ViewBinding
     * ==============================================================================
     *
     * 1. `findViewById(R.id.x)` caută în arborele de view-uri la RUNTIME și întoarce
     *    `View`. Cast-ul la tipul concret este implicit prin generică, dar nu este
     *    verificat de compilator: un ID greșit sau un tip greșit dă eroare abia la
     *    runtime.
     *
     * 2. ViewBinding generează o clasă `ActivityItemBinding` cu un câmp pentru fiecare
     *    ID. `binding.etTitle` este verificat la COMPILARE. În plus, nu mai e nevoie
     *    de 13 linii de `findViewById`.
     *
     * 3. De ce nu este folosit aici: la acest laborator se predă metoda clasică.
     *    ViewBinding apare în DMA05, iar cursul îl tratează separat.
     *
     * 4. Amestecul este permis, dar costă: dacă folosești binding pentru 12 câmpuri
     *    și findViewById pentru al 13-lea, ai două mecanisme de întreținut.
     * ==============================================================================
     */

    // ==================================================================================
    // initControls — CELE 13 findViewById
    // ==================================================================================
    // CE FACE: leagă fiecare câmp de widget-ul cu ID-ul corespunzător.
    // CUM: `findViewById(R.id.x)` pentru fiecare.
    // RISC: un ID greșit întoarce `null` FĂRĂ eroare de compilare. NPE-ul apare la
    //   primul acces, undeva departe. De aceea toate sunt grupate aici.
    // REF: ID-urile din `activity_item.xml`.
    // ==================================================================================
    private void initControls() {

        // 1. Titlu.
        etTitle = findViewById(R.id.etTitle);

        // 2. Data.
        etRelease = findViewById(R.id.etRelease);

        // 3. Genul.
        spGenre = findViewById(R.id.spGenre);

        // 4. Bugetul.
        etBudget = findViewById(R.id.etBudget);

        // 5. Durata.
        sbDuration = findViewById(R.id.sbDuration);

        // 6. Rating-ul.
        rbRating = findViewById(R.id.rbRating);

        // 7. Ghidajul.
        rgApproval = findViewById(R.id.rgApproval);

        // 8. Bifa. ID-ul are „On" mare, câmpul are „on" mic: `cbOnWatchList` vs
        //    `cbonWatchList`. O greșeală de tastare aici dă NPE la fillForm.
        cbonWatchList = findViewById(R.id.cbOnWatchList);

        // 9. Switch-ul.
        swWatched = findViewById(R.id.swWatched);

        // 10. Posterul.
        etPoster = findViewById(R.id.etPoster);

        // 11. Butonul. ID-ul este `btnMovieAction`, câmpul `btMovieAction`.
        btMovieAction = findViewById(R.id.btnMovieAction);

        // 12. Eticheta de durată.
        tvDurationValue = findViewById(R.id.tvDurationValue);

        // 13. Eticheta de switch.
        tvWatchedValue = findViewById(R.id.tvWatchedValue);
    }

    // ==================================================================================
    // initListeners — CEI 4 LISTENERI
    // ==================================================================================
    // CE FACE: leagă evenimentele de metode.
    // CUM: click pe dată, click pe buton, schimbare switch, mișcare slider.
    // RISC: listener-ii sunt clase anonime; `this` din interior NU este activitatea.
    // REF: `saveMovie()` și `showReleaseDatePicker()` sunt metodele apelate.
    // ==================================================================================
    private void initListeners() {

        // 1. Click pe câmpul de dată -> dialog.
        //    `EditText` nu are eveniment „click" dedicat; `OnClickListener` funcționează
        //    pentru că EditText este un View.
        etRelease.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showReleaseDatePicker();
            }
        });

        // 2. Click pe buton -> salvare.
        btMovieAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveMovie();
            }
        });

        // 3. Switch -> eticheta „Yes"/„No".
        //    `OnCheckedChangeListener` este apelat și când schimbarea vine din COD
        //    (`setChecked` în fillForm), nu doar de la utilizator.
        swWatched.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull CompoundButton buttonView, boolean isChecked) {
                // Ternary: o singură expresie pentru ambele stări.
                tvWatchedValue.setText(isChecked ? "Yes" : "No");
            }
        });

        // 4. Slider -> eticheta de durată.
        sbDuration.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            // 4a. La fiecare schimbare de progres — inclusiv când vine din cod.
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // `progress` este int; concatenarea cu String îl convertește automat.
                tvDurationValue.setText(progress + " min.");
            }

            // 4b. La atingerea sliderului. Nu avem nevoie de nimic aici.
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            // 4c. La ridicarea degetului. Nu avem nevoie de nimic aici.
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    // ==================================================================================
    // saveMovie — WIDGET-URI -> MODEL -> INTENT
    // ==================================================================================
    // CE FACE: construiește un `Movie` din valorile formularului, îl pune într-un
    //   Intent și îl trimite înapoi în MainActivity.
    // CUM: 1) new Movie(); 2) 9 setter-e; 3) buclă pentru ghidaj; 4) putExtra;
    //   5) setResult + finish.
    // RISC: `Double.parseDouble("")` aruncă `NumberFormatException`; `releaseCal`
    //   poate conține o dată veche (vezi BUG 3).
    // REF: perechea inversă a lui `fillForm`.
    // ==================================================================================
    private void saveMovie() {

        // 1. Obiect gol. Aici se vede de ce constructorul fără parametri este obligatoriu.
        Movie movie = new Movie();

        // 2. Titlul, direct din text.
        movie.setTitle(etTitle.getText().toString());

        // 3. Data — din CALENDAR, nu din text.
        movie.setRelease(releaseCal.getTime());

        // 4. Genul — textul selectat în Spinner.
        movie.setGenre(spGenre.getSelectedItem().toString());

        // 5. Bugetul — parsare. BUG 1: dacă utilizatorul lasă câmpul gol,
        //    `Double.parseDouble("")` aruncă `NumberFormatException` și aplicația se închide.
        movie.setBudget(Double.parseDouble(etBudget.getText().toString()));

        // 6. Durata — direct din progres.
        movie.setDuration(sbDuration.getProgress());

        // 7. Rating-ul.
        movie.setRating(rbRating.getRating());

        // 8. Switch-ul.
        movie.setWatched(swWatched.isChecked());

        // 9. Bifa.
        movie.setOnWatchlist(cbonWatchList.isChecked());

        // 10. Posterul. Poate fi un string gol dacă utilizatorul nu a scris nimic.
        movie.setPosterUrl(etPoster.getText().toString());

        // 11. Ghidajul: parcurgem grupul și luăm textul butonului bifat.
        for(int index=0; index<rgApproval.getChildCount(); index++)
        {
            RadioButton rb = (RadioButton) rgApproval.getChildAt(index);

            // `isChecked()` — un singur buton poate fi bifat în RadioGroup.
            if(rb.isChecked())
            {
                movie.setApproval(rb.getText().toString());

                // Am găsit butonul; nu mai căutăm.
                break;
            }
        }

        // 12. Intent FĂRĂ target. Nu pornim o activitate; doar transportăm date
        //     înapoi. `new Intent()` este suficient pentru `setResult`.
        Intent intent = new Intent();

        // 13. Obiectul, serializat automat.
        intent.putExtra(MainActivity.MOVIE_EXTRA, movie);

        // 14. Poziția, DOAR în modul edit. În insert, cheia lipsește, iar
        //     `getIntExtra(..., -1)` din MainActivity întoarce -1.
        if (position != -1)
            intent.putExtra(MainActivity.POSITION_EXTRA, position);

        // 15. Rezultatul. Fără acest apel, MainActivity ar primi RESULT_CANCELED.
        setResult(RESULT_OK, intent);

        // 16. Închiderea activității. Fără ea, formularul rămâne pe ecran.
        finish();
    }

    // ==================================================================================
    // showReleaseDatePicker — DIALOGUL DE DATĂ
    // ==================================================================================
    // CE FACE: deschide un `DatePickerDialog` poziționat pe data din `releaseCal`.
    // CUM: 1) încearcă să parseze textul curent; 2) deschide dialogul; 3) la confirmare,
    //   scrie în calendar și în text.
    // RISC: textul tastat manual nu ajunge în calendar decât dacă dialogul este deschis.
    // REF: singurul loc care scrie în `releaseCal` după inițializare.
    // ==================================================================================
    private void showReleaseDatePicker() {

        // 1. Textul curent din câmp.
        String current = etRelease.getText().toString();

        // 2. Dacă nu este gol, încearcă să îl transforme în Date.
        if (!current.isEmpty()) {
            try {
                // `parse` întoarce `null` dacă textul nu poate fi interpretat.
                Date parsed = dateFormat.parse(current);

                // Dacă avem o dată validă, o punem în calendar.
                if (parsed != null)
                    releaseCal.setTime(parsed);

            // 3. Text invalid: ignorăm. Dialogul se deschide pe data veche.
            } catch (ParseException ignored) { }
        }

        // 4. Dialogul. Constructorul cere: Context, listener, an, lună, zi.
        new DatePickerDialog(
                this,

                // 4a. Listener-ul de confirmare.
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int day) {

                        // Scriem în calendar. `month` este 0-based, exact cum îl vrea Calendar.
                        releaseCal.set(year, month, day);

                        // Curățăm o eventuală eroare afișată pe câmp.
                        etRelease.setError(null);

                        // Actualizăm textul afișat din calendar.
                        etRelease.setText(dateFormat.format(releaseCal.getTime()));
                    }
                },

                // 4b. Valorile inițiale: din calendar, nu din text.
                releaseCal.get(Calendar.YEAR),
                releaseCal.get(Calendar.MONTH),
                releaseCal.get(Calendar.DAY_OF_MONTH)

        // 5. `show()` — fără el, dialogul este construit dar nu apare.
        ).show();
    }
}
