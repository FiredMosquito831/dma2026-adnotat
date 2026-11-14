/*
 * ======================================================================================
 * FILE: IMovieListener.java — THE ROW CLICK CONTRACT
 * ======================================================================================
 * WHAT THIS FILE IS:
 *   An interface with a single method. It says what someone who wants to be
 *   notified when the user touches a row in the list must know how to do.
 *
 * WHY IT EXISTS:
 *   `RecyclerView` has no `setOnItemClickListener`. The click is bound manually, on
 *   `itemView`, in the `ViewHolder` (38.2) — and from there it must reach somewhere.
 *   The interface is that “somewhere”, without the adapter knowing who the recipient is.
 *
 * WHAT IT DEPENDS ON:
 *   Only on `Movie` (the parameter's type). It does not depend on Android, on `View`,
 *   on `Context` or on any class in the app.
 *
 * WHO DEPENDS ON IT:
 *   - `MovieAdapter` — it has an `IMovieListener` field and calls it on click;
 *   - `MainActivity` — it implements it and passes itself to the adapter.
 *
 * WHAT IT DOES NOT DO:
 *   It does not know what happens after the click, does not start activities, does not keep the position.
 *   It is only a signature.
 *
 * WHAT BREAKS WITHOUT IT:
 *   The adapter would have to receive a `MainActivity` directly — that is, exactly the coupling
 *   the interface avoids. And if you changed the method's signature, the two
 *   places that depend on it (the adapter and the activity) would refuse to compile.
 * ======================================================================================
 */

// The `adapter` package, not `activities`: the contract belongs to the adapter, which asks for it.
//   MainActivity imports it from here. The direction of the dependency: activities -> adapter -> model.
package com.example.group1098.adapter;

// `Movie` is the type carried by the event — not `Object`, not `View`.
import com.example.group1098.model.Movie;

/*
 * ======================================================================================
 * THEORY BLOCK: THE INTERFACE AS CALLBACK (INVERSION OF CONTROL)
 * ======================================================================================
 *
 * 1. **The problem.** A list row is an inert view: it knows how to draw itself, it does not know
 *    what “my being touched” means. Someone must decide what happens on click —
 *    and that someone is the ACTIVITY, not the adapter.
 *
 * 2. **The naive solution.** The adapter could receive `MainActivity` directly and could
 *    build the `Intent` itself. It would work, but the adapter would depend on three things
 *    that have nothing to do with drawing a card: the `ItemActivity` class, the
 *    `MOVIE_EXTRA` / `POSITION_EXTRA` constants and a `Context` for `startActivity`.
 *
 * 3. **The interface solution.** The adapter depends on A SINGLE name: `IMovieListener`.
 *    It does not know who implements it, nor what happens after the call. The activity passes
 *    itself (`this`) and takes on the behaviour.
 *
 * 4. **Inversion of control.** The adapter does not call the activity “by name”; it calls a
 *    method from a contract, and the activity has decided to respond to it. Control
 *    over “what happens” is inverted compared with the normal flow of calls.
 *
 * 5. **The practical consequence at the exam.** For the requirement “bind the row click to
 *    the start of another activity”, the answer has exactly three pieces:
 *      1) the interface (this file),
 *      2) the call from the ViewHolder (`listener.onMovieClick(...)`),
 *      3) `implements` on the activity + `new MovieAdapter(movies, this)`.
 * ======================================================================================
 */
// `public interface`: public = visible from any package; interface = a contract without
//   implementation. The methods are implicitly `public abstract`.
public interface IMovieListener {

    // ==================================================================================
    // METHOD: onMovieClick — THE CONTRACT'S ONLY REQUIREMENT
    // ==================================================================================
    // WHAT IT DOES: it defines what someone who wants to hear about a row click receives.
    // HOW: it has no body — each implementation decides what to do (here: start the screen).
    // RISK: `Movie` can be null if the list contained a null element; `position`
    //   is the position in the list, valid only if the list has not changed in the meantime.
    // REF: the call from `MovieAdapter.MovieViewHolder` and the implementation in MainActivity.
    // ==================================================================================
    void onMovieClick(Movie movie, int position);
}
