package frc.robot.commands.wrist

import edu.wpi.first.wpilibj2.command.CommandBase
import frc.robot.subsystems.Wrist
import kotlin.math.PI

class SetWristAngle(
    private val wrist: Wrist,
    private val angle: Double
) : CommandBase() {
    init {
        addRequirements(wrist)
    }

    override fun initialize() {
        wrist.setPosition(angle.coerceIn(-PI / 2, PI / 2))
    }

    override fun execute() {
    }

    override fun end(interrupted: Boolean) {

    }

    override fun isFinished(): Boolean {
        return true
    }
}