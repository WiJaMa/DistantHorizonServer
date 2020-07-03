package com.dibujaron.distanthorizon.ship.controller

import com.dibujaron.distanthorizon.navigation.NavigationRoute
import com.dibujaron.distanthorizon.orbiter.OrbiterManager
import com.dibujaron.distanthorizon.ship.*
import java.lang.IllegalStateException
import java.lang.StringBuilder
import kotlin.math.roundToInt

class AIShipController : ShipController() {

    val ONLY_MAIN_SYSTEM = true
    var nextDepartureTime = computeNextDeparture()
    var currentRoute: NavigationRoute? = null
    var fakeHoldOccupied: Int = 0

    var diagnosticBuilder = StringBuilder()
    override fun getDiagnostic(): String {
        val cr = currentRoute;
        if(cr != null){
            return "active: " + cr.getDiagnostic()
        } else {
            return "docked: " + diagnosticBuilder.toString();
        }
    }

    override fun dockedTick(delta: Double, coursePlottingAllowed: Boolean) {
        diagnosticBuilder = StringBuilder()
        val t1 = System.currentTimeMillis()
        if (System.currentTimeMillis() > nextDepartureTime && coursePlottingAllowed) {
            plotNewCourse()
            val t2 = System.currentTimeMillis()
            diagnosticBuilder.append("plotting=${t2 - t1} ")
            fakeHoldOccupied = (Math.random() * ship.holdCapacity).toInt()
            ship.undock()
            val t3 = System.currentTimeMillis()
            diagnosticBuilder.append("undocking=${t3-t2} ")
        }

    }

    fun plotNewCourse() {
        val stations = OrbiterManager.getStations().asSequence().filter {
            !ship.isDocked() || ship.dockedToPort!!.station != it
        }.filter{ !ONLY_MAIN_SYSTEM || it.getStar().name == "S-Regalis" }.toList()
        val destStation = stations.random()
        val destPort = destStation.dockingPorts.random()
        val myPort = ship.myDockingPorts.random()
        val newRoute = NavigationRoute(ship, myPort, destPort)
        currentRoute = newRoute
    }

    fun dock() {
        ship.attemptDock(2000.0, 2000.0)
        if (ship.isDocked()) {
            nextDepartureTime = computeNextDeparture()
        } else {
            println("AI ship ${ship.uuid} should have docked but failed to dock.")
            ShipManager.markForRemove(ship)
            //spawn a new one
            ShipManager.markForAdd(Ship(ShipClassManager.getShipClasses().random(), ShipColor.random(), ShipColor.random(), currentRoute!!.destination.station.getState(), AIShipController()))
        }
    }

    override fun getHoldOccupied(): Int {
        return fakeHoldOccupied
    }

    override fun computeNextState(delta: Double): ShipState {
        val route = currentRoute
        if (route != null && route.hasNext(delta)) {
            return route.next(delta)
        } else {
            if(route != null /*&& route.destination.station.name == "Stn_Innerstellar Launch"*/){
                val distToTarget = (route.getEndState().position - ship.currentState.position).length.roundToInt()
                val distToStation = (route.destination.station.globalPos() - ship.currentState.position).length.roundToInt()
                val duration = route.currentPhase.durationTicks
                val elapsedTime = route.currentPhase.ticksSinceStart
                println("route complete. target error=$distToTarget, true error=$distToStation, finalT=${route.currentPhase.previousT}, true ticks=$elapsedTime, expected ticks=$duration")
            }
            dock()
            return ship.currentState
        }
    }

    override fun getCurrentControls(): ShipInputs {
        return ShipInputs()
    }

    override fun navigatingToTarget(): Boolean {
        return !ship.isDocked()
    }

    //this is called when we send our heartbeat. It'd be a good time to recalculate.
    override fun getNavTarget(): ShipState {
        val route = currentRoute
        if(route == null){
            throw IllegalStateException("not navigating currently")
        } else {
            return route.getEndState()
        }
    }
    companion object {
        fun computeNextDeparture(): Long
        {
            return System.currentTimeMillis() + 5000 + (Math.random() * 10000).roundToInt()
        }
    }
}