package com.thefourthspecies.ttunertouch

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import kotlin.properties.Delegates
import kotlin.properties.ReadWriteProperty
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
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
    private var mHintColor: Int by attributable(0)

    // Paint Objects
    lateinit var mLinePaint: Paint
    lateinit var mLabelPaint: Paint
    lateinit var mDotPaint: Paint
    lateinit var mHintPaint: Paint
    lateinit var mHintLabelPaint: Paint

    // Dimensions
    private val _textBounds = Rect() // For centering labels
    private var mOuterRadius = 0.0f
    private var mInnerRadius = 0.0f
    private var mDotRadius = 0.0f
    private var mLabelRadius = 0.0f
    private var mCenterX = 0.0f
    private var mCenterY = 0.0f
    private var mInnerCircleBounds = RectF()

    // Temperament Data
    data class Note(val position: Double, val name: String, var isHint: Boolean = false)
    var mNotes: MutableSet<Note> = mutableSetOf()
    data class Relationship(val note1: Note, val note2: Note, val label: String, val isArc: Boolean)
    var mRelationships: MutableSet<Relationship> = mutableSetOf()

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
            mHintColor = a.getColor(R.styleable.NoteRing_hintColor, -0x1000000)
        } finally {
            a.recycle()
        }

        initPaint()
        initHintData()

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

        mHintPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mHintPaint.color = mHintColor
        mHintPaint.style = Paint.Style.STROKE
        mHintPaint.strokeWidth = mLineThickness

        mHintLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mHintLabelPaint.textSize = mLabelHeight
        mHintLabelPaint.color = mHintColor
    }

    fun initHintData() {
        val isHint = true
        mNotes = mutableSetOf(
                Note(0/12.0, "C", isHint),
                Note(1/12.0, "G", isHint),
                Note(2/12.0, "D", isHint),
                Note(3/12.0, "A", isHint),
                Note(4/12.0, "E", isHint),
                Note(5/12.0, "B", isHint),
                Note(6/12.0, "F$SHARP", isHint),
                Note(7/12.0, "C$SHARP", isHint),
                Note(8/12.0, "A$FLAT", isHint),
                Note(9/12.0, "E$FLAT", isHint),
                Note(10/12.0, "B$FLAT", isHint),
                Note(11/12.0, "F", isHint)
        )
    }

    fun initTestData() {
        // Kirnberger III-esque
        val C = Note(0/12.0, "C")
        val G =  Note(1/12.0, "G")
        val D =  Note(2/12.0, "D")
        val A =  Note(3/12.0, "A")
        val E =  Note(4/12.0, "E")
        val B =  Note(5/12.0, "B")
        val Fs =  Note(6/12.0, "F$SHARP")
        val Df =  Note(7/12.0, "D$FLAT")
        val Af =  Note(8/12.0, "A$FLAT")
        val Ef =  Note(9/12.0, "E$FLAT")
        val Bf =  Note(10/12.0, "B$FLAT")
        val F =  Note(11/12.0, "F")

        mNotes = mutableSetOf(C, G, D, A, E, B, Fs, Df, Af, Ef, Bf, F)

        val isArc = true
        mRelationships = mutableSetOf(
                Relationship(C, E, "", !isArc),
                Relationship(C, G, "-1/4S", !isArc),
                Relationship(G, D, "-1/4S", !isArc),
                Relationship(D, A, "-1/4S", !isArc),
                Relationship(A, E, "-1/4S", !isArc),
                Relationship(E, B, "", isArc),
                Relationship(B, Fs, "", isArc),
                Relationship(C, F, "", isArc),
                Relationship(F, Bf, "", isArc),
                Relationship(Bf, Ef, "", isArc),
                Relationship(Ef, Af, "", isArc),
                Relationship(Af, Df, "", isArc)
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

        val radiusDiff = mOuterRadius - mInnerRadius
        mInnerCircleBounds = RectF(
                0.0f,
                0.0f,
                mInnerRadius*2,
                mInnerRadius*2)
        mInnerCircleBounds.offsetTo(paddingLeft.toFloat() + radiusDiff,
                paddingTop.toFloat() + radiusDiff)

//        onDataChanged()
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

//        canvas.drawCircle(mCenterX, mCenterY, mOuterRadius, mHintPaint)
        canvas.drawCircle(mCenterX, mCenterY, mInnerRadius, mHintPaint)

        val noteButtons: List<NoteButton> = calculateNoteButtons()
        for (nb in noteButtons) {
            nb.draw(canvas)
        }
        for (rel in mRelationships) {
            val start = Point(rel.note1.position, mInnerRadius)
            val end = Point(rel.note2.position, mInnerRadius)
            if (rel.isArc) {
                val isFilled = true
                drawArc(canvas, start.position, end.position, isFilled)
            } else {
                canvas.drawLine(start.x, start.y, end.x, end.y, mLinePaint)
            }
        }
    }

    fun drawArc(canvas: Canvas, start: Double, end: Double, isFilled: Boolean) {
        // isEnclosed ... useCenter, stroked. See docs for drawArc

        canvas.drawArc(mInnerCircleBounds,0f, 350f, false, mLinePaint)
    }


    private fun calculateNoteButtons(): List<NoteButton> {
        val noteButtons = mutableListOf<NoteButton>()
        mNotes.mapTo(noteButtons) { NoteButton(it) }
        noteButtons.sortBy { it.note.position }

        if (noteButtons.size > 1) {
            var nbCurr: NoteButton = noteButtons.last()
            var nbNext: NoteButton = noteButtons[0]
            var nextEdgePosition = average(1.0, nbCurr.note.position)
            nbCurr.endPosition = nextEdgePosition
            nbNext.startPosition = nextEdgePosition - 1 // Start position must be smaller than end position to ensure button boundaries for ring's minor segment

            for (i in 0..noteButtons.size - 2) {
                nbCurr = noteButtons[i]
                nbNext = noteButtons[i+1]
                nextEdgePosition = average(nbCurr.note.position, nbNext.note.position)
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


        textView.text = """
            |Center: ($mCenterX, $mCenterY)
            |Screen: (${p.x}, ${p.y})
            |Polar: (${p.position}, ${p.distance})
            |ScreenAngle: ${p.screenAngle}
            """.trimMargin()
        return true
    }

    /**
     * Edge: a radial line drawn only from the inner to outer radii
     *
     */
    inner class Edge(val position: Double) {
        val start = Point.polar(position, mInnerRadius, mCenterX, mCenterY)
        val end = Point.polar(position, mOuterRadius, mCenterX, mCenterY)

        fun draw(canvas: Canvas, paint: Paint) {
            canvas.drawLine(start.x, start.y, end.x, end.y, paint)
        }
    }

    /**
     * Radial: a radial line drawn only to the inner radius
     *
     */
    inner class Radial(val position: Double) {
        val end = Point(position, mInnerRadius)

        fun draw(canvas: Canvas, paint: Paint) {
            canvas.drawLine(mCenterX, mCenterY, end.x, end.y, paint)
        }
    }


    /**
     * A circle drawn on the inner border
     */
    inner class Dot(val position: Double) {
        val point = Point(position, mInnerRadius)

        fun draw(canvas: Canvas, paint: Paint) {
            canvas.drawCircle(point.x, point.y, mDotRadius, paint)
        }
    }

    /**
     * The note name between the inner and outer borders
     */
    inner class Label (val position: Double, val text: String) {
        val point = Point(position, mLabelRadius)

        fun draw(canvas: Canvas, paint: Paint) {
            drawTextCentered(canvas, paint, text, point.x, point.y)
        }
    }

    private fun Point(position: Double, distance: Float): Point {
        return Point.polar(position, distance, mCenterX, mCenterY)
    }

     // Start position must be smaller than end position to ensure button boundaries for ring's minor segment
    /**
     * A button along the NoteRing. Positions are ratio of 360 degrees along the ring, starting at the top.
     * @note the note associated with this button
     * @startPosition the edge of the button prior to the position
     * @endPosition the edge of the button after the position
     */
    inner class NoteButton(val note: Note,
                           var startPosition: Double = 0.0, var endPosition: Double = 0.0) {

        init {
            assert(0.0 <= note.position && note.position <= 1.0) {
                "NoteButton[${note.position}, ${note.name}]: 'position' must be from 0 to 1."
            }
        }

        fun draw(canvas: Canvas) {
            assert(startPosition != endPosition) {
                "NoteButton[${note.position}, ${note.name}] has no width"
            }
            Radial(note.position).draw(canvas, mHintPaint)
//            Edge(startPosition).draw(canvas, mHintPaint)
//            Edge(endPosition).draw(canvas, mHintPaint)

            if (!note.isHint) {
                Dot(note.position).draw(canvas, mDotPaint)
            }
            Label(note.position, note.name).draw(canvas, if (note.isHint) mHintLabelPaint else mLabelPaint)
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
