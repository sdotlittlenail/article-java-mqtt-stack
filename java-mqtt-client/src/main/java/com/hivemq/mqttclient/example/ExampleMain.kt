package com.hivemq.mqttclient.example

import com.hivemq.mqttclient.example.climate.ClimateBackend
import com.hivemq.mqttclient.example.common.Utils
import org.pmw.tinylog.Configurator
import org.pmw.tinylog.Level
import org.pmw.tinylog.Logger
import org.pmw.tinylog.writers.ConsoleWriter

object ExampleMain {

    @JvmStatic
    fun main(args: Array<String>) {
        Configurator.defaultConfig()
                .writer(ConsoleWriter())
                .formatPattern("Backend Client: {message}")
                .level(Level.INFO)
                .activate()
        Logger.info("START CLIMATE CONTROLLER BACKEND")

        //Backend Service
        ClimateBackend().startClimateControllerBackend()

        //do not exit
        Utils.idle(3)
    }
}