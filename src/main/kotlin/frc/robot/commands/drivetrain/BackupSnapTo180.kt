package frc.robot.commands.drivetrain

import edu.wpi.first.math.geometry.Pose2d
import edu.wpi.first.wpilibj2.command.CommandBase
import frc.robot.RobotContainer
import frc.robot.commands.pathing.MoveToPosition
import frc.robot.constants.drivetrain

class BackupSnapTo180 (
    val robotContainer: RobotContainer
) : CommandBase(){

    override fun execute() {
        //MoveToPosition(robotContainer.drivetrain, Pose2d())
        DriveCommand(robotContainer.drivetrain, { 0.0 }, { 0.0 }, )
        MoveToPosition()
    }
    override fun end(interrupted: Boolean) {
        robotContainer.rotateTo180 = false
    }

}