package frc.robot.commands.pathing

import edu.wpi.first.math.controller.ProfiledPIDController
import edu.wpi.first.math.geometry.Pose2d
import edu.wpi.first.math.geometry.Rotation2d
import edu.wpi.first.math.geometry.Transform2d
import edu.wpi.first.math.geometry.Translation2d
import edu.wpi.first.math.kinematics.ChassisSpeeds
import edu.wpi.first.math.trajectory.TrapezoidProfile
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard
import edu.wpi.first.wpilibj2.command.CommandBase
import edu.wpi.first.wpilibj2.command.WaitCommand
import frc.robot.commands.alltogether.Idle
import frc.robot.commands.alltogether.SetPosition
import frc.robot.commands.manipulator.CloseManipulator
import frc.robot.commands.manipulator.SetManipulatorSpeed
import frc.robot.subsystems.*
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.random.Random

class MoveToPosition(
    private val drivetrain: Drivetrain,
    /**
     * The desired position of the robot (in meters)
     */
    private val pose: Pose2d,
    /**
     * The desired velocity of the robot (in meters per second)
     */
    private val velocity: Transform2d = Transform2d(),
    private val toleranceppos: Double = 0.1,
    private val tolerancepvel: Double = 0.1,
    private val tolerancerpos: Double = 0.025,
    private val tolerancervel: Double = 0.1,
) : CommandBase() {
    constructor(drivetrain: Drivetrain, x: Double = 0.0, y: Double = 0.0, angle: Double = 0.0) : this(
        drivetrain,
        Pose2d(x, y, Rotation2d.fromDegrees(angle)),
        Transform2d(Translation2d(0.0, 0.0), Rotation2d.fromDegrees(0.0)),
    )

    init {
        addRequirements(drivetrain)
    }

    // these entries are used to debug how fast the robot wants to move to get
    // to the desired position
    val speedx = drivetrain.Idrc.add("speedx1${Random.nextDouble()}", 0.0)
        .entry
    val speedy = drivetrain.Idrc.add("speedy1${Random.nextDouble()}", 0.0)
        .entry
    val speedr = drivetrain.Idrc.add("speedr1${Random.nextDouble()}", 0.0)
        .entry

    val xPIDController = ProfiledPIDController(
        Companion.xP, 0.0, 0.0, TrapezoidProfile.Constraints(
            4.0,
            3.0
        )
    ).also {
        it.reset(drivetrain.poseEstimator.estimatedPosition.translation.x, 0.0)
        it.setTolerance(toleranceppos, tolerancepvel)
    }
    val yPIDController = ProfiledPIDController(
        Companion.yP, 0.0, 0.0, TrapezoidProfile.Constraints(
            4.0,
            3.0
        )
    ).also {
        it.reset(drivetrain.poseEstimator.estimatedPosition.translation.y, 0.0)
        it.setTolerance(toleranceppos, tolerancepvel)
    }
    val rPIDController = ProfiledPIDController(
        Companion.rP, 0.0, 0.0, TrapezoidProfile.Constraints(
            PI / 2, PI / 1.5
        )
    ).also {
        it.enableContinuousInput(-PI, PI)
        it.reset(drivetrain.poseEstimator.estimatedPosition.rotation.radians, 0.0)
        it.setTolerance(tolerancerpos, tolerancervel)
    }

    val start = drivetrain.poseEstimator.estimatedPosition

    override fun initialize() {
        xPIDController.reset(drivetrain.poseEstimator.estimatedPosition.translation.x,0.0)
        yPIDController.reset(drivetrain.poseEstimator.estimatedPosition.translation.y, 0.0)
        rPIDController.reset(drivetrain.poseEstimator.estimatedPosition.rotation.radians, 0.0)
    }

    // on command start and every time the command is executed, calculate the

    override fun execute() {
        val current = drivetrain.poseEstimator.estimatedPosition
        val desired = pose

        // log the current position and the desired position
        SmartDashboard.putNumber("curr-x", current.translation.x)
        SmartDashboard.putNumber("curr-y", current.translation.y)
        SmartDashboard.putNumber("curr-r", current.rotation.radians)
        SmartDashboard.putNumber("des-x", desired.translation.x)
        SmartDashboard.putNumber("des-y", desired.translation.y)
        SmartDashboard.putNumber("des-r", desired.rotation.radians)


        // calculate the speeds needed to get to the desired position
        val speeds = ChassisSpeeds(
            xPIDController.calculate(
                current.translation.x,
                TrapezoidProfile.State(
                    desired.translation.x,
                    velocity.translation.x
                )
            ),
            yPIDController.calculate(
                current.translation.y,
                TrapezoidProfile.State(
                    desired.translation.y,
                    velocity.translation.y
                )
            ),
            rPIDController.calculate(
                current.rotation.radians,
                TrapezoidProfile.State(
                    desired.rotation.radians,
                    velocity.rotation.radians
                )
            )
        )

        SmartDashboard.putNumber("xpiderr", xPIDController.positionError)
        SmartDashboard.putNumber("ypiderr", yPIDController.positionError)
        SmartDashboard.putNumber("rpiderr", rPIDController.positionError)

        SmartDashboard.putNumber("xpidvel", xPIDController.velocityError)
        SmartDashboard.putNumber("ypidvel", yPIDController.velocityError)
        SmartDashboard.putNumber("rpidvel", rPIDController.velocityError)

        SmartDashboard.putData("xpid", xPIDController)
        SmartDashboard.putData("ypid", yPIDController)
        SmartDashboard.putData("rpid", rPIDController)

        SmartDashboard.putNumber("xpidPosTolerance", xPIDController.positionTolerance)
        SmartDashboard.putNumber("ypidPosTolerance", yPIDController.positionTolerance)
        SmartDashboard.putNumber("rpidPosTolerance", rPIDController.positionTolerance)

        SmartDashboard.putNumber("xpidVelTolerance", xPIDController.velocityTolerance)
        SmartDashboard.putNumber("ypidVelTolerance", yPIDController.velocityTolerance)
        SmartDashboard.putNumber("rpidVelTolerance", rPIDController.velocityTolerance)

        // at goal
        SmartDashboard.putBoolean("xpidAtGoal", xPIDController.atGoal())
        SmartDashboard.putBoolean("ypidAtGoal", yPIDController.atGoal())
        SmartDashboard.putBoolean("rpidAtGoal", rPIDController.atGoal())

        SmartDashboard.putNumber("SPEED", hypot(speeds.vxMetersPerSecond, speeds.vyMetersPerSecond))
//        val speeds = ChassisSpeeds(
//          1.0, 0.0, 0.0
//        )

        // set the debug entries to the speeds so we can see values in the
        // smartdashboard
        speedx.setDouble(speeds.vxMetersPerSecond)
        speedy.setDouble(speeds.vyMetersPerSecond)
        speedr.setDouble(speeds.omegaRadiansPerSecond)


        // tell the drivetrain to drive at the calculated speeds
        drivetrain.drive(speeds, true)
    }

    override fun isFinished(): Boolean {
        // stop when the robot is within 0.1 meters of the desired position
        return drivetrain.poseEstimator.estimatedPosition.minus(Pose2d(pose.translation, Rotation2d())).translation.norm < toleranceppos
                && drivetrain.poseEstimator.estimatedPosition.rotation.minus(Rotation2d(pose.rotation.radians)).radians < tolerancerpos
    }

    override fun end(interrupted: Boolean) {
        drivetrain.drive(ChassisSpeeds(), true)
        // reset the PID controllers
        xPIDController.reset(0.0, 0.0)
        yPIDController.reset(0.0, 0.0)
        rPIDController.reset(0.0, 0.0)
    }

    companion object {
        const val rP = 4.0
        const val yP = 2.25
        const val xP = 2.25

            fun pathBlue(drivetrain: Drivetrain, elevator: Elevator, arm: Arm, wrist: Wrist, manipulator: Manipulator) =
                run {
                    (drivetrain.poseEstimator.estimatedPosition)
                    CloseManipulator(manipulator).andThen(MoveToPosition(drivetrain, 1.8, 1.0))
                        .andThen(SetPosition.high(elevator, arm, wrist)
                            .withTimeout(3.0))
                        .andThen(SetManipulatorSpeed(manipulator, -1.0, true).deadlineWith(WaitCommand(1.0)))
                        .andThen(MoveToPosition(drivetrain, 6.0, 0.75, 180.0))
                }
        fun pathRed(drivetrain: Drivetrain, elevator: Elevator, arm: Arm, wrist: Wrist, manipulator: Manipulator) =
            run {
                (drivetrain.poseEstimator.estimatedPosition)
                CloseManipulator(manipulator).andThen(MoveToPosition(drivetrain, 14.66, 1.05, 180.0).withTimeout(1.0))
                    .andThen(SetPosition.high(elevator, arm, wrist)
                        .withTimeout(3.0))
                    .andThen(SetManipulatorSpeed(manipulator, -1.0, true).deadlineWith(WaitCommand(1.0)))
                    .andThen(Idle(elevator, arm, wrist).withTimeout(0.5))
                    .andThen(MoveToPosition(drivetrain, 14.0,1.0, 180.0).withTimeout(1.0))
                    .andThen(MoveToPosition(drivetrain, 10.5, 1.0, 180.0).withTimeout(6.0))
            }
    }
}