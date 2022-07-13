package fastcampus.app.part2.chapter07

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {

    private val visualizerView: SoundVisualizerView by lazy {
        findViewById(R.id.visuallizer)
    }
    private val recordTimeView: CountUpView by lazy {
        findViewById(R.id.record_time)
    }
    private val resetBtn: Button by lazy {
        findViewById(R.id.reset_btn)
    }
    private val recordButton: RecordButton by lazy {
        findViewById(R.id.record_btn)
    }
    private val requiredPermission = arrayOf(Manifest.permission.RECORD_AUDIO)
    private val recordingFilePath: String by lazy { // 외부 저장소에 녹음 파일을 지정
        "${externalCacheDir?.absolutePath}/recording.3gp" // 파일 경로
    }
    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var state = State.BEFORE_RECORDING
        set(value) {// 새로운value를 할당 할때마다 아이콘 업데이트 메서드가 호출됨
            field = value
            resetBtn.isEnabled = (value == State.AFTER_RECORDING) ||
                    (value == State.ON_PLAYING)
            recordButton.updateIcon(value)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestPermission()
        initViews()
        onClick()
        initVariables()
    }

    // 권한이 허용되었는지 여기서 확인
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val audioRecordPermissionGranted =
            requestCode == REQUEST_RECORD_AUDIO_PERMISSION &&
                    grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED

        if (!audioRecordPermissionGranted) { //권한이 부여가 되지 않았을 경우
            finish()// 앱 종료
        }
    }

    private fun requestPermission() {
        requestPermissions(requiredPermission, REQUEST_RECORD_AUDIO_PERMISSION)
    }

    private fun initViews() {
        recordButton.updateIcon(state)
    }

    private fun onClick() {
        visualizerView.onRequestCurrentAmplitude = {
            recorder?.maxAmplitude ?: 0
        }
        resetBtn.setOnClickListener {
            stopPlaying()
            visualizerView.clearVisualization()
            recordTimeView.clearCountTime()
            state = State.BEFORE_RECORDING
        }
        recordButton.setOnClickListener {
            when (state) {
                State.BEFORE_RECORDING -> {
                    startRecording()
                }
                State.ON_RECORDING -> {
                    stopRecording()
                }
                State.AFTER_RECORDING -> {
                    startPlaying()
                }
                State.ON_PLAYING -> {
                    stopPlaying()
                }
            }
        }
    }

    private fun initVariables() {
        state = State.BEFORE_RECORDING
    }

    private fun startRecording() {//녹음 시작
        recorder = MediaRecorder().apply { // 미디어 리코더를 사용하기 위한 설정
            setAudioSource(MediaRecorder.AudioSource.MIC)//마이크에 접근
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)//인코더
            setOutputFile(recordingFilePath)//녹음된 오디오를 압축해서 저장할곳
            prepare()//이걸 해줘야지 실제로 녹음이 가능한 상태가 됨
        }
        recorder?.start()//녹음 시작
        visualizerView.startVisualizing(false)
        recordTimeView.startCountUp()
        state = State.ON_RECORDING
    }

    private fun stopRecording() {//녹음 정지
        recorder?.run {
            stop()
            release()//메모리 해제
        }
        recorder = null
        visualizerView.stopVisualizing()
        recordTimeView.stopCountUp()
        state = State.AFTER_RECORDING
    }

    private fun startPlaying() {//재생 시작
        player = MediaPlayer().apply {
            setDataSource(recordingFilePath)//재생할 데이터의 경로
            prepare()// 재생을 할수있는 상태로 만들어줌
        }
        player?.setOnCompletionListener {
            stopPlaying()
            state = State.AFTER_RECORDING
        }
        player?.start()
        visualizerView.startVisualizing(true)
        recordTimeView.startCountUp()
        state = State.ON_PLAYING
    }

    private fun stopPlaying() {//재생 중지
        player?.release()
        player = null
        visualizerView.stopVisualizing()
        recordTimeView.stopCountUp()
        state = State.AFTER_RECORDING
    }

    companion object {
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 201
    }
}