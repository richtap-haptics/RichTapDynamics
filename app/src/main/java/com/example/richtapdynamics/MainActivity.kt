package com.example.richtapdynamics

import android.content.Context
import android.content.res.AssetManager
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import com.example.richtapdynamics.databinding.ActivityMainBinding
import com.apprichtap.haptic.RichTapUtils
import com.apprichtap.haptic.base.PrebakedEffectId.*
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.lang.Exception
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {

    private val TAG = "RichTap-SAMPLE"
    private lateinit var binding: ActivityMainBinding

    private lateinit var heJSON: String
    private lateinit var heFilePath: String
    private var currentPrebakedId = 0

    private val mediaPlayer = MediaPlayer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize RichTap SDK
        // Make sure we're using the correct version of SDK
        RichTapUtils.getInstance().let {
            it.init(this)
            if (it.isSupportedRichTap) {
                binding.txtHasRichTap.text = "Congrats! Your device supports high-fidelity haptic feedback, which is powered by RichTap."
            } else {
                binding.txtHasRichTap.text = "Sorry, but your device only supports low-fidelity haptic feedback..."
            }
        }

        // 从assets目录加载效果HE文件内容
        heJSON = loadHeFromAssets("Car Ignite.he")
        heFilePath = dumpAssetToDataStorage("Car Ignite.he")

        // 演示SDK的预置触感效果，Effect ID范围：[PREBAKED_ID_MIN, PREBAKED_ID_MAX]
        // 各个ID的具体含义参见 PreBakedEffectId.class
        currentPrebakedId = PREBAKED_ID_MIN
        binding.btnPlayPrebaked.setOnClickListener {
            val amplitude = binding.sbPrebakedAmp.progress
            RichTapUtils.getInstance().playExtPrebaked(currentPrebakedId, amplitude)
            binding.prebakedInfo.text = "Prebaked Effect - ID: $currentPrebakedId"
        }
        binding.btnPlayNext.setOnClickListener {
            if (++currentPrebakedId > PREBAKED_ID_MAX) {
                currentPrebakedId = PREBAKED_ID_MIN
            }
            val amplitude = binding.sbPrebakedAmp.progress
            RichTapUtils.getInstance().playExtPrebaked(currentPrebakedId, amplitude)
            binding.prebakedInfo.text = "Prebaked Effect - ID: $currentPrebakedId"
        }
        binding.btnPlayPrevious.setOnClickListener {
            if (--currentPrebakedId < PREBAKED_ID_MIN) {
                currentPrebakedId = PREBAKED_ID_MAX
            }
            val amplitude = binding.sbPrebakedAmp.progress
            RichTapUtils.getInstance().playExtPrebaked(currentPrebakedId, amplitude)
            binding.prebakedInfo.text = "Prebaked Effect - ID: $currentPrebakedId"
        }
        // 调整预置效果的振动强度
        binding.sbPrebakedAmp.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    binding.txtPreAmplitude.text = "Amplitude (${progress}):"
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }
            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }
        })

        // Demonstrate how to play a haptic file instead of HE Json string
        // 演示如何循环播放一个振动效果，以及对它打断/停止
        binding.btnPlayLoop.setOnClickListener {
            // NOTE: Don't pass filepath as String.
            //  Don't call sendLoopParameter before calling playHaptic.
            val heFile = File(heFilePath)
            val interval = binding.sbLoopInterval.progress
            val amplitude = binding.sbLoopAmp.progress
            val frequency = binding.sbLoopFreq.progress
            RichTapUtils.getInstance().playHaptic(heFile, 100, interval, amplitude, frequency) // 循环100次
        }
        binding.btnStopLoop.setOnClickListener {
            RichTapUtils.getInstance().stop()
        }
        binding.sbLoopAmp.setOnSeekBarChangeListener(seekBarListener)
        binding.sbLoopFreq.setOnSeekBarChangeListener(seekBarListener)
        binding.sbLoopInterval.setOnSeekBarChangeListener(seekBarListener)

        // Demonstrate how to play haptics with a sound effect
        // 演示如何伴随音效一起振动
        binding.ivGun.setOnClickListener {
            try {
                val fd = assets.openFd("Car Ignite.wav")
                mediaPlayer.run {
                    reset()
                    setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                    prepare()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            mediaPlayer.start()
            RichTapUtils.getInstance().playHaptic(heJSON, 0)
        }
    }

    private val seekBarListener =  object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            if (fromUser) {
                when (seekBar.id) {
                    R.id.sbLoopAmp -> binding.txtLoopAmplitude.text = "Amplitude (${progress}):"
                    R.id.sbLoopFreq -> binding.txtLoopFrequency.text = "Frequency (${progress}):"
                    R.id.sbLoopInterval -> binding.txtLoopInterval.text = "Interval (${progress}):"
                }
            }
        }
        override fun onStartTrackingTouch(seekBar: SeekBar) {
        }
        override fun onStopTrackingTouch(seekBar: SeekBar) {
            adjustLoopParameters()
        }
    }

    private fun adjustLoopParameters() {
        val amplitude = binding.sbLoopAmp.progress
        val interval = binding.sbLoopInterval.progress
        val frequency = binding.sbLoopFreq.progress
        RichTapUtils.getInstance().sendLoopParameter(amplitude, interval, frequency)
    }

    override fun onDestroy() {
        // RichTap SDK反初始化
        RichTapUtils.getInstance().quit()

        mediaPlayer.stop()
        mediaPlayer.release()

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
                    setMessage("App Version: ${BuildConfig.VERSION_NAME}\n" +
                            "RichTap SDK: ${RichTapUtils.VERSION_NAME}")
                    setCancelable(true)
                    setPositiveButton("OK") { _, _ ->}
                    show()
                }
            }

            R.id.close -> {
                finish()
                exitProcess(0)
            }
        }
        return true
    }
}