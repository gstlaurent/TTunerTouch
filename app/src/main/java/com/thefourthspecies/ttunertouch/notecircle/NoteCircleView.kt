package com.thefourthspecies.ttunertouch.notecircle

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
import com.thefourthspecies.ttunertouch.R
import com.thefourthspecies.ttunertouch.edittemperament.Direction
import com.thefourthspecies.ttunertouch.model.Note
import com.thefourthspecies.ttunertouch.util.DEBUG_TAG
import com.thefourthspecies.ttunertouch.util.RingList


internal const val FLAT = "♭" // U+267D
internal const val SHARP = "♯" // U+266F

//const val FLAT = "b" // U+266D
//const val SHARP = "#" // U+266F

internal typealias Position = Double

/**
 * Created by Graham on 2017-12-25.
 */
internal class NoteCircleView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), NoteCircleContract.View {

    override lateinit var presenter: NoteCircleContract.Presenter
    lateinit var textView: TextView // TODO: remove
    override lateinit var temperament: NCTemperament



    fun update() {
        // TODO
    }

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
    internal lateinit var mLinePaint: Paint
    internal lateinit var mLabelPaint: Paint
    internal lateinit var mDotPaint: Paint
    internal lateinit var mHintPaint: Paint
    internal lateinit var mHintLabelPaint: Paint
    lateinit private var mSelectPaint: Paint
    internal lateinit var mHighlightPaint: Paint
    lateinit private var mDirectPaint: Paint

    // Dimensions
    private val _textBounds = Rect() // For centering labels
    private var mOuterRadius = 0.0f
    internal var mInnerRadius = 0.0f
    private var mButtonRadius = 0.0f
    internal var mDotRadius = 0.0f
    internal var mLabelRadius = 0.0f
    internal var mCenterX = 0.0f
    internal var mCenterY = 0.0f
    private var mInnerCircleBounds = RectF()

    // Temperament Data
    private var mSectors: List<IntervalSector> = listOf()
    private var mNoteButtons: List<NoteButton> = listOf()

    // UX
    private var mDetector: GestureDetector
    private var mTouchInput: TouchInput? = null

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
        mOuterRadius = outerDiameter / 2f
        mInnerRadius = mOuterRadius * mInnerRadiusRatio
        mDotRadius = mInnerRadius * mDotRadiusRatio
        mButtonRadius = mInnerRadius / 2f

        mLabelRadius = (3 * mOuterRadius + mInnerRadius) / 4f
        mCenterX = paddingLeft.toFloat() + mOuterRadius
        mCenterY = paddingTop.toFloat() + mOuterRadius

        val radiusDiff = mOuterRadius - mInnerRadius
        mInnerCircleBounds = RectF(
            0.0f,
            0.0f,
            mInnerRadius * 2,
            mInnerRadius * 2)
        mInnerCircleBounds.offsetTo(paddingLeft.toFloat() + radiusDiff,
            paddingTop.toFloat() + radiusDiff)

        Point.center(mCenterX, mCenterY)

        onDataChange()
    }

    private fun onDataChange() {
        Log.d(DEBUG_TAG, "onDataChange: $temperament")

        mSectors = generateIntervalSectors()
        mNoteButtons = calculateNoteButtons()
        for (note in temperament.notes) {
            note.updatePoints()
        }
//        for (point in mPoints.values) {
//            point.moveByDistance(mInnerRadius)
//        }
    }

    private fun generateIntervalSectors(): List<IntervalSector> {
        val notes = temperament.notes.toMutableList()
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
        temperament.notes.mapTo(noteButtons) { NoteButton(it) }
        noteButtons.sortBy { it.note.position }

        if (noteButtons.size > 1) {
            var nbCurr: NoteButton = noteButtons.last()
            var nbNext: NoteButton = noteButtons[0]
            var nextEdgePosition = average(1.0, nbCurr.note.position)
            nbCurr.sector.endPosition = nextEdgePosition
            nbNext.sector.startPosition = nextEdgePosition

            for (i in 0..noteButtons.size - 2) {
                nbCurr = noteButtons[i]
                nbNext = noteButtons[i + 1]
                nextEdgePosition = average(nbCurr.note.position, nbNext.note.position)
                nbCurr.sector.endPosition = nextEdgePosition
                nbNext.sector.startPosition = nextEdgePosition
            }
        }
        return noteButtons
    }

    private fun average(start: Double, end: Double): Double {
        return (start + end) / 2
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val p = Point.screen(event!!.x, event.y)
        textView.text = """
            |Center: ($mCenterX, $mCenterY)
            |Screen: (${p.x}, ${p.y})
            |Polar: (${p.position}, ${p.distance})
            |ScreenAngle: ${p.screenAngle}
            |TouchInput: $mTouchInput
            """.trimMargin()

        // Let the GestureDetector interpret this event
        val isGesture: Boolean = mDetector.onTouchEvent(event);
        if (isGesture) {
            return true
        }
        return onNonGestureTouchEvent(event)
    }

    private fun onNonGestureTouchEvent(event: MotionEvent): Boolean {
        return when (event.actionMasked) {
            MotionEvent.ACTION_UP -> onUp(event)
            else -> super.onTouchEvent(event)

        }
    }

    private fun onUp(upEvent: MotionEvent): Boolean {
        mTouchInput?.let {
            mTouchInput = null

            val fromNote = it.fromNote
            val toNote = it.toNote
            if (fromNote != null && toNote != null) {
                Log.d(DEBUG_TAG, "Inputting touch event: $mTouchInput")
                it.input(fromNote, toNote)
            }
        }

        invalidate()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (note in temperament.notes) {
            note.drawLabel(canvas)
            note.drawRadial(canvas)
        }

        for (rel in temperament.relationships) {
            rel.draw(canvas, this)
        }
        for (note in temperament.notes) {
            note.drawDot(canvas)
        }

        mTouchInput?.draw(canvas)
    }


    inner class IntervalSector(val startNote: NCNote, val endNote: NCNote, var isSelected: Boolean = false) { // Direction is Clockwise
        var start = Point(startNote.position, mInnerRadius)
        var end = Point(endNote.position, mInnerRadius)
        val sweepAngle: Float = run {
            val diffAngle = end.screenAngle - start.screenAngle
            if (diffAngle < 0) {
                diffAngle + 360f
            } else diffAngle
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

    private fun Point(position: Double, distance: Float): Point {
        return Point.polar(position, distance)
    }

    /**
     * A button along the NoteRing. Positions are ratio of 360 degrees along the ring, starting at the top.
     */
    inner class NoteButton(val note: NCNote, val sector: Sector = Sector()) {
        constructor(note: NCNote, startPosition: Double, endPosition: Double) :
                this(note, Sector(startPosition, endPosition))

        init {
            note.button = this
        }

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
    class Sector(startPosition: Position = 0.0, endPosition: Position = 0.0) {
        var startPosition: Position = normalizePosition(startPosition)
        var endPosition: Position = normalizePosition(endPosition)

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
    fun <T> attributable(initialValue: T): ReadWriteProperty<Any?, T> {
        return Delegates.observable(initialValue) { prop, old, new ->
            invalidate()
            requestLayout()
        }
    }

    /**
     * Extends [GestureDetector.SimpleOnGestureListener] to provide custom gesture
     * processing.
     */
    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {

        // onDown needs to be implemented (and return true) for any other gestures to be detected
        override fun onDown(e: MotionEvent): Boolean {
            val p = Point.screen(e.x, e.y)
            Log.d(DEBUG_TAG, "OnDown. $p")
            val button = mNoteButtons.find { p in it }

            if (button == null) return false

            Log.d(DEBUG_TAG, "NoteButton pressed: ${button.note.name}")
            val startPoint = Point(button.note.position, mInnerRadius)
            mTouchInput = LineInput(startPoint)

            invalidate()
            return true
        }


        override fun onScroll(firstEvent: MotionEvent, currentEvent: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            val touchPoint = Point.screen(currentEvent.x, currentEvent.y)
            mTouchInput = mTouchInput?.next(touchPoint)

            invalidate()
            return true
        }
    }

    inner class LineInput(startPoint: Point, touchPoint: Point, sweepAngle: Float) :
        TouchInput(startPoint, touchPoint, sweepAngle) {

        override val fromNote: Note?
            get() = startNote?.note
        override val toNote: Note?
            get() = endNote?.note

        constructor (startPoint: Point) : this(startPoint, startPoint, 0f)

        val startNote: NCNote?

        init {
            val startButton = mNoteButtons.find { startPoint in it }
            this.startNote = startButton?.note
        }

        var endNote: NCNote?
        val endPoint: Point

        init {
            val touchButton: NoteButton? = mNoteButtons.find { touchPoint in it }
            if (touchButton == null || touchButton.note == startNote) {
                this.endNote = null
                this.endPoint = touchPoint
            } else {
                this.endNote = touchButton.note
                this.endPoint = Point(touchButton.note.position, mInnerRadius)
            }
        }

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
            startNote?.drawHighlighted(canvas)
            endNote?.drawHighlighted(canvas)
        }

        override fun input(fromNote: Note, toNote: Note) {
            // TODO
            // mController.inputLine(fromNote, toNote)
            Log.d(DEBUG_TAG, "LineInput.input(): not implemented")
        }
    }


    inner class ArcInput(startPoint: Point, touchPoint: Point, sweepAngle: Float) :
        TouchInput(startPoint, touchPoint, sweepAngle) {

        override val fromNote: Note?
            get() = startNote?.note
        override val toNote: Note?
            get() = endNote?.note

        val startNote: NCNote? by lazy {
            val startButton = mNoteButtons.find { startPoint in it }
            startButton?.note
        }

        val endNote: NCNote? by lazy {
            if (isFullCircle()) {
                startNote
            } else {
                val endButton = mNoteButtons.find { touchPoint in it.sector }
                endButton?.note
            }
        }

        private fun isFullCircle(): Boolean {
            return if (direction == Direction.ASCENDING) {
                isFullCircleAscending()
            } else {
                isFullCircleDescending()
            }
        }

        private fun isFullCircleDescending(): Boolean {
            com.thefourthspecies.ttunertouch.util.assert(direction == Direction.DESCENDING) {
                "isFullCircleDescending called when direction == ${direction.name}"
            }

            val pos = startNote?.position
            val edgePos = startNote?.button?.sector?.endPosition

            if (pos == null || edgePos == null) return false

            val diff = pos - edgePos
            val boundedDiff = normalizePosition(-diff)
            val limit = 1.0 - boundedDiff
            val limitDegrees = limit * -360f

            return sweepAngle < limitDegrees
        }

        private fun isFullCircleAscending(): Boolean {
            com.thefourthspecies.ttunertouch.util.assert(direction == Direction.ASCENDING) {
                "isFullCircleAscending called when direction == ${direction.name}"
            }

            val pos = startNote?.position
            val edgePos = startNote?.button?.sector?.startPosition

            if (pos == null || edgePos == null) return false

            val diff = pos - edgePos
            val boundedDiff = normalizePosition(diff)
            val limit = 1.0 - boundedDiff
            val limitDegrees = limit * 360f

            return sweepAngle > limitDegrees
        }

        override fun draw(canvas: Canvas) {
            val useCenter = true
            canvas.drawArc(mInnerCircleBounds, startPoint.screenAngle, sweepAngle, useCenter, mSelectPaint)

            for (rel in temperament.relationships) {
                rel.draw(canvas, this)
            }

            drawDotsAndRadials(canvas, startNote, endNote)
        }

        fun drawDotsAndRadials(canvas: Canvas, startNote: NCNote?, endNote: NCNote?) {
            if (startNote == null || endNote == null) return

            val iterator = if (isFullCircle()) {
                temperament.notes.iterateFrom(startNote, direction.RingList())
            } else {
                temperament.notes.iterateUntilExcluding(startNote, endNote, direction.RingList())
            }

            for (note in iterator) {
                note.drawRadial(canvas, mHighlightPaint)
                note.drawDot(canvas, mDotPaint)
            }
            startNote.drawHighlighted(canvas)
            endNote.drawHighlighted(canvas)
        }


        override fun next(touchPoint: Point): TouchInput {
            updateSweepAngle(touchPoint)
            return if (touchPoint.distance < mInnerRadius) {
                LineInput(startPoint, touchPoint, sweepAngle)
            } else {
                ArcInput(startPoint, touchPoint, sweepAngle)
            }
        }

        override fun input(fromNote: Note, toNote: Note) {
            // TODO
            Log.d(DEBUG_TAG, "ArcInput.input(): not implemented")
//            mController.inputArc(fromNote, toNote, direction)
        }
    }
}


abstract class TouchInput(
    protected val startPoint: Point, // TODO: startNote, when points are easily accessible from a note
    protected var touchPoint: Point,
    sweepAngle: Float
) {
    fun Direction.RingList(): RingList.Direction {
        return if (this == Direction.ASCENDING) {
            RingList.Direction.ASCENDING
        } else {
            RingList.Direction.DESCENDING
        }
    }

    abstract val fromNote: Note?
    abstract val toNote: Note?

    protected var sweepAngle: Float = sweepAngle
        set(newAngle) { // So that you never exceed 720 degrees of having to scroll back
            val offset = when {
                newAngle > 360f -> 360f
                newAngle < -360f -> -360f
                else -> 0f
            }
            field = offset + (newAngle % 360f)
        }

    val direction: Direction
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
    abstract fun input(fromNote: Note, toNote: Note)

    override fun toString() = "${javaClass.simpleName}[from: ${fromNote?.name}, to: ${toNote?.name}, sweepAngle: %.1f]".format(sweepAngle)

}

// Ensures position is in range [0.0, 1.0)
internal fun normalizePosition(position: Position): Position {
    val pos = position % 1.0
    return if (pos < 0) { pos + 1.0 } else pos
}




