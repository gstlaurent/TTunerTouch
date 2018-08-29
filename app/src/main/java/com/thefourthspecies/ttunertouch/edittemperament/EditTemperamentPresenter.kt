package com.thefourthspecies.ttunertouch.edittemperament

import android.util.Log
import android.content.DialogInterface
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import com.thefourthspecies.ttunertouch.model.*
import com.thefourthspecies.ttunertouch.notecircle.NoteCirclePresenter
import com.thefourthspecies.ttunertouch.util.DEBUG_TAG



/**
 * Created by Graham on 2018-02-21.
 */
class EditTemperamentPresenter(
    private val temperament: Temperament,
    private val view: EditTemperamentContract.View
) : EditTemperamentContract.Presenter {


    init {
        view.presenter = this
    }


    override fun start() {
        view.temperament = temperament
    }

    override fun getDefaultTemper(fromNote: Note, toNote: Note): Temper {
        // TODO
        Log.d(DEBUG_TAG, "getDefaultTemper: not implemented")
        return Temper(Interval.PERFECT_FIFTH, Comma.PYTHAGOREAN, Fraction(1, 6), Temper.Direction.NARROWER)
    }

    override fun getDefaultInterpolatedTemper(
        fromNote: Note,
        toNote: Note,
        direction: Direction
    ): Temper {
        //TODO
        Log.d(DEBUG_TAG, "getDefaultInterpolatedTemper: not implemented")
        return Temper(Interval.PERFECT_FIFTH, Comma.PYTHAGOREAN, Fraction(1, 6), Temper.Direction.NARROWER)
    }

    override fun setTemper(fromNote: Note, toNote: Note) {
        //TODO
        Log.d(
            DEBUG_TAG,
            "setTemper: not implemented"
        )//To change body of created functions use File | Settings | File and Code Templates.
    }

    override fun setInterpolatedTemper(lowNote: Note, highNote: Note) {
        //TODO
        Log.d(
            DEBUG_TAG,
            "setInterpolatedTemper: not implemented"
        )//To change body of created functions use File | Settings | File and Code Templates.
    }

    override fun renameNote(note: Note, name: String) {
        //TODO
        Log.d(
            DEBUG_TAG,
            "renameNote: not implemented"
        )//To change body of created functions use File | Settings | File and Code Templates.
    }

    override fun splitNote(note: Note): Note {
        // TODO
        Log.d(
            DEBUG_TAG,
            "splitNote: not implemented"
        ) //To change body of created functions use File | Settings | File and Code Templates.
        return note
    }

    init {
        view.presenter = this
    }









//    override fun input(touchInput: TouchInput) {
//        Log.d(DEBUG_TAG, "Inputting touch event: $touchInput")
//
//        val fromNote = touchInput.fromNote
//        val toNote = touchInput.toNote
//
//        if (fromNote == null || toNote == null) {
//            Log.d(DEBUG_TAG, "Ignoring TouchInput. fromNote=$fromNote and toNote=$toNote must both be non-null")
//            return
//        }
//
//        if (touchInput.isDirect) {
//            inputLine(fromNote, toNote)
//        } else {
//            inputArc(fromNote, toNote, touchInput.direction)
//        }
//    }
//
//    override fun inputLine(fromNote: Note, toNote: Note) {
//        view.displayLineDetails()
//        // Display single input dialog with default values
//        val dialog = FireMissilesDialogFragment()
//        val fm = fragmentManager
//        if (fm == null) {
//            Log.d(DEBUG_TAG, "fragmentManager is null")
//        } else {
//            dialog.show(fm, "Input Line Fragment Test")
//        }
//    }
//
//    override fun inputArc(fromNote: Note, toNote: Note, direction: Direction) {
//        com.thefourthspecies.ttunertouch.util.assert(order == Order.FIFTHS) {
//            "Can only input by arc if circle of FIFTHS. Actual: $order"
//        }
//        // Display group input dialog with default values
////        val dialog = FireMissilesDialogFragment()
////        dialog.show(fragmentManager, "Input Arc Fragment Test")
//    }





//
//    fun input(input: TouchInput) {
//        Log.d(DEBUG_TAG, "Inputting touch event: $input")
//
//        val fromNote = input.fromNote
//        val toNote = input.toNote
//        if (fromNote != null && toNote != null) {
//            val interval = fromNote.intervalTo(toNote)
//            if (interval != null) {
//                val temper = if (input.isDirect)
//                    Temper(interval)
//                else
//                    Temper(interval, Comma.PYTHAGOREAN, Fraction(1, 6), Temper.Change.NARROWER)
//
//                // Do it:
//                Log.d(DEBUG_TAG, "Setting relationship: fromNote=$fromNote, toNote=$toNote, temper=$temper")
//
//                defaultNotes.remove(fromNote)
//                defaultNotes.remove(toNote)
//                temperament.setRelationship(fromNote, toNote, temper)
//                update()
//            }
//
//        }
//    }







}


class FireMissilesDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Use the Builder class for convenient dialog construction
        com.thefourthspecies.ttunertouch.util.assert(activity != null) {
            "Activity is null"
        }
        val builder = AlertDialog.Builder(activity)
//        builder.setMessage(R.string.dialog_fire_missiles)
//                .setPositiveButton(R.string.fire, DialogInterface.OnClickListener { dialog, id ->
//                    // FIRE ZE MISSILES!
//                })
//                .setNegativeButton(R.string.cancel, DialogInterface.OnClickListener { dialog, id ->
//                    // User cancelled the dialog
//                })
//

        builder.setMessage("Fire missiles?")
                .setPositiveButton("fire", DialogInterface.OnClickListener { dialog, id ->
                    Log.d(DEBUG_TAG, "FIRE ZE MISSILES!")
                })
                .setNegativeButton("CANCEL", DialogInterface.OnClickListener { dialog, id ->
                    Log.d(DEBUG_TAG, "User cancelled the dialog")
                })
        // Create the AlertDialog object and return it
        return builder.create()
    }
}
