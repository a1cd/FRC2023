package frc.kyberlib.command

import edu.wpi.first.wpilibj.*
import frc.kyberlib.math.units.extensions.seconds

object Game {
    val real = RobotBase.isReal()
    val sim = RobotBase.isSimulation()

    inline val disabled
        get() = RobotState.isDisabled()
    inline val AUTO
        get() = RobotState.isAutonomous()
    inline val OPERATED
        get() = RobotState.isTeleop()
    inline val STOPPED
        get() = RobotState.isEStopped()
    inline val COMPETITION
        get() = DriverStation.isFMSAttached()

    inline val time
        get() = Timer.getFPGATimestamp().seconds
    var startTime = time

    inline val alliance: DriverStation.Alliance
        get() = DriverStation.getAlliance()
}