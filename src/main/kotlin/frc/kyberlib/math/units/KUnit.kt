package frc.kyberlib.math.units

import kotlin.math.absoluteValue

/**
 * Unit system inspired by those of FalconLibrary and SaturnLibrary.
 * Allows for dimensional analysis
 * @author Trevor
 */
@JvmInline
value class KUnit<T>(val value: Double) : Comparable<KUnit<T>> {
    operator fun minus(other: KUnit<T>): KUnit<T> = KUnit(value - other.value)
    operator fun div(other: KUnit<T>) = this.value / other.value
    operator fun rem(other: KUnit<T>) = KUnit<T>(value % other.value)

    val absoluteValue inline get() = KUnit<T>(value.absoluteValue)
    override fun compareTo(other: KUnit<T>) = value.compareTo(other.value)

}

