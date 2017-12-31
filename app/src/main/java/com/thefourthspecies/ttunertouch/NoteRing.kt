package com.thefourthspecies.ttunertouch

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import kotlin.properties.Delegates
import kotlin.properties.ReadWriteProperty
import android.graphics.Paint
import android.graphics.Rect


const val FLAT = "♭" // U+266D
const val SHARP = "♯" // U+266F

//const val FLAT = "b" // U+266D
//const val SHARP = "#" // U+266F


/**
 * Created by Graham on 2017-12-25.
 */
class NoteRing @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Custom attributes
    private var mLabelColor: Int by attributable(0)
    private var mLabelHeight: Float by attributable(0.0f)
    private var mLabelWidth: Float by attributable(0.0f)
    private var mLineColor: Int by attributable(0)
    private var mLineThickness: Float by attributable(0.0f)
    private var mDotColor: Int by attributable(0)
    private var mDotRadiusRatio: Float by attributable(0.0f) // Dot Radius to Inner Radius
    private var mInnerRadiusRatio: Float by attributable(0.0f) // Inner Radius to Outer Radius

    // Paint Objects
    lateinit var mLinePaint: Paint
    lateinit var mLabelPaint: Paint
    lateinit var mDotPaint: Paint

    // Dimensions
    private val _textBounds = Rect() // For centering labels
    private var mOuterRadius = 0.0f
    private var mInnerRadius = 0.0f
    private var mDotRadius = 0.0f
    private var mLabelRadius = 0.0f
    private var mCenterX = 0.0f
    private var mCenterY = 0.0f

    // Note Data
    data class Note(val position: Double, val name: String)
    var mNotes: MutableList<Note> = mutableListOf()



    init {
        val a = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.NoteRing,
            0, 0)
        try {
            mLabelColor = a.getColor(R.styleable.NoteRing_labelColor, -0x1000000)
            mLabelHeight = a.getDimension(R.styleable.NoteRing_labelHeight, 0.0f)
            mLabelWidth = a.getDimension(R.styleable.NoteRing_labelHeight, 0.0f) // todo: remove?
            mLineColor = a.getColor(R.styleable.NoteRing_lineColor, -0x1000000)
            mLineThickness = a.getDimension(R.styleable.NoteRing_lineThickness, 0.0f)
            mDotColor = a.getColor(R.styleable.NoteRing_dotColor, -0x1000000)
            mDotRadiusRatio = a.getFloat(R.styleable.NoteRing_dotRadiusRatio, 0.0f)
            mInnerRadiusRatio = a.getFloat(R.styleable.NoteRing_innerRadiusRatio, 0.5f)
        } finally {
            a.recycle()
        }

        initPaint()

        initTestData()
    }

    fun initPaint() {
        mLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mLabelPaint.textSize = mLabelHeight
        mLabelPaint.color = mLabelColor

        mLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mLinePaint.color = mLineColor
        mLinePaint.style = Paint.Style.STROKE
        mLinePaint.strokeWidth = mLineThickness

        mDotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mDotPaint.color = mDotColor
    }

    fun initTestData() {
        mNotes = mutableListOf(
                Note(0.0, "A"),
                Note(0.1, "B$FLAT"),
                Note(0.2, "B"),
                Note(0.25, "C"),
                Note(0.3, "C$SHARP"),
                Note(0.4, "D"),
                Note(0.5, "E$FLAT"),
                Note(0.6, "E"),
                Note(0.7, "F"),
                Note(0.75, "F$SHARP"),
                Note(0.8, "G"),
                Note(0.9, "A$FLAT")
        )
    }




    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // Account for padding
        val xpad = (paddingLeft + paddingRight).toFloat()
        val ypad = (paddingTop + paddingBottom).toFloat()

        val ww = w.toFloat() - xpad
        val hh = h.toFloat() - ypad

        // Calculate inner and outer circle bounds for the ring
        val outerDiameter = Math.min(ww, hh)
        mOuterRadius = outerDiameter/2.0f
        mInnerRadius = mOuterRadius * mInnerRadiusRatio
        mDotRadius = mInnerRadius * mDotRadiusRatio

        mLabelRadius = (mOuterRadius + mInnerRadius)/2
        mCenterX = paddingLeft.toFloat() + mOuterRadius
        mCenterY = paddingTop.toFloat() + mOuterRadius


//        onDataChanged()
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw the shadow
        canvas.drawCircle(mCenterX, mCenterY, mOuterRadius, mLinePaint)
        canvas.drawCircle(mCenterX, mCenterY, mInnerRadius, mLinePaint)

        val noteButtons: List<NoteButton> = calculateNoteButtons()
        for (nb in noteButtons) {
            nb.draw(canvas)
        }

//        drawTestImage(canvas)
    }

    private fun calculateNoteButtons(): List<NoteButton> {
        val noteButtons = mutableListOf<NoteButton>()
        mNotes.mapTo(noteButtons) { NoteButton(it.position, it.name) }
        noteButtons.sortBy { it.position }

        if (noteButtons.size > 1) {
            var nbCurr: NoteButton = noteButtons.last()
            var nbNext: NoteButton = noteButtons[0]
            var nextEdgePosition = average(1.0, nbCurr.position)
            nbCurr.endPosition = nextEdgePosition
            nbNext.startPosition = nextEdgePosition - 1 // Start position must be smaller than end position to ensure button boundaries for ring's minor segment

            for (i in 0..noteButtons.size - 2) {
                var nbCurr = noteButtons[i]
                var nbNext = noteButtons[i+1]
                nextEdgePosition = average(nbCurr.position, nbNext.position)
                nbCurr.endPosition = nextEdgePosition
                nbNext.startPosition = nextEdgePosition
            }
        }

        return noteButtons
    }

    private fun average(start: Double, end: Double): Double {
        return (start + end)/2
    }


    /**
     * Edge: a radial line drawn only from the inner to outer radii
     *
     */
    inner class Edge(val position: Double) {
        val start = Point.polar(position, mInnerRadius, mCenterX, mCenterY)
        val end = Point.polar(position, mOuterRadius, mCenterX, mCenterY)

        fun draw(canvas: Canvas) {
            canvas.drawLine(start.x, start.y, end.x, end.y, mLinePaint)
        }
    }


    /**
     * A circle drawn on the inner border
     */
    inner class Dot(val position: Double) {
        val point = Point.polar(position, mInnerRadius, mCenterX, mCenterY)

        fun draw(canvas: Canvas) {
            canvas.drawCircle(point.x, point.y, mDotRadius, mDotPaint)
        }
    }

    /**
     * The note name between the inner and outer borders
     */
    inner class Label (val position: Double, val text: String) {
        val point = Point.polar(position, mLabelRadius, mCenterX, mCenterY)

        fun draw(canvas: Canvas) {
            drawTextCentered(canvas, mLabelPaint, text, point.x, point.y)
        }
    }

     // Start position must be smaller than end position to ensure button boundaries for ring's minor segment
    /**
     * A button along the NoteRing. Positions are ratio of 360 degrees along the ring, starting at the top.
     * @position the location of the Dot
     * @name the label within the button
     * @startPosition the edge of the button prior to the Dot
     * @endPosition the edge of the button after the Dot
     */
    inner class NoteButton(val position: Double, val name: String,
                           var startPosition: Double = 0.0, var endPosition: Double = 0.0) {

        init {
            assert(0.0 <= position && position <= 1.0) {
                "NoteButton[$position, $name]: 'position' must be from 0 to 1."
            }
        }

        fun draw(canvas: Canvas) {
            assert(startPosition != endPosition) {
                "NoteButton[$position, $name] has no width"
            }
            Dot(position).draw(canvas)
            Edge(startPosition).draw(canvas)
            Edge(endPosition).draw(canvas)

            val labelPosition = average(startPosition, endPosition)
            Label(labelPosition, name).draw(canvas)
        }


    }

    fun drawTestImage(canvas: Canvas) {
        val n = 24
        val u = 1.0 / n
        var p = -1.0 / (2 * n)

        var notes = Array<NoteButton>(n) {
            var name = if (it % 2 == 0) {
                "C$SHARP"
            } else {
                "B$FLAT"
            }
            val oldP = p
            p += u
            NoteButton(u*it, name, oldP, p)
        }
        for (nb in notes) {
            nb.draw(canvas)
        }






//
//        var i = 0
//        while (i < n) {
//            var t = if (i % 2 == 0) {
//                "C$SHARP"
//            } else {
//                "B$FLAT"
//            }
//            Dot(u * i).draw(canvas)
//            Label(u * i, t).draw(canvas)
//            i++
//        }
    }


    /**
     * https://stackoverflow.com/questions/4909367/how-to-align-text-vertically
     * Requires Draw.Align.LEFT (default)
     */
    fun drawTextCentered(canvas: Canvas, paint: Paint, text: String, cx: Float, cy: Float) {
        paint.getTextBounds(text, 0, text.length, _textBounds)
        canvas.drawText(text, cx - _textBounds.exactCenterX(), cy - _textBounds.exactCenterY(), paint)
    }



    // Adaptation of: https://discuss.kotlinlang.org/t/property-getters-setters-in-custom-view/2300/5
    inline fun <T> attributable (initialValue: T): ReadWriteProperty<Any?, T> {
        return Delegates.observable(initialValue) {
            prop, old, new ->
            invalidate()
            requestLayout()
        }
    }
}

