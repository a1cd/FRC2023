package frc.robot.subsystems

import com.revrobotics.CANSparkMax
import com.revrobotics.CANSparkMaxLowLevel
import com.revrobotics.ColorSensorV3
import edu.wpi.first.math.filter.Debouncer
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard
import edu.wpi.first.wpilibj.util.Color
import edu.wpi.first.wpilibj2.command.SubsystemBase
import frc.robot.commands.manipulator.SetManipulatorSpeed
import frc.robot.constants.manipulator.motorId


class Manipulator : SubsystemBase() {

    private val motor = CANSparkMax(motorId, CANSparkMaxLowLevel.MotorType.kBrushless).apply {
        restoreFactoryDefaults()
        idleMode = CANSparkMax.IdleMode.kBrake
        // make the motor report less often to reduce network traffic

        setPeriodicFramePeriod(CANSparkMaxLowLevel.PeriodicFrame.kStatus0, 10)
        setPeriodicFramePeriod(CANSparkMaxLowLevel.PeriodicFrame.kStatus1, 20)
    }

    val motorIdleDebouncer = Debouncer(0.075)

    var lastPercent = 0.0
    var lastIdleMode = CANSparkMax.IdleMode.kBrake

    var motorPercentage: Double
        get() = motor.get()
        set(value) {
            motor.set(value)
        }

    val tab = Shuffleboard.getTab("Manipulator")
    val motorCurrent = tab.add("Motor Current", 0.0)
        .withWidget("Number Bar")
        .withProperties(mapOf("min" to 0.0, "max" to 40.0))
        .entry

    init {
        defaultCommand = SetManipulatorSpeed(this, 0.05)
    }

    override fun periodic() {
        motorCurrent.setDouble(motor.outputCurrent)
        SmartDashboard.putNumber("INTAKETEMP", motor.motorTemperature)
    }
}