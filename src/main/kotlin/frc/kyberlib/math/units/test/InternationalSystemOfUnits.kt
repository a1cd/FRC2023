package frc.kyberlib.math.units.test

/*
 * Copyright 2019 Kunal Sheth
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

import java.io.Serializable

sealed class OperationProof
object times : OperationProof()
object div : OperationProof()

//@Deprecated(
//    message = "Explicit boxing should only be used to circumnavigate compiler bugs",
//    replaceWith = ReplaceWith("a newer version of Kotlin")
//)
//fun <Q : Quan<Q>> box(x: Q) = x as Quan<Q>


data class Dimension(
    val name: String = "Unitless",
    val L: Int = 0,
    val A: Int = 0,
    val M: Int = 0,
    val T: Int = 0,
    val I: Int = 0,
    val Theta: Int = 0,
    val N: Int = 0,
    val J: Int = 0
) : Serializable {

    init {
        if (name != "Unitless") {
            InternationalSystemOfUnits.quantities.add(Quantity(name, this))
            InternationalSystemOfUnits.units.add(UnitOfMeasure(unitType[name]!!, 1.0, this))
        }
    }


    operator fun div(other: Dimension): Dimension {
        return Dimension(
            L = L - other.L,
            A = A - other.A,
            M = M - other.M,
            T = T - other.T,
            I = I - other.I,
            Theta = Theta - other.Theta,
            N = N - other.N,
            J = J - other.J
        )
    }

    operator fun times(other: Dimension): Dimension {
        return Dimension(
            L = L + other.L,
            A = A + other.A,
            M = M + other.M,
            T = T + other.T,
            I = I + other.I,
            Theta = Theta + other.Theta,
            N = N + other.N,
            J = J + other.J
        )
    }
}


data class Quantity(val name: String, val dimension: Dimension) : Serializable {
    override fun toString() = "`$name`"
}

data class UnitOfMeasure(val name: String, val factorToSI: Double, val dimension: Dimension) : Serializable {
    override fun toString() = "`$name`"
}

object InternationalSystemOfUnits {

    val quantities = mutableSetOf<Quantity>()

    val units = mutableSetOf<UnitOfMeasure>()
}

val unitType = mutableMapOf(
    "Metre" to "Length",
    "Kilogram" to "Mass",
    "Second" to "Time",
    "Ampere" to "ElectricCurrent",
    "Kelvin" to "Temperature",
    "Mole" to "AmountOfSubstance",
    "Candela" to "LuminousIntensity",
    "Hertz" to "Frequency",
    "Radian" to "Angle",
    "Steradian" to "SolidAngle",
    "Newton" to "Force",
    "Pascal" to "Pressure",
    "Pascal" to "Stress",
    "Joule" to "Energy",
    "Joule" to "Work",
    "Joule" to "Heat",
    "Watt" to "Power",
    "Watt" to "RadiantFlux",
    "Coulomb" to "ElectricCharge",
    "Volt" to "ElectricalPotential",
    "Farad" to "ElectricalCapacitance",
    "Ohm" to "ElectricalResistance",
    "Siemens" to "ElectricalConductance",
    "Weber" to "MagneticFlux",
    "Tesla" to "MagneticFieldStrength",
    "Tesla" to "MagneticFluxDensity",
    "Henry" to "ElectricalInductance",
    "Lumen" to "LuminousFlux",
    "Lux" to "Illuminance",
    "Becquerel" to "Radioactivity",
    "Gray" to "AbsorbedDose",
    "Sievert" to "EquivalentDose",
    "Katal" to "CatalyticActivity"
).also { it.forEach { (k, v) -> it[v] = k } }.toMap()

