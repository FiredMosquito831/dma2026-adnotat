/*
 * ======================================================================================
 * FIȘIER: MainActivity.java — ECRANUL CU LISTA
 * ======================================================================================
 * CE ESTE: activitatea de start. Ține lista de filme, o afișează cu un `ArrayAdapter`
 *   într-un `ListView` și pornește `ItemActivity` pentru insert sau edit.
 *
 * DE CE EXISTĂ: este singurul loc care deține STAREA aplicației (lista `movies`).
 *   `ItemActivity` este doar un formular care produce un `Movie` și îl trimite înapoi.
 *   Fără `MainActivity`, rezultatul salvat nu ar avea unde să se întoarcă.
 *
 * DE CE DEPINDE:
 *   - `activity_main.xml` — pentru `R.id.main`, `R.id.listView`, `R.id.floatingActionButton`.
 *   - `ItemActivity` — clasa pe care o pornește prin `Intent`.
 *   - `Movie` — tipul elementelor din listă.
 *   - `androidx.activity.result.*` — pentru `registerForActivityResult`.
 *
 * CINE DEPINDE DE EL:
 *   `AndroidManifest.xml` îl declară ca LAUNCHER. `ItemActivity` îi citește
 *   constantele `MOVIE_EXTRA` și `POSITION_EXTRA`.
 *
 * CE NU FACE:
 *   - Nu validează nimic: acceptă orice `Movie` întors de formular.
 *   - Nu persistă lista: la rotire sau la moartea procesului, lista se pierde.
 *   - Nu are layout de rând propriu: folosește `android.R.layout.simple_list_item_1`.
 *   - Nu încarcă postere: URL-ul rămâne text în model.
 *
 * CE S-AR RUPE FĂRĂ EL:
 *   Aplicația ar porni direct într-un formular fără listă. `MOVIE_EXTRA` și
 *   `POSITION_EXTRA` nu ar exista, deci `ItemActivity` nu ar compila.
 * ======================================================================================
 */

// Pachetul `activities` — ecranele. Separat de `model`, ca să nu amestece
// tipurile de date cu cele de UI.
package ro.ase.ie.g1096.activities;

// ======================================================================================
// IMPORTURI — GRUPATE PE SURSĂ
// ======================================================================================
// android.*        — clase din framework (Intent, Bundle, View, widget-uri)
// androidx.*       — biblioteci Jetpack (AppCompat, EdgeToEdge, insets, activity result)
// com.google.*     — Material Components (FloatingActionButton)
// java.util.*      — colecții și date
// ro.ase.ie.g1096  — modelul și clasa R generată
//
// ÎN G1096, DOUĂ IMPORTURI SUNT MOARTE: `java.math.BigDecimal` și
// `android.widget.Toast`. În G1097 moarte sunt `java.util.Date` și `Toast`.
// Vezi diferența 29.0.5.
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
// Face parte din diferența 29.0.5 — apare mort în ambele grupe.
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

// BigDecimal: numere zecimale cu precizie EXACTĂ. Importat în sursă, dar NEUTILIZAT.
// DIFERENȚA FAȚĂ DE G1097: acolo acest import nu există; în schimb există `java.util.Date`,
//   și el nefolosit. Aici `Date` nici nu este importat — și nu este nevoie, pentru că
//   numele `Date` nu apare niciodată în fișier; `getTime()` este pasat direct mai departe.
import java.math.BigDecimal;

// ArrayList: implementarea concretă a listei.
import java.util.ArrayList;

// Calendar: câmpurile de dată (YEAR, MONTH, DAY_OF_MONTH) și constantele lunilor.
import java.util.Calendar;

// GregorianCalendar: constructorul rapid „an, lună, zi" folosit în datele demo.
import java.util.GregorianCalendar;

// List: interfața de listă. Câmpul este declarat `List`, nu `ArrayList`.
import java.util.List;

// Clasa R, generată la build din toate fișierele din `res/`. Fără ea, `R.id.listView`
// nu ar exista ca simbol.
import ro.ase.ie.g1096.R;

// Modelul de date — singura dependență către `model`.
import ro.ase.ie.g1096.model.Movie;

/*
 * ======================================================================================
 * BLOC DE TEORIE: CINE DEȚINE STAREA ȘI CINE O AFIȘEAZĂ
 * ======================================================================================
 *
 * 1. UN SINGUR PROPRIETAR AL LISTEI
 *    `movies` este câmp de instanță în `MainActivity`. `ItemActivity` nu are nicio
 *    referință la ea. Formularul produce un `Movie` și îl trimite înapoi prin
 *    `setResult`; abia `MainActivity` decide ce face cu el (`add` sau `set`).
 *    Aceasta este separarea care face fluxul ușor de urmărit.
 *
 * 2. `final` NU ÎNSEAMNĂ IMUTABIL
 *    `private final List<Movie> movies = new ArrayList<>();`
 *    `final` blochează REATRIBUIREA referinței (`movies = new ArrayList<>()` nu
 *    compilează), dar NU blochează modificarea conținutului. `movies.add(...)` și
 *    `movies.set(...)` sunt perfect legale. Este exact ce vrem: aceeași listă,
 *    mereu, cu conținut care se schimbă.
 *
 * 3. ADAPTERUL ȚINE REFERINȚA, NU O COPIE
 *    `new ArrayAdapter<>(this, ..., movies)` primește lista prin referință. De aceea
 *    `movies.add(movie)` este vizibil pentru adapter — dar numai după
 *    `notifyDataSetChanged()`. ListView nu observă lista singur.
 *
 * 4. DOUĂ MODURI, UN SINGUR FORMULAR
 *    - FAB  -> `Intent` FĂRĂ extra-uri -> `ItemActivity` în modul INSERT.
 *    - Rând -> `Intent` CU `MOVIE_EXTRA` și `POSITION_EXTRA` -> modul EDIT.
 *    `ItemActivity` decide modul după `existing != null`, nu după un flag separat.
 *
 * 5. CICLUL DE VIAȚĂ AL LISTEI
 *    Lista trăiește cât activitatea. La rotire, Android distruge și recreează
 *    activitatea, iar `movies` se reinițializează cu cele 3 filme demo. Nu există
 *    `onSaveInstanceState`, deci filmele adăugate se pierd. Vezi RISC mai jos.
 * ======================================================================================
 */

// `extends AppCompatActivity` — activitatea de bază AndroidX. Oferă `setContentView`,
// `findViewById`, `getIntent`, `setResult`, `finish`, `RESULT_OK`.
public class MainActivity extends AppCompatActivity {

    // ==================================================================================
    // CONSTANTELE DE PROTOCOL DINTRE CELE DOUĂ ACTIVITĂȚI
    // ==================================================================================
    // `protected static final` = constantă de clasă, vizibilă în subclase și în pachet.
    // `ItemActivity` este în ACELAȘI pachet (`ro.ase.ie.g1096.activities`), deci ar
    // funcționa și fără `protected`. Este declarat așa din obișnuință.
    //
    // ATENȚIE — CAPCANĂ FINĂ: `static final String` cu valoare literală este o
    // CONSTANTĂ DE COMPILARE. Compilatorul o INLINUIEȘTE în fiecare clasă care o
    // folosește. Dacă schimbi „movie" în „film" și recompilezi doar MainActivity,
    // ItemActivity compilat anterior păstrează „movie" în bytecode. Se rezolvă cu
    // un clean build. La examen nu se cere, dar explică de ce constantele publice
    // se schimbă rar.
    // ==================================================================================
    protected static final String POSITION_EXTRA = "position";
    protected static final String MOVIE_EXTRA = "movie";

    // Butonul rotund „+". Tipul este clasa Material, nu `ImageButton`.
    private FloatingActionButton fabInsert;

    // Lista derulantă. Umple tot ecranul; FAB-ul plutește peste ea.
    private ListView listView;

    // Launcher-ul de rezultat. NU este inițializat aici — primește valoare în `onCreate`,
    // DUPĂ `initListeners()`. Vezi explicația de la `initListeners`.
    private ActivityResultLauncher<Intent> launcher;

    // Lista de filme — singura stare a aplicației.
    // `final` = referința nu se schimbă niciodată; conținutul, da.
    // `new ArrayList<>()` = diamond operator; tipul se deduce din declarație.
    private final List<Movie> movies = new ArrayList<>();

    // Adapterul care leagă lista de ListView. Tipul generic este `Movie`, deci
    // `getItem(position)` întoarce `Movie`, nu `Object`.
    private ArrayAdapter<Movie> movieAdapter;

    // ==================================================================================
    // onCreate — ORDINEA CONTEAZĂ
    // ==================================================================================
    // CE FACE: construiește ecranul, populează lista demo, creează adapterul, leagă
    //   controalele de cod, atașează listenerii și înregistrează launcher-ul.
    // CUM: 1. super 2. EdgeToEdge 3. setContentView 4. insets 5. date demo
    //   6. adapter 7. findViewById 8. listeneri 9. launcher.
    // RISC: fiecare pas depinde de cel dinainte. `findViewById` înainte de
    //   `setContentView` întoarce null; `setAdapter` înainte de `new ArrayAdapter`
    //   ar da NullPointerException.
    // REF: exact aceeași ordine în G1097 (capitolul 22, secțiunea 22.2).
    // ==================================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // 1. Lansează lanțul de inițializare al clasei de bază. OBLIGATORIU primul.
        //    Fără el, `AppCompat` nu își instalează delegate-ul și tema nu se aplică.
        super.onCreate(savedInstanceState);

        // 2. Activează desenarea edge-to-edge: conținutul intră sub status bar și
        //    sub bara de navigare. Consecința: trebuie să ne ocupăm noi de padding,
        //    altfel primul rând de text stă sub ceas.
        EdgeToEdge.enable(this);

        // 3. Umflă arborele de view-uri din `activity_main.xml`. Din acest moment
        //    `findViewById` poate găsi ceva. Înainte de acest apel, întoarce null.
        setContentView(R.layout.activity_main);

        // 4. Corectează padding-ul pentru barele de sistem.
        //    `findViewById(R.id.main)` = FrameLayout-ul rădăcină din XML.
        //    Lambda-ul este `View.OnApplyWindowInsetsListener` (interfață funcțională).
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {

            // 4a. Extrage dimensiunile barelor de sistem: status bar + navigation bar.
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            // 4b. Aplică-le ca padding pe containerul rădăcină. Copiii se înghesuie
            //     automat înăuntru; nu trebuie să îi atingem pe fiecare.
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);

            // 4c. Întoarce `insets` NEconsumat, ca să ajungă și la copii.
            //     Dacă ai returna `WindowInsetsCompat.CONSUMED`, nimeni altcineva
            //     nu ar mai primi informația.
            return insets;
        });

        // 5. Pune în listă cele 3 filme demo. Se face ÎNAINTE de crearea adapterului,
        //    ca adapterul să numere de la început 3 elemente. Dacă ai face-o după,
        //    ar funcționa la fel (adapterul ține referința), dar ar fi nevoie de
        //    `notifyDataSetChanged()` ca să redeseneze.
        addSampleMovies();

        // 6. Creează adapterul.
        //    `this` = Context (activitatea).
        //    `android.R.layout.simple_list_item_1` = layout-ul de rând din FRAMEWORK,
        //      nu din aplicație. Conține un singur TextView cu id `android.R.id.text1`.
        //      De aceea rândul afișează `toString()`-ul modelului.
        //    `movies` = sursa de date, prin referință.
        movieAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, movies);

        // 7. Leagă câmpurile Java de view-urile din XML.
        initControls();

        // 8. Atașează listenerii. Aici `launcher` este încă `null`, dar este OK:
        //    listenerii citesc câmpul abia la CLICK, iar până atunci pasul 9 l-a setat.
        initListeners();

        // 9. Înregistrează contractul de rezultat. TREBUIE apelat înainte ca
        //    activitatea să ajungă în starea STARTED, deci în `onCreate` este locul
        //    corect. Întoarce un `ActivityResultLauncher<Intent>`.
        launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),

                // Callback-ul: o clasă anonimă care implementează interfața cu o metodă.
                // Se execută pe thread-ul principal, după ce activitatea secundară
                // s-a închis și aceasta a revenit în prim-plan.
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult o) {

                        // FILTRUL 1: a întors ceva util? `RESULT_OK` este -1.
                        // Dacă utilizatorul a apăsat Back, codul este RESULT_CANCELED (0)
                        // și nu facem nimic — lista rămâne neatinsă.
                        if(o.getResultCode() == RESULT_OK)
                        {
                            // `getData()` = Intent-ul construit de ItemActivity în `saveMovie()`.
                            // Poate fi null teoretic; aici RESULT_OK garantează că nu este.
                            Intent intent = o.getData();

                            // Scoate obiectul serializat. ATENȚIE: este O COPIE.
                            // `getSerializableExtra(String)` este DEPRECAT din API 33;
                            // varianta modernă este `IntentCompat.getSerializableExtra(...)`,
                            // folosită deja în ItemActivity. Aici a rămas varianta veche —
                            // inconsistență reală între cele două fișiere.
                            Movie movie = (Movie) intent.getSerializableExtra(MOVIE_EXTRA);

                            // A doua informație din Intent: poziția. Default -1 = „nu există".
                            // -1 este convenția aplicației pentru „mod insert".
                            int position = intent.getIntExtra(POSITION_EXTRA, -1);

                            // RAMURA 1: poziția lipsește -> film NOU.
                            if(position == -1)
                            {
                                // Adaugă la sfârșitul listei.
                                movies.add(movie);

                                // Anunță adapterul. Fără acest apel, ListView nu știe
                                // că lista are un element mai mult și nu redesenează.
                                movieAdapter.notifyDataSetChanged();
                            }
                            // RAMURA 2: poziția există -> ÎNLOCUIRE, nu adăugare.
                            else
                            {
                                // `set` suprascrie elementul de pe poziția dată.
                                // Dacă ai folosi `add`, ai avea un duplicat.
                                movies.set(position, movie);
                                movieAdapter.notifyDataSetChanged();
                            }
                        }
                    }
                });
    }

    // ==================================================================================
    // initListeners — CE SE ÎNTÂMPLĂ LA APĂSARE
    // ==================================================================================
    // CE FACE: atașează două comportamente — FAB-ul pornește formularul gol, click-ul
    //   pe rând pornește formularul plin.
    // CUM: 1. `fabInsert.setOnClickListener` 2. `listView.setOnItemClickListener`.
    // RISC: `launcher` este încă null când rulează această metodă. Este sigur pentru
    //   că listenerii sunt executate abia după `onCreate`, când `launcher` are valoare.
    // REF: același pattern de clase anonime ca în `ItemActivity.initListeners()`.
    // ==================================================================================
    private void initListeners() {

        // Clasă anonimă care implementează `View.OnClickListener`.
        // `new View.OnClickListener() { ... }` = „un obiect care știe doar să reacționeze
        // la click". Nu are nume, nu se refolosește.
        fabInsert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // `MainActivity.this` = Context-ul activității.
                // `this` singur ar fi listener-ul (clasa anonimă), nu o activitate —
                // iar constructorul `Intent(Context, Class)` ar primi tipul greșit.
                Intent intent = new Intent(MainActivity.this, ItemActivity.class);

                // FĂRĂ extra-uri! Absența lui `MOVIE_EXTRA` este exact semnalul
                // pe care `ItemActivity` îl citește ca „mod insert".
                launcher.launch(intent);
            }
        });

        // Listener pentru click pe un rând. Interfața are 4 parametri:
        //   parent   = ListView-ul
        //   view     = rândul apăsat (un TextView, aici)
        //   position = indexul în listă (0-based)
        //   id       = id-ul rândului; pentru ArrayAdapter este egal cu position
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                // Același target ca la FAB — dar de data asta ducem și date.
                Intent intent = new Intent(MainActivity.this, ItemActivity.class);

                // Obiectul de pe rândul apăsat. Este serializat automat la `launch`.
                // `movies.get(position)` întoarce REFERINȚA din listă; `putExtra`
                // o serializează într-o copie. Originalul rămâne neatins.
                intent.putExtra(MOVIE_EXTRA, movies.get(position));

                // Poziția, ca `ItemActivity` să știe ce rând să înlocuiască la salvare.
                intent.putExtra(POSITION_EXTRA, position);

                // Pornește activitatea prin launcher, nu prin `startActivity`.
                // Diferența: launcher-ul garantează că rezultatul ajunge în callback.
                launcher.launch(intent);
            }
        });
    }

    // ==================================================================================
    // initControls — FINDVIEWBYID, PUR ȘI SIMPLU
    // ==================================================================================
    // CE FACE: leagă cele două view-uri de câmpurile Java și instalează adapterul.
    // CUM: 1. FAB 2. ListView 3. setAdapter.
    // RISC: `findViewById` întoarce null dacă ID-ul nu există în layout-ul curent.
    //   Aici toate cele trei ID-uri există în `activity_main.xml`.
    // REF: în `ItemActivity`, aceeași metodă are 13 linii, nu 3.
    // ==================================================================================
    private void initControls() {

        // ID-ul din XML este `@+id/floatingActionButton`; câmpul se numește `fabInsert`.
        // Numele diferă, ID-ul contează.
        fabInsert = findViewById(R.id.floatingActionButton);

        // ID-ul din XML este `@+id/listView`.
        listView = findViewById(R.id.listView);

        // Instalează adapterul. Din acest moment ListView are ce desena.
        // `android:entries="@array/items"` din XML este IGNORAT de acum înainte.
        listView.setAdapter(movieAdapter);
    }

    // ==================================================================================
    // addSampleMovies — DATELE DEMO
    // ==================================================================================
    // CE FACE: adaugă 3 filme în listă, ca ecranul să nu pornească gol.
    // CUM: 3 apeluri ale constructorului cu 10 parametri, fiecare cu `new GregorianCalendar`.
    // RISC: ordinea argumentelor este POZIȚIONALĂ. Inversarea a două argumente de
    //   același tip (String/String sau boolean/boolean) compilează și produce date greșite.
    // REF: constructorul este documentat în 29.2; aici se vede SINGURUL lui consumator.
    // ==================================================================================
    private void addSampleMovies() {

        // FILMUL 1 — „Inception".
        // `new Movie(` deschide apelul; fiecare argument este pe linia lui, ca să se
        // vadă poziția. Ordinea este cea din semnătură: title, release, genre, budget,
        // duration, rating, watched, approval, onWatchlist, posterUrl.
        movies.add(new Movie(
                "Inception",                                              // 1. title
                // 2. release: `GregorianCalendar(an, lună, zi)` — LUNA ESTE 0-BASED!
                //    `Calendar.JULY` = 6, nu 7. Dacă ai scrie `new GregorianCalendar(2010, 7, 16)`
                //    ai obține 16 AUGUST. `getTime()` transformă Calendar în Date.
                new GregorianCalendar(2010, Calendar.JULY, 16).getTime(),
                "Sci-Fi",                                                 // 3. genre
                160000000,                                                // 4. budget (int -> double)
                148,                                                      // 5. duration (minute)
                4.5f,                                                     // 6. rating (float!)
                true,                                                     // 7. watched
                "PG-13",                                                  // 8. approval
                false,                                                    // 9. onWatchlist
                "https://example.com/posters/inception.jpg"));            // 10. posterUrl

        // FILMUL 2 — „Toy Story".
        // Aceeași structură. `4f` este un `float` literal; fără `f` ar fi `double`
        // și constructorul nu ar găsi potrivirea pentru parametrul `float rating`.
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

        // FILMUL 3 — „The Matrix".
        // Singurul cu `watched = false` și `onWatchlist = true`: cele două booleene
        // sunt independente, iar acest film demonstrează exact asta.
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

    // ==================================================================================
    // CE LIPSEȘTE DIN ACEST FIȘIER — ȘI SE CERE LA EXAMEN
    // ==================================================================================
    // - `onSaveInstanceState(Bundle)`: nu există. La rotire, activitatea este
    //   recreată, `movies` se reinițializează, iar filmele adăugate DISPAR.
    //   RISC REAL, nu teoretic: întoarce telefonul după ce ai adăugat un film.
    // - `onResume()` / `onStart()`: nu există. Nu e nevoie; totul se face în `onCreate`.
    // - `onDestroy()`: nu există. Nu există resurse de eliberat.
    // - Meniu, `OptionsMenu`, `Toolbar`: nu există. Ecranul este doar listă + FAB.
    // ==================================================================================
}
