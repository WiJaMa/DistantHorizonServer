package com.dibujaron.distanthorizon.ship

import com.dibujaron.distanthorizon.Vector2
import com.dibujaron.distanthorizon.commodity.CommodityType
import com.dibujaron.distanthorizon.docking.DockingPort
import com.dibujaron.distanthorizon.docking.ShipClassDockingPort
import com.dibujaron.distanthorizon.docking.ShipDockingPort
import com.dibujaron.distanthorizon.docking.StationDockingPort
import com.dibujaron.distanthorizon.orbiter.OrbiterManager
import org.json.JSONObject
import java.lang.Math.pow
import java.util.*
import kotlin.math.pow

class Ship(
    val type: ShipClass,
    var velocity: Vector2,
    var globalPos: Vector2,
    var rotation: Double
) {
    val gravityConstant = 6.67408 * 10.0.pow(-11.0)
    val uuid = UUID.randomUUID()
    //controls
    var mainEnginesActive: Boolean = false
    var portThrustersActive: Boolean = false
    var stbdThrustersActive: Boolean = false
    var foreThrustersActive: Boolean = false
    var aftThrustersActive: Boolean = false
    var rotatingLeft: Boolean = false
    var rotatingRight: Boolean = false

    val myDockingPorts = type.dockingPorts.asSequence().map{ShipDockingPort(this, it)}.toList()
    var dockedToPort: StationDockingPort? = null
    var myDockedPort: ShipDockingPort? = null

    var holdCapacity = type.holdSize
    var hold = HashMap<String, Int>()

    fun holdOccupied(): Int
    {
        return hold.values.asSequence().sum()
    }

    fun createHoldStatusMessage(): JSONObject
    {
        val retval = JSONObject()
        CommodityType.values().asSequence()
            .map{Pair(it.identifyingName, hold[it.identifyingName]?:0)}
            .forEach { retval.put(it.first, it.second)}
        return retval
    }
    fun process(delta: Double) {
        val dockedTo = dockedToPort
        val dockedFrom = myDockedPort
        if(dockedTo != null && dockedFrom != null){
            velocity = dockedTo.getVelocity()
            val myPortRelative = dockedFrom.relativePosition()
            rotation = dockedTo.globalRotation() + dockedFrom.relativeRotation()
            globalPos = dockedTo.globalPosition() + (myPortRelative * -1.0).rotated(rotation)
        }
        else {
            if (mainEnginesActive) {
                velocity += Vector2(0, -type.mainThrust).rotated(rotation) * delta
            }
            if (stbdThrustersActive) {
                velocity += Vector2(-type.manuThrust, 0).rotated(rotation) * delta
            }
            if (portThrustersActive) {
                velocity += Vector2(type.manuThrust, 0).rotated(rotation) * delta
            }
            if (foreThrustersActive) {
                velocity += Vector2(0, type.manuThrust).rotated(rotation) * delta
            }
            if (aftThrustersActive) {
                velocity += Vector2(0, -type.manuThrust).rotated(rotation) * delta
            }
            if (rotatingLeft) {
                rotation -= type.rotationPower * delta
            }
            if (rotatingRight) {
                rotation += type.rotationPower * delta
            }
            velocity += gravityAccelAtTime(0.0, globalPos)
            globalPos += velocity * delta
            //velocity += gravityAccelAtTime(delta, globalPos)
        }
    }

    fun toJSON(): JSONObject
    {
        val retval = JSONObject()
        retval.put("id", uuid)
        retval.put("type", type.qualifiedName)
        retval.put("velocity", velocity.toJSON())
        retval.put("global_pos", globalPos.toJSON())
        retval.put("rotation", rotation)
        retval.put("main_engines", mainEnginesActive)
        retval.put("port_thrusters", portThrustersActive)
        retval.put("stbd_thrusters", stbdThrustersActive)
        retval.put("fore_thrusters", foreThrustersActive)
        retval.put("aft_thrusters", aftThrustersActive)
        retval.put("rotating_left", rotatingLeft)
        retval.put("rotating_right", rotatingRight)
        return retval
    }

    fun gravityAccelAtTime(timeOffset: Double, globalPosAtTime: Vector2): Vector2 {
        var accel = Vector2(0, 0)
        OrbiterManager.getPlanets().asSequence()
            .map {
                val planetPosAtTime = it.globalPosAtTime(timeOffset)
                val offset = (planetPosAtTime - globalPosAtTime)
                var rSquared = offset.lengthSquared
                if (rSquared < it.minRadiusSquared) {
                    rSquared = it.minRadiusSquared.toDouble()
                }
                val forceMag = gravityConstant * it.mass / rSquared
                offset.normalized() * forceMag
            }
            .forEach { accel += it }
        return accel
    }

    fun dockOrUndock(){
        if(dockedToPort == null){
            attemptDock()
        } else {
            dockedToPort = null;
            myDockedPort = null;
        }
    }
    fun attemptDock(){
        println("attempting to dock.");
        val maxDockDist = 50.0//10000.0//50.0
        val maxDistSquared = maxDockDist.pow(2)

        val maxClosingSpeed = 500.0//5000.0
        val maxClosingSpeedSquared = maxClosingSpeed.pow(2)

        val match = OrbiterManager.getStations().asSequence()
            .flatMap{it.dockingPorts.asSequence()}
            .flatMap{stationPort -> myDockingPorts.asSequence()
                .map{shipPort -> Pair(shipPort, stationPort)}}
            .filter{(it.first.getVelocity() - it.second.getVelocity()).lengthSquared < maxClosingSpeedSquared}
            .map{Triple(it.first, it.second, (it.first.globalPosition() - it.second.globalPosition()).lengthSquared)}
            .filter{it.third < maxDistSquared}
            .minBy{it.third}
        if(match != null){
            val bestShipPort = match.first
            val bestStationPort = match.second
            this.myDockedPort = bestShipPort
            this.dockedToPort = bestStationPort
            println("docked to ${bestStationPort.station.name}");
        }
    }
}