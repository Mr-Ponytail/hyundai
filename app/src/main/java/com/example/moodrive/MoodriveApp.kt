package com.example.moodrive

import android.app.Application
import android.util.Log
import com.example.moodrive.di.AppContainer
import com.example.moodrive.ui.common.LOG_TAG
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class MoodriveApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.vehicle.connect()
        recoverPreviousSession()
    }

    /**
     * 이전 프로세스가 Scene 실행 중에 죽었다면 차량을 Scene 적용 전 상태로 되돌린다.
     * SDK는 연결 전 호출을 대기열에 쌓았다가 연결되면 처리하므로 connect() 직후 바로 시작해도 된다.
     * 복원하는 동안 세션이 Restoring이라 홈 화면은 '처리 중'으로 보이고, 안내 모드면 배너가 뜬다.
     */
    private fun recoverPreviousSession() {
        container.appScope.launch {
            try {
                container.sceneSession.recoverIfNeeded()?.let { Log.i(LOG_TAG, "이전 세션 복구: $it") }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(LOG_TAG, "이전 세션 복구 실패: ${e.message}", e)
            }
        }
    }
}
