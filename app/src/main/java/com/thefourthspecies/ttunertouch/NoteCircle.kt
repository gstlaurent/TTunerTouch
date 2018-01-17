package com.thefourthspecies.ttunertouch

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View
import kotlin.properties.Delegates
import kotlin.properties.ReadWriteProperty
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.TextView

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
    lateinit var mSectorPaint: Paint


    // Dimensions
    private val _textBounds = Rect() // For centering labels
    private var mOuterRadius = 0.0f
    private var mInnerRadius = 0.0f
    private var mButtonRadius = 0.0f
    private var mDotRadius = 0.0f
    private var mLabelRadius = 0.0f
    private var mCenterX = 0.0f
    private var mCenterY = 0.0f
    private var mInnerCircleBounds = RectF()

    // Temperament Data
    private var mRelationships: MutableSet<Relationship> = mutableSetOf()
    private var mNoteButtons: List<NoteButton> = listOf()

    private var mNotes: MutableSet<Note> = mutableSetOf()
    var notes: MutableSet<Note>
        get() = mNotes
        set(value) {
            mNotes = value
            onDataChange()
        }

    var mSectors: List<IntervalSector> = listOf()

    lateinit var textView: TextView
    var mDetector: GestureDetector

    // Drawings
    var mLineStart: Point? = null
    var mLineEnd: Point? = null


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
        // Create a gesture detector to handle onTouch messages
        mDetector = GestureDetector(getContext(), GestureListener())

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

        mSectorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mSectorPaint.color = Color.BLUE // todo
        mSectorPaint.style = Paint.Style.FILL
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

//        mSectors = mutableListOf(
//                IntervalSector(Ef, Bf),
//                IntervalSector(Bf, F)
//        )
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
        mButtonRadius = mInnerRadius - (2*mDotRadius) // times 2 for safety

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

        for (rel in mRelationships) rel.setDistanceFromCenter(mInnerRadius)

        onDataChange()
    }

    private fun onDataChange() {
        mSectors = generateIntervalSectors()
        mNoteButtons = calculateNoteButtons()
    }

    private fun generateIntervalSectors(): List<IntervalSector> {
        val notes = mNotes.toMutableList()
        notes.sortBy { it.position }

        val sectors = mutableListOf<IntervalSector>()
        if (notes.size > 0) {
            var prev = notes.last()

            for (curr in notes) {
                sectors.add(IntervalSector(prev, curr))
                prev = curr
            }
        }
        return sectors
    }



    /**
     * Start and End are assuming clockwise direction
     */
    fun drawArc(canvas: Canvas, startAngle: Float, endAngle: Float) {
        val sweepAngle = endAngle - startAngle
        canvas.drawArc(mInnerCircleBounds, startAngle, sweepAngle, false, mLinePaint)
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
            nbNext.startPosition = nextEdgePosition - 1 // Start position must be smaller than endNote position to ensure button boundaries for ring's minor segment

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

        // Let the GestureDetector interpret this event
        val result: Boolean = mDetector.onTouchEvent(event);
        if (!result) {
            return super.onTouchEvent(event)
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

//        canvas.drawCircle(mCenterX, mCenterY, mOuterRadius, mHintPaint)
//        canvas.drawCircle(mCenterX, mCenterY, mInnerRadius, mHintPaint)

//        for (sec in mSectors) {
//            sec.draw(canvas)
//        }

        for (rel in mRelationships) {
            rel.draw(canvas)
        }

        for (note in mNotes) {
            note.draw(canvas)
        }

        if (mLineStart != null && mLineEnd != null) {
            canvas.drawLine(mLineStart?.x ?: 0f, mLineStart?.y ?: 0f, mLineEnd?.x ?: 0f, mLineEnd?.y ?: 0f, mDotPaint)
        }

    }

    inner class Relationship(val note1: Note, val note2: Note, val label: String, val isArc: Boolean) {
        var start = Point(note1.position, mInnerRadius)
        var end = Point(note2.position, mInnerRadius)

        fun setDistanceFromCenter(radius: Float) {
            start = Point(note1.position, radius)
            end = Point(note2.position, radius)
        }

        fun draw(canvas: Canvas) {
            if (isArc) {
                drawArc(canvas, start.screenAngle, end.screenAngle)
            } else {
                canvas.drawLine(start.x, start.y, end.x, end.y, mLinePaint)
            }
        }



        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Relationship) return false

            if (note1 != other.note1) return false
            if (note2 != other.note2) return false
            if (label != other.label) return false
            if (isArc != other.isArc) return false

            return true
        }

        override fun hashCode(): Int {
            var result = note1.hashCode()
            result = 31 * result + note2.hashCode()
            result = 31 * result + label.hashCode()
            result = 31 * result + isArc.hashCode()
            return result
        }
    }

    inner class Note(position: Double, val name: String, var isHint: Boolean = false) {
        val position: Double = run {
            var pos = position % 1
            if (pos < 0) { pos + 1 } else pos
        }

        init {
            assert(0.0 <= this.position && this.position < 1.0)
        }

        fun draw(canvas: Canvas) {
            val dot = Dot(position)
            val label = Label(position, name)
            if (!isHint) {
                dot.draw(canvas, mDotPaint)
            }
            label.draw(canvas, if (isHint) mHintLabelPaint else mLabelPaint)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Note) return false

            if (position != other.position) return false
            if (name != other.name) return false
            if (isHint != other.isHint) return false

            return true
        }

        override fun hashCode(): Int {
            var result = position.hashCode()
            result = 31 * result + name.hashCode()
            result = 31 * result + isHint.hashCode()
            return result
        }
    }

    inner class IntervalSector(val startNote: Note, val endNote: Note, var isSelected: Boolean = false) { // Direction is Clockwise
        var start = Point(startNote.position, mInnerRadius)
        var end = Point(endNote.position, mInnerRadius)
        val sweepAngle: Float = run {
            val diffAngle = end.screenAngle - start.screenAngle
            if (diffAngle < 0) {
                diffAngle + 360f
            } else diffAngle
        }

        fun setDistanceFromCenter(radius: Float) {
            start = Point(startNote.position, radius)
            end = Point(endNote.position, radius)
        }

        fun draw(canvas: Canvas) {
            canvas.drawLine(mCenterX, mCenterY, start.x, start.y, mHintPaint)
            canvas.drawLine(mCenterX, mCenterY, end.x, end.y, mHintPaint)
            if (isSelected) {
                canvas.drawArc(mInnerCircleBounds, start.screenAngle, sweepAngle, isSelected, mSectorPaint)
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is IntervalSector) return false

            if (startNote != other.startNote) return false
            if (endNote != other.endNote) return false

            return true
        }

        override fun hashCode(): Int {
            var result = startNote.hashCode()
            result = 31 * result + endNote.hashCode()
            return result
        }

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

     // Start position must be smaller than endNote position to ensure button boundaries for ring's minor segment
    /**
     * A button along the NoteRing. Positions are ratio of 360 degrees along the ring, starting at the top.
     * @note the note associated with this button
     * @startPosition the edge of the button prior to the position
     * @endPosition the edge of the button after the position
     */
    inner class NoteButton(val note: Note,
                           var startPosition: Double = 0.0, var endPosition: Double = 0.0) {

        init {
            assert(0.0 <= note.position && note.position < 1.0) {
                "NoteButton[${note.position}, ${note.name}]: 'position' must be in range [0-1)."
            }
        }

        fun draw(canvas: Canvas) {
            assert(startPosition != endPosition) {
                "NoteButton[${note.position}, ${note.name}] has no width"
            }
//            Radial(note.position).draw(canvas, mHintPaint)
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

    enum class Direction {
        CLOCKWISE,
        COUNTERCLOCKWISE
    }

    /**
     * Extends [GestureDetector.SimpleOnGestureListener] to provide custom gesture
     * processing.
     */
    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        var prevSelected: IntervalSector? = null
        var firstSelected: IntervalSector? = null
        var selectionDirection = Direction.CLOCKWISE // actual selectionDirection is reset during some onScrolls
        var startNote: Note? = null

        // For some reason, onDown needs to be implemented for this to behave properly
        override fun onDown(e: MotionEvent): Boolean {
            val p = Point.screen(e.x, e.y, mCenterX, mCenterY)
            Log.d(DEBUG_TAG, "OnDown. position ${p.position}, distance ${p.distance}")
            val button = findButtonAtPosition(p)
            if (button == null) {
                startNote = null
                return false
            }

            Log.d(DEBUG_TAG, "NoteButton pressed: ${button.note.name}")

            startNote = button.note
            mLineStart = Point(button.note.position, mInnerRadius)
            return true


//            val selected = findSectorAtPosition(p)
//            if (selected == null) return false
//
//            prevSelected = null
//            firstSelected = null
//            return true
        }


        override fun onScroll(firstEvent: MotionEvent, currentEvent: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            if (startNote == null) return false

            mLineEnd = Point.screen(currentEvent.x, currentEvent.y, mCenterX, mCenterY)
            invalidate()
            return true






//            val p = Point.screen(currentEvent.x, currentEvent.y, mCenterX, mCenterY)
//            val selected = findSectorAtPosition(p)
//            if (selected == null) return false
//
//            if (firstSelected == null || prevSelected == null) {
//                selected.isSelected = true
//                prevSelected = selected
//                firstSelected = selected
//            }
//
//            if (prevSelected != selected) {
//                val direction = calculateDirection(p, distanceX, distanceY)
//                if (prevSelected == firstSelected) {
//                    selected.isSelected = true
//                    selectionDirection = direction
//                } else {
//                    if (direction == selectionDirection) {
//                        selected.isSelected = true
//                    } else {
//                        prevSelected?.isSelected = false
//                    }
//
//                }
//                prevSelected = selected
//            }
//            invalidate()
//            return true
        }

        private fun calculateDirection(p: Point, distanceX: Float, distanceY: Float): NoteCircle.Direction {
            val movedRight = distanceX > 0
            val movedDown = distanceY > 0
            return when {
                0.0 <= p.position && p.position < 0.125 -> if (movedRight) Direction.CLOCKWISE else Direction.COUNTERCLOCKWISE
                0.125 <= p.position && p.position < 0.375 -> if (movedDown) Direction.CLOCKWISE else Direction.COUNTERCLOCKWISE
                0.375 <= p.position && p.position < 0.625 -> if (!movedRight) Direction.CLOCKWISE else Direction.COUNTERCLOCKWISE
                0.625 <= p.position && p.position < 0.875 -> if (!movedDown) Direction.CLOCKWISE else Direction.COUNTERCLOCKWISE
                0.875 <= p.position && p.position < 1.0 -> if (movedRight) Direction.CLOCKWISE else Direction.COUNTERCLOCKWISE
                else -> throw Exception("Can't calculate direction. position: ${p.position}, distanceX: $distanceX, distanceY: $distanceY")
            }
        }

        private fun findButtonAtPosition(p: Point): NoteButton? {
            if (p.distance > mOuterRadius || p.distance < mButtonRadius) return null

            assert(0.0 <= p.position && p.position < 1.0)
            val button = mNoteButtons.find {
                if (it.startPosition <= it.endPosition) {
                    it.startPosition < p.position &&
                            p.position < it.endPosition
                } else {
                    p.position > it.startPosition ||
                            p.position < it.endPosition
                }
            }
            return button
        }

        private fun findSectorAtPosition(p: Point): IntervalSector? {
            val selected = mSectors.find {
                if (it.start.position <= it.end.position) {
                    it.start.position < p.position &&
                            p.position < it.end.position
                } else { // In case the sector crosses over position 0.
                    p.position > it.start.position ||
                            p.position < it.end.position
                }
            }
            return selected
        }
    }
}
