package com.dibujaron.distanthorizon.ship

import java.io.File
import java.io.FileReader
import java.util.*
import kotlin.collections.HashMap

object ShipClassManager {
    private val shipClassMap: HashMap<String, ShipClass> = HashMap()

    init {
        File("./shipclasses/").walk()
            .filter { it.name.endsWith(".properties") }
            .map { FileReader(it) }
            .map { loadProperties(it) }
            .map { ShipClass(it) }
            .forEach {
                shipClassMap[it.qualifiedName] = it
            }
    }

    fun loadProperties(reader: FileReader): Properties {
        val props = Properties()
        props.load(reader)
        return props
    }

    fun getShipClass(qualName: String): ShipClass?
    {
        return shipClassMap[qualName]
    }

    fun getShipClasses(): Collection<ShipClass>
    {
        return shipClassMap.values
    }
}