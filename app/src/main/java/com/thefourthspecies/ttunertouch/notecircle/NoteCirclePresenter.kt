package com.thefourthspecies.ttunertouch.notecircle

import android.util.Log
import com.thefourthspecies.ttunertouch.edittemperament.EditTemperamentContract
import com.thefourthspecies.ttunertouch.model.Temperament
import com.thefourthspecies.ttunertouch.util.DEBUG_TAG

/* The Presenter for NoteCircleView */
internal class NoteCirclePresenter(
    temperament: Temperament
    val view: NoteCircleContract.View
): NoteCircleContract.Presenter {

    init {
        view.presenter = this
    }

    override fun start() {
        // nothing?
        view.temperament = NCTemperament.createFrom(temperament, view)
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