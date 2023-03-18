package frc.robot.commands.drivetrain

import edu.wpi.first.math.geometry.Translation2d
import edu.wpi.first.math.kinematics.ChassisSpeeds
import edu.wpi.first.wpilibj.DriverStation
import edu.wpi.first.wpilibj2.command.CommandBase
import frc.kyberlib.command.Game
import frc.robot.constants.Constants
import frc.robot.constants.Field2dLayout
import frc.robot.controls.ControlScheme
import frc.robot.subsystems.Drivetrain
import frc.robot.subsystems.slewLimited

class DriveCommand(
    var drivetrain: Drivetrain,
    var controlScheme: ControlScheme,
) : CommandBase() {
    init {
        addRequirements(drivetrain)
    }
    override fun execute() {
        val alianceMulitplier = when (Game.alliance) {
            DriverStation.Alliance.Invalid -> 1.0
            DriverStation.Alliance.Blue -> Field2dLayout.Axes.Blue.fieldOffsetMultiplier
            DriverStation.Alliance.Red -> Field2dLayout.Axes.Red.fieldOffsetMultiplier
        }
        val vec = Translation2d(-controlScheme.forward, -controlScheme.strafe)
            .times(3.5)
        drivetrain.drive(
            ChassisSpeeds(
                vec.x * Constants.powerPercent * alianceMulitplier * (if (drivetrain.invertx.getBoolean(false)) -1 else 1),
                vec.y * Constants.powerPercent * alianceMulitplier * (if (drivetrain.inverty.getBoolean(false)) -1 else 1),
                -controlScheme.rotation * 2 * Math.PI *
                        Constants.powerPercent * .5 * (if (drivetrain.invertrot.getBoolean(false)) -1 else 1)
            ).slewLimited(drivetrain.xSlewRateLimiter, drivetrain.ySlewRateLimiter, drivetrain.rotSlewRateLimiter),
            true,
            Translation2d() // chris wants in the middle
        )
    }
}