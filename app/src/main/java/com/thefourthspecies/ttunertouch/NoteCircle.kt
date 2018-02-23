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
import kotlin.math.abs

const val DEBUG_TAG = "TTuner"

const val FLAT = "♭" // U+267D
const val SHARP = "♯" // U+266F

//const val FLAT = "b" // U+266D
//const val SHARP = "#" // U+266F


typealias Position = Double

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
    private var mSelectColor: Int by attributable(0)
    private var mHighlightColor: Int by attributable(0)
    private var mDirectThickness: Float by attributable(0f)

    // Paint Objects
    lateinit var mLinePaint: Paint
    lateinit var mLabelPaint: Paint
    lateinit var mDotPaint: Paint
    lateinit var mHintPaint: Paint
    lateinit var mHintLabelPaint: Paint
    lateinit var mSectorPaint: Paint
    lateinit var mSelectPaint: Paint
    lateinit var mHighlightPaint: Paint
    lateinit var mDirectPaint: Paint



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

    private var mNotes: RingList<Note> = RingList<Note>()
    var notes: Set<Note>
        get() = mNotes.toSet()
        set(noteSet) {
            mNotes = RingList<Note>(noteSet)
            onDataChange()
        }

    var mSectors: List<IntervalSector> = listOf()

    lateinit var textView: TextView
    var mDetector: GestureDetector

    // Drawings
    var mStartButton: NoteButton? = null
    var mTouchPoint: Point? = null
    var mIsScrollDone: Boolean = false
    var mSweepAngle: Float = 0f
    var mTouchInput: TouchInput? = null

    init {
        val a = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.NoteCircle,
            0, 0)
        try {
            mLabelColor = a.getColor(R.styleable.NoteCircle_labelColor, -0x1000000)
            mLabelHeight = a.getDimension(R.styleable.NoteCircle_labelHeight, 0.0f)
            mLabelWidth = a.getDimension(R.styleable.NoteCircle_labelHeight, 0.0f) // todo: remove?
            mLineColor = a.getColor(R.styleable.NoteCircle_lineColor, -0x1000000)
            mLineThickness = a.getDimension(R.styleable.NoteCircle_lineThickness, 0.0f)
            mDirectThickness = a.getDimension(R.styleable.NoteCircle_directThickness, 0.0f)
            mDotColor = a.getColor(R.styleable.NoteCircle_dotColor, -0x1000000)
            mDotRadiusRatio = a.getFloat(R.styleable.NoteCircle_dotRadiusRatio, 0.0f)
            mInnerRadiusRatio = a.getFloat(R.styleable.NoteCircle_innerRadiusRatio, 0.5f)
            mHintColor = a.getColor(R.styleable.NoteCircle_hintColor, -0x1000000)
            mSelectColor = a.getColor(R.styleable.NoteCircle_selectColor, Color.BLUE)
            mHighlightColor = a.getColor(R.styleable.NoteCircle_highlightColor, Color.RED)
        } finally {
            a.recycle()
        }
        // Create a gesture detector to handle onTouch messages
        mDetector = GestureDetector(getContext(), GestureListener())

        initPaint()
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

        mSelectPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mSelectPaint.color = mSelectColor
        mSelectPaint.style = Paint.Style.FILL

        mHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mHighlightPaint.color = mHighlightColor
        mHighlightPaint.style = Paint.Style.FILL

        mDirectPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mDirectPaint.color = mSelectColor
        mDirectPaint.style = Paint.Style.STROKE
        mDirectPaint.strokeWidth = mDirectThickness
        mDirectPaint.strokeCap = Paint.Cap.BUTT
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
        mOuterRadius = outerDiameter/2f
        mInnerRadius = mOuterRadius * mInnerRadiusRatio
        mDotRadius = mInnerRadius * mDotRadiusRatio
        mButtonRadius = mInnerRadius/2f

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

        Point.center(mCenterX, mCenterY)

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
        var sweepAngle = endAngle - startAngle
        if (sweepAngle < 0) {
            sweepAngle += 360f
        }
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
            nbCurr.sector.endPosition = nextEdgePosition
            nbNext.sector.startPosition = nextEdgePosition - 1 // Start position must be smaller than endNote position to ensure button boundaries for ring's minor segment

            for (i in 0..noteButtons.size - 2) {
                nbCurr = noteButtons[i]
                nbNext = noteButtons[i+1]
                nextEdgePosition = average(nbCurr.note.position, nbNext.note.position)
                nbCurr.sector.endPosition = nextEdgePosition
                nbNext.sector.startPosition = nextEdgePosition
            }
        }
        return noteButtons
    }

    private fun average(start: Double, end: Double): Double {
        return (start + end)/2
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val p = Point.screen(event!!.x, event.y)
        textView.text = """
            |Center: ($mCenterX, $mCenterY)
            |Screen: (${p.x}, ${p.y})
            |Polar: (${p.position}, ${p.distance})
            |ScreenAngle: ${p.screenAngle}
            |SweepAngle: $mSweepAngle
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

        for (note in mNotes) {
            val labelPaint = if (note.isHint) mHintLabelPaint else mLabelPaint
            Label(note.position, note.name).draw(canvas, labelPaint)
            Radial(note.position).draw(canvas, mHintPaint)
        }


        for (rel in mRelationships) {
            rel.draw(canvas)
        }
        for (note in mNotes) {
            val dotPaint = if (note.isHint) mHintLabelPaint else mDotPaint
            Dot(note.position).draw(canvas, dotPaint)
        }

        mTouchInput?.draw(canvas)
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

    inner class Note(position: Double, val name: String, var isHint: Boolean = false) : Comparable<Note> {
        val position: Double = run {
            var pos = position % 1
            if (pos < 0) { pos + 1 } else pos
        }

        init {
            assert(0.0 <= this.position && this.position < 1.0) {
                "$this: 'position' must be in range [0,1)"
            }
        }

        fun draw(canvas: Canvas, paint: Paint = if (isHint) mHintLabelPaint else mLabelPaint) {
            val dot = Dot(position)
            val label = Label(position, name)
            val radial = Radial(position)
            radial.draw(canvas, mHintPaint)
            if (!isHint) {
                dot.draw(canvas, mDotPaint)
            }
            label.draw(canvas, paint)

        }

        override fun compareTo(other: Note): Int {
            return position.compareTo(other.position)
        }


        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Note) return false

            if (name != other.name) return false
            return true
        }

        override fun hashCode(): Int {
            return name.hashCode()
        }

        override fun toString(): String {
            return "Note(position=$position, name='$name', isHint=$isHint)"
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
                canvas.drawArc(mInnerCircleBounds, start.screenAngle, sweepAngle, isSelected, mSelectPaint)
            }
        }

        operator fun contains(p: Point): Boolean {
            val result = if (start.position <= end.position) {
                start.position < p.position && p.position < end.position
            } else { // In case the sector crosses over position 0.
                p.position > start.position || p.position < end.position
            }
            return result
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
        val start = Point.polar(position, mInnerRadius)
        val end = Point.polar(position, mOuterRadius)

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
        return Point.polar(position, distance)
    }

    /**
     * A button along the NoteRing. Positions are ratio of 360 degrees along the ring, starting at the top.
     */
    inner class NoteButton(val note: Note, val sector: Sector = Sector()) {
        constructor(note: Note, startPosition: Double, endPosition: Double) :
                this(note, Sector(startPosition, endPosition))

        operator fun contains(p: Point): Boolean =
                p.distance in mButtonRadius..mOuterRadius &&
                        p in sector

        fun inSector(p: Point): Boolean = p in sector

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is NoteButton) return false
            if (note != other.note) return false
            if (sector != other.sector) return false

            return true
        }

        override fun hashCode(): Int {
            var result = note.hashCode()
            result = 31 * result + sector.hashCode()
            return result
        }
    }

    // Variable since currently the notebutton calculate function must set them
    data class Sector(var startPosition: Double = 0.0, var endPosition: Double = 0.0) {
        operator fun contains(p: Point): Boolean {
            val result = if (startPosition <= endPosition) {
                startPosition < p.position && p.position < endPosition
            } else {
                p.position > startPosition || p.position < endPosition
            }
            return result
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

        // For some reason, onDown needs to be implemented for this to behave properly
        override fun onDown(e: MotionEvent): Boolean {
            val p = Point.screen(e.x, e.y)
            Log.d(DEBUG_TAG, "OnDown. $p")
            val button = mNoteButtons.find { p in it }

            if (button != null) {
                Log.d(DEBUG_TAG, "NoteButton pressed: ${button.note.name}")
                val startPoint = Point(button.note.position, mInnerRadius)
                mTouchInput = LineInput(startPoint)
            }

            mStartButton = button
            mSweepAngle = 0f
            mTouchPoint = null
            mIsScrollDone = false
            return true
        }


        override fun onScroll(firstEvent: MotionEvent, currentEvent: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            val touchPoint = Point.screen(currentEvent.x, currentEvent.y)
            mTouchInput = mTouchInput?.next(touchPoint)

            invalidate()
            return true
     }



        override fun onFling(downEvent: MotionEvent?, upEvent: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
            if (upEvent == null) return false

            val touchPoint = Point.screen(upEvent.x, upEvent.y)
            mTouchInput = mTouchInput?.next(touchPoint)

            invalidate()
            return true
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

        private fun findSectorAtPosition(p: Point): IntervalSector? {
            val selected = mSectors.find { p in it }
            return selected
        }
    }

    private fun extendLine(startButton: NoteButton, p: Point) {
        val endButton = mNoteButtons.find { p in it }
        val endPoint = if (endButton == null || endButton == startButton) {
            p
        } else {
            Point(endButton.note.position, mInnerRadius)
        }
    }

    inner class LineInput(startPoint: Point, touchPoint: Point, sweepAngle: Float) :
            TouchInput(startPoint, touchPoint, sweepAngle) {
        constructor (startPoint: Point) : this(startPoint, startPoint, 0f)

        val startNote: Note? by lazy {
            val startButton = mNoteButtons.find { startPoint in it}
            startButton?.note
        }


        val endPoint: Point by lazy {
            val touchButton: NoteButton? = mNoteButtons.find { touchPoint in it}
            if (touchButton == null || touchButton.note.position == startPoint.position) {
                touchPoint
            } else {
                endNote = touchButton.note
                Point(touchButton.note.position, mInnerRadius)
            }
        }

        var endNote: Note? = null

        override fun next(touchPoint: Point): TouchInput {
            updateSweepAngle(touchPoint)
            sweepAngle = sweepAngle % 360f
            return if (touchPoint.distance < mInnerRadius) {
                LineInput(startPoint, touchPoint, sweepAngle)
            } else {
                ArcInput(startPoint, touchPoint, sweepAngle)
            }
        }

        override fun draw(canvas: Canvas) {
            canvas.drawLine(startPoint.x, startPoint.y, endPoint.x, endPoint.y, mDirectPaint)
            startNote?.let {
                Dot(it.position).draw(canvas, mHighlightPaint)
                Radial(it.position).draw(canvas, mHighlightPaint)
            }

            endNote?.let {
                Dot(it.position).draw(canvas, mHighlightPaint)
                Radial(it.position).draw(canvas, mHighlightPaint)
            }
        }
    }

    inner class ArcInput(startPoint: Point, touchPoint: Point, sweepAngle: Float) :
            TouchInput(startPoint, touchPoint, sweepAngle) {
        constructor (startPoint: Point) : this(startPoint, startPoint, 0f)

        val startNote: Note? by lazy {
            val startButton = mNoteButtons.find { startPoint in it}
            startButton?.note
        }

        val endNote: Note? by lazy {
            val endButton = mNoteButtons.find { touchPoint in it.sector}
            endButton?.note
        }

        override fun draw(canvas: Canvas) {
            val useCenter = true
            canvas.drawArc(mInnerCircleBounds, startPoint.screenAngle, sweepAngle, useCenter, mSelectPaint)

            for (rel in mRelationships) {
                rel.draw(canvas)
            }

            drawDotsAndRadials(canvas, startNote, endNote)
        }

        fun drawDotsAndRadials(canvas: Canvas, startNote: Note?, endNote: Note?) {
            if (startNote == null || endNote == null) return

            if (startNote != endNote) {
                for (note in mNotes.iterateUntilExcluding(startNote, endNote, direction)) {
                    Radial(note.position).draw(canvas, mHighlightPaint)
                    Dot(note.position).draw(canvas, mDotPaint)
                }
            }

            Radial(startNote.position).draw(canvas, mHighlightPaint)
            Dot(startNote.position).draw(canvas, mHighlightPaint)

            if (abs(sweepAngle) < 360f) {
                Radial(endNote.position).draw(canvas, mHighlightPaint)
                Dot(endNote.position).draw(canvas, mHighlightPaint)
            }
        }

        override fun next(touchPoint: Point): TouchInput {
            updateSweepAngle(touchPoint)
            return if (touchPoint.distance < mInnerRadius) {
                LineInput(startPoint, touchPoint, sweepAngle)
            } else {
                ArcInput(startPoint, touchPoint, sweepAngle)
            }
        }
    }
}


abstract class TouchInput(
    protected val startPoint: Point, // TODO: startNote, when points are easily accessible from a note
    protected var touchPoint: Point,
    sweepAngle: Float
) {

    protected var sweepAngle: Float = sweepAngle
        set(newAngle) { // So that you never exceed 720 degrees of having to scroll back
            val offset = when {
                newAngle > 360f -> 360f
                newAngle < -360f -> -360f
                else -> 0f
            }
            field = offset + (newAngle % 360f)
        }

    protected val direction: Direction
        get() = if (sweepAngle > 0) Direction.ASCENDING else Direction.DESCENDING

    protected fun updateSweepAngle(nextTouchPoint: Point) {
        val sweep = nextTouchPoint.screenAngle - touchPoint.screenAngle
        val sweepIncrement = when {
            sweep < -180f -> sweep + 360f
            sweep > 180f -> sweep - 360f
            else -> sweep
        }
        sweepAngle += sweepIncrement
    }

    abstract fun draw(canvas: Canvas)
    abstract fun next(touchPoint: Point): TouchInput
}

