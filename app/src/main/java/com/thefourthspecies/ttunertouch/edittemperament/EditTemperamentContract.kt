package com.thefourthspecies.ttunertouch.edittemperament

import com.thefourthspecies.ttunertouch.model.Note
import com.thefourthspecies.ttunertouch.model.Temper
import com.thefourthspecies.ttunertouch.model.Temperament
import com.thefourthspecies.ttunertouch.util.BasePresenter
import com.thefourthspecies.ttunertouch.util.BaseView

enum class Direction {
    ASCENDING,
    DESCENDING
}

/**
 * Created by Graham on 2018-05-04.
 */
interface EditTemperamentContract {
    interface View : BaseView<Presenter> {
        var temperament: Temperament
    }

    interface Presenter : BasePresenter {
        fun getDefaultTemper(fromNote: Note, toNote: Note): Temper
        fun getDefaultInterpolatedTemper(fromNote: Note, toNote: Note, direction: Direction): Temper
        fun setTemper(fromNote: Note, toNote: Note)
        fun setInterpolatedTemper(lowNote: Note, highNote: Note)

        fun splitNote(note: Note): Note
        fun renameNote(note: Note, name: String)
    }
}

//        fun displayLineDetails()





//        val temperament: Temperament
//        val uiNotes: Set<NoteCirclePresenter.UINote>
//        val uiRelationships: Set<NoteCirclePresenter.UIRelationship>
//
//        fun inputArc(fromNote: Note, toNote: Note, direction: Direction)
//        fun inputLine(fromNote: Note, toNote: Note)
//
