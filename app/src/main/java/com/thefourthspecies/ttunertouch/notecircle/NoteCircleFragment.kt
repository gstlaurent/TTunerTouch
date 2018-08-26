package com.thefourthspecies.ttunertouch.notecircle

import android.support.v4.app.Fragment
import com.thefourthspecies.ttunertouch.edittemperament.EditTemperamentContract
import com.thefourthspecies.ttunertouch.model.Temperament
import android.os.Bundle
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import com.thefourthspecies.ttunertouch.R
import kotlinx.android.synthetic.main.note_circle_fragment.*


class NoteCircleFragment : Fragment(), EditTemperamentContract.View {

    override lateinit var temperament: Temperament
    override lateinit var presenter: EditTemperamentContract.Presenter

    internal lateinit var noteCirclePresenter: NoteCirclePresenter

    override fun onResume() {
        super.onResume()
        presenter.start()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.note_circle_fragment, container, false)
        // with (root) {
        noteCircleView.textView = textView // TODO remove this
        noteCirclePresenter = NoteCirclePresenter(temperament, noteCircleView)
        return root
    }
}