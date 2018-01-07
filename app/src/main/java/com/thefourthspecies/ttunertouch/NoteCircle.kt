package com.thefourthspecies.ttunertouch

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import kotlin.properties.Delegates
import kotlin.properties.ReadWriteProperty
import android.graphics.Paint
import android.graphics.Rect
import android.view.MotionEvent
import android.widget.TextView
import android.support.v4.view.MotionEventCompat
import android.util.Log

const val DEBUG_TAG = "TTuner"


/**
 * Created by Graham on 2017-12-25.
 */
class NoteCircle @JvmOverloads constructor(
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

    lateinit var textView: TextView


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
                Note(0/12.0, "C"),
                Note(1/12.0, "G"),
                Note(2/12.0, "D"),
                Note(3/12.0, "A"),
                Note(4/12.0, "E"),
                Note(5/12.0, "B"),
                Note(6/12.0, "F$SHARP"),
                Note(7/12.0, "C$SHARP"),
                Note(8/12.0, "A$FLAT"),
                Note(9/12.0, "E$FLAT"),
                Note(10/12.0, "B$FLAT"),
                Note(11/12.0, "F")
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

        mLabelRadius = (3*mOuterRadius + mInnerRadius)/4f
        mCenterX = paddingLeft.toFloat() + mOuterRadius
        mCenterY = paddingTop.toFloat() + mOuterRadius


//        onDataChanged()
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawCircle(mCenterX, mCenterY, mInnerRadius, mLinePaint)

        val noteButtons: List<NoteButton> = calculateNoteButtons()
        for (nb in noteButtons) {
            nb.draw(canvas)
        }

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

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val p = Point.screen(event!!.x, event!!.y, mCenterX, mCenterY)


        textView.text = "Center: ($mCenterX, $mCenterY)\n" +
                "Screen: (${p.x}, ${p.y})\n" +
                "Polar: (${p.position}, ${p.distance})"

        return true
    }


    /**
     * Edge: a radial line drawn only to the inner radius
     *
     */
    inner class Radial(val position: Double) {
        val end = Point(position, mInnerRadius)

        fun draw(canvas: Canvas) {
            canvas.drawLine(mCenterX, mCenterY, end.x, end.y, mLinePaint)
        }
    }


    /**
     * A circle drawn on the inner border
     */
    inner class Dot(val position: Double) {
        val point = Point(position, mInnerRadius)

        fun draw(canvas: Canvas) {
            canvas.drawCircle(point.x, point.y, mDotRadius, mDotPaint)
        }
    }

    /**
     * The note name between the inner and outer borders
     */
    inner class Label (val position: Double, val text: String) {
        val point = Point(position, mLabelRadius)

        fun draw(canvas: Canvas) {
            drawTextCentered(canvas, mLabelPaint, text, point.x, point.y)
        }
    }

    private fun Point(position: Double, distance: Float): Point {
        return Point.polar(position, distance, mCenterX, mCenterY)
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
            Radial(position).draw(canvas)
            Label(position, name).draw(canvas)
        }


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
