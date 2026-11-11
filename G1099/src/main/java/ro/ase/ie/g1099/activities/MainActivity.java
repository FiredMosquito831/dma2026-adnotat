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
 * DE CE DEPINDE: de `ListView`, `ArrayAdapter`, `Intent`, de Activity Result API
 *   și de modelul `Movie`. Nu depinde de niciun widget de formular.
 *
 * CINE DEPINDE DE EL: `ItemActivity`, care citește `MainActivity.MOVIE_EXTRA` și
 *   `MainActivity.POSITION_EXTRA` — cele două constante sunt contractul dintre ecrane.
 *
 * CE NU FACE: nu validează date, nu desenează rânduri personalizate, nu salvează
 *   nimic pe disc. Lista trăiește cât trăiește procesul.
 *
 * CE S-AR RUPE FĂRĂ EL: aplicația ar porni direct într-un formular fără listă,
 *   iar rezultatul salvat nu ar avea unde să se întoarcă.
 * ======================================================================================
 */

// PACHETUL: `activities` — stratul de UI. Modelul stă în `model`, nu aici.
package ro.ase.ie.g1099.activities;

// ======================================================================================
// IMPORTURI — GRUPATE PE SURSĂ
// ======================================================================================
// android.*        — clase din framework (Intent, Bundle, View, widget-uri)
// androidx.*       — biblioteci Jetpack (AppCompat, EdgeToEdge, insets, activity result)
// com.google.*     — Material Components (FloatingActionButton)
// java.util.*      — colecții și date
// ro.ase.ie.g1099  — modelul și clasa R generată
//
// REGULĂ DE CITIRE: fiecare import trebuie să aibă un consumator în fișier.
//   Două nu îl au — `Toast` și `Date`. Sunt marcate mai jos. Vezi 31.0.6.
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
//   Nu există niciun `Toast.makeText(...)` în tot fișierul. Import mort.
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
//   ATENȚIE: numele `Date` nu este scris nicăieri în corpul fișierului. `getTime()`
//   întoarce un `Date`, dar tipul este inferat, nu declarat. Import mort.
import java.util.Date;

// GregorianCalendar: constructorul cu (an, lună, zi) folosit pentru filmele demo.
import java.util.GregorianCalendar;

// List: tipul DECLARAT al listei. Interfața, nu implementarea.
import java.util.List;

// R: clasa generată din resurse. Fără ea, `R.layout.activity_main` nu există.
import ro.ase.ie.g1099.R;

// Movie: modelul de date.
import ro.ase.ie.g1099.model.Movie;

/*
 * ======================================================================================
 * BLOC DE TEORIE: CINE DEȚINE STAREA
 * ======================================================================================
 *
 * 1. **O singură sursă de adevăr.** `movies` este `final` și trăiește cât activitatea.
 *    `ItemActivity` nu are voie să modifice lista direct: primește o COPIE a unui
 *    film și trimite înapoi un `Movie` NOU. MainActivity decide ce face cu el.
 *
 * 2. **Adapterul ține REFERINȚA, nu o copie.** `new ArrayAdapter<>(..., movies)`
 *    primește lista, nu conținutul ei. De aceea `movies.add(...)` se vede în adapter
 *    imediat ce apelezi `notifyDataSetChanged()`. Dacă ai scrie
 *    `new ArrayList<>(movies)`, modificările nu s-ar mai vedea.
 *
 * 3. **Activity Result API înlocuiește `startActivityForResult`.** Nu mai există
 *    `onActivityResult` suprascris în activitate. Există un `launcher` cu un
 *    callback înregistrat o singură dată, în `onCreate`.
 *
 * 4. **Ordinea din `onCreate` este semantică.** `initListeners()` se apelează ÎNAINTE
 *    ca `launcher` să fie atribuit. Funcționează pentru că listener-ul citește
 *    CÂMPUL `launcher` la momentul click-ului, nu la momentul atașării.
 *
 * VIZUAL: două intrări, o singură ieșire
 *
 *   FAB apăsat  ----------------------+
 *                                     |
 *   rând apăsat ---+                  |
 *                  |                  v
 *                  |        new Intent(MainActivity.this, ItemActivity.class)
 *                  |                  |
 *                  |        (FAB: fără extra-uri  -> insert)
 *                  |        (rând: MOVIE + POSITION -> edit)
 *                  |                  |
 *                  +------------------+--> launcher.launch(intent)
 *                                             |
 *                                             v
 *                                     ItemActivity
 *                                             |
 *                                   setResult(RESULT_OK, intent)
 *                                             |
 *                                             v
 *                              callback: RESULT_OK?
 *                                  position == -1 -> movies.add(movie)
 *                                  position >= 0  -> movies.set(position, movie)
 *                                             |
 *                                             v
 *                                  movieAdapter.notifyDataSetChanged()
 * ======================================================================================
 */
public class MainActivity extends AppCompatActivity {

    // ==================================================================================
    // CELE DOUĂ CONSTANTE — CONTRACTUL DINTRE CELE DOUĂ ECRANE
    // ==================================================================================
    // CHEIA sub care călătorește filmul în Intent. `"movie"` este un String arbitrar;
    //   important este să fie ACEEAȘI valoare la trimitere și la citire.
    // DE CE `protected` și nu `private`: `ItemActivity` este în același pachet
    //   (`ro.ase.ie.g1099.activities`) și le citește ca `MainActivity.MOVIE_EXTRA`.
    //   `protected` acoperă și subclasele, dar aici pachetul este motivul real.
    // DE CE `static final`: sunt constante de CLASĂ, nu de instanță. Nu depind de
    //   niciun obiect MainActivity.
    // RISC: o greșeală de literă într-una din cele două locuri produce `null`
    //   la citire, fără nicio excepție și fără nicio eroare de compilare.
    // ==================================================================================
    protected static final String POSITION_EXTRA = "position";
    protected static final String MOVIE_EXTRA = "movie";

    // Butonul „+". Tipul este clasa Material, nu `ImageButton`.
    //   Este căutat în `initControls` și folosit în `initListeners`.
    private FloatingActionButton fabInsert;

    // Lista derulantă. Căutată în `initControls`, primește adapterul tot acolo.
    private ListView listView;

    // Launcher-ul de rezultat. NU este inițializat aici: primește valoarea abia
    //   la finalul lui `onCreate`, după `registerForActivityResult`.
    // DE CE un câmp și nu o variabilă locală: listener-ii din `initListeners`
    //   trebuie să îl poată citi la momentul click-ului, adică mult după `onCreate`.
    private ActivityResultLauncher<Intent> launcher;

    // LISTA — singura stare reală a aplicației.
    // `final` înseamnă că REFERINȚA nu se schimbă: nu poți scrie `movies = ...`.
    //   Conținutul, însă, se schimbă liber: `add`, `set`, `clear` sunt permise.
    // DE CE `List` și nu `ArrayList` la declarare: tipul declarat este interfața,
    //   implementarea este ascunsă. Schimbi `ArrayList` cu `LinkedList` fără să
    //   atingi restul fișierului.
    // DE CE `new ArrayList<>()` aici: lista există înainte de `onCreate`, deci
    //   `addSampleMovies()` o poate umple fără verificări de `null`.
    private final List<Movie> movies = new ArrayList<>();

    // ADAPTERUL. Ține REFERINȚA la `movies`, nu o copie.
    // Nu este inițializat aici pentru că are nevoie de `Context` (`this`), iar
    //   `this` este utilizabil abia după `super.onCreate`.
    private ArrayAdapter<Movie> movieAdapter;

    // ==================================================================================
    // onCreate — NOUĂ PAȘI, ÎN ORDINE
    // ==================================================================================
    // CE FACE: construiește ecranul, încarcă datele demo, creează adapterul, leagă
    //   listener-ii și înregistrează launcher-ul de rezultat.
    // CUM: 1. infrastructură (super, edge-to-edge, layout, insets)
    //      2. date (addSampleMovies)
    //      3. adapter
    //      4. controale (findViewById + setAdapter)
    //      5. listener-i
    //      6. launcher
    // RISC: `registerForActivityResult` trebuie apelat ÎNAINTE ca activitatea să
    //   fie STARTED. Aici este în `onCreate`, deci este legal. Mutat în `onResume`,
    //   ar arunca `IllegalStateException`.
    // REF: același șablon de insets apare și în `ItemActivity.onCreate`.
    // ==================================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Edge-to-edge: conținutul este desenat și sub bara de status și sub cea de
        // navigare. Fără el, aplicația ar avea o bandă proprie în partea de sus.
        EdgeToEdge.enable(this);

        // Umflă `activity_main.xml` în arborele de view-uri. Din acest moment,
        //   `findViewById` poate găsi `R.id.main`, `R.id.listView` și FAB-ul.
        setContentView(R.layout.activity_main);

        // Insets: măsoară barele de sistem și le transformă în padding pe container.
        // `(v, insets) -> { ... }` este o expresie lambda pentru
        //   `OnApplyWindowInsetsListener` — interfața are o singură metodă.
        // DE CE `R.id.main`: FrameLayout-ul rădăcină din `activity_main.xml`.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            // `systemBars()` = bara de status + bara de navigare, împreună.
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Padding în loc de margin: margin-ul ar fi ignorat de părintele FrameLayout.
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            // `return insets` = „nu am consumat nimic"; evenimentul poate merge mai departe.
            return insets;
        });

        // Pasul 2: umple lista cu cele 3 filme demo.
        // DE CE ÎNAINTE de crearea adapterului: ca adapterul să pornească deja cu 3
        //   elemente. Dacă ar fi după, ar funcționa la fel, dar ar cere un
        //   `notifyDataSetChanged()` suplimentar.
        addSampleMovies();

        // Pasul 3: creează adapterul.
        // `android.R.layout.simple_list_item_1` = un singur TextView pe rând, din
        //   framework. Nu este un layout al proiectului — de aceea prefixul `android.`.
        // Al treilea argument este LISTA, nu o copie a ei. Vezi blocul de teorie.
        movieAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, movies);

        // Pasul 4: leagă referințele la view-uri și instalează adapterul.
        initControls();

        // Pasul 5: atașează listener-ii.
        // ATENȚIE: `launcher` este încă `null` în acest moment. Este în regulă —
        //   listener-ii îl citesc abia la click, iar până atunci `onCreate` s-a terminat.
        initListeners();

        // Pasul 6: înregistrează launcher-ul.
        // `StartActivityForResult` este contractul care pornește o activitate și
        //   așteaptă un rezultat — echivalentul modern al lui `startActivityForResult`.
        // Callback-ul este o clasă anonimă, nu o expresie lambda, pentru că are
        //   două niveluri de indentare și un singur metodă cu corp lung.
        launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult o) {

                        // FILTRUL: rezultatul contează doar dacă activitatea secundară
                        //   a apelat `setResult(RESULT_OK, ...)`. `RESULT_CANCELED`
                        //   (butonul Back) intră pe ramura falsă și nu face nimic.
                        if(o.getResultCode() == RESULT_OK)
                        {
                            // `getData()` = Intent-ul construit în `ItemActivity.saveMovie`.
                            // RISC: dacă o activitate ar întoarce RESULT_OK fără date,
                            //   `intent` ar fi `null` și linia următoare ar arunca NPE.
                            //   Aici nu se întâmplă: `saveMovie` pune mereu un Intent.
                            Intent intent = o.getData();

                            // Citește filmul. Metoda este cea VECHE, cu cast explicit —
                            //   nu `IntentCompat`, cum folosește `ItemActivity`.
                            //   Este depreciată din API 33, dar funcționează.
                            // OBIECTUL ESTE O COPIE deserializată, nu originalul.
                            Movie movie = (Movie) intent.getSerializableExtra(MOVIE_EXTRA);

                            // Citește poziția. `-1` este valoarea implicită atunci când
                            //   extra-ul lipsește — adică exact în modul insert.
                            int position = intent.getIntExtra(POSITION_EXTRA, -1);

                            // RAMURA INSERT: nu există poziție.
                            if(position == -1)
                            {
                                // Adaugă la finalul listei.
                                movies.add(movie);

                                // Anunță adapterul. Fără acest apel, ListView-ul NU
                                //   știe că lista s-a schimbat și nu redesenează nimic.
                                movieAdapter.notifyDataSetChanged();
                            }
                            // RAMURA EDIT: există poziție.
                            else
                            {
                                // ÎNLOCUIEȘTE elementul de pe poziție. Nu `add` — ar
                                //   duplica filmul. Nu `remove` + `add` — ar schimba ordinea.
                                movies.set(position, movie);

                                // Același anunț. Ambele ramuri au nevoie de el.
                                movieAdapter.notifyDataSetChanged();
                            }
                        }
                    }
                });
    }

    // ==================================================================================
    // initListeners — CE SE ÎNTÂMPLĂ LA APĂSARE
    // ==================================================================================
    // CE FACE: atașează doi listener-i — unul pe FAB, unul pe rândurile listei.
    // CUM: 1. FAB -> Intent fără extra-uri -> insert
    //      2. rând -> Intent cu MOVIE + POSITION -> edit
    // RISC: metoda este apelată înainte ca `launcher` să fie atribuit. Funcționează
    //   doar pentru că listener-ii citesc câmpul la click, nu la atașare.
    // REF: același model de listener anonim apare de patru ori în `ItemActivity`.
    // ==================================================================================
    private void initListeners() {

        // Listener pe FAB. Clasa anonimă implementează `View.OnClickListener`.
        fabInsert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // `MainActivity.this` — NU `this`. În interiorul clasei anonime,
                //   `this` este listener-ul, nu activitatea. `Context`-ul cerut de
                //   constructorul lui Intent este activitatea.
                // Al doilea argument este clasa activității de pornit.
                Intent intent = new Intent(MainActivity.this, ItemActivity.class);

                // FĂRĂ extra-uri. Absența lui MOVIE_EXTRA este exact semnalul de
                //   „mod insert" pe care îl citește `ItemActivity.onCreate`.
                launcher.launch(intent);
            }
        });

        // Listener pe rândurile listei. `AdapterView.OnItemClickListener` are patru
        //   parametri: părintele, view-ul rândului, poziția și id-ul.
        // `id` este al patrulea și este IGNORAT aici: identitatea este poziția.
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                // Același Intent către aceeași activitate.
                Intent intent = new Intent(MainActivity.this, ItemActivity.class);

                // AICI este diferența față de FAB: filmul pleacă spre formular.
                //   `movies.get(position)` este obiectul din listă; `putExtra` îl
                //   SERIALIZEAZĂ. Formularul va primi o copie.
                intent.putExtra(MOVIE_EXTRA, movies.get(position));

                // Poziția pleacă separat, ca `int`. Ea nu face parte din model.
                intent.putExtra(POSITION_EXTRA, position);

                // Pornește activitatea prin launcher, ca rezultatul să ajungă în callback.
                launcher.launch(intent);
            }
        });
    }

    // ==================================================================================
    // initControls — FINDVIEWBYID ȘI SETADAPTER
    // ==================================================================================
    // CE FACE: leagă cele două câmpuri de view-urile din XML și instalează adapterul.
    // CUM: 1. FAB după id  2. ListView după id  3. setAdapter
    // RISC: dacă un id nu există în layout, `findViewById` întoarce `null` și
    //   apelul următor aruncă NPE. Aici ambele id-uri există în `activity_main.xml`.
    // REF: `ItemActivity.initControls` face același lucru pentru 13 view-uri.
    // ==================================================================================
    private void initControls() {

        // `R.id.floatingActionButton` — id-ul din XML, nu numele câmpului.
        fabInsert = findViewById(R.id.floatingActionButton);

        // `R.id.listView` — id-ul din XML.
        listView = findViewById(R.id.listView);

        // Instalează adapterul. Din acest moment, `android:entries="@array/items"`
        //   din XML este IGNORAT: `setAdapter` îl înlocuiește complet.
        listView.setAdapter(movieAdapter);
    }

    // ==================================================================================
    // addSampleMovies — CELE TREI FILME DEMO
    // ==================================================================================
    // CE FACE: adaugă trei `Movie` în listă, folosind constructorul cu 10 parametri.
    // CUM: trei apeluri `movies.add(new Movie(...))`, fiecare cu 10 argumente
    //   poziționale, în ordinea din semnătură.
    // RISC: ordinea argumentelor este POZIȚIONALĂ. `"Sci-Fi"` trebuie să fie al
    //   treilea, nu al doilea — altfel ajunge în `release` și nu compilează, sau
    //   ajunge în `genre` când tipurile coincid și compilează greșit.
    // REF: constructorul este adnotat în 31.2; tabelul de poziții este acolo.
    // ==================================================================================
    private void addSampleMovies() {

        // FILMUL 1 — Inception.
        // `new GregorianCalendar(an, lună, zi)` — ATENȚIE: luna este 0-based.
        //   `Calendar.JULY` este 6, nu 7. `getTime()` transformă calendarul în `Date`.
        // Argumentele 4..10: buget (int lărgit la double), durată, rating (`4.5f`),
        //   watched, approval, onWatchlist, posterUrl.
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

        // FILMUL 2 — Toy Story. `4f` este un float literal: fără `f`, ar fi double
        //   și nu ar încăpea în parametrul `float rating`.
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

        // FILMUL 3 — The Matrix. `Calendar.MARCH` este 2 (0-based).
        //   `watched = false`, `onWatchlist = true` — jedința combinație care diferă
        //   de primele două și care arată că cele două booleene sunt independente.
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
