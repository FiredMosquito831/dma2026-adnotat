/*
 * ======================================================================================
 * FIȘIER: MainActivity.java — ECRANUL CU LISTA, VERSIUNEA 2
 * ======================================================================================
 * CE ESTE ACEST FIȘIER:
 *   Activitatea de start. Ține lista de filme într-un `ArrayList<Movie>`, o afișează
 *   cu un `RecyclerView` printr-un adaptor propriu (`MovieAdapter`) și pornește
 *   `ItemActivity` — pentru inserare, din FAB, și pentru editare, din click pe rând.
 *
 * DE CE EXISTĂ:
 *   Este singurul loc care deține STAREA aplicației: lista `movies`. Toate celelalte
 *   ecrane primesc un `Movie` prin `Intent` și întorc un `Movie` prin rezultat.
 *   Fără această clasă nu există unde să se întoarcă rezultatul.
 *
 * DE CE DEPINDE:
 *   - `Movie` (modelul), `MovieAdapter` și `IMovieListener` (pachetul `adapter`),
 *   - `R.layout.activity_main` și patru ID-uri: `main`, `rvMovies`,
 *     `floatingActionButton`, `btnVlad`,
 *   - clasele `ItemActivity` și `PlaceCiocanul`, pornite prin `Intent`.
 *
 * CINE DEPINDE DE EL:
 *   `ItemActivity` citește `MainActivity.MOVIE_EXTRA` și `MainActivity.POSITION_EXTRA`
 *   (le vede fiindcă sunt `protected static` și fișierul este în ACELAȘI pachet).
 *   `AndroidManifest.xml` îl declară cu intent-filter-ul LAUNCHER.
 *
 * CE NU FACE:
 *   Nu validează date, nu ține o poziție „selectată", nu salvează nimic pe disc.
 *   Lista trăiește în memorie și dispare odată cu procesul.
 *
 * CE S-AR RUPE FĂRĂ EL:
 *   Aplicația nu ar avea punct de intrare: fără activitatea din LAUNCHER, iconița
 *   nu pornește nimic. Iar fără `movies` de aici, formularul ar avea unde să trimită
 *   rezultatul, dar nimeni nu l-ar primi.
 * ======================================================================================
 */

// ======================================================================================
// PACHETUL ȘI IMPORTURILE
// ======================================================================================
package com.example.group1098.activities;

// Intent: obiectul care descrie „ce activitate pornești" și ce date duce cu ea.
//   Aici apare de trei ori: la FAB, la al doilea FAB și în callback-ul de rezultat.
import android.content.Intent;

// Bundle: sacul de stare salvat de sistem. Apare doar în semnătura lui `onCreate`.
import android.os.Bundle;

// View: clasa de bază a oricărui element de UI. Tipul parametrului din `onClick`.
import android.view.View;

// AdapterView: clasa de bază pentru ListView/Spinner, folosită de
//   `AdapterView.OnItemClickListener`.
// RISC: IMPORT NEFOLOSIT în versiunea 2. Singura referință a rămas în blocul
//   comentat din `initListeners`. Codul compilează, dar Android Studio îl arată gri.
import android.widget.AdapterView;

// ArrayAdapter: puntea dintre `List<Movie>` și rândurile de ListView.
// RISC: IMPORT NEFOLOSIT — la fel, supraviețuiește doar în comentarii.
import android.widget.ArrayAdapter;

// ListView: widget-ul versiunii vechi. Nici măcar nu mai există în layout.
// RISC: IMPORT NEFOLOSIT — rămășiță a migrării.
import android.widget.ListView;

// Toast: mesaje scurte pe ecran. Nu este folosit nicăieri în acest fișier —
//   nici în versiunea veche nu era (capitolul 22 îl semnala la fel).
import android.widget.Toast;

// EdgeToEdge: desenare sub barele de sistem (status + navigare).
import androidx.activity.EdgeToEdge;

// ActivityResult: rezultatul întors de activitatea secundară: cod + Intent.
import androidx.activity.result.ActivityResult;

// ActivityResultCallback: interfața cu o singură metodă, apelată la întoarcere.
import androidx.activity.result.ActivityResultCallback;

// ActivityResultLauncher: obiectul care pornește activitatea și ține legătura
//   cu callback-ul. Este câmpul `launcher`.
import androidx.activity.result.ActivityResultLauncher;

// ActivityResultContracts: contractele predefinite. Aici `StartActivityForResult`,
//   adică exact vechiul `startActivityForResult`, ambalat modern.
import androidx.activity.result.contract.ActivityResultContracts;

// AppCompatActivity: activitatea de bază, cu temă Material și suport AppCompat.
import androidx.appcompat.app.AppCompatActivity;

// Insets: valorile în pixeli ale barelor care acoperă conținutul.
import androidx.core.graphics.Insets;

// ViewCompat: versiunea compatibilă a API-urilor de View (insets pe orice API).
import androidx.core.view.ViewCompat;

// WindowInsetsCompat: descrierea insets-urilor, independent de versiunea Android.
import androidx.core.view.WindowInsetsCompat;

// LinearLayoutManager: singurul layout manager folosit aici. El decide ARANJAREA
//   rândurilor într-o listă verticală — `RecyclerView` nu are una implicită.
import androidx.recyclerview.widget.LinearLayoutManager;

// RecyclerView: widget-ul care înlocuiește ListView. Containerul, nu adaptorul.
import androidx.recyclerview.widget.RecyclerView;

// IMovieListener: contractul de click. MainActivity îl IMPLEMENTEAZĂ.
import com.example.group1098.adapter.IMovieListener;

// MovieAdapter: adaptorul propriu, scris în pachetul `adapter`.
import com.example.group1098.adapter.MovieAdapter;

// FloatingActionButton: butonul rotund. Sunt DOUĂ în acest ecran.
import com.google.android.material.floatingactionbutton.FloatingActionButton;

// ArrayList: implementarea concretă a listei de filme.
import java.util.ArrayList;

// Calendar: constantele lunilor (`Calendar.JULY`, `NOVEMBER`, `MARCH`).
import java.util.Calendar;

// GregorianCalendar: constructorul rapid „an, lună, zi" din datele demo.
import java.util.GregorianCalendar;

// List: interfața de listă.
// RISC: IMPORT NEFOLOSIT — câmpul este declarat `ArrayList<Movie>`, nu `List<Movie>`.
import java.util.List;

// R: clasa generată din resurse. Aici pentru `R.layout.activity_main` și `R.id.*`.
import com.example.group1098.R;

// Movie: modelul de date. Singura dependență către pachetul `model`.
import com.example.group1098.model.Movie;

/*
 * ======================================================================================
 * BLOC DE TEORIE: ACTIVITY, RECYCLERVIEW, CALLBACK
 * ======================================================================================
 *
 * 1. **Activity = un ecran cu o stare.** `MainActivity` este `Context` (are acces la
 *    resurse și la sistem) și `LifecycleOwner` (are un ciclu de viață: onCreate ->
 *    onStart -> onResume -> onPause -> onStop -> onDestroy).
 *
 * 2. **RecyclerView = un container fără opinii.** Nu știe ce afișează, nu știe cum
 *    arată un rând și nu știe în ce direcție se derulează. Tot ce știe este să ceară
 *    de la adaptor: „câte rânduri sunt?" și „dă-mi rândul de la poziția N".
 *
 * 3. **Adapter = traducătorul.** Pentru fiecare rând vizibil, creează (o singură dată)
 *    vederea și apoi o umple cu date (de fiecare dată). Cele două momente sunt două
 *    metode diferite: `onCreateViewHolder` și `onBindViewHolder`.
 *
 * 4. **Callback = întoarcerea click-ului.** Un rând este o vedere inertă. Ca să ajungă
 *    la activitate, adaptorul primește un obiect care implementează `IMovieListener`;
 *    aici obiectul acela este CHIAR `MainActivity` (`new MovieAdapter(movies, this)`).
 *
 * 5. **Launcher = contractul cu rezultatul.** `registerForActivityResult` se apelează
 *    O DATĂ, în `onCreate`, înainte ca activitatea să fie vizibilă. Rezultatul vine
 *    în `onActivityResult`, cu un cod (`RESULT_OK` / `RESULT_CANCELED`) și un `Intent`.
 *
 * FLUXUL VIZUAL AL FIECĂRUI RÂND:
 *
 *   movies (ArrayList<Movie>)            <- datele, deținute de activitate
 *        |
 *        v
 *   MovieAdapter                         <- getItemCount() + onBindViewHolder()
 *        |
 *        v
 *   RecyclerView  <--- LinearLayoutManager   <- UNDE stau rândurile
 *        |
 *        v
 *   movie_item.xml  ->  un card per film
 *
 * CINE ȚINE CE:
 *
 *   MainActivity  ---- deține ---->  ArrayList<Movie> movies
 *        |                                  ^
 *        | creează                          | aceeași referință
 *        v                                  |
 *   MovieAdapter  ----------------------> movies
 *        |
 *        +---- listează click-ul către ----> IMovieListener ( = MainActivity )
 *
 * DE CE `implements IMovieListener` ESTE OBLIGATORIU:
 *   `new MovieAdapter(movies, this)` cere ca al doilea argument să fie un
 *   `IMovieListener`. Fără `implements`, linia NU COMPILEAZĂ — și este bine:
 *   compilatorul verifică exact contractul pe care îl vrei.
 * ======================================================================================
 */

// Activitatea de start. `implements IMovieListener` o face destinatarul click-urilor.
public class MainActivity extends AppCompatActivity implements IMovieListener {

    // ==================================================================================
    // CELE DOUĂ CHEI ALE COMUNICĂRII CU ItemActivity
    // ==================================================================================
    // `static final` = constantă de clasă: aceeași valoare pentru toate instanțele,
    //   stabilită o dată, la compilare.
    // `protected` = vizibilă în subclase ȘI în tot pachetul. ItemActivity este în
    //   `com.example.group1098.activities`, deci vede constantele fără import.
    // DE CE NU `private`: ItemActivity le-ar primi ca text literal „movie" / „position";
    //   o greșeală de tastare ar da un extra inexistent, fără nicio eroare.
    // RISC: dacă schimbi valoarea „movie" aici, ItemActivity o vede automat — dar
    //   orice `getStringExtra("movie")` scris de mână în alt fișier s-ar rupe silențios.
    // ==================================================================================
    protected static final String POSITION_EXTRA = "position";

    // A doua cheie: filmul serializat. Aceeași convenție.
    protected static final String MOVIE_EXTRA = "movie";

    // FAB-ul „+", din colțul dreapta-jos. Deschide ItemActivity în modul inserare.
    private FloatingActionButton fabInsert;

    // Codul versiunii vechi, PĂSTRAT INTENȚIONAT:
    // private ListView listView;
    // DE CE A RĂMAS: ca cititorul să vadă exact ce s-a înlocuit și cu ce. În codul
    //   livrat, comentariul nu are efect; îl poți citi ca pe un „înainte".
    // REF: capitolul 22, secțiunea 22.2 — versiunea în care linia era activă.

    // SUCCESORUL lui `listView`: containerul de listă al versiunii 2.
    private RecyclerView rvMovies;

    // Launcher-ul de rezultat. Un singur obiect deservește AMBELE ecrane
    //   (`ItemActivity` și `PlaceCiocanul`), pentru că amândouă se pornesc la fel.
    // RISC: este atribuit ABIA la finalul lui `onCreate`, dar este folosit în
    //   listeneri creați mai devreme. Vezi explicația pasului 8 din `onCreate`.
    private ActivityResultLauncher<Intent> launcher;

    // LISTA — singura sursă de date a ecranului.
    // `final` înseamnă că REFERINȚA nu se schimbă niciodată; conținutul, da.
    //   De aceea `movies.add(...)` este legal, iar `movies = new ArrayList<>()` nu.
    // DE CE `final` ajută aici: adaptorul primește ACEASTĂ referință; dacă ai
    //   înlocui lista cu alta, adaptorul ar rămâne pe cea veche, fără nicio eroare.
    private final ArrayList<Movie> movies = new ArrayList<>();

    // Codul versiunii vechi, păstrat comentat:
    //private ArrayAdapter<Movie> movieAdapter;
    // Observă spațierea: originalul nu avea spațiu după `//`. Este mărturia exactă
    //   a unei linii comentate cu Ctrl+/ în Android Studio, nu rescrisă de mână.
    // REF: capitolul 22 — `ArrayAdapter` cu `simple_list_item_1`.

    // Adaptorul propriu. Tipul este clasa scrisă în pachetul `adapter`, nu una de sistem.
    private MovieAdapter movieAdapter;

    // Al doilea FAB, adăugat în versiunea 2. Jos-stânga; deschide ecranul de test.
    // Numele (`btnVlad`) este cel din XML (`@+id/btnVlad`) — se potrivește exact.
    private FloatingActionButton btnVlad;

    // ==================================================================================
    // METODA: onCreate — CONSTRUCȚIA ECRANULUI
    // ==================================================================================
    // CE FACE: creează ecranul, încarcă datele demo, construiește adaptorul, leagă
    //   view-urile de cod, instalează cei doi listeneri de buton și înregistrează
    //   launcher-ul de rezultat.
    // CUM: în ordinea de mai jos — și ordinea este singura parte greu de reparat.
    // RISC: fiecare pas depinde de cel dinainte. Mută `initControls()` după
    //   `initListeners()` și primești `NullPointerException` pe `fabInsert`,
    //   pentru că listener-ul s-ar lega pe un câmp încă `null`.
    // REF: capitolul 22, 22.2 — aceeași schemă, cu ListView în loc de RecyclerView.
    // ==================================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // 1. Lanțul obligatoriu al clasei de bază: fără el, AppCompat nu își
        //    instalează delegate-ul și tema nu se aplică.
        super.onCreate(savedInstanceState);

        // 2. Ferestrele desenează sub barele de sistem (status bar + navigare).
        EdgeToEdge.enable(this);

        // 3. Se umflă arborele de view-uri din XML. Din acest moment,
        //    `findViewById` are ce căuta.
        setContentView(R.layout.activity_main);

        // 4. Compensarea barelor: containerul `R.id.main` primește padding egal cu
        //    înălțimea barelor, ca primul rând să nu stea sub status bar.
        //    Lambda primește `insets` și întoarce exact ce a primit — contractul API.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {

            // 4a. Insets-urile „system bars": status bar sus, navigare jos.
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            // 4b. Padding pe cele patru laturi. `left/right` acoperă și cazul RTL,
            //     pentru că sunt deja calculate de sistem pe direcția corectă.
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);

            // 4c. Se întoarce obiectul primit: altfel sistemul ar considera insets-urile
            //     consumate și alte view-uri nu le-ar mai primi.
            return insets;
        });

        // 5. Datele demo: trei filme, adăugate ÎNAINTE ca adaptorul să fie creat.
        //    (Tehnic, ordinea nu ar strica: adaptorul ține referința la listă, nu o copie.)
        addSampleMovies();

        // Codul versiunii vechi, păstrat comentat — adaptorul gata făcut:
        // movieAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, movies);
        // CE FĂCEA: lega lista de un rând standard, care afișa `movie.toString()`,
        //   adică tot obiectul, pe un singur rând de text.
        // CE FACE ÎN LOCUL LUI: linia de mai jos, cu adaptorul propriu.

        // 6. Adaptorul propriu. `this` este al doilea parametru pentru că MainActivity
        //    implementează `IMovieListener` — ea este destinatarul click-urilor.
        //    Se pasează REFERINȚA la listă: adaptorul vede orice modificare ulterioară.
        movieAdapter = new MovieAdapter(movies, this);

        // 7a. Legarea view-urilor: în acest moment `rvMovies`, `fabInsert` și `btnVlad`
        //     primesc obiectele reale. `movieAdapter` este deja construit, deci
        //     `setAdapter` are ce primi.
        initControls();

        // 7b. Listenerii de butoane. Au nevoie de view-urile de la 7a — de aceea
        //     ordinea aceasta și nu invers.
        initListeners();

        // 7c. Al doilea listener, într-o metodă separată: FAB-ul `btnVlad`.
        initSecondListener();

        // 8. Launcher-ul de rezultat. `registerForActivityResult` trebuie apelat
        //    înainte ca activitatea să fie STARTED; `onCreate` este locul canonic.
        //    Contractul `StartActivityForResult` este echivalentul modern al vechiului
        //    `startActivityForResult(intent, REQUEST_CODE)`.
        launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),

                // Callback-ul: o clasă anonimă care implementează o singură metodă.
                new ActivityResultCallback<ActivityResult>() {

                    // Se apelează la întoarcerea din ItemActivity sau PlaceCiocanul.
                    @Override
                    public void onActivityResult(ActivityResult o) {

                        // Poarta: se lucrează doar când ecranul secundar a spus
                        //   „am un rezultat". `RESULT_CANCELED` (butonul Back) nu
                        //   schimbă nimic în listă.
                        if(o.getResultCode() == RESULT_OK)
                        {
                            // Intent-ul de rezultat. Poate fi `null` dacă activitatea
                            //   a apelat `setResult(RESULT_OK)` fără Intent.
                            // RISC: aici nu se verifică `null`. Azi este sigur, pentru
                            //   că ItemActivity pune mereu un Intent (capitolul 22, 22.3),
                            //   iar PlaceCiocanul nu apelează niciodată `setResult`.
                            Intent intent = o.getData();

                            // Filmul călător: o COPIE deserializată, nu originalul.
                            //   Cast-ul este necesar: `getSerializableExtra` întoarce
                            //   `Serializable`, tipul comun al tuturor obiectelor puse.
                            Movie movie = (Movie) intent.getSerializableExtra(MOVIE_EXTRA);

                            // Poziția: -1 = „nu există", adică modul inserare.
                            //   Valoarea implicită -1 este chiar convenția aplicației.
                            int position = intent.getIntExtra(POSITION_EXTRA, -1);

                            // Ramura 1: nu există poziție -> film NOU.
                            if(position == -1)
                            {
                                // Adăugare la finalul listei.
                                movies.add(movie);

                                // Anunțul către RecyclerView. Fără el, lista rămâne
                                //   vizual neschimbată: adaptorul nu observă ArrayList-ul.
                                // REF: capitolul 23 — varianta fină, `notifyItemInserted`.
                                movieAdapter.notifyDataSetChanged();
                            }

                            // Ramura 2: poziție validă -> ÎNLOCUIRE, nu adăugare.
                            else
                            {
                                // `set` suprascrie elementul de la poziția dată.
                                // RISC: dacă lista s-ar fi schimbat între timp (ștergere,
                                //   reordonare), poziția ar putea indica alt film.
                                //   Aici lista nu se schimbă în afara acestor două ramuri.
                                movies.set(position, movie);

                                // Același anunț: un rând s-a schimbat.
                                movieAdapter.notifyDataSetChanged();
                            }
                        }
                    }
                });
    }

    // ==================================================================================
    // METODA: initListeners — FAB-ul de INSERARE (și codul vechi, comentat)
    // ==================================================================================
    // CE FACE: leagă `fabInsert` de pornirea ItemActivity fără extra-uri. Un singur
    //   listener activ; în plus, păstrează comentat listener-ul de listă al versiunii 1.
    // CUM: 1) listener pe FAB; 2) construiește Intent explicit; 3) lansează.
    // RISC: `launcher` este încă `null` în momentul în care se creează listener-ul —
    //   dar este citit abia la click, când `onCreate` s-a terminat de mult.
    // REF: capitolul 22, 22.2 — aceeași metodă, fără blocul comentat.
    // ==================================================================================
    private void initListeners() {

        // Listener pe FAB. Clasa anonimă, nu lambda: stilul proiectului (și al examenului).
        fabInsert.setOnClickListener(new View.OnClickListener() {

            // Se execută la fiecare apăsare.
            @Override
            public void onClick(View v) {

                // Intent EXPLICIT: sursa (`MainActivity.this`) și destinația
                //   (`ItemActivity.class`). Fără extra-uri: ItemActivity vede că nu are
                //   `MOVIE_EXTRA` și intră în modul inserare.
                // OBSERVAȚIE: clasa este scrisă cu numele complet al pachetului, deși
                //   ItemActivity este în ACELAȘI pachet. Este redundant, dar legal.
                Intent intent = new Intent(MainActivity.this, com.example.group1098.activities.ItemActivity.class);

                // Pornirea prin launcher: rezultatul va ajunge în callback-ul de la
                //   pasul 8 din `onCreate`.
                launcher.launch(intent);
            }
        });

        // ==============================================================================
        // CODUL VERSIUNII VECHI — CLICK-UL PE RÂND, PĂSTRAT COMENTAT
        // ==============================================================================
        // Acesta este cel mai valoros bloc didactic din fișier: arată EXACT ce s-a
        // pierdut când ListView a fost înlocuit cu RecyclerView și unde a fost mutat.
        //
        //   ÎNAINTE (ListView):                ACUM (RecyclerView):
        //   listView.setOnItemClickListener    listener în MovieAdapter (pe itemView)
        //   onItemClick(..., position, id)     onMovieClick(Movie, position)
        //   movies.get(position)               movies.get(position)
        //   putExtra(MOVIE_EXTRA, ...)         putExtra(MOVIE_EXTRA, ...)
        //   putExtra(POSITION_EXTRA, ...)      putExtra(POSITION_EXTRA, ...)
        //   launcher.launch(intent)            launcher.launch(intent)
        //
        // Ultimele patru linii sunt IDENTICE cu `onMovieClick` de la finalul fișierului.
        // Diferența este doar CINE le apelează: înainte ListView, acum adaptorul.
        /* listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(MainActivity.this, com.example.group1098.activities.ItemActivity.class);
                intent.putExtra(MOVIE_EXTRA, movies.get(position));
                intent.putExtra(POSITION_EXTRA, position);
                launcher.launch(intent);
            }
        });*/
        // DE CE A FOST NECESARĂ MUTAREA: un rând de RecyclerView nu este un element de
        //   listă observat de widget, ci o vedere oarecare. Nu există „onItemClick";
        //   click-ul se leagă manual, pe `itemView`, în `ViewHolder` (38.2).
        // RISC: dacă ai lăsa cele două active (blocul decomentat + `onMovieClick`),
        //   click-ul ar porni ItemActivity de două ori — două activități suprapuse.
    }

    // ==================================================================================
    // METODA: initSecondListener — AL DOILEA FAB
    // ==================================================================================
    // CE FACE: leagă `btnVlad` de pornirea ecranului `PlaceCiocanul`.
    // CUM: 1) listener; 2) Intent explicit; 3) ACELAȘI launcher.
    // RISC: același launcher primește și rezultatul acestui ecran. PlaceCiocanul nu
    //   cheamă `setResult`, deci codul întors este `RESULT_CANCELED` și callback-ul
    //   nu face nimic — exact comportamentul dorit.
    // REF: `initListeners`, cu o singură diferență: clasa destinație.
    // ==================================================================================
    private void initSecondListener() {

        // Listener pe al doilea FAB (jos-stânga, iconița cu ciocan).
        btnVlad.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                // Variabila se numește `intent2` — suficient pentru a nu se confunda
                //   cu `intent` din metoda de mai sus, deși sunt în metode diferite.
                Intent intent2 = new Intent(MainActivity.this, com.example.group1098.activities.PlaceCiocanul.class);

                // Același launcher: un singur contract de rezultat pentru două ecrane.
                launcher.launch(intent2);
            }
        });
    }

    // ==================================================================================
    // METODA: initControls — FINDVIEWBYID ȘI CONFIGURAREA LISTEI
    // ==================================================================================
    // CE FACE: leagă câmpurile Java de view-urile din XML și configurează RecyclerView
    //   (layout manager + adaptor).
    // CUM: 1) FAB; 2) RecyclerView; 3) layout manager; 4) adaptor; 5) al doilea FAB.
    // RISC: `setLayoutManager` trebuie apelat ÎNAINTE de `setAdapter`. Fără layout
    //   manager, RecyclerView nu desenează nimic și scrie în Logcat:
    //   „No layout manager attached; skipping layout".
    // REF: capitolul 22, 22.2 — aceeași metodă, cu `ListView.setAdapter` în loc.
    // ==================================================================================
    private void initControls() {

        // 1. FAB-ul de inserare, după ID-ul din `activity_main.xml`.
        fabInsert = findViewById(R.id.floatingActionButton);

        // Codul versiunii vechi, păstrat comentat — legarea ListView-ului:
        // listView = findViewById(R.id.listView);
        // listView.setAdapter(movieAdapter);
        // OBSERVAȚIE: ambele linii sunt comentate, dar `R.id.listView` nici măcar nu
        //   mai există în layout. Dacă le-ai decomenta, fișierul NU AR COMPILA.

        // 2. RecyclerView-ul. ID-ul `rvMovies` există în layout-ul versiunii 2.
        rvMovies = findViewById(R.id.rvMovies);

        // 3. Layout manager: OBLIGATORIU, înainte de adaptor. `this` este Context-ul.
        //    LinearLayoutManager = listă verticală, un rând sub altul.
        //    Alte variante: GridLayoutManager (grilă), StaggeredGridLayoutManager.
        rvMovies.setLayoutManager(new LinearLayoutManager(this));

        // 4. Adaptorul construit în `onCreate`. Din acest moment, RecyclerView știe
        //    câte rânduri are lista și cum să le construiască.
        rvMovies.setAdapter(movieAdapter);

        // 5. Al doilea FAB. Căutat DUPĂ ce adaptorul este legat — ordinea între
        //    view-uri nu contează, toate există de la `setContentView`.
        btnVlad = findViewById(R.id.btnVlad);
    }

    // ==================================================================================
    // METODA: addSampleMovies — DATELE DEMO
    // ==================================================================================
    // CE FACE: adaugă în listă trei filme construite cu constructorul cu 10 parametri.
    // CUM: trei apeluri `new Movie(...)`, fiecare cu aceeași ordine de argumente.
    // RISC: constructorul este POZIȚIONAL. Inversarea a două argumente de ACELAȘI tip
    //   (String cu String, boolean cu boolean) compilează și produce date greșite.
    // REF: capitolul 22, 22.1 — tabelul pozițiilor din constructorul lui `Movie`.
    // ==================================================================================
    private void addSampleMovies() {

        // Filmul 1. Argumentele, pe poziții:
        //   1 title="Inception", 2 release=16 iulie 2010, 3 genre="Sci-Fi",
        //   4 budget=160000000, 5 duration=148, 6 rating=4.5f,
        //   7 watched=true, 8 approval="PG-13", 9 onWatchlist=false, 10 posterUrl=...
        movies.add(new Movie(

                // 1. Titlul.
                "Inception",

                // 2. Data. `GregorianCalendar(an, lună, zi)` — luna este 0-based:
                //    `Calendar.JULY` este 6, nu 7. `.getTime()` transformă calendarul
                //    în `Date`, tipul cerut de model.
                new GregorianCalendar(2010, Calendar.JULY, 16).getTime(),

                // 3. Genul — text liber, nu enum.
                "Sci-Fi",

                // 4. Bugetul — `double`, de aceea fără sufix.
                160000000,

                // 5. Durata în minute — `int`.
                148,

                // 6. Ratingul — `float`: sufixul `f` este OBLIGATORIU. Fără el,
                //    literalul `4.5` este `double` și apelul nu compilează.
                4.5f,

                // 7. „Am văzut filmul" — boolean.
                true,

                // 8. Ghidajul parental — textul butonului radio.
                "PG-13",

                // 9. „Pe lista de revăzut" — al doilea boolean, independent de primul.
                false,

                // 10. URL-ul posterului. Aici este un URL de exemplu: aplicația nu
                //     descarcă nicio imagine, deci valoarea nu ajunge niciodată pe ecran.
                "https://example.com/posters/inception.jpg"));

        // Filmul 2. Aceeași structură, alte valori. `Calendar.NOVEMBER` = 10 (0-based).
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

        // Filmul 3. `Calendar.MARCH` = 2 (0-based); rating 5f — nota maximă;
        //   `watched=false`, `onWatchlist=true`: filmul este pus deoparte, nu văzut.
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
    // METODA: onMovieClick — IMPLEMENTAREA CONTRACTULUI DE CLICK
    // ==================================================================================
    // CE FACE: primește de la adaptor filmul și poziția, apoi pornește ItemActivity
    //   în modul editare, cu ambele extra-uri.
    // CUM: 1) Intent explicit; 2) pune filmul; 3) pune poziția; 4) lansează.
    // RISC: metoda folosește `movies.get(position)`, NU parametrul `movie`. Cele două
    //   sunt consistente aici (adaptorul citește din aceeași listă). Dacă adaptorul ar
    //   primi altă listă, parametrul ar fi sursa corectă.
    // REF: blocul comentat din `initListeners` — acesta este `onItemClick` mutat.
    // ==================================================================================
    @Override
    public void onMovieClick(Movie movie, int position) {

        // 1. Același Intent explicit ca la FAB, dar cu extra-uri.
        Intent intent = new Intent(MainActivity.this, com.example.group1098.activities.ItemActivity.class);

        // 2. Filmul, serializat în Bundle. `putExtra(String, Serializable)`.
        //    Se ia din listă după poziție — sursa de adevăr a ecranului.
        intent.putExtra(MOVIE_EXTRA, movies.get(position));

        // 3. Poziția: prezența ei este semnalul „mod editare" pentru ItemActivity.
        intent.putExtra(POSITION_EXTRA, position);

        // 4. Pornirea. Rezultatul (filmul modificat) vine în callback-ul din `onCreate`
        //    și intră pe ramura `movies.set(position, movie)`.
        launcher.launch(intent);
    }
}
