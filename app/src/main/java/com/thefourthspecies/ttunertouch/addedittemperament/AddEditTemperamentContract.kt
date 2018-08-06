package com.thefourthspecies.ttunertouch.addedittemperament

import com.thefourthspecies.ttunertouch.model.Note
import com.thefourthspecies.ttunertouch.model.Temperament
import com.thefourthspecies.ttunertouch.util.BasePresenter
import com.thefourthspecies.ttunertouch.util.BaseView

/**
 * Created by Graham on 2018-05-04.
 */
interface AddEditTemperamentContract {
    interface View : BaseView<Presenter> {
//        fun displayLineDetails()
        fun setTemperament(temperament: UITemperament)
    }

    interface Presenter : BasePresenter {

        val temperament: Temperament
        val uiNotes: Set<NoteCircle.UINote>
        val uiRelationships: Set<NoteCircle.UIRelationship>

        fun inputArc(fromNote: Note, toNote: Note, direction: Direction)
        fun inputLine(fromNote: Note, toNote: Note)
    }
}