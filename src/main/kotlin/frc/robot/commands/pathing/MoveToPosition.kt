package frc.robot.commands.pathing

import edu.wpi.first.math.MathUtil
import edu.wpi.first.math.controller.ProfiledPIDController
import edu.wpi.first.math.geometry.Pose2d
import edu.wpi.first.math.geometry.Rotation2d
import edu.wpi.first.math.geometry.Transform2d
import edu.wpi.first.math.geometry.Translation2d
import edu.wpi.first.math.kinematics.ChassisSpeeds
import edu.wpi.first.math.trajectory.TrapezoidProfile
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard
import edu.wpi.first.wpilibj2.command.Command
import edu.wpi.first.wpilibj2.command.CommandBase
import frc.robot.subsystems.Drivetrain
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.hypot
import kotlin.math.max
import frc.robot.constants.drivetrain as drivetrainConstants

val Pose2d.flipped: Pose2d
    get() = Pose2d(
        Translation2d(
            -(this.translation.x-8.3) + 8.3,
            this.translation.y
        ),
        -rotation
    )

open class MoveToPosition(
    private val drivetrain: Drivetrain,
    /**
     * The desired position of the robot (in meters)
     */
    private var pose: (
        xPID: ProfiledPIDController,
        yPID: ProfiledPIDController,
        rPID: ProfiledPIDController
    ) -> Pose2d,
    /**
     * The desired velocity of the robot (in meters per second)
     */
    private val velocity: Transform2d = Transform2d(),
    private val toleranceppos: Double = 0.02,
    private val tolerancepvel: Double = 0.1,
    private val tolerancerpos: Double = 0.01,
    private val tolerancervel: Double = 0.1,
    private val snapMode: Boolean = false
) : CommandBase() {
    constructor(drivetrain: Drivetrain, x: Double = 0.0, y: Double = 0.0, angle: Double = 0.0) : this(
        drivetrain,
        Pose2d(x, y, Rotation2d.fromDegrees(angle)),
        Transform2d(Translation2d(0.0, 0.0), Rotation2d.fromDegrees(0.0)),
    )

    constructor(
        drivetrain: Drivetrain,
        pose: Pose2d,
        velocity: Transform2d = Transform2d(),
        toleranceppos: Double = 0.075,
        tolerancepvel: Double = 0.1,
        tolerancerpos: Double = 0.01,
        tolerancervel: Double = 0.1,
        snapMode: Boolean = false
    ) : this(
        drivetrain,
        { _, _, _ -> pose },
        velocity,
        toleranceppos,
        tolerancepvel,
        tolerancerpos,
        tolerancervel,
        snapMode
    )

    init {
        addRequirements(drivetrain)
    }

    // these entries are used to debug how fast the robot wants to move to get
    // to the desired position
//    val speedx = drivetrain.Idrc.add("speedx1${Random.nextDouble()}", 0.0)
//        .entry
//    val speedy = drivetrain.Idrc.add("speedy1${Random.nextDouble()}", 0.0)
//        .entry
//    val speedr = drivetrain.Idrc.add("speedr1${Random.nextDouble()}", 0.0)
//        .entry

    val xPIDController = ProfiledPIDController(
        Companion.xP, 0.0, 0.05, TrapezoidProfile.Constraints(
            7.0,
            max(10.0, drivetrainConstants.maxAcceleration)
        )
    ).also {
        it.reset(drivetrain.estimatedPose2d.translation.x, 0.0)
        it.setTolerance(toleranceppos, tolerancepvel)
    }
    val yPIDController = ProfiledPIDController(
        Companion.yP, 0.0, 0.05, TrapezoidProfile.Constraints(
            7.0,
            max(10.0, drivetrainConstants.maxAcceleration)
        )
    ).also {
        it.reset(drivetrain.estimatedPose2d.translation.y, 0.0)
        it.setTolerance(toleranceppos, tolerancepvel)
    }
    val rPIDController = ProfiledPIDController(
        Companion.rP, 0.0, 0.0, TrapezoidProfile.Constraints(
            PI / 1.0, max(PI * 2, drivetrainConstants.maxAngularAcceleration)
        )
    ).also {
        it.enableContinuousInput(-PI, PI)
        it.reset(
            drivetrain.estimatedPose2d.rotation.radians,
            0.0
        )
        it.setTolerance(tolerancerpos, tolerancervel)
    }

    val start = drivetrain.estimatedPose2d

    val visualization = drivetrain.field2d.getObject("MoveToPosition")
    override fun initialize() {
        if (snapMode) pose = { _, _, _ ->
            SnapToPostion.closestPose(drivetrain)
        }
        xPIDController.reset(drivetrain.estimatedPose2d.translation.x, drivetrain.estimatedVelocity.translation.x)
        yPIDController.reset(drivetrain.estimatedPose2d.translation.y, drivetrain.estimatedVelocity.translation.y)
        rPIDController.reset(drivetrain.estimatedPose2d.rotation.radians, drivetrain.estimatedVelocity.rotation.radians)

        visualization.pose = pose(xPIDController, yPIDController, rPIDController)
    }

    // on command start and every time the command is executed, calculate the

    override fun execute() {
        if (!drivetrain.canTrustPose) return initialize()
        val current = drivetrain.estimatedPose2d
        val desired = pose(xPIDController, yPIDController, rPIDController)

        visualization.pose = desired

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
//        speedx.setDouble(speeds.vxMetersPerSecond)
//        speedy.setDouble(speeds.vyMetersPerSecond)
//        speedr.setDouble(speeds.omegaRadiansPerSecond)


        // tell the drivetrain to drive at the calculated speeds
        drivetrain.drive(speeds, true)
    }

    override fun isFinished(): Boolean {
        // stop when the robot is within 0.1 meters of the desired position
        return xPIDController.atGoal() && yPIDController.atGoal() && rPIDController.atGoal()
        //return drivetrain.estimatedPose2d.minus(Pose2d(pose().translation, Rotation2d())).translation.norm < toleranceppos
        //       && rPIDController.atGoal()
    }

    override fun end(interrupted: Boolean) {
        drivetrain.drive(ChassisSpeeds(0.0, 0.0, 0.0), true)
    }

    val flipped: MoveToPosition
        get() = MoveToPosition(
            drivetrain,
            { x, y, r -> pose(x, y, r).flipped },
            velocity,
            toleranceppos,
            tolerancepvel,
            tolerancerpos,
            tolerancervel,
            snapMode
        )
    companion object {
        const val rP = 8.0
        const val yP = 5.0
        const val xP = 5.0
//        /**
//         * Auto 1: Only places game piece
//         * Use if swerve broken
//         */
//        fun swerveBrokenAuto(drivetrain: Drivetrain, elevator: Elevator, arm: Arm, manipulator: Manipulator) =
//            run {
//            //(drivetrain.poseEstimator.estimatedPosition)
//                SetPosition.high(elevator, arm).withTimeout(1.0)
//                    .andThen(SetManipulatorSpeed(manipulator, -1.0).withTimeout(1.0))
//                    .andThen(Idle(elevator, arm).alongWith(SetManipulatorSpeed(manipulator, 0.0)))
//                    .withTimeout(15.0)
//            }
//
//        /**
//         * Auto 4: Place, pick up, and then get on charge station
//         */
//        fun blueauto2(drivetrain: Drivetrain, elevator: Elevator, arm: Arm, manipulator: Manipulator) =
//            run {
//                (drivetrain.poseEstimator.estimatedPosition)
//                MoveToPosition(drivetrain,2.0, 1.05, 180.0)
//                    .alongWith(SetPosition.high(elevator, arm).withTimeout(1.0))
//                    .andThen(SetManipulatorSpeed(manipulator, -1.0).withTimeout(1.0))
//                    .andThen(MoveToPosition(drivetrain, 0.0, 2.0 , 180.0))
//            }
//
//        /**
//         * auto3
//         * Score cube high
//         * Go get cone (or switch for cube)
//         * Go score cone high (or switch with cube score mid)
//         * Get as close to human player as possible
//         */
//        fun blueauto3(drivetrain: Drivetrain, elevator: Elevator, arm: Arm, manipulator: Manipulator) =
//            run {
//                (drivetrain.estimatedPose2d)
//                MoveToPosition(drivetrain, 1.87 + .5, 4.42, 0.0)
//                    .deadlineWith(SetPosition.idle(elevator, arm))
//                    .andThen(SetPosition.high(elevator, arm, true).withTimeout(3.0))
//                    .andThen(
//                        SetManipulatorSpeed(manipulator, -1.0)
//                            .withTimeout(0.25)
//                    )// place cube
//                    .andThen(
//                        // move to intake new cube, but dont rotate drivetrain
//                        // until we are close to the cube to prevent tipping,
//                        // damage to the arm, and collisions with the wall
//                        MoveToPosition(
//                            drivetrain,
//                            {
//                                Pose2d(
//                                    5.25,
//                                    if (true) 1.5 else 2.5,
//                                    Rotation2d(
//                                        if (drivetrain.estimatedPose2d.x > 3.5 || arm.armPosition.absoluteValue < 0.5) PI
//                                        else 0.0
//                                    )
//                                )
//                            },
//                        )
//                            .alongWith(
//                                SetPosition.idle(elevator, arm)
//                                    .alongWith(SetManipulatorSpeed(manipulator, 0.0)).withTimeout(3.0)
//                                    .until {
//                                        ((drivetrain.estimatedPose2d.rotation.radians.absoluteValue - PI).absoluteValue) < 1.0
//                                    }
//                                    .andThen(
//                                        WaitUntilCommand {
//                                            ((drivetrain.estimatedPose2d.rotation.radians.absoluteValue - PI).absoluteValue) < 1.0
//                                        }
//                                            .andThen(
//                                                IntakePositionForward(elevator, arm, true)
//                                                    .withTimeout(3.0)
//                                            )
//                                    )
//                            )
//                            // start moving to intake the cube and keep arm
//                            // deployed until moving is done
//                            .andThen(
//                                MoveToPosition(
//                                    drivetrain, 6.0, 4.625, 180.0
//                                )
//                                    .deadlineWith(
//                                        IntakePositionForward(elevator, arm)
//                                            .alongWith(SetManipulatorSpeed(manipulator, 1.0))
//                                    )
//                            )
//                    )
//                    // start idling and moving back to the placement zone at the
//                    // same time but dont rotate drivetrain until the arm is
//                    // back in the idle position
//                    .andThen(
//                        SetManipulatorSpeed(manipulator, 0.1)
//                            .alongWith(SetPosition.idle(elevator, arm))
//                            .withTimeout(3.0)
//                            .deadlineWith(
//                                MoveToPosition(drivetrain, {
//                                    Pose2d(
//                                        2.7,
//                                        4.6,//if (drivetrain.estimatedPose2d.run {this.x > 5.0}) 4.6 else 4.425,
//                                        if (arm.armPosition.absoluteValue > 0.5) Rotation2d(PI) else Rotation2d()
//                                    )
//                                })
//                            )
//                    )
//                    .andThen(
//                        MoveToPosition(drivetrain,
//                            {
//                                Pose2d(
//                                    2.7,
//                                    if (drivetrain.estimatedPose2d.x > 5.0) 4.6 else 4.425,
//                                    if (arm.armPosition.absoluteValue > 0.5) Rotation2d(PI) else Rotation2d()
//                                )
//                            })
//                            .deadlineWith(
//                                SetManipulatorSpeed(manipulator, 0.1)
//                                    .alongWith(SetPosition.idle(elevator, arm))
//                                    .withTimeout(3.0)
//                            )
//                    )
//                    .andThen(
//                        MoveToPosition(drivetrain, 1.89 + .5, 4.42, 0.0)
//                            .alongWith(
//                                SetPosition.high(elevator, arm).withTimeout(3.0)
//                                    .alongWith(
//                                        SetManipulatorSpeed(manipulator, 0.1).withTimeout(0.5)
//                                    )
//                            )
//                    )
//                    .andThen(SetManipulatorSpeed(manipulator, 1.0).withTimeout(0.5))
//                    .andThen(
//                        MoveToPosition(
//                            drivetrain,
//                            Pose2d(Translation2d(4.48, 5.00), Rotation2d()),
//                            velocity = Transform2d(Translation2d(1.0, 1.0), Rotation2d())
//                        )
//                            .alongWith(
//                                SetManipulatorSpeed(manipulator, 0.0).withTimeout(0.1)
//                            )
//                            .deadlineWith(
//                                SetPosition.idle(elevator, arm).withTimeout(3.0)
//                            )
//                    )
//                    .andThen(
//                        MoveToPosition(drivetrain, 7.59, 6.45, 180.0)
//                            .deadlineWith(
//                                SetPosition.idle(elevator, arm).withTimeout(3.0)
//                            )
//                    )
//
//            }
//        fun blueauto7(drivetrain: Drivetrain, elevator: Elevator, arm: Arm, manipulator: Manipulator) =
//            run {
//                (drivetrain.poseEstimator.estimatedPosition)
//                MoveToPosition(drivetrain, 1.92, 4.41, 180.0).withTimeout(3.0)
//                    .alongWith(
//                       SetPosition.high(elevator, arm).withTimeout(3.0)
//                    )
//                    .andThen(SetManipulatorSpeed(manipulator, -1.0).withTimeout(0.5))
//                    .andThen(Idle(elevator, arm).alongWith(SetManipulatorSpeed(manipulator, 0.0))).withTimeout(.25)
//                    .alongWith(MoveToPosition(drivetrain, 6.58, 4.59, 0.0).withTimeout(3.0)
//                    .alongWith(
//                        IntakePositionForward(elevator, arm).alongWith(WaitCommand(0.5).andThen(SetManipulatorSpeed(manipulator, 1.0))).withTimeout(3.0)
//                        )
//                    ).andThen(
//                        MoveToPosition(drivetrain, 5.50, 2.75).withTimeout(1.5)
//                            .alongWith(
//                            SetPosition.high(elevator, arm).alongWith(SetManipulatorSpeed(manipulator, 0.0)).withTimeout(1.5)
//                            )
//                    ).andThen(
//                        MoveToPosition(drivetrain, 3.85, 2.75).withTimeout(1.5).andThen(MoveToPosition(drivetrain, 3.88, 2.75)).withTimeout(.25)
//                    )
//            }
//
//
////        fun pathRed(drivetrain: Drivetrain, elevator: Elevator, arm: Arm, manipulator: Manipulator) =
////            run {
////                (drivetrain.poseEstimator.estimatedPosition)
////                MoveToPosition(drivetrain, 14.66, 1.05, 180.0).withTimeout(1.0)
////                    .andThen(SetManipulatorSpeed(manipulator, 1.0).withTimeout(1.0))
////                    .andThen(Idle(elevator, arm).withTimeout(0.5).alongWith(SetManipulatorSpeed(manipulator, 0.0).withTimeout(0.5)))
////                    .andThen(MoveToPosition(drivetrain, 14.0,1.0, 180.0).withTimeout(1.0))
////                    .andThen(MoveToPosition(drivetrain, 10.5, 1.0, 180.0).withTimeout(6.0))
////            }
//
//        fun pathBlueAdvanced(drivetrain: Drivetrain, elevator: Elevator, arm: Arm, manipulator: Manipulator) =
//            run {
//                (drivetrain.estimatedPose2d)
//                // start
//                    // move arm into position
//                    SetPosition.high(elevator, arm)
//                        .withTimeout(3.0)
//                    //eject cube
//                    .andThen(SetManipulatorSpeed(manipulator, -1.0).withTimeout(3.0))
//                    // stop manipulator and move to idle
//                    .andThen(
//                        Idle(elevator, arm).alongWith(SetManipulatorSpeed(manipulator, 0.0))
//                            // while this is happening, wait and then begin movement to fit position 1
//                            .alongWith(WaitCommand(0.5))
//                                .andThen(MoveToPosition(drivetrain, 3.22, 0.73,45.0).withTimeout(6.0))
//                                .andThen(MoveToPosition(drivetrain, 4.8,.66, 180.0).withTimeout(6.5))
//                            ).withTimeout(12.6)
//                    // move into deploy position and deploy
//                    .andThen(
//                        MoveToPosition(drivetrain, 5.72, 0.93, 180.0)
//                            // while moving, deploy
//                            .alongWith(
//                                IntakePositionForward(elevator, arm)
//                                    .alongWith(
//                                        SetManipulatorSpeed(manipulator, 1.0)
//                                            .withTimeout(1.5)
//                                    )
//                                    // withTimeout of total deploy time
//                                    .withTimeout(1.75)
//                            )
//                            //and then move to the should have intaked position
//                            .andThen(MoveToPosition(drivetrain, 6.66, 0.93))
//                    )
//                        .andThen(
//                            Idle(elevator, arm).alongWith(SetManipulatorSpeed(manipulator, 0.1))
//                                // start moving back
//                                .alongWith(WaitCommand(0.5))
//                                .andThen(MoveToPosition(drivetrain, 4.8, .66, 0.0))
//                                .andThen(MoveToPosition(drivetrain, 1.95, 1.05, 0.0))
//                                .andThen(MoveToPosition(drivetrain, 1.87, 1.05, 0.0))
//                        ).withTimeout(15.0)
//                        //place
//                        .andThen(SetPosition.high(elevator, arm).withTimeout(1.0))
//                    .andThen(SetManipulatorSpeed(manipulator, -1.0).withTimeout(1.0))
//            }
//
//        fun pathRedAdvanced(drivetrain: Drivetrain, elevator: Elevator, arm: Arm, manipulator: Manipulator) =
//            run {
//                (drivetrain.poseEstimator.estimatedPosition)
//                // start
//                    // move arm into position
//                    SetPosition.high(elevator, arm)
//                        .withTimeout(3.0)
//                    //eject cube
//                    .andThen(SetManipulatorSpeed(manipulator, -1.0).withTimeout(1.0))
//                    // stop manipulator and move to idle
//                    .andThen(
//                        Idle(elevator, arm).alongWith(SetManipulatorSpeed(manipulator, 0.0))
//                            // while this is happening, wait and then begin movement to fit position 1
//                            .alongWith(WaitCommand(0.5)
//                                .andThen(MoveToPosition(drivetrain, flipped(3.22), 0.73,45.0).withTimeout(1.5))
//                                .andThen(MoveToPosition(drivetrain, flipped(4.8),.66, 180.0).withTimeout(1.5))
//                            ).withTimeout(3.6)
//                    )
//                    // move into deploy position and deploy
//                    .andThen(
//                        MoveToPosition(drivetrain, flipped(5.72), 0.94, 180.0).withTimeout(1.0)
//                            // while moving, deploy
//                            .alongWith(
//                                IntakePositionForward(elevator, arm)
//                                    .alongWith(
//                                        SetManipulatorSpeed(manipulator, 1.0)
//                                        .withTimeout(1.5)
//                                    )
//                            )
//                            // withtimeout of total depoy time
//                            .withTimeout(1.75)
//                            //and then move to the should have intaked position
//                            .andThen(MoveToPosition(drivetrain, flipped(6.66),0.93).withTimeout(2.0))
//                    )
//                    .andThen(
//                        Idle(elevator, arm).alongWith(SetManipulatorSpeed(manipulator, 0.1).withTimeout(5.0))
//                            // start moving back
//                            .alongWith(WaitCommand(0.5)
//                                .andThen(MoveToPosition(drivetrain, flipped(4.8), .66,0.0).withTimeout(1.75))
//                                .andThen(MoveToPosition(drivetrain, flipped(1.95), 1.05, 0.0).withTimeout(3.5))
//                                .andThen(MoveToPosition(drivetrain, flipped(1.87),1.05, 0.0).withTimeout(1.0))
//                            ).withTimeout(6.0)
//                    )
//                    //place
//                    .andThen(SetPosition.high(elevator, arm)
//                        .withTimeout(3.0))
//                    .andThen(SetManipulatorSpeed(manipulator, -1.0).withTimeout(1.0))
//                    .withTimeout(15.0)
//            }

        fun snapToYValue(
            drivetrain: Drivetrain,
            y: () -> Double,
            yTolerance: Double = 0.1,
            r: () -> Rotation2d = { drivetrain.estimatedPose2d.rotation },
            rTolerance: Double = 0.1,
        ) =
            run {
                (drivetrain.estimatedPose2d)
                MoveToPosition(
                    drivetrain,
                    { _, _, _ ->
                        Pose2d(
                            drivetrain.estimatedPose2d.translation.x,
                            y(),
                            r()
                        )
                    },
                    toleranceppos = yTolerance,
                    tolerancerpos = rTolerance
                ).withTimeout(3.5)
            }

        /**
         * @param rotValues The angle in radians
         */
        fun snapToScoring(drivetrain: Drivetrain, yValues: () -> Iterable<Double>, rotValues: () -> Iterable<Double>): Command =
            snapToYValue(
                drivetrain,
                {yValues().minByOrNull { value ->
                    (value - drivetrain.estimatedPose2d.y).absoluteValue
                }?: drivetrain.estimatedPose2d.y},
                yTolerance = 0.05,
                {
                    Rotation2d.fromRadians(
                        rotValues().minByOrNull { value ->
                            (
                                    MathUtil.angleModulus(value)
                                            -
                                            MathUtil.angleModulus(
                                                drivetrain.estimatedPose2d.rotation.radians
                                            )
                                    ).absoluteValue
                        }?: drivetrain.estimatedPose2d.rotation.radians
                    )
                },
                rTolerance = 0.15
            )

    }
}
//this function should be used when copying and pasting an auto function, and you need to flip the x-coordinates.
fun flipped(x: Double): Double {
    val newX: Double
    if(x<8.3)
        newX=8.3+(8.3-x)
    else
        newX=8.3-(x-8.3)

    return newX
}