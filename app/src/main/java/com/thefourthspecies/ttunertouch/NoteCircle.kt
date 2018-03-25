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

    lateinit var controller: TemperamentController
    lateinit var textView: TextView

    // I/O
    private var mTouchInput: TouchInput? = null // TODO delete
    private var mTouchOut: TouchOut? = null

//
//    private var mNoteCycles = NoteCycles(emptyList())
//        set(cycles) {
//            mPoints.clear()
//            cycles.uiNotes.forEach { it.point } // to put UINote.point values into mPoints HashMap
//            field = cycles
//        }
//
//    private var mRelationships: List<UIRelationship> = emptyList()
//    private var mNotes: List<List<UINote>> = emptyList()
//        set(uinotes) {
//            mPoints.clear()
//            uinotes.forEach { it.forEach { it.point } } // to put UINote.point values into mPoints HashMap
//            field = uinotes
//        }

    private val mPoints = HashMap<UINote, Point>()
//
//    fun update(notes: List<UINote>? = null, relationships: List<UIRelationship>? = null, output: TouchOut? = null) {
//        if (notes != null) {
//            mNotes = notes
//        }
//        if (relationships != null) {
//            mRelationships = relationships
//        }
//        mTouchOut = output
//        onDataChange()
//        invalidate()
//    }

    fun update(output: TouchOut?) {
        mTouchOut = output
        invalidate()
    }


    var relationships: Set<UIRelationship>
        get() = mRelationships
        set(rels) {
            mRelationships = rels
            onDataChange()
        }
    var notes: Set<UINote>
        get() = mNotes.toSet()
        set(noteSet) {
            mNotes = RingList<UINote>(noteSet)
            onDataChange()
        }

    fun update(controller: TemperamentController) {
        mNotes = RingList<UINote>(controller.uiNotes)
        mRelationships = controller.uiRelationships
        onDataChange()
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
    lateinit private var mLinePaint: Paint
    lateinit private var mLabelPaint: Paint
    lateinit private var mDotPaint: Paint
    lateinit private var mHintPaint: Paint
    lateinit private var mHintLabelPaint: Paint
    lateinit private var mSelectPaint: Paint
    lateinit private var mHighlightPaint: Paint
    lateinit private var mDirectPaint: Paint

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
//    private var mSectors: List<IntervalSector> = listOf()
    private var mNoteButtons: List<NoteButton> = listOf()
    private var mRelationships: Set<UIRelationship> = setOf()
    private var mNotes: RingList<UINote> = RingList<UINote>()

    private var mDetector: GestureDetector


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
        Log.d(DEBUG_TAG, "onDataChange: notes=$mNotes; relationships=$mRelationships")

//        mSectors = generateIntervalSectors()
        mNoteButtons = calculateNoteButtons()
    }
//
//    private fun generateIntervalSectors(): List<IntervalSector> {
//        val notes = mNoteCycles.uiNotes.toMutableList()
//        notes.sort()
//
//        val sectors = mutableListOf<IntervalSector>()
//        if (notes.size > 0) {
//            var prev = notes.last()
//
//            for (curr in notes) {
//                sectors.add(IntervalSector(prev, curr))
//                prev = curr
//            }
//        }
//        return sectors
//    }

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
        noteButtons.sortBy { it.uiNote.position }

        if (noteButtons.size > 1) {
            var nbCurr: NoteButton = noteButtons.last()
            var nbNext: NoteButton = noteButtons[0]
            var nextEdgePosition = average(1.0, nbCurr.uiNote.position)
            nbCurr.sector.endPosition = nextEdgePosition
            nbNext.sector.startPosition = nextEdgePosition - 1 // Start position must be smaller than endNote position to ensure button boundaries for ring's minor segment

            for (i in 0..noteButtons.size - 2) {
                nbCurr = noteButtons[i]
                nbNext = noteButtons[i+1]
                nextEdgePosition = average(nbCurr.uiNote.position, nbNext.uiNote.position)
                nbCurr.sector.endPosition = nextEdgePosition
                nbNext.sector.startPosition = nextEdgePosition
            }
            // TODO: handle multiple enaharmonically-equivalent notes
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

        // TODO: handle multiple enaharmonically-equivalent notes
        mNotes.forEach {
            it.drawRadial(canvas)
            it.drawLabel(canvas)
        }
        mRelationships.forEach { it.draw(canvas) }
        mNotes.forEach { it.drawDot(canvas) }

        mTouchInput?.draw(canvas)
    }

    fun TouchOut.draw(canvas: Canvas) {
        // TODO
    }

    inner class UIRelationship(val note1: UINote, val note2: UINote, val label: String, val isArc: Boolean) {
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
            if (other !is UIRelationship) return false

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

    inner class UINote(position: Double, val note: Note, var isHint: Boolean = false) : Comparable<UINote> {
        val position: Double
        init {
            val pos = position % 1
            this.position = if (pos < 0) { pos + 1 } else pos
        }

        val name: String = note.name
        val point: Point = mPoints.getOrPut(this, { Point(position, mInnerRadius)})

        private val paint = if (isHint) mHintLabelPaint else mLabelPaint


        init {
            assert(0.0 <= this.position && this.position < 1.0) {
                "$this: 'position' must be in range [0,1)"
            }
        }

        fun drawLabel(canvas: Canvas) {
            drawTextCentered(canvas, mLabelPaint, name, point.x, point.y)
        }
        fun drawDot(canvas: Canvas, paint: Paint = this.paint) {
            canvas.drawCircle(point.x, point.y, mDotRadius, paint)
        }
        fun drawRadial(canvas: Canvas, paint: Paint = mHintPaint) {
            canvas.drawLine(mCenterX, mCenterY, point.x, point.y, mHintPaint)
        }
        fun drawHighlighted(canvas: Canvas) {
            drawDot(canvas, mHighlightPaint)
            drawRadial(canvas, mHighlightPaint)
        }

//        fun draw(canvas: Canvas) {
//            drawRadial(canvas)
//            drawDot(canvas)
//            drawLabel(canvas)
//        }

        override fun compareTo(other: UINote): Int {
            var comp = position.compareTo(other.position)
            if (comp == 0) {
                comp = note.compareTo(other.note)
            }
            return comp
        }


        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is UINote) return false

            if (note != other.note) return false
            return true
        }

        override fun hashCode(): Int {
            return note.hashCode()
        }

        override fun toString(): String {
            return "Note(position=$position, name='$name', isHint=$isHint)"
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
    inner class NoteButton(val uiNote: UINote, val sector: Sector = Sector()) {
        constructor(note: UINote, startPosition: Double, endPosition: Double) :
                this(note, Sector(startPosition, endPosition))

        operator fun contains(p: Point): Boolean =
                p.distance in mButtonRadius..mOuterRadius &&
                        p in sector

        fun inSector(p: Point): Boolean = p in sector

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is NoteButton) return false
            if (uiNote != other.uiNote) return false
            if (sector != other.sector) return false

            return true
        }

        override fun hashCode(): Int {
            var result = uiNote.hashCode()
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

    /**
     * Extends [GestureDetector.SimpleOnGestureListener] to provide custom gesture
     * processing.
     */
    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {

        // onDown needs to be implemented (and return true) for any other gestures to be detected
        override fun onDown(e: MotionEvent): Boolean {
            val point = Point.screen(e.x, e.y)
            Log.d(DEBUG_TAG, "OnDown. $point")
            val button = mNoteButtons.find { point in it }

            if (button == null) {
                Log.d(DEBUG_TAG, "Resetting touch input")
                mTouchInput = null
            } else {
                Log.d(DEBUG_TAG, "NoteButton pressed: ${button.uiNote.name}")
                val startPoint = Point(button.uiNote.position, mInnerRadius)
                mTouchInput = LineInput(startPoint)
            }
            invalidate()
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
            val touchInput = mTouchInput?.next(touchPoint)

            if (touchInput != null) {
                Log.d(DEBUG_TAG, "Input detected: $touchInput")
                mTouchInput = null
                // TODO
                // controller.update(...)
            }

            return true
        }

    }

    inner class LineInput(startPoint: Point, touchPoint: Point, sweepAngle: Float) :
            TouchInput(startPoint, touchPoint, sweepAngle) {

        override val fromNote: Note?
            get() = startNote?.note
        override val toNote: Note?
            get() = endNote?.note
        override val isDirect: Boolean
            get() = true

        constructor (startPoint: Point) : this(startPoint, startPoint, 0f)

        val startNote: UINote?
        init {
            val startButton = mNoteButtons.find { startPoint in it}
            this.startNote = startButton?.uiNote
        }

        var endNote: UINote?
        val endPoint: Point
        init {
            val touchButton: NoteButton? = mNoteButtons.find { touchPoint in it}
            if (touchButton == null || touchButton.uiNote == startNote) {
                this.endNote = null
                this.endPoint = touchPoint
            } else {
                this.endNote = touchButton.uiNote
                this.endPoint = Point(touchButton.uiNote.position, mInnerRadius)
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
    }

    inner class ArcInput(startPoint: Point, touchPoint: Point, sweepAngle: Float) :
            TouchInput(startPoint, touchPoint, sweepAngle) {

        override val fromNote: Note?
            get() = startNote?.note
        override val toNote: Note?
            get() = endNote?.note
        override val isDirect: Boolean
            get() = false

        constructor (startPoint: Point) : this(startPoint, startPoint, 0f)

        val startNote: UINote? by lazy {
            val startButton = mNoteButtons.find { startPoint in it}
            startButton?.uiNote
        }

        val endNote: UINote? by lazy {
            val endButton = mNoteButtons.find { touchPoint in it.sector}
            endButton?.uiNote
        }

        override fun draw(canvas: Canvas) {
            val useCenter = true
            canvas.drawArc(mInnerCircleBounds, startPoint.screenAngle, sweepAngle, useCenter, mSelectPaint)

            for (rel in mRelationships) {
                rel.draw(canvas)
            }

            drawDotsAndRadials(canvas, startNote, endNote)
        }

        fun drawDotsAndRadials(canvas: Canvas, startNote: UINote?, endNote: UINote?) {
            if (startNote == null || endNote == null) return

            if (startNote != endNote) {
                for (note in mNotes.iterateUntilExcluding(startNote, endNote, direction)) {
                    note.drawRadial(canvas, mHighlightPaint)
                    note.drawDot(canvas, mDotPaint)
                }
            }
            startNote.drawHighlighted(canvas)

            if (abs(sweepAngle) < 360f) {
                endNote.drawHighlighted(canvas)
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
    protected val startPoint: Point,
    protected var touchPoint: Point,
    sweepAngle: Float
) {
    abstract val fromNote: Note?
    abstract val toNote: Note?
    abstract val isDirect: Boolean

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
    override fun toString(): String {
        return "TouchInput(fromNote=${fromNote?.name}, toNote=${toNote?.name}, isDirect=$isDirect)"
    }
}

data class NoteCycles(val cycles: List<List<NoteCircle.UINote>>) {
    val uiNotes: List<NoteCircle.UINote>
        get() = cycles.flatten()


}

