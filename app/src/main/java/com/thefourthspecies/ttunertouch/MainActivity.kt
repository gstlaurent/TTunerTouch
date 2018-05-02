package com.thefourthspecies.ttunertouch

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    lateinit var controller: NewTemperamentController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        noteCircle.textView = textView


        controller = NewTemperamentController(noteCircle, supportFragmentManager)




        // // Example of a call to a native method
        // sample_text.text = stringFromJNI()
    }

   //
   // /**
   //  * A native method that is implemented by the 'native-lib' native library,
   //  * which is packaged with this application.
   //  */
   // external fun stringFromJNI(): String

   // companion object {

   //     // Used to load the 'native-lib' library on application startup.
   //     init {
   //         System.loadLibrary("native-lib")
   //     }
   // }
}

//class MainActivity : Activity(), GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {
//    private var mDetector: GestureDetectorCompat? = null
//
//    // Called when the activity is first created.
//    public override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
//
//        noteCircle.textView = textView
//        // Instantiate the gesture detector with the
//        // application context and an implementation of
//        // GestureDetector.OnGestureListener
//        mDetector = GestureDetectorCompat(this, this)
//        // Set the gesture detector as the double tap
//        // listener.
//        mDetector!!.setOnDoubleTapListener(this)
//    }
//
//    override fun onTouchEvent(event: MotionEvent): Boolean {
//        this.mDetector!!.onTouchEvent(event)
//        // Be sure to call the superclass implementation
//        return super.onTouchEvent(event)
//    }
//
//    override fun onDown(event: MotionEvent): Boolean {
//        Log.d(DEBUG_TAG, "onDown: " + event.toString())
//        return true
//    }
//
//    override fun onFling(event1: MotionEvent, event2: MotionEvent,
//                         velocityX: Float, velocityY: Float): Boolean {
//        Log.d(DEBUG_TAG, "onFling: " + event1.toString() + event2.toString())
//        return true
//    }
//
//    override fun onLongPress(event: MotionEvent) {
//        Log.d(DEBUG_TAG, "onLongPress: " + event.toString())
//    }
//
//    override fun onScroll(event1: MotionEvent, event2: MotionEvent, distanceX: Float,
//                          distanceY: Float): Boolean {
//        Log.d(DEBUG_TAG, "onScroll: " + event1.toString() + event2.toString())
//        return true
//    }
//
//    override fun onShowPress(event: MotionEvent) {
//        Log.d(DEBUG_TAG, "onShowPress: " + event.toString())
//    }
//
//    override fun onSingleTapUp(event: MotionEvent): Boolean {
//        Log.d(DEBUG_TAG, "onSingleTapUp: " + event.toString())
//        return true
//    }
//
//    override fun onDoubleTap(event: MotionEvent): Boolean {
//        Log.d(DEBUG_TAG, "onDoubleTap: " + event.toString())
//        return true
//    }
//
//    override fun onDoubleTapEvent(event: MotionEvent): Boolean {
//        Log.d(DEBUG_TAG, "onDoubleTapEvent: " + event.toString())
//        return true
//    }
//
//    override fun onSingleTapConfirmed(event: MotionEvent): Boolean {
//        Log.d(DEBUG_TAG, "onSingleTapConfirmed: " + event.toString())
//        return true
//    }
//
//    companion object {
//
//        private val DEBUG_TAG = "Gestures"
//    }
//}
//

