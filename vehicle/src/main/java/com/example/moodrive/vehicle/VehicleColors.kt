package com.example.moodrive.vehicle

/**
 * 차량이 낼 수 있는 앰비언트 색. SDK Capability가 색 목록을 주지 않아 차종별로 여기서 관리한다.
 *
 * TODO: 에뮬레이터 가상 콘솔에서 각 색을 고른 뒤 getAmbientLightBrightnessColor 반환값으로 실제 int 값을 확인해 교체할 것.
 *       (현재 값은 이름 기준 추정치)
 */
object VehicleColors {
    val EMULATOR = listOf(
        0xFF00FFFF.toInt(), // Cyan
        0xFF0000FF.toInt(), // Blue
        0xFF8000FF.toInt(), // Purple
        0xFFFFFFFF.toInt(), // White
    )
}
