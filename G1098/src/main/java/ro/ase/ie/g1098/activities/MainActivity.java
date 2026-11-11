/*
 * ======================================================================================
 * FIȘIER: MainActivity.java — ECRANUL CU LISTA
 * ======================================================================================
 * CE ESTE: activitatea de start. Ține lista de filme, o afișează cu un `ArrayAdapter`
 *   într-un `ListView` și pornește `ItemActivity` pentru insert sau edit.
 *
 * DE CE EXISTĂ: este singurul loc care deține STAREA aplicației (lista `movies`).
 *   `ItemActivity` este doar un formular care produce un `Movie` și îl trimite înapoi.
 *
 * CE ȘTIE: lista, adapterul, cele două extra-uri, launcher-ul de rezultat.
 *   Nu știe cum arată formularul și nu validează date.
 *
 * CE NU FACE: nu salvează nimic pe disc. Lista trăiește cât trăiește procesul.
 *   La rotirea ecranului, activitatea este recreată și `addSampleMovies()` rulează
 *   din nou — filmele adăugate se pierd. Persistența vine în laboratorul următor.
 *
 * CE S-AR RUPE FĂRĂ EL: aplicația ar porni direct într-un formular fără listă,
 *   iar rezultatul salvat nu ar avea unde să se întoarcă.
 * ======================================================================================
 */

// ======================================================================================
// PACHETUL
// ======================================================================================
// `activities` ține ecranele. Modelul stă în `model`, iar `R` în rădăcina pachetului.
// ======================================================================================
package ro.ase.ie.g1098.activities;

// ======================================================================================
// IMPORTURI — GRUPATE PE SURSĂ
// ======================================================================================
// android.*        — clase din framework (Intent, Bundle, View, widget-uri)
// androidx.*       — biblioteci Jetpack (AppCompat, EdgeToEdge, insets, activity result)
// com.google.*     — Material Components (FloatingActionButton)
// java.util.*      — colecții și date
// ro.ase.ie.g1098  — modelul și clasa R generată
//
// OBSERVAȚIE DE IGIENĂ: singurul import nefolosit din fișier este `android.widget.Toast`.
//   G1097 mai are unul: `java.util.Date`. Ambele sunt inofensive la runtime — un import
//   nu ajunge în bytecode — dar sugerează o dependență care nu există.
// ======================================================================================

// Intent: obiectul care descrie „ce activitate pornești" și ce date duce cu ea.
import android.content.Intent;

// Bundle: sacul de stare salvat de sistem (folosit aici doar în semnătura lui onCreate).
import android.os.Bundle;

// View: clasa de bază pentru orice element de UI. Apare în semnăturile listener-ilor.
import android.view.View;

// AdapterView: clasa de bază pentru ListView/Spinner. Aici doar pentru OnItemClickListener.
import android.widget.AdapterView;

// ArrayAdapter: puntea dintre `List<Movie>` și rândurile din ListView.
import android.widget.ArrayAdapter;

// ListView: widget-ul care afișează lista derulabilă.
import android.widget.ListView;

// Toast: mesaje scurte. Importat în sursă, dar NEUTILIZAT în acest fișier.
//   Este singurul import mort al grupei G1098 în MainActivity.
import android.widget.Toast;

// EdgeToEdge: activează desenarea sub barele de sistem (status + navigare).
import androidx.activity.EdgeToEdge;

// ActivityResult: rezultatul întors de activitatea secundară (cod + Intent).
import androidx.activity.result.ActivityResult;

// ActivityResultCallback: interfața cu o singură metodă, apelată la întoarcere.
import androidx.activity.result.ActivityResultCallback;

// ActivityResultLauncher: obiectul care pornește activitatea și ține legătura cu callback-ul.
import androidx.activity.result.ActivityResultLauncher;

// ActivityResultContracts: contractele predefinite (aici, StartActivityForResult).
import androidx.activity.result.contract.ActivityResultContracts;

// AppCompatActivity: activitatea de bază cu suport pentru teme Material și ActionBar.
import androidx.appcompat.app.AppCompatActivity;

// Insets: valorile în pixeli ale barelor care acoperă conținutul.
import androidx.core.graphics.Insets;

// ViewCompat: versiunea compatibilă a API-urilor de View (insets pe orice API).
import androidx.core.view.ViewCompat;

// WindowInsetsCompat: descrierea insets-urilor, independent de versiunea Android.
import androidx.core.view.WindowInsetsCompat;

// FloatingActionButton: butonul rotund din colțul dreapta-jos.
import com.google.android.material.floatingactionbutton.FloatingActionButton;

// ArrayList: implementarea concretă a listei.
import java.util.ArrayList;

// Calendar: câmpurile de dată (YEAR, MONTH, DAY_OF_MONTH) și constantele lunilor.
import java.util.Calendar;

// GregorianCalendar: constructorul de dată folosit în addSampleMovies.
//   `new GregorianCalendar(an, luna, zi).getTime()` produce un `Date` direct.
import java.util.GregorianCalendar;

// List: tipul declarat al câmpului `movies` (interfața, nu implementarea).
import java.util.List;

// R: clasa generată de build care mapăează numele din XML la întregi.
import ro.ase.ie.g1098.R;

// Movie: modelul de date.
import ro.ase.ie.g1098.model.Movie;

/*
 * ======================================================================================
 * BLOC DE TEORIE: CINE ȚINE STAREA ȘI CINE O AFIȘEAZĂ
 * ======================================================================================
 *
 * 1. **Un singur proprietar al listei.** `movies` este `final` și este singura listă
 *    din aplicație. `ItemActivity` nu are listă; primește un `Movie` și întoarce un
 *    `Movie`. Regula „o singură sursă de adevăr" previne liste care se contrazic.
 *
 * 2. **Adapterul nu copiază lista.** `new ArrayAdapter<>(this, layout, movies)` primește
 *    REFERINȚA la listă. Adapterul citește din ea la fiecare `getView`. De aceea
 *    `movies.add(...)` se vede imediat în adapter — dar ListView-ul nu redesenează
 *    decât după `notifyDataSetChanged()`.
 *
 * 3. **Ordinea din onCreate este contract.** `addSampleMovies()` trebuie să ruleze
 *    înainte de crearea adapterului, ca lista să aibă conținut la primul bind.
 *    `initControls()` trebuie să ruleze înainte de `initListeners()`, ca
 *    `fabInsert` și `listView` să fie deja legate când li se pun listeneri.
 *
 * 4. **Launcher-ul se înregistrează în onCreate, nu în listener.** `registerForActivityResult`
 *    trebuie apelat înainte ca activitatea să ajungă în starea STARTED. Apelat dintr-un
 *    `onClick`, ar arunca `IllegalStateException`.
 *
 * CICLUL DE VIAȚĂ, PE SCURT:
 *
 *   onCreate  -> super, EdgeToEdge, setContentView, insets, date, adapter,
 *                controls, listeners, launcher
 *   onStart   -> activitatea devine vizibilă
 *   onResume  -> primește input
 *   onPause   -> pierde input (ItemActivity apare peste ea)
 *   onStop    -> nu mai este vizibilă
 *   onDestroy -> distrusă (rotire, back, proces omorât)
 *
 *   La întoarcerea din ItemActivity: onStart -> onResume -> callback-ul launcher-ului.
 * ======================================================================================
 */
public class MainActivity extends AppCompatActivity {

    // ==================================================================================
    // CHEILE EXTRA — CONTRACTUL DINTRE CELE DOUĂ ACTIVITĂȚI
    // ==================================================================================
    // `protected static final`: `static` pentru că nu depinde de instanță, `protected`
    //   ca să fie vizibil din ItemActivity (același pachet `activities`), `final`
    //   pentru că valoarea nu se schimbă niciodată.
    // VALOAREA este un String oarecare: „position" și „movie". Convenția este să fie
    //   unică în aplicație; altfel două extra-uri cu aceeași cheie se suprascriu.
    // ==================================================================================
    protected static final String POSITION_EXTRA = "position";
    protected static final String MOVIE_EXTRA = "movie";

    // Butonul „+". Tipul concret este FloatingActionButton, nu View: avem nevoie
    // doar de setOnClickListener, care există pe orice View, dar tipul concret
    // documentează ce widget este.
    private FloatingActionButton fabInsert;

    // Lista derulantă.
    private ListView listView;

    // Launcher-ul de rezultat. Este atribuit în onCreate, DUPĂ initListeners().
    // `ActivityResultLauncher<Intent>`: pornește un Intent și întoarce un ActivityResult.
    private ActivityResultLauncher<Intent> launcher;

    // LISTA — starea aplicației. `final` înseamnă că referința nu se schimbă;
    // conținutul, da. `List` este interfața; `ArrayList` este implementarea.
    // Inițializarea la declarație garantează că lista există înainte de orice metodă.
    private final List<Movie> movies = new ArrayList<>();

    // Adapterul. Este creat în onCreate, după ce lista are deja cele 3 filme demo.
    private ArrayAdapter<Movie> movieAdapter;

    /*
     * ==============================================================================
     * BLOC DE TEORIE: ACTIVITY RESULT API
     * ==============================================================================
     *
     * 1. Înainte de AndroidX Activity 1.2, rezultatul se lua cu:
     *      startActivityForResult(intent, REQUEST_CODE);
     *      onActivityResult(requestCode, resultCode, data)
     *    Ambele sunt DEPRECATE. Problema: codul de request era un `int` global, ușor
     *    de duplicat, iar metoda `onActivityResult` creștea cu un `switch` pentru
     *    fiecare activitate pornită.
     *
     * 2. API-ul nou leagă un CALLBACK de un CONTRACT, o singură dată:
     *      launcher = registerForActivityResult(contract, callback)
     *      launcher.launch(intent)
     *
     * 3. `StartActivityForResult` este contractul generic: primește un `Intent` și
     *    întoarce un `ActivityResult` (cod + Intent).
     *
     * 4. Înregistrarea trebuie făcută în `onCreate`, înainte de starea STARTED.
     *    Un `launcher` înregistrat prea târziu aruncă `IllegalStateException`.
     *
     * 5. Callback-ul rulează pe UI thread, după `onResume`, când activitatea este
     *    din nou vizibilă. De aceea `notifyDataSetChanged()` este sigur acolo.
     * ==============================================================================
     */

    // ==================================================================================
    // onCreate — LOCUL ÎN CARE SE CONSTRUIEȘTE ECRANUL
    // ==================================================================================
    // CE FACE: leagă layout-ul, populează lista, creează adapterul, leagă widget-urile,
    //   instalează listenerii și înregistrează launcher-ul de rezultat.
    // CUM: în ordinea de mai jos. Ordinea este parte din lecție.
    // RISC: dacă `initListeners()` ar rula înainte de `initControls()`, `fabInsert`
    //   ar fi `null` și `setOnClickListener` ar arunca `NullPointerException`.
    // REF: același schelet în toate grupele; capitolul 22, secțiunea 22.2.
    // ==================================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // 1. Lanțul către superclasă. OBLIGATORIU primul: AppCompatActivity își face
        //    inițializările interne (temă, fragment manager, saved state) aici.
        //    Dacă îl muți mai jos, primești excepții greu de diagnosticat.
        super.onCreate(savedInstanceState);

        // 2. Edge-to-edge: conținutul este desenat sub bara de status și sub cea de
        //    navigare. Fără pasul 4, primele rânduri ar sta sub ceas.
        EdgeToEdge.enable(this);

        // 3. Layout-ul. `R.layout.activity_main` este ID-ul generat pentru fișierul
        //    `res/layout/activity_main.xml`. De aici încolo, `findViewById` poate
        //    găsi widget-urile.
        setContentView(R.layout.activity_main);

        // 4. Insets: măsurăm barele de sistem și le transformăm în padding pe rădăcină.
        //    `findViewById(R.id.main)` este FrameLayout-ul din XML — de aceea ID-ul
        //    `main` este obligatoriu acolo.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {

            // 4a. Cât ocupă barele de sistem, în pixeli.
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            // 4b. Padding pe toate cele patru laturi. Astfel conținutul începe sub
            //     bara de status și se termină deasupra barei de navigare.
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);

            // 4c. `return insets` = „nu am consumat nimic"; copiii primesc mai departe.
            return insets;
        });

        // 5. Datele de demonstrație. Trebuie să ruleze ÎNAINTE de crearea adapterului,
        //    ca lista să aibă conținut la primul bind.
        addSampleMovies();

        // 6. Adapterul. `android.R.layout.simple_list_item_1` este un layout din
        //    framework: un singur TextView. `this` este Context-ul cerut de constructor.
        //    Al treilea argument este lista — adapterul păstrează REFERINȚA, nu o copie.
        movieAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, movies);

        // 7. Legarea widget-urilor la câmpuri.
        initControls();

        // 8. Instalarea listener-ilor. Rulează după initControls, ca widget-urile
        //    să fie deja non-null.
        initListeners();

        // 9. Launcher-ul de rezultat. Se înregistrează O SINGURĂ DATĂ, în onCreate.
        //    `StartActivityForResult` este contractul care spune: „pornesc o activitate
        //    și vreau un rezultat înapoi".
        launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),

                // Callback-ul: o clasă anonimă care implementează o singură metodă.
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult o) {

                        // 9a. Verificăm codul. `RESULT_OK` înseamnă că ItemActivity a
                        //     apelat `setResult(RESULT_OK, intent)`. Dacă utilizatorul
                        //     a apăsat Back, codul este `RESULT_CANCELED` și nu facem nimic.
                        if(o.getResultCode() == RESULT_OK)
                        {
                            // 9b. Intent-ul rezultat. Poate fi `null` teoretic, dar
                            //     ItemActivity îl trimite mereu când codul este OK.
                            Intent intent = o.getData();

                            // 9c. Scoatem obiectul. `getSerializableExtra` întoarce
                            //     `Serializable`, deci cast-ul la `Movie` este obligatoriu.
                            //     Obiectul este O COPIE deserializată, nu originalul.
                            Movie movie = (Movie) intent.getSerializableExtra(MOVIE_EXTRA);

                            // 9d. Poziția. Lipsa extra-ului înseamnă insert, adică -1.
                            int position = intent.getIntExtra(POSITION_EXTRA, -1);

                            // 9e. Ramura de INSERT: poziția lipsește.
                            if(position == -1)
                            {
                                // Adăugăm la finalul listei.
                                movies.add(movie);

                                // Anunțăm adapterul. Fără acest apel, ListView-ul nu
                                // știe că lista are un element nou și nu redesenează.
                                movieAdapter.notifyDataSetChanged();
                            }
                            // 9f. Ramura de EDIT: poziția există.
                            else
                            {
                                // ÎNLOCUIRE, nu adăugare. `set` păstrează ordinea.
                                movies.set(position, movie);

                                // Și aici este nevoie de notificare.
                                movieAdapter.notifyDataSetChanged();
                            }
                        }
                    }
                });
    }

    // ==================================================================================
    // initListeners — CE SE ÎNTÂMPLĂ LA APĂSARE
    // ==================================================================================
    // CE FACE: instalează doi listeneri — unul pe FAB (insert), unul pe rândul de listă
    //   (edit).
    // CUM: `setOnClickListener` pe FAB; `setOnItemClickListener` pe ListView.
    // RISC: listener-ii sunt clase anonime. În interiorul lor, `this` NU este
    //   MainActivity, ci obiectul listener. De aceea se scrie `MainActivity.this`.
    // REF: capitolul 22, secțiunea 22.2.
    // ==================================================================================
    private void initListeners() {

        // 1. FAB -> formular gol (insert).
        fabInsert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Intent EXPLICIT: (Context, Class). Al doilea argument este clasa
                // activității care trebuie pornită. Intent-ul implicit ar căuta
                // o aplicație care știe să facă ceva, nu clasa noastră.
                Intent intent = new Intent(MainActivity.this, ItemActivity.class);

                // Pornirea prin launcher, nu prin `startActivityForResult` (deprecat).
                // Launcher-ul știe ce callback să apeleze la întoarcere.
                launcher.launch(intent);
            }
        });

        // 2. Click pe rând -> formular plin (edit).
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                // 2a. Același Intent explicit.
                Intent intent = new Intent(MainActivity.this, ItemActivity.class);

                // 2b. Obiectul care călătorește. `movies.get(position)` este filmul
                //     de pe rândul apăsat. Serializarea se face automat.
                intent.putExtra(MOVIE_EXTRA, movies.get(position));

                // 2c. Poziția, ca ItemActivity să știe ce să înlocuiască la întoarcere.
                intent.putExtra(POSITION_EXTRA, position);

                // 2d. Pornire.
                launcher.launch(intent);
            }
        });
    }

    // ==================================================================================
    // initControls — FINDVIEWBYID
    // ==================================================================================
    // CE FACE: leagă cele două widget-uri de câmpuri și instalează adapterul pe listă.
    // CUM: `findViewById` cu ID-ul din XML; apoi `setAdapter`.
    // RISC: `findViewById` întoarce `null` dacă ID-ul nu există în layout-ul setat.
    //   Un ID greșit nu dă eroare la compilare — dă `NullPointerException` la runtime.
    // REF: `R.id.floatingActionButton` și `R.id.listView` din `activity_main.xml`.
    // ==================================================================================
    private void initControls() {

        // ID-ul din XML este `floatingActionButton`, iar câmpul se numește `fabInsert`.
        // Numele nu trebuie să coincidă; contează doar ID-ul din `R.id`.
        fabInsert = findViewById(R.id.floatingActionButton);

        // Lista.
        listView = findViewById(R.id.listView);

        // Adapterul se instalează pe listă. Din acest moment, ListView-ul întreabă
        // adapterul ce să afișeze pe fiecare rând.
        listView.setAdapter(movieAdapter);
    }

    // ==================================================================================
    // addSampleMovies — DATELE DE DEMONSTRAȚIE
    // ==================================================================================
    // CE FACE: adaugă trei filme în listă, ca ecranul să nu fie gol la prima pornire.
    // CUM: trei apeluri ale constructorului cu 10 parametri.
    // RISC: ordinea argumentelor este POZIȚIONALĂ. Aici sunt 3 String-uri și 2 boolean-uri,
    //   deci inversarea lor compilează. Verifică tabelul din 30.2.
    // REF: constructorul `Movie(...)` din `Movie.java`.
    // ==================================================================================
    private void addSampleMovies() {

        // FILMUL 1 — Inception.
        // `new GregorianCalendar(an, luna, zi)`: luna este 0-based! `Calendar.JULY` = 6,
        // nu 7. `.getTime()` transformă Calendar în Date, tipul cerut de constructor.
        movies.add(new Movie(
                "Inception",
                new GregorianCalendar(2010, Calendar.JULY, 16).getTime(),
                "Sci-Fi",
                160000000,
                148,
                4.5f,
                true,
                "PG-13",
                false,
                "https://example.com/posters/inception.jpg"));

        // FILMUL 2 — Toy Story.
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

        // FILMUL 3 — The Matrix.
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
}
