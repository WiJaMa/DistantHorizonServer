package com.dibujaron.distanthorizon.orbiter

import com.dibujaron.distanthorizon.Vector2
import java.io.File
import java.io.FileReader
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.pow

object OrbiterManager {
    private const val MIN_GRAVITY_FORCE_CUTOFF = 0.01

    private val orbitersMap: HashMap<String, Orbiter> = HashMap()
    private val planetsMap: HashMap<String, Planet> = HashMap()
    private val stationsMap: HashMap<String, Station> = HashMap()
    private const val GRAVITY_FUDGE = 50.0
    val GRAVITY_CONSTANT = 6.67408 * 10.0.pow(-11.0) * GRAVITY_FUDGE

    fun tick() {
        getOrbiters().forEach { it.tick() }
    }

    fun getPlanet(name: String): Planet? {
        return planetsMap[name];
    }

    fun getPlanets(): Collection<Planet> {
        return planetsMap.values
    }

    fun getOrbiter(name: String): Orbiter? {
        return orbitersMap[name];
    }

    fun getOrbiters(): Collection<Orbiter> {
        return orbitersMap.values
    }

    fun getStation(name: String): Station? {
        return stationsMap[name];
    }

    fun getStations(): Collection<Station> {
        return stationsMap.values
    }

    fun calculateGravityAtTick(tickOffset: Double, globalPosAtTick: Vector2): Vector2 {
        var accel = Vector2(0, 0)
        getPlanets().asSequence()
            .forEach {
                val planetPosAtTick = it.globalPosAtTick(tickOffset)
                val offset = (planetPosAtTick - globalPosAtTick)
                var rSquared = offset.lengthSquared
                if (rSquared < it.minRadiusSquared) {
                    rSquared = it.minRadiusSquared.toDouble()
                }
                val forceMag = GRAVITY_CONSTANT * it.mass / rSquared
                if(forceMag > MIN_GRAVITY_FORCE_CUTOFF) {
                    accel += (offset.normalized() * forceMag)
                }
            }
        return accel
    }

    init {
        recursiveInitOrbiters(null, File("./world"))
        orbitersMap.values.forEach { it.initialize() }
    }

    private fun recursiveInitOrbiters(parentName: String?, folder: File){
        folder.walk()
            .filter{it.name.endsWith(".properties")}
            .forEach {
                val reader = FileReader(it)
                val props = Properties()
                props.load(reader)
                val orbiterName = it.nameWithoutExtension
                if(orbiterName.startsWith("Stn_")){
                    val stn = Station(parentName, orbiterName, props)
                    stationsMap[orbiterName] = stn
                    orbitersMap[orbiterName] = stn
                } else {
                    val planet = Planet(parentName, orbiterName, props)
                    planetsMap[orbiterName] = planet
                    orbitersMap[orbiterName] = planet
                }
                val containingFolder = it.parentFile
                val descendantFolder = File(containingFolder.path + "/" + orbiterName)
                if(descendantFolder.exists()){
                    recursiveInitOrbiters(orbiterName, descendantFolder)
                }
            }
    }
}