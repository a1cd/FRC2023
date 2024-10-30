package frc.kyberlib.math.units.extensions

import frc.kyberlib.math.units.KUnit
import frc.kyberlib.math.units.Second

/**
 * KUnit representing time.
 * Should not be created directly. Use the number extensions instead.
 */
typealias Time = KUnit<Second>

val Number.seconds inline get() = Time(this.toDouble())

val Time.seconds inline get() = value
