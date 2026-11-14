/*
 * ======================================================================================
 * FIȘIER: MovieAdapter.java — ADAPTORUL LISTEI
 * ======================================================================================
 * CE ESTE ACEST FIȘIER:
 *   Puntea dintre `ArrayList<Movie>` (date) și `RecyclerView` (UI). Extinde
 *   `RecyclerView.Adapter<MovieAdapter.MovieViewHolder>` și conține două clase:
 *   adaptorul propriu-zis și `ViewHolder`-ul care ține referințele rândului.
 *
 * DE CE EXISTĂ:
 *   `RecyclerView` nu știe să afișeze un `Movie`. El știe doar să ceară:
 *   „câte elemente ai?" (`getItemCount`), „dă-mi o vedere de rând" (`onCreateViewHolder`)
 *   și „umple vederea asta cu elementul de la poziția N" (`onBindViewHolder`).
 *
 * DE CE DEPINDE:
 *   - `Movie` (modelul) și `IMovieListener` (contractul de click),
 *   - `R.layout.movie_item` (layout-ul rândului) și nouă ID-uri din el,
 *   - `Chip` din Material Components, `RatingBar` și `ImageView` din framework.
 *
 * CINE DEPINDE DE EL:
 *   `MainActivity` îl construiește (`new MovieAdapter(movies, this)`) și îl pasează
 *   lui `rvMovies.setAdapter(...)`.
 *
 * CE NU FACE:
 *   Nu deține lista (o primește prin referință), nu decide aranjarea rândurilor
 *   (o face `LayoutManager`-ul), nu știe ce se întâmplă la click — doar anunță
 *   listener-ul.
 *
 * CE S-AR RUPE FĂRĂ EL:
 *   `rvMovies.setAdapter(...)` nu ar avea argument; lista ar rămâne goală pe ecran,
 *   deși `movies` ar avea trei elemente.
 * ======================================================================================
 */

// Pachetul `adapter`: separat de `activities` și de `model`. Adaptorul depinde de amândouă,
//   dar niciuna dintre ele nu depinde de adaptor în afară de MainActivity.
package com.example.group1098.adapter;

// LayoutInflater: singurul mod de a transforma un XML într-un arbore de view-uri.
import android.view.LayoutInflater;

// View: tipul rădăcinii unui rând inflate și tipul parametrului din `onClick`.
import android.view.View;

// ViewGroup: părintele în care va sta rândul (RecyclerView-ul însuși).
//   Din el se ia `Context`-ul pentru inflate.
import android.view.ViewGroup;

// ImageView: posterul. Declarat în holder, dar NEUMPLUT în `onBindViewHolder`.
import android.widget.ImageView;

// RatingBar: stelele. Declarat în holder, dar NEUMPLUT — rămâne la valoarea din XML.
import android.widget.RatingBar;

// TextView: cele patru texte ale cardului (titlu, subtitlu, valoare rating, facts).
import android.widget.TextView;

// NonNull: adnotare de contract. Nu schimbă bytecode-ul; îi spune lui lint (și
//   cititorului) că parametrul nu are voie să fie null, iar returul nu este null.
import androidx.annotation.NonNull;

// RecyclerView: clasa de bază. `RecyclerView.Adapter` și `RecyclerView.ViewHolder`
//   sunt clasele interioare pe care le extindem aici.
import androidx.recyclerview.widget.RecyclerView;

// R: `R.layout.movie_item` și cele nouă ID-uri căutate cu `findViewById`.
import com.example.group1098.R;

// Movie: elementul afișat.
import com.example.group1098.model.Movie;

// Chip: eticheta rotunjită din Material Components. Trei bucăți pe card.
import com.google.android.material.chip.Chip;

// SimpleDateFormat: transformă `Date` în an, pentru subtitlul „gen. an".
import java.text.SimpleDateFormat;

// ArrayList: tipul listei primite prin constructor.
import java.util.ArrayList;

/*
 * ======================================================================================
 * BLOC DE TEORIE: CONTRACTUL RecyclerView.Adapter
 * ======================================================================================
 *
 * 1. **Trei metode obligatorii, nimic altceva:**
 *      - `getItemCount()`        -> câte rânduri există în total;
 *      - `onCreateViewHolder()`  -> construiește o vedere de rând GOALĂ;
 *      - `onBindViewHolder()`    -> umple o vedere EXISTENTĂ cu date.
 *
 * 2. **De ce două metode și nu una.** `onCreateViewHolder` este scump: citește XML-ul
 *    și construiește arborele de view-uri. Se apelează o singură dată pentru fiecare
 *    rând care încape pe ecran. `onBindViewHolder` este ieftin: doar `setText`.
 *    Se apelează de fiecare dată când un rând reapare la derulare. Dacă ai 10 000 de
 *    filme și 8 rânduri vizibile, se creează ~10 vederi și se leagă de mii de ori.
 *
 * 3. **Reciclarea.** Când un rând iese din ecran, vederea lui nu se aruncă: intră
 *    într-un bazin. Când e nevoie de un rând nou, se scoate o vedere din bazin și
 *    se re-leagă — fără inflate. De aici numele „RecyclerView".
 *
 *    ecran:  [0][1][2][3][4][5][6][7]      (8 vizibile)
 *    derulezi in jos:
 *       randul 0 iese  ->  vederea lui ajunge in bazin
 *       randul 8 intra <-  primeste vederea reciclata, re-legata cu datele lui 8
 *
 * 4. **ViewHolder = cache-ul de findViewById.** Fără el, fiecare bind ar căuta din nou
 *    opt view-uri prin arbore. `ViewHolder` le ține o dată, în câmpuri `final`.
 *    `RecyclerView` garantează că obiectul `ViewHolder` călătorește împreună cu vederea
 *    lui, deci `holder.title` este mereu TextView-ul rândului curent.
 *
 * 5. **Poziția nu este a ta.** `position` vine ca parametru la bind, dar la click nu
 *    mai există: se cere cu `getAbsoluteAdapterPosition()`. Între timp, lista se poate
 *    schimba, iar poziția salvată într-un câmp ar deveni greșită.
 *
 * 6. **`viewType`.** Aici este ignorat: toate rândurile arată la fel. Dacă ai avea două
 *    tipuri de carduri (film văzut / film nou), `getItemViewType(position)` ar decide,
 *    iar `onCreateViewHolder` ar primi un `viewType` diferit și ar inflata alt XML.
 * ======================================================================================
 */

// Adaptorul. Generic-ul din `<...>` este TIPUL HOLDER-ULUI, nu tipul elementului.
//   De aceea `RecyclerView.Adapter<MovieAdapter.MovieViewHolder>`: clasa interioară
//   este referită cu numele complet, pentru că apare în semnătura clasei care o conține.
public class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.MovieViewHolder> {

    // ==================================================================================
    // CELE DOUĂ REFERINȚE
    // ==================================================================================
    // Lista NU se copiază: adaptorul ține referința primită de la activitate.
    //   Consecința bună: `movies.add(...)` din MainActivity se vede aici imediat,
    //   fără să trebuiască reconstruit adaptorul.
    // Consecința periculoasă: dacă MainActivity ar înlocui lista cu alta
    //   (`movies = new ArrayList<>()`), adaptorul ar rămâne pe cea veche.
    // `final` = referința nu se reasignează. Conținutul se poate schimba liber.
    // Fără modificator de acces = vizibil în pachetul `adapter`.
    final ArrayList<Movie> movies;

    // Listener-ul de click: cine primește evenimentul. Aici, MainActivity.
    // RISC: nu se verifică `null`. Dacă ai construi adaptorul cu `null`, totul ar
    //   funcționa până la primul click, când ai lua NullPointerException.
    final IMovieListener listener;

    // ==================================================================================
    // CONSTRUCTORUL — DOAR DOUĂ ATRIBUIRI
    // ==================================================================================
    // CE FACE: primește lista și listener-ul și le ține pentru toată viața adaptorului.
    // CUM: două atribuiri, fără logică.
    // RISC: nu se clonează lista. Dacă ai vrea o copie, ar trebui `new ArrayList<>(movies)`
    //   — dar atunci modificările din activitate nu s-ar mai vedea în listă.
    // REF: `new MovieAdapter(movies, this)` în MainActivity, 38.1.
    // ==================================================================================
    public MovieAdapter(ArrayList<Movie> movies, IMovieListener listener) {

        // `this.movies` = câmpul; `movies` = parametrul. Aceeași capcană ca la `Movie`.
        this.movies = movies;

        // Același model pentru listener.
        this.listener = listener;
    }

    // ==================================================================================
    // METODA: onCreateViewHolder — CONSTRUCȚIA UNUI RÂND GOL
    // ==================================================================================
    // CE FACE: umflă `movie_item.xml` într-o vedere nouă și o învelește într-un holder.
    // CUM: 1) ia inflater-ul din contextul părintelui; 2) umflă layout-ul cu `false`;
    //   3) întoarce un `MovieViewHolder` nou.
    // RISC: al treilea argument trebuie să fie `false`. Cu `true`, vederea ar fi lipită
    //   imediat de părinte, iar RecyclerView ar încerca să o adauge din nou -> crash.
    // REF: capitolul 23, exercițiul despre `ViewHolder` — același cod, pas cu pas.
    // ==================================================================================
    @NonNull
    @Override
    public MovieViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        // 1. Inflater-ul se ia DIN CONTEXTUL PĂRINTELUI, nu din activitate: adaptorul
        //    nu are și nu trebuie să aibă o referință către Activity. `parent.getContext()`
        //    este activitatea care găzduiește RecyclerView-ul, deci tema este cea corectă.
        // 2. `inflate(R.layout.movie_item, parent, false)`:
        //      - primul argument: XML-ul rândului;
        //      - al doilea: părintele, din care se iau `LayoutParams`-ul corect;
        //      - al treilea `false`: NU atașa acum. RecyclerView decide când.
        //    „Nu atașa" este răspunsul corect în 99% din cazurile de adaptor.
        // 3. Rezultatul este o vedere de card, încă fără nicio dată în ea.
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.movie_item, parent, false);

        // 4. Se învelește în holder. Din acest moment, `holder.title` etc. sunt valide:
        //    constructorul holder-ului face toate `findViewById`-urile.
        return new MovieViewHolder(view);
    }

    // ==================================================================================
    // METODA: onBindViewHolder — UMPLEREA RÂNDULUI CU DATE
    // ==================================================================================
    // CE FACE: ia filmul de la poziția dată și scrie în holder titlul și subtitlul.
    // CUM: 1) un formatator de an; 2) filmul; 3) titlul; 4) „gen. an".
    // RISC: doar DOUĂ câmpuri sunt umplute din nouă. Restul rămân cu valorile din XML
    //   — vezi BUG-ul de mai jos.
    // REF: capitolul 22, 22.6 — `movie_item.xml`, cardul care se vede acum.
    // ==================================================================================
    @Override
    public void onBindViewHolder(@NonNull MovieViewHolder holder, int position) {

        // 1. Formatator pentru an. Se creează LA FIECARE BIND — un obiect nou pentru
        //    fiecare rând, de fiecare dată când rândul reapare la derulare.
        // RISC: `SimpleDateFormat` este scump de construit. Corect ar fi un câmp
        //   `private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy");`
        //   construit o singură dată. Aici, cu trei filme, nu se observă.
        // ATENȚIE: `"yyyy"` este an calendaristic. `"YYYY"` ar fi an de săptămână
        //   (ISO week-year) și ar da 2027 pentru câteva zile din decembrie 2026.
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy");

        // 2. Filmul de la poziția cerută. `position` este dat de RecyclerView și este
        //    garantat în intervalul 0..getItemCount()-1 în timpul unui bind.
        Movie movie = movies.get(position);

        // 3. Titlul. `setText` acceptă CharSequence; `getTitle()` întoarce String.
        holder.title.setText(movie.getTitle());

        // 4. Subtitlul: „gen. an". Trei observații, toate verificabile:
        //    - `movie.getGenre()` este deja `String`; `.toString()` pe un String
        //      întoarce ACELAȘI obiect — este redundant, dar inofensiv.
        //      RISC: dacă `genre` ar fi null, apelul ar arunca NullPointerException.
        //      DANGER!: excepția izbucnește în onBindViewHolder, în timpul derulării,
        //        nu la pornire — un crash care apare abia când lista ajunge la filmul
        //        respectiv. Aici `addSampleMovies` și formularul pun mereu gen, deci nu
        //        se întâmplă; dar modelul permite `null` prin constructorul gol.
        //    - separatorul este „. " (punct și spațiu), nu „ · " cum sugerează
        //      comentariul din `movie_item.xml`. Rezultatul vizibil: „Sci-Fi. 2010".
        //    - `sdf.format(movie.getRelease())` cere un `Date` nenull. Dacă filmul
        //      ar avea data null (posibil prin constructorul gol), ar arunca NPE.
        holder.subtitle.setText(movie.getGenre().toString() + ". " + sdf.format(movie.getRelease()));

        // OBSERVAȚIE: metoda se termină aici. Nu există apel `super.onBindViewHolder`.
        //   Nu este obligatoriu — clasa de bază nu face nimic în el.
    }

    // ==================================================================================
    // METODA: getItemCount — CÂTE RÂNDURI EXISTĂ
    // ==================================================================================
    // CE FACE: întoarce numărul de elemente din listă.
    // CUM: `movies.size()`, citit la fiecare apel — deci mereu la zi.
    // RISC: dacă ai returna o valoare constantă sau un câmp actualizat manual,
    //   RecyclerView ar cere bind-uri pentru poziții inexistente -> IndexOutOfBounds.
    // REF: `notifyDataSetChanged()` din MainActivity — anunțul care face RecyclerView
    //   să întrebe din nou această metodă.
    // ==================================================================================
    @Override
    public int getItemCount() {

        // Sursa de adevăr este lista, nu un contor paralel.
        return movies.size();
    }

    /*
     * ==================================================================================
     * BUG: CARDUL AFIȘEAZĂ DATE STATICE PENTRU TOATE FILMELE
     * ==================================================================================
     * `MovieViewHolder` caută nouă view-uri, dar `onBindViewHolder` scrie doar în două:
     * `title` și `subtitle`. Celelalte șapte rămân exact cum au fost inflate din XML:
     *
     *   din XML (aceeași valoare pe FIECARE rând)     din model (ignorat)
     *   --------------------------------------------  ------------------------
     *   movieFacts      = "142 min  ·  $160M"          duration, budget
     *   movieRatingValue= "4.5"                        rating
     *   movieRating     = 0 stele (fără `rating=` in XML)  rating
     *   chipApproval    = "Approved"                   approval
     *   chipWatched     = "Watched"                    watched
     *   chipWatchlist   = "Watchlist"                  onWatchlist
     *   moviePoster     = iconița galeriei             posterUrl
     *
     * Consecința: Inception (148 min, $160M, PG-13) și Toy Story (81 min, $30M, G)
     * apar cu ACELEAȘI facts-uri și ACELEAȘI etichete. Datele sunt în model, dar nu
     * ajung niciodată pe ecran. Nu este o eroare de sintaxă: compilează și rulează.
     * ==================================================================================
     */

    // ==================================================================================
    // CLASA INTERIOARĂ: MovieViewHolder
    // ==================================================================================
    // CE FACE: ține referințele celor nouă view-uri ale cardului și leagă click-ul.
    // CUM: `findViewById` o singură dată, în constructor; click pe `itemView`.
    // RISC: este o clasă interioară NEstatică — ține o referință implicită către adaptor.
    //   Aici este convenabil (vede `movies` și `listener` direct), dar înseamnă că
    //   holder-ul nu poate trăi fără adaptor.
    // REF: `RecyclerView.ViewHolder`, care ține `itemView` — vederea rândului întreg.
    // ==================================================================================
    class MovieViewHolder extends RecyclerView.ViewHolder {

        // ==============================================================================
        // CELE NOUĂ CÂMPURI — CACHE-UL DE findViewById
        // ==============================================================================
        // Toate `final`: se atribuie o dată, în constructor, și nu se mai schimbă.
        //   Un rând nou = un holder nou = un set nou de referințe.
        // Cele nouă corespund celor nouă ID-uri din `movie_item.xml` pe care le folosește
        //   codul. În layout mai există două ID-uri pe care holder-ul NU le caută:
        //   `movieStatusGroup` (ChipGroup-ul) și `chipCiocan` (al patrulea chip).
        // ==============================================================================

        // Posterul. Căutat, dar niciodată umplut — vezi BUG-ul de mai sus.
        final ImageView poster;

        // Titlul — UMPLUT în `onBindViewHolder`.
        final TextView title;

        // Subtitlul „gen. an" — UMPLUT în `onBindViewHolder`.
        final TextView subtitle;

        // Stelele. Niciodată setate: `android:rating` nu există în XML, deci 0 stele.
        final RatingBar rating;

        // Valoarea numerică a ratingului. Rămâne „4.5" din XML pentru orice film.
        final TextView ratingValue;

        // „durata · bugetul". Rămâne „142 min · $160M" din XML.
        final TextView facts;

        // Cele trei etichete de stare. Raman textele din XML.
        final Chip chipApproval;
        final Chip chipWatched;
        final Chip chipWatchList;

        // ==============================================================================
        // CONSTRUCTORUL HOLDER-ULUI
        // ==============================================================================
        // CE FACE: leagă fiecare câmp de view-ul lui din rândul primit și instalează
        //   listener-ul de click pe rândul întreg.
        // CUM: `super(itemView)` obligatoriu, apoi nouă `findViewById`, apoi un listener.
        // RISC: `findViewById` pe `itemView`, NU pe activitate. Dacă ai căuta în
        //   activitate, ai găsi primul view cu acel ID din tot ecranul — nu neapărat
        //   din rândul curent.
        // REF: capitolul 22, 22.6 — ID-urile căutate aici sunt exact cele din layout.
        // ==============================================================================
        public MovieViewHolder(@NonNull View itemView) {

            // `super(itemView)` este obligatoriu: clasa de bază păstrează vederea
            //   rândului și o folosește pentru poziționare și pentru animații.
            super(itemView);

            // Fiecare `findViewById` întoarce `View`; atribuirea într-un câmp de tip
            //   concret face cast-ul automat (generics), fără `(TextView)` scris de mână.
            //   Dacă ID-ul ar lipsi din layout, s-ar întoarce `null` — fără eroare la
            //   compilare, doar un NPE la primul bind.
            poster = itemView.findViewById(R.id.moviePoster);
            title = itemView.findViewById(R.id.movieTitle);
            subtitle = itemView.findViewById(R.id.movieSubtitle);
            rating = itemView.findViewById(R.id.movieRating);
            ratingValue = itemView.findViewById(R.id.movieRatingValue);
            facts = itemView.findViewById(R.id.movieFacts);
            chipApproval = itemView.findViewById(R.id.chipApproval);
            chipWatched = itemView.findViewById(R.id.chipWatched);
            chipWatchList = itemView.findViewById(R.id.chipWatchlist);

            // ==========================================================================
            // CLICK-UL PE RÂND
            // ==========================================================================
            // Aici este întregul mecanism care înlocuiește `setOnItemClickListener`.
            // Se leagă pe `itemView` — cardul întreg, nu pe un text anume.
            // DE CE în constructor și nu în bind: listener-ul este același pentru toată
            //   viața vederii; legarea lui la fiecare bind ar aloca un obiect degeaba.
            // ==========================================================================
            itemView.setOnClickListener(new View.OnClickListener() {

                // Se execută la apăsare. `v` este cardul.
                @Override
                public void onClick(View v) {

                    // Poziția se cere ACUM, nu se ține într-un câmp.
                    // `getAbsoluteAdapterPosition()` întoarce poziția în lista
                    //   adaptorului, sau `NO_POSITION` (-1) dacă rândul a fost scos
                    //   din listă între timp (animație de ștergere, listă schimbată).
                    int pos = getAbsoluteAdapterPosition();

                    // Garda: poziție validă? Fără ea, `movies.get(-1)` ar arunca
                    //   IndexOutOfBoundsException. Aici nu se șterg elemente, dar
                    //   garda este obligatorie ca tipar.
                    if (pos != RecyclerView.NO_POSITION) {

                        // Filmul de la poziția respectivă.
                        Movie movie = movies.get(pos);

                        // Anunțul către activitate. `listener` este MainActivity.
                        //   Din acest apel se ajunge în `onMovieClick`, care pornește
                        //   ItemActivity cu două extra-uri (38.1).
                        listener.onMovieClick(movie, pos);
                    }
                }
            });
        }
    }
}
