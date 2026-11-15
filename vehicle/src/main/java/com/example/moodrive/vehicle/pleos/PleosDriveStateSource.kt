package com.example.moodrive.vehicle.pleos

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.listener.GearSelectionListener
import com.example.moodrive.domain.model.DriveMode
import com.example.moodrive.domain.port.DriveStateSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * 기어 기준 주행 상태: P = 정차, 그 외 = 주행.
 *
 * TODO: 기획서 기준은 AAOS CarUxRestrictionsManager(+ 속도). 에뮬레이터에서 동작을 확인한 뒤
 *       CarUxRestrictions 기반 구현으로 교체하거나 두 신호를 함께 쓴다.
 */
class PleosDriveStateSource(private val vehicle: Vehicle) : DriveStateSource {

    override val driveMode: Flow<DriveMode> = callbackFlow {
        val gear = vehicle.getGear()
        gear.getCurrentGearSelectionState({ trySend(it.toDriveMode()) }, { trySend(DriveMode.UNKNOWN) })

        val listener = object : GearSelectionListener {
            override fun onSelected(value: VehicleGear?) { trySend(value.toDriveMode()) }
            override fun onFailed(e: Exception) { trySend(DriveMode.UNKNOWN) }
        }
        gear.registerGearSelection(listener)
        awaitClose { gear.unregisterGearSelection(listener) }
    }.distinctUntilChanged()

    private fun VehicleGear?.toDriveMode() = when (this) {
        VehicleGear.GEAR_PARK -> DriveMode.PARKED
        null, VehicleGear.GEAR_UNKNOWN -> DriveMode.UNKNOWN
        else -> DriveMode.DRIVING
    }
}
