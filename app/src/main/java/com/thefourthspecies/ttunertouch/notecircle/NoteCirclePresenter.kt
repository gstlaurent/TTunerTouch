package com.thefourthspecies.ttunertouch.notecircle

import android.util.Log
import com.thefourthspecies.ttunertouch.edittemperament.EditTemperamentContract
import com.thefourthspecies.ttunertouch.model.Note
import com.thefourthspecies.ttunertouch.model.Temperament
import com.thefourthspecies.ttunertouch.util.DEBUG_TAG

private val DEFAULT_TOP_NOTE = Note(Note.Letter.C)

internal enum class Order {
    FIFTHS,
    PITCH;

    companion object {
        const val NUM_FIFTHS = 12
        val DEFAULT = FIFTHS
    }
}

/* The Presenter for NoteCircleView */
internal class NoteCirclePresenter(
    val temperament: NCTemperament,
    val view: NoteCircleContract.View
): NoteCircleContract.Presenter {
    var topNote = DEFAULT_TOP_NOTE
    var order = Order.DEFAULT

    init {
        view.presenter = this
    }

    override fun start() {
        // nothing?
        view.temperament = temperament
    }


    override fun tapNote(note: NCNote) {
        Log.d(
            DEBUG_TAG,
            "tapNote: not implemented"
        )//To change body of created functions use File | Settings | File and Code Templates.
//TODO
    }

    override fun scrollNotesInside(fromNote: NCNote, toNote: NCNote) {
        Log.d(
            DEBUG_TAG,
            "scrollNotesInside: not implemented"
        )//To change body of created functions use File | Settings | File and Code Templates.
//TODO
    }

    override fun scrollNotesOutside(fromNote: NCNote, toNote: NCNote) {
        Log.d(
            DEBUG_TAG,
            "scrollNotesOutside: not implemented"
        )//To change body of created functions use File | Settings | File and Code Templates.
//TODO
    }

}