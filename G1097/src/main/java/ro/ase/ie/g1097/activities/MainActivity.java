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
 * CE S-AR RUPE FĂRĂ EL: aplicația ar porni direct într-un formular fără listă,
 *   iar rezultatul salvat nu ar avea unde să se întoarcă.
 * ======================================================================================
 */

package ro.ase.ie.g1097.activities;

// ======================================================================================
// IMPORTURI — GRUPATE PE SURSĂ
// ======================================================================================
// android.*        — clase din framework (Intent, Bundle, View, widget-uri)
// androidx.*       — biblioteci Jetpack (AppCompat, EdgeToEdge, insets, activity result)
// com.google.*     — Material Components (FloatingActionButton)
// java.util.*      — colecții și date
// ro.ase.ie.g1097  — modelul și clasa R generată
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

// Date: tipul stocat în model pentru data de lansare.
import java.util.Date;

// GregorianCalendar: constructorul rapid „an, lună, zi" folosit în datele demo.
import java.util.GregorianCalendar;

// List: interfața de listă. Câmpul este declarat `List`, nu `ArrayList`.
import java.util.List;

// Modelul de date — singura dependență către `model`.
import ro.ase.ie.g1097.model.Movie;

// R: clasa generată din resurse. Aici pentru `R.layout.activity_main` și `R.id.*`.
import ro.ase.ie.g1097.R;

/*
 * ======================================================================================
 * BLOC DE TEORIE: ACTIVITY, INTENT, ADAPTER
 * ======================================================================================
 *
 * 1. **Activity = un ecran cu o stare.** `MainActivity` este un `Context` (are acces la
 *    resurse, temă, sistem) și un `LifecycleOwner` (are un ciclu de viață: onCreate ->
 *    onStart -> onResume -> onPause -> onStop -> onDestroy).
 *
 * 2. **Intent = un mesaj.** Un `Intent` explicit conține clasa destinatar și un `Bundle`
 *    de extra-uri. Pornirea se face cu `startActivity` (fără rezultat) sau cu un
 *    `ActivityResultLauncher` (cu rezultat).
 *
 * 3. **Adapter = traducătorul.** `ListView` nu știe ce este un `Movie`. `ArrayAdapter`
 *    știe: pentru fiecare poziție, ia `movies.get(position)` și îl transformă într-un
 *    rând de UI. Fără adapter, `ListView` ar afișa o listă de obiecte goale.
 *
 * 4. **Ordinea în onCreate este contract, nu stil.** Fiecare pas depinde de cel dinainte:
 *    fără `super`, AppCompat nu are delegate; fără `setContentView`, `findViewById`
 *    întoarce null; fără adapter, `setAdapter` primește null.
 *
 * VIZUAL: ciclul de viață în timpul unui insert
 *
 *   MainActivity:  onCreate -> onStart -> onResume -> onPause
 *                                                        |
 *                                                        v
 *   ItemActivity:                                   onCreate -> onStart -> onResume
 *                                                        |
 *                                                   (utilizatorul salvează)
 *                                                        |
 *                                                   setResult + finish
 *                                                        |
 *   MainActivity:  onStart -> onResume -> [callback launcher] -> listă redesenată
 *
 *   MainActivity NU este distrusă cât timp ItemActivity este deasupra. Este doar
 *   oprită. De aceea lista `movies` supraviețuiește și de aceea callback-ul o poate
 *   modifica direct.
 * ======================================================================================
 */
public class MainActivity extends AppCompatActivity {

    // ==================================================================================
    // CONSTANTELE DE EXTRA
    // ==================================================================================
    // DE CE CONSTANTE ȘI NU LITERALI: cheia „movie" este scrisă în două fișiere.
    //   Dacă într-un loc scrii "movie" și în altul "Movie", `getSerializableExtra`
    //   întoarce `null` FĂRĂ NICIO EROARE. Compilatorul nu te ajută cu String-uri.
    //
    // DE CE `protected`: `ItemActivity` este în același pachet, deci ar funcționa și
    //   `package-private`. `protected` documentează intenția: „se folosește din
    //   subclase / din cod apropiat".
    //
    // DE CE `static`: valoarea aparține clasei, nu instanței. `MainActivity.MOVIE_EXTRA`
    //   se poate scrie fără să existe un obiect MainActivity.
    //
    // DE CE `final`: cheia nu trebuie schimbată niciodată la runtime.
    // ==================================================================================
    protected static final String POSITION_EXTRA = "position";
    protected static final String MOVIE_EXTRA = "movie";

    // ==================================================================================
    // CÂMPUL 1: fabInsert
    // ==================================================================================
    // TIP: FloatingActionButton — butonul „+".
    // DE CE CÂMP: este legat în `initControls` și folosit în `initListeners`.
    //   O variabilă locală în `initControls` ar muri la sfârșitul metodei.
    // ==================================================================================
    private FloatingActionButton fabInsert;

    // ==================================================================================
    // CÂMPUL 2: listView
    // ==================================================================================
    // TIP: ListView — widget-ul care afișează rândurile.
    // DE CE CÂMP: legat în `initControls`, i se atașează listener în `initListeners`.
    // ==================================================================================
    private ListView listView;

    // ==================================================================================
    // CÂMPUL 3: launcher
    // ==================================================================================
    // TIP: ActivityResultLauncher<Intent> — obiectul returnat de `registerForActivityResult`.
    // DE CE CÂMP: este creat în `onCreate` și folosit în `initListeners`, în două
    //   locuri (FAB și click pe rând).
    // RISC DE ORDINE: `initListeners()` rulează ÎNAINTE de atribuirea acestui câmp.
    //   Funcționează doar pentru că listener-ii se execută la CLICK, nu la înregistrare.
    // ==================================================================================
    private ActivityResultLauncher<Intent> launcher;

    // ==================================================================================
    // CÂMPUL 4: movies
    // ==================================================================================
    // TIP: `final List<Movie>` inițializat cu `ArrayList`.
    // DE CE `List` și nu `ArrayList`: tipul declarat este interfața. Implementarea
    //   poate fi schimbată (ex. `LinkedList`) fără să modifici restul codului.
    // DE CE `final`: REFERINȚA nu se schimbă niciodată. Poți face `movies.add(...)`,
    //   `movies.set(...)`, dar nu `movies = new ArrayList<>()`.
    // DE CE `new ArrayList<>()` aici: lista trebuie să existe înainte de `onCreate`,
    //   pentru că `ArrayAdapter` primește referința la ea.
    // ==================================================================================
    private final List<Movie> movies = new ArrayList<>();

    // ==================================================================================
    // CÂMPUL 5: movieAdapter
    // ==================================================================================
    // TIP: ArrayAdapter<Movie> — adapterul care leagă lista de ListView.
    // DE CE NU este `final`: este atribuit în `onCreate`, după `addSampleMovies()`.
    // DE CE CÂMP: `notifyDataSetChanged()` este apelat din interiorul callback-ului,
    //   unde ai nevoie de aceeași instanță.
    // ==================================================================================
    private ArrayAdapter<Movie> movieAdapter;

    // ==================================================================================
    // onCreate — CONSTRUCTORUL DE FAPT AL ECRANULUI
    // ==================================================================================
    // Semnătura este impusă de framework: `protected void onCreate(Bundle)`.
    // `savedInstanceState` este `null` la prima creare și un `Bundle` la recreare
    //   (rotire de ecran, schimbare de temă). Aici NU este folosit: lista se pierde
    //   la rotire. Este o limitare cunoscută a acestui commit.
    // ==================================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // PASUL 1: lansează lanțul de inițializare al clasei părinte.
        // CE FACE: AppCompat își instalează delegate-ul, încarcă tema, creează
        //   ActionBar-ul și pregătește `FragmentManager`-ul.
        // CE SE RUPE DACĂ LIPSEȘTE: `SuperNotCalledException` la `onStart`, sau
        //   `NullPointerException` în `setContentView` pentru că delegate-ul nu există.
        // CE SE RUPE DACĂ E PREA TÂRZIU (după setContentView): AppCompat nu mai poate
        //   aplica tema pe fereastră; poți vedea un flash de culoare greșită.
        super.onCreate(savedInstanceState);

        // PASUL 2: activează modul edge-to-edge.
        // CE FACE: setează ferestrele să deseneze și sub barele de sistem, astfel
        //   încât conținutul să poată ocupa tot ecranul. Barele devin transparente.
        // DE CE ÎNAINTE DE setContentView: configurarea ferestrei trebuie făcută
        //   înainte ca prima vedere să fie atașată, altfel poți vedea o recolorare.
        // CONSECINȚĂ: conținutul intră SUB ceas și sub bara de navigare. De aceea
        //   pasul 4 aplică padding.
        EdgeToEdge.enable(this);

        // PASUL 3: umple fereastra cu arborele de view-uri din XML.
        // CE FACE: inflatează `res/layout/activity_main.xml` și îl atașează la fereastră.
        // DE CE ÎNAINTE DE findViewById: înainte de acest apel, `findViewById` nu are
        //   unde să caute și întoarce `null` pentru orice ID.
        // CE SE RUPE DACĂ LIPSEȘTE: ecran alb; orice `findViewById` -> null -> NPE.
        setContentView(R.layout.activity_main);

        // PASUL 4: aplică padding egal cu barele de sistem pe rădăcină.
        // ==============================================================================
        // VIZUAL: zona sigură (safe area)
        //
        //   +--------------------------------------------------+  ^
        //   |  ceas   baterie   notificări                     |  | status bar (~24dp)
        //   +--------------------------------------------------+  v
        //   |                                                  |  ^
        //   |                                                  |  |
        //   |            CONȚINUT (padding aplicat)            |  | ecran
        //   |                                                  |  |
        //   |                                                  |  |
        //   +--------------------------------------------------+  v
        //   |  back   home   recente                           |  ^  navigation bar (~48dp)
        //   +--------------------------------------------------+  v
        //
        //   Fără padding: primul rând din listă intră sub ceas, iar FAB-ul intră
        //   sub bara de navigare și devine imposibil de apăsat.
        //   Cu padding pe R.id.main: TOȚI copiii sunt împinși în zona sigură.
        // ==============================================================================
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {

            // Extrage insets-urile pentru „systemBars": status bar + navigation bar
            // + caption bar. Nu include tastatura (IME), care ar fi `Type.ime()`.
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            // Aplică padding pe toate cele patru laturi, în pixeli.
            // DE CE pe rădăcină: padding-ul pe un copil ar împinge doar acel copil;
            //   ceilalți ar rămâne sub bare.
            // EFECT SECUNDAR: padding-ul se REAPLICĂ la fiecare schimbare de insets
            //   (rotire, bară de navigare care apare/dispare). `setPadding` nu adună,
            //   ci înlocuiește — deci nu se acumulează.
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);

            // `return insets` (nu `CONSUMED`): insets-urile continuă să circule către
            // copii. Dacă ai returna `WindowInsetsCompat.CONSUMED`, niciun copil nu
            // ar mai primi informația și ar trebui să își aplice singur padding.
            return insets;
        });

        // PASUL 5: umple lista cu 3 filme demo.
        // DE CE ÎNAINTE de crearea adapterului: adapterul primește REFERINȚA la listă.
        //   Umplând lista înainte, adapterul vede deja 3 elemente la construcție și
        //   nu ai nevoie de `notifyDataSetChanged()`.
        // Dacă ai apela `addSampleMovies()` după `new ArrayAdapter(...)`, ar funcționa
        //   și atunci (aceeași referință), dar ar trebui un `notifyDataSetChanged()`
        //   ca ListView-ul să redeseneze.
        addSampleMovies();

        // PASUL 6: creează adapterul.
        // Parametri:
        //   1. `this` — Context-ul (MainActivity este un Context).
        //   2. `android.R.layout.simple_list_item_1` — un layout care conține UN TextView.
        //   3. `movies` — lista de date, ținută prin REFERINȚĂ, nu copiată.
        // DE CE `android.R.layout` și nu `R.layout`: este un layout din framework,
        //   nu din proiect.
        // DE CE `<>` (diamond): compilatorul deduce `ArrayAdapter<Movie>` din `movies`.
        movieAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, movies);

        // PASUL 7: leagă view-urile de câmpuri și atașează adapterul.
        // DE CE DUPĂ crearea adapterului: `initControls` face `listView.setAdapter(movieAdapter)`.
        //   Dacă `movieAdapter` ar fi null, `setAdapter(null)` ar lăsa lista goală.
        initControls();

        // PASUL 8: atașează listener-ii.
        // DE CE DUPĂ `initControls`: listener-ii au nevoie de `fabInsert` și `listView`,
        //   care sunt atribuite acolo. Înainte, ar fi null -> NPE la înregistrare.
        // OBSERVAȚIE: `launcher` este încă null în acest moment. Vezi pasul 9.
        initListeners();

        // PASUL 9: înregistrează callback-ul pentru rezultat.
        // ==============================================================================
        // DE CE AICI ȘI NU ÎNTR-UN LISTENER:
        //   `registerForActivityResult` trebuie apelat ÎNAINTE ca activitatea să
        //   ajungă în starea STARTED. Altfel primești:
        //   "LifecycleOwner ... is attempting to register while current state is RESUMED.
        //    LifecycleOwners must call register before they are STARTED."
        //   De aceea înregistrarea stă în `onCreate`, nu în `onClick`.
        //
        // FLUXUL:
        //   [onCreate] registerForActivityResult(contract, callback)
        //        |
        //        v
        //   [click FAB] launcher.launch(intent)
        //        |
        //        v
        //   ItemActivity se deschide; MainActivity -> onPause
        //        |
        //        v
        //   ItemActivity: setResult(RESULT_OK, intent) + finish()
        //        |
        //        v
        //   MainActivity revine: onStart -> onResume
        //        |
        //        v
        //   callback.onActivityResult(result)   <-- aici se modifică lista
        // ==============================================================================
        launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),

                // Callback-ul este o clasă anonimă. Se poate scrie și cu lambda, dar
                // sursa folosește forma explicită — utilă la examen pentru claritate.
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult o) {

                        // VERIFICARE OBLIGATORIE: a venit un rezultat bun?
                        // Dacă utilizatorul apasă Back, `ItemActivity` se închide cu
                        // RESULT_CANCELED, iar `o.getData()` este null.
                        // Fără acest `if`, următorul `intent.getSerializableExtra`
                        // ar arunca NullPointerException.
                        if(o.getResultCode() == RESULT_OK)
                        {
                            // Extrage Intent-ul de rezultat. Poate fi null dacă
                            // activitatea a apelat `setResult(RESULT_OK)` fără Intent —
                            // aici nu se întâmplă, dar este un risc teoretic.
                            Intent intent = o.getData();

                            // Citește obiectul serializat. Cast-ul este necesar pentru
                            // că `getSerializableExtra` întoarce `Serializable`.
                            // RISC: dacă obiectul nu ar fi `Movie`, cast-ul ar arunca
                            //   `ClassCastException` la runtime.
                            Movie movie = (Movie) intent.getSerializableExtra(MOVIE_EXTRA);

                            // Citește poziția. Valoarea implicită `-1` este CHEIA
                            // întregului protocol: -1 înseamnă „nu există poziție",
                            // adică insert.
                            int position = intent.getIntExtra(POSITION_EXTRA, -1);

                            // MOD INSERT: ItemActivity nu a trimis poziție.
                            if(position == -1)
                            {
                                // Adaugă la finalul listei.
                                movies.add(movie);

                                // Anunță adapterul că datele s-au schimbat.
                                // FĂRĂ ACEASTĂ LINIE: filmul este în listă, dar
                                // ListView-ul nu se redesenează și utilizatorul
                                // nu vede nimic până la următoarea recreare.
                                movieAdapter.notifyDataSetChanged();
                            }
                            // MOD EDIT: ItemActivity a trimis poziția.
                            else
                            {
                                // Înlocuiește elementul de la poziția respectivă.
                                // `set` nu schimbă dimensiunea listei.
                                movies.set(position, movie);

                                // Și aici este obligatoriu: `set` nu notifică nimeni.
                                movieAdapter.notifyDataSetChanged();
                            }
                        }

                        // Dacă resultCode != RESULT_OK, nu facem nimic. Lista rămâne
                        // neatinsă, exact ca înainte de deschiderea formularului.
                    }
                });
    }

    // ==================================================================================
    // initListeners — CE SE ÎNTÂMPLĂ LA INTERACȚIUNE
    // ==================================================================================
    // Două acțiuni: FAB (insert) și click pe rând (edit).
    // Ambele pornesc ACEEAȘI activitate, cu extra-uri diferite.
    // ==================================================================================
    private void initListeners() {

        // FAB -> INSERT
        // Clasă anonimă `View.OnClickListener` cu o singură metodă `onClick`.
        fabInsert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // `MainActivity.this` și NU `this`.
                // ==========================================================================
                // DE CE: în interiorul clasei anonime, `this` este INSTANȚA LISTENER-ULUI,
                //   nu activitatea. `new Intent(this, ItemActivity.class)` ar căuta un
                //   constructor `Intent(View.OnClickListener, Class)` — inexistent —
                //   și NU ar compila.
                // `MainActivity.this` este referința calificată către instanța exterioară,
                //   care ESTE un `Context`.
                // ==========================================================================
                Intent intent = new Intent(MainActivity.this, ItemActivity.class);

                // Pornește activitatea PRIN LAUNCHER, nu prin `startActivity`.
                // DE CE: `startActivity` pornește ecranul, dar nu are unde să întoarcă
                //   rezultatul. `launcher.launch` știe cine primește răspunsul.
                // FĂRĂ EXTRA-URI: ItemActivity vede `existing == null` -> insert mode.
                launcher.launch(intent);
            }
        });

        // CLICK PE RÂND -> EDIT
        // `AdapterView.OnItemClickListener` are o singură metodă cu 4 parametri.
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                // Parametrii:
                //   `parent`   — ListView-ul (aici nu e folosit)
                //   `view`     — rândul apăsat (aici nu e folosit)
                //   `position` — indexul din adapter; ASTA ne interesează
                //   `id`       — id-ul rândului; pentru ArrayAdapter este egal cu poziția

                // Același tip de Intent: aceeași activitate, alt conținut.
                Intent intent = new Intent(MainActivity.this, ItemActivity.class);

                // Trimite filmul selectat. `putExtra(String, Serializable)` este
                // suprascrierea aleasă pentru că `Movie implements Serializable`.
                // OBIECTUL ESTE SERIALIZAT — în ItemActivity ajunge o COPIE.
                intent.putExtra(MOVIE_EXTRA, movies.get(position));

                // Trimite și poziția. Ea este singurul lucru care distinge edit de insert.
                // `int` -> `putExtra(String, int)`.
                intent.putExtra(POSITION_EXTRA, position);

                // Pornește pentru rezultat, exact ca la insert.
                launcher.launch(intent);
            }
        });
    }

    // ==================================================================================
    // initControls — LEGĂTURA XML <-> COD
    // ==================================================================================
    // Trei linii, trei responsabilități: două `findViewById` și o atașare de adapter.
    // ==================================================================================
    private void initControls() {

        // `findViewById` caută în arborele inflat la `setContentView`.
        // În Java modern, metoda este generică: tipul se deduce din variabila din stânga.
        //   Echivalentul vechi: `(FloatingActionButton) findViewById(R.id.floatingActionButton)`.
        // Dacă ID-ul nu există în layout, întoarce `null` FĂRĂ eroare — NPE la primul acces.
        fabInsert = findViewById(R.id.floatingActionButton);

        // Același lucru pentru listă. ID-ul `listView` există în `activity_main.xml`.
        listView = findViewById(R.id.listView);

        // Atașează adapterul. Din acest moment, ListView-ul știe ce să afișeze.
        // DE CE ULTIMA LINIE: are nevoie de `listView` (deja atribuit) și de
        //   `movieAdapter` (atribuit în onCreate înainte de apelul acestei metode).
        listView.setAdapter(movieAdapter);
    }

    // ==================================================================================
    // addSampleMovies — DATELE DEMO
    // ==================================================================================
    // Trei filme construite cu constructorul cu 10 parametri. Aici se vede, în practică,
    // capcana pozițională descrisă în 22.1: fiecare apel are exact 10 argumente, în
    // ordinea din semnătură.
    //
    // DE CE EXISTĂ: ca să ai ce vedea la prima pornire, fără să introduci date manual.
    // DE CE ÎN COD ȘI NU ÎN RESURSE: sunt obiecte, nu texte; un array XML ar cere
    //   parsare și conversii de tip.
    // ==================================================================================
    private void addSampleMovies() {

        // FILMUL 1: Inception
        // Argumentele, în ordine:
        //   1. "Inception"                                          -> String title
        //   2. new GregorianCalendar(2010, Calendar.JULY, 16).getTime() -> Date release
        //   3. "Sci-Fi"                                             -> String genre
        //   4. 160000000                                            -> double budget
        //   5. 148                                                  -> int duration
        //   6. 4.5f                                                 -> float rating
        //   7. true                                                 -> boolean watched
        //   8. "PG-13"                                              -> String approval
        //   9. false                                                -> boolean onWatchlist
        //  10. "https://example.com/posters/inception.jpg"          -> String poster
        movies.add(new Movie(
                "Inception",

                // `GregorianCalendar(an, lună, zi)`:
                //   - anul este ABSOLUT (2010), nu relativ;
                //   - luna este 0-INDEXATĂ: `Calendar.JULY` = 6, nu 7. De aceea se
                //     folosesc constantele, nu numerele — `new GregorianCalendar(2010, 7, 16)`
                //     ar însemna 16 AUGUST;
                //   - ziua este 1-indexată, ca în viața reală.
                // `.getTime()` transformă Calendar-ul într-un `Date` (milisecunde).
                //   `GregorianCalendar` ESTE un `Calendar`, dar NU este un `Date`.
                new GregorianCalendar(2010, Calendar.JULY, 16).getTime(),

                // Genul — text simplu, nu trebuie să existe în `@array/movie_genre`.
                "Sci-Fi",

                // Bugetul: literal `int` (160 000 000) lărgit automat la `double`.
                // Nu are nevoie de sufix; `160000000.0` ar fi echivalent.
                160000000,

                // Durata: literal `int`, în minute.
                148,

                // Rating: literal `float`. Sufixul `f` este OBLIGATORIU.
                // Fără el, `4.5` ar fi `double` și nu s-ar potrivi la parametrul `float`
                // fără cast explicit.
                4.5f,

                // `watched` — film văzut.
                true,

                // `approval` — textul butonului radio, nu ID-ul.
                "PG-13",

                // `onWatchlist` — nu este pe lista de revăzut.
                false,

                // `poster` — URL fictiv. Nu este descărcat nicăieri în acest commit.
                "https://example.com/posters/inception.jpg"));

        // FILMUL 2: Toy Story
        // Aceeași structură, alte valori. `Calendar.NOVEMBER` = 10 (0-indexat).
        // `4f` este tot un literal `float`, doar fără zecimale.
        // ATENȚIE: genul „Animation" NU există în `@array/movie_genre` (Drama, Action,
        //   Comedy, Horror, Romance, Thriller, Sci-Fi, Adventure, Fantasy, Mystery).
        //   Consecința apare la editare: `fillForm` nu găsește potrivirea, spinner-ul
        //   rămâne pe „Drama", iar salvarea schimbă genul filmului. Vezi 22.3.
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

        // FILMUL 3: The Matrix
        // `Calendar.MARCH` = 2 (0-indexat). `5f` = rating maxim.
        // `watched = false` și `onWatchlist = true` — combinația „nu l-am văzut,
        // dar vreau să-l văd". Este singurul film din cele trei cu această combinație,
        // utilă pentru verificarea celor două controale independente.
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
