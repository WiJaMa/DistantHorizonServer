package com.dibujaron.distanthorizon.docking

import com.dibujaron.distanthorizon.Vector2
import com.dibujaron.distanthorizon.orbiter.Station

class StationDockingPort(val station: Station, val relativePos: Vector2): DockingPort
{
    override fun globalPosition(): Vector2 {
        return station.globalPos() + relativePos
    }
    override fun getVelocity(): Vector2 {
        return station.velocity()
    }
}