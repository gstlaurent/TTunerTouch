package com.thefourthspecies.ttunertouch.notecircle

import com.thefourthspecies.ttunertouch.model.Note
import com.thefourthspecies.ttunertouch.model.Temper
import com.thefourthspecies.ttunertouch.model.Temperament
import com.thefourthspecies.ttunertouch.util.BasePresenter
import com.thefourthspecies.ttunertouch.util.BaseView


/**
 * Created by Graham on 2018-05-04.
 */
internal interface NoteCircleContract {
    interface View : BaseView<Presenter> {
        var temperament: NCTemperament
    }

    interface Presenter : BasePresenter {
        fun tapNote(note: NCNote)
        fun scrollNotesInside(fromNote: NCNote, toNote: NCNote)
        fun scrollNotesOutside(fromNote: NCNote, toNote: NCNote)
    }
}

