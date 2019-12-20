package com.hivemq.mqttclient.example.common

import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import org.pmw.tinylog.Logger

object Utils {

    const val BROKER_HIVEMQ_ADR = "localhost"
    const val BROKER_HIVEMQ_PORT = 1883

    private fun sleep(seconds: Int) = try {
        Thread.sleep(1000 * seconds.toLong())
    } catch (e: InterruptedException) {
        Logger.error(e.message)
    }

    fun idle(sleepInterval: Int) {
        while (true) {
            sleep(sleepInterval)
            print(".")
        }
    }

    fun disconnectOnExit(client: Mqtt5AsyncClient?) {
        client?.let {
            Logger.info("Disconnect Client " + client.config.clientIdentifier.get())
            client.disconnect()
        }
    }

    @JvmStatic
    fun addDisconnectOnRuntimeShutDownHock(client: Mqtt5AsyncClient?) = Runtime
            .getRuntime()
            .addShutdownHook(object : Thread() {
                override fun run() = disconnectOnExit(client)
            })
}