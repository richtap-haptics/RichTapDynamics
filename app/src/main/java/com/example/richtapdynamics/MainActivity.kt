package com.example.richtapdynamics

import android.content.Context
import android.content.res.AssetManager
import android.graphics.PointF
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import com.example.richtapdynamics.databinding.ActivityMainBinding
import com.apprichtap.haptic.RichTapUtils
import com.apprichtap.haptic.base.PreBakedEffectId.PREBAKED_ID_MAX
import com.apprichtap.haptic.base.PreBakedEffectId.PREBAKED_ID_MIN
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.lang.Thread.sleep

class MainActivity : AppCompatActivity() {

    private val TAG = "RichTap-SAMPLE"
    private lateinit var binding: ActivityMainBinding

    private lateinit var heBowDrag: String
    private lateinit var heBowRelease: String
    private lateinit var heFilePath: String
    private lateinit var bezierCurve: BezierCurve
    private var currentPrebakedId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize RichTap SDK
        // Make sure we're using the correct version of SDK
        binding.sdkInfo.text = "RichTap SDK: ${RichTapUtils.VERSION_NAME}"
        RichTapUtils.getInstance().let {
            it.init(this)
            if (it.isSupportedRichTap) {
                binding.txtHasRichTap.text = "Congrats! Your device supports high-fidelity haptic feedback which is powered by RichTap."
            } else {
                binding.txtHasRichTap.text = "Sorry, but your device only supports low-fidelity haptic feedback..."
            }
        }

        // 从assets目录加载效果HE文件内容
        heBowDrag = loadHeFromAssets("bow_drag.he")
        heBowRelease = loadHeFromAssets("bow_release.he")
        heFilePath = dumpAssetToDataStorage("heartbeat.he")

        // 构建一条贝塞尔曲线：起点（0, 0）-> 终点（100, 255）
        // 拉弓：按住往右拖动 / 放箭：松开手指 - 动态调整振动强度
        binding.txtDraggingBow.text = "Drag & release to feel dynamic intensity change of the vibration"
        bezierCurve = BezierCurve(PointF(0F,0F), PointF(100F, 255F)).apply {
            setControlPoints(PointF(89F, 73.95F), PointF(99F, 140.25F))
            buildBezierPoints()
        }

        // 用一个进度条模拟“拉弓-射箭”过程中的动态调参
        binding.triggerBow.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                Log.d(TAG, "onProgressChanged: $progress, fromUser: $fromUser")
                if (fromUser) {
                    val amplitude = bezierCurve.getYFromX(progress)
                    Log.d(TAG, ">> The new amplitude: $amplitude")
                    if (amplitude > 0) {
                        // Dynamically change the amplitude / intensity of the vibration
                        // 动态调整振动的强度
                        RichTapUtils.getInstance().sendLoopParameter(amplitude, 0)
                    }
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {
                Log.d(TAG, "onStartTrackingTouch")
                // Keep playing a haptic (an infinite loop) till another haptic is played
                // 开始拉弓，强度初始为1，无限循环
                RichTapUtils.getInstance().playHaptic(heBowDrag, -1, 1)
            }
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                Log.d(TAG, "onStopTrackingTouch")
                seekBar.progress = 0
                RichTapUtils.getInstance().playHaptic(heBowRelease, 0) // 模拟放箭
            }
        })

        // 演示SDK的预置触感效果，Effect ID范围：[PREBAKED_ID_MIN, PREBAKED_ID_MAX]
        // 各个ID的具体含义参见 PreBakedEffectId.class
        currentPrebakedId = PREBAKED_ID_MIN
        binding.btnPlayPre.setOnClickListener {
            RichTapUtils.getInstance().playExtPreBaked(currentPrebakedId, 255)
            binding.prebakedInfo.text = "Prebaked Effect - ID: $currentPrebakedId"
        }
        binding.btnPlayPreNext.setOnClickListener {
            if (++currentPrebakedId > PREBAKED_ID_MAX) {
                currentPrebakedId = PREBAKED_ID_MIN
            }
            RichTapUtils.getInstance().playExtPreBaked(currentPrebakedId, 255)
            binding.prebakedInfo.text = "Prebaked Effect - ID: $currentPrebakedId"
        }

        // Demonstrate how to play a haptic file instead of HE Json string
        // 演示如何循环播放一个振动效果，以及对它打断/停止
        binding.btnPlayLoop.setOnClickListener {
            // NOTE: Don't pass filepath as String.
            //  Don't call sendLoopParameter before calling playHaptic
            val heFile = File(heFilePath)
            RichTapUtils.getInstance().playHaptic(heFile, 100, 350, 255, 0) // 循环100次
        }
        binding.btnStopLoop.setOnClickListener {
            RichTapUtils.getInstance().stop()
        }
    }

    override fun onDestroy() {
        // RichTap SDK反初始化
        RichTapUtils.getInstance().quit()

        super.onDestroy()
    }

    private fun loadHeFromAssets(fileName: String): String {
        val sb = StringBuilder()
        try {
            val stream = assets.open(fileName, AssetManager.ACCESS_STREAMING)
            val reader = BufferedReader(InputStreamReader(stream, "utf-8"))
            reader.use {
                reader.forEachLine {
                    sb.append(it)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return sb.toString()
    }

    private fun dumpAssetToDataStorage(filename: String) : String {
        val destFile = getFileStreamPath(filename)
        try {
            if (!destFile.exists()) {
                assets.open(filename, Context.MODE_PRIVATE).use {
                    val output = openFileOutput(filename, Context.MODE_PRIVATE)
                    it.copyTo(output)
                    output.close()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            return destFile.toString()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.about -> {
                AlertDialog.Builder(this).apply {
                    setTitle("About...")
                    setMessage("Version: ${BuildConfig.VERSION_NAME}")
                    setCancelable(true)
                    setPositiveButton("OK") { _, _ ->}
                    show()
                }
            }

            R.id.close -> finish()
        }
        return true
    }
}