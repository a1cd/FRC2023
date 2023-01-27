package frc.robot

import kotlin.math.PI

object Constants {
    const val gyroReversed = false
    const val FRDriveMotorId = 10//fr
    const val BLDriveMotorId = 11//bl
    const val FLDriveMotorId = 12//fl
    const val BRDriveMotorId = 13

    const val FRTurnMotorId = 14//fr
    const val BLTurnMotorId = 15//bl
    const val FLTurnMotorId = 16//fl
    const val BRTurnMotorId = 17

    const val FRTurnEncoderId = 6//fr
    const val BLTurnEncoderId = 7//bl
    const val FLTurnEncoderId = 8//fl
    const val BRTurnEncoderId = 9

    const val WHEEL_RADIUS = .0508
    val WHEEL_CIRCUMFRENCE = WHEEL_RADIUS * 2 * PI
    const val DRIVE_GEAR_RATIO = 6.75
}