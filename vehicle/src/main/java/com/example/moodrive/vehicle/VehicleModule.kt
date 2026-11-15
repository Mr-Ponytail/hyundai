package com.example.moodrive.vehicle

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.constant.area.SeatValue
import android.content.Context
import com.example.moodrive.domain.model.Access
import com.example.moodrive.domain.model.DriveMode
import com.example.moodrive.domain.port.ClimateController
import com.example.moodrive.domain.port.DriveStateSource
import com.example.moodrive.domain.port.GuidedLightController
import com.example.moodrive.vehicle.mock.MockClimateController
import com.example.moodrive.vehicle.mock.MockDriveStateSource
import com.example.moodrive.vehicle.mock.MockLightController
import com.example.moodrive.vehicle.pleos.PleosClimateController
import com.example.moodrive.vehicle.pleos.PleosDriveStateSource
import com.example.moodrive.vehicle.pleos.PleosLightController

/**
 * 차량 연동 진입점. 앱은 이 클래스만 알면 되고 Pleos SDK 타입은 보지 않는다.
 * 조명은 항상 [GuidedLightController]로 감싼다 → 제어 권한이 없으면 자동으로 안내 모드.
 */
class VehicleModule private constructor(
    val light: GuidedLightController,
    val climate: ClimateController,
    val driveState: DriveStateSource,
    private val onConnect: () -> Unit,
) {
    /**
     * SDK 서비스 바인딩은 비동기지만, 연결 전 호출은 SDK가 대기열에 쌓았다가 연결되면 처리한다.
     * 그래서 connect()만 먼저 불러 두면 바로 조회해도 된다. (connect() 없이 부르면 즉시 실패)
     */
    fun connect() = onConnect()

    companion object {
        fun pleos(context: Context): VehicleModule {
            val vehicle = Vehicle(context.applicationContext)
            val driver = SeatArea(SeatValue.ROW_1_LEFT)
            return VehicleModule(
                light = GuidedLightController(PleosLightController(vehicle, driver, VehicleColors.EMULATOR)),
                climate = PleosClimateController(vehicle, driver),
                driveState = PleosDriveStateSource(vehicle),
                onConnect = vehicle::initialize,
            )
        }

        /** [lightAccess]를 READ로 두면 안내 모드, [driveMode]를 바꾸면 주행 상태를 재현할 수 있다 */
        fun mock(
            lightAccess: Access = Access.READ_WRITE,
            driveMode: DriveMode = DriveMode.PARKED,
        ): VehicleModule = VehicleModule(
            light = GuidedLightController(MockLightController(VehicleColors.EMULATOR, lightAccess)),
            climate = MockClimateController(),
            driveState = MockDriveStateSource(driveMode),
            onConnect = {},
        )
    }
}
