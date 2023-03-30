package frc.robot.commands.intake

import edu.wpi.first.wpilibj2.command.CommandBase
import frc.robot.subsystems.Intake

class IdleIntake (
    private val intake: Intake,
) : CommandBase() {

    init{
        addRequirements(intake)
    }

    override fun execute() {
        intake.setDeployAngle(0.0)
        intake.setModeAngle(0.0)
    }

    override fun isFinished() =
        intake.deployPID.atGoal() &&
        intake.modePID.atGoal()
}

