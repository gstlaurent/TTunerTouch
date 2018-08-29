package com.thefourthspecies.ttunertouch.notecircle

import android.support.v4.app.Fragment
import com.thefourthspecies.ttunertouch.edittemperament.EditTemperamentContract
import com.thefourthspecies.ttunertouch.model.Temperament
import android.os.Bundle
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import com.thefourthspecies.ttunertouch.R
import com.thefourthspecies.ttunertouch.edittemperament.DEFAULT_REFERENCE_NOTE
import kotlinx.android.synthetic.main.note_circle_fragment.*


class NoteCircleFragment : Fragment(), EditTemperamentContract.View {

    override lateinit var temperament: Temperament
    override lateinit var presenter: EditTemperamentContract.Presenter

    internal lateinit var noteCirclePresenter: NoteCirclePresenter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.note_circle_fragment, container, false)
        // with (root) {
        noteCircleView.textView = textView // TODO remove this
        val ncTemperament = NCTemperament.createFrom(temperament, noteCircleView, Order.DEFAULT, DEFAULT_REFERENCE_NOTE) // TODO: deal with default values
        noteCirclePresenter = NoteCirclePresenter(ncTemperament, noteCircleView)
        return root
    }

    override fun onResume() {
        super.onResume()
        presenter.start()
        noteCirclePresenter.start()
    }

}