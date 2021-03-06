package com.dibujaron.distanthorizon.script

import com.dibujaron.distanthorizon.orbiter.Station
import com.dibujaron.distanthorizon.ship.ShipInputs

interface ScriptWriter {
    fun writeAction(action: ShipInputs)
    fun completeScript(dockedStation: Station)
}