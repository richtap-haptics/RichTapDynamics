package com.example.richtapdynamics

import android.graphics.PointF
import kotlin.math.abs
import kotlin.math.pow

class BezierCurve(private val startPoint: PointF, private val endPoint: PointF) {

    private lateinit var controlPoint1: PointF
    private lateinit var controlPoint2: PointF
    private val curvePoints = ArrayList<PointF>()
    private val POINT_NUM = 100 // How many points we'll calculate for the curve

    fun setControlPoints(p1: PointF, p2: PointF) {
        controlPoint1 = p1
        controlPoint2 = p2
    }

    fun buildBezierPoints() {
        for (i in 0..POINT_NUM) {
            val pf = getCoord(1.0f * i / POINT_NUM)
            curvePoints.add(pf)
            //Log.v("BezierCurve", "$pf")
        }
    }

    // https://stackoverflow.com/questions/7348009/y-coordinate-for-a-given-x-cubic-bezier
    fun getYFromX(x: Int) : Int {
        val index = binarySearch(0, POINT_NUM - 1, x)
        return if (index != -1) {
            curvePoints[index].y.toInt()
        } else {
            -1
        }
    }

    private fun binarySearch(low: Int, high: Int, value: Int): Int {
        if (low > high) {
            return -1
        }
        while (low <= high) {
            val middle = (low + high) / 2
            if (abs(curvePoints[middle].x - value) < 1.0F) {
                return middle  // Cheers!
            }
            if (value > curvePoints[middle].x) { // 值在右边，将底边界移动到middle左边
                   return binarySearch(middle + 1 , high, value)
            }
            if (value < curvePoints[middle].x) { // 值在左边，将顶边界移动到middle右边
                return binarySearch(low, middle - 1, value)
            }
        }
        return -1 // can't find the value
    }

    private fun getCoord(t: Float) : PointF {
        val _t = 1 - t
        val coefficient0 = _t.pow(3)
        val coefficient1 = 3 * t * _t.pow(2)
        val coefficient2 = 3 * _t * t.pow(2)
        val coefficient3 = t.pow(3)
        val px = coefficient0 * startPoint.x + coefficient1 * controlPoint1.x + coefficient2 * controlPoint2.x + coefficient3 * endPoint.x
        val py = coefficient0 * startPoint.y + coefficient1 * controlPoint1.y + coefficient2 * controlPoint2.y + coefficient3 * endPoint.y
        return PointF(px, py)
    }
}