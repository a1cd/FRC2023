package frc.robot.subsystems

import com.ctre.phoenix.motorcontrol.can.WPI_TalonFX
import edu.wpi.first.wpilibj2.command.SubsystemBase
import edu.wpi.first.math.system.plant.DCMotor
class Intake : SubsystemBase() {
//Chang to SparkMax after the WPILib for it starts working Phoenix is a temporary patch.
var motor = WPI_TalonFX(12)




}
