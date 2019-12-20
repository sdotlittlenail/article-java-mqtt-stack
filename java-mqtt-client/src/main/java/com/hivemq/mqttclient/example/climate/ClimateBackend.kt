package com.hivemq.mqttclient.example.climate

import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.datatypes.MqttTopicFilter
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PublishResult
import com.hivemq.client.mqtt.mqtt5.message.subscribe.Mqtt5Subscription
import com.hivemq.client.mqtt.mqtt5.message.subscribe.suback.Mqtt5SubAck
import com.hivemq.mqttclient.example.common.Utils.BROKER_HIVEMQ_ADR
import com.hivemq.mqttclient.example.common.Utils.BROKER_HIVEMQ_PORT
import com.hivemq.mqttclient.example.common.Utils.addDisconnectOnRuntimeShutDownHock
import org.pmw.tinylog.Logger

class ClimateBackend {

    fun startClimateControllerBackend() {
        val client: Mqtt5BlockingClient = MqttClient.builder()
                .serverHost(BROKER_HIVEMQ_ADR)
                .serverPort(BROKER_HIVEMQ_PORT)
                .useMqttVersion5()
                .identifier(CLIMATE_CONTROLLER_BACKEND)
                .buildBlocking()
        val ack = client.connect()
        if (ack.reasonCode.isError) { //do error handling
        }
        doSubscribeToRooms(client.toAsync())
        addDisconnectOnRuntimeShutDownHock(client.toAsync())
    }

    private fun doSubscribeToRooms(client: Mqtt5AsyncClient) {
        Logger.info("Start subscribing to rooms temperature topic")
        val subscriptionTemperature = Mqtt5Subscription.builder()
                .topicFilter(TOPIC_ROOMS_TEMP)
                .qos(MqttQos.AT_LEAST_ONCE).build()
        client.subscribeWith()
                .addSubscription(subscriptionTemperature)
                .callback { publish: Mqtt5Publish ->
                    Logger.info(
                            "Message received on topic: {} with payload: {} and with QoS: {}",
                            publish.topic,
                            String(publish.payloadAsBytes),
                            publish.qos.code)
                    doPublishCommand(client, publish)
                }
                .send()
                .whenComplete { subAck: Mqtt5SubAck?, throwable: Throwable? ->
                    throwable?.let {
                        Logger.error("Subscription to topics failed. ", throwable.message)
                    } ?: Logger.info("Successful subscribed to topics: {} ", TOPIC_ROOMS_TEMP)
                }
    }

    private fun doPublishCommand(client: Mqtt5AsyncClient, publish: Mqtt5Publish) {
        val value = getIntValueFromPayload(publish)
        Logger.info("Try publish to topic: {}/command ", publish.topic)
        if (MqttTopicFilter.of(TOPIC_ROOMS_TEMP).matches(publish.topic)) {
            value?.let {
                val result = temperatureControl(value)
                result?.let {
                    publishCommand(client, result, publish.topic.toString())
                } ?: Logger.info("Temperature ok")
            }
        } else {
            Logger.warn("Topic not match to command filter: {} ", publish.topic.toString())
        }
    }

    private fun getIntValueFromPayload(publish: Mqtt5Publish): Int? {
        val buffer = if (publish.payload.isPresent) publish.payload.get() else null
        buffer?.let {
            val dst = ByteArray(buffer.limit())
            try { // deep copy necessary
                buffer[dst]
                return Integer.valueOf(String(dst))
            } catch (any: Exception) {
                Logger.error("Cannot read integer value from Payload, error: ", any.message)
            }
        }
        return null
    }

    private fun publishCommand(client: Mqtt5AsyncClient, payload: String?, topic: String) {
        payload?.let {
            val command = Mqtt5Publish.builder()
                    .topic("$topic/command")
                    .qos(MqttQos.AT_LEAST_ONCE)
                    .contentType("TEXT")
                    .payload(payload.toByteArray())
                    .build()
            client.publish(command).whenComplete { publishResult: Mqtt5PublishResult?, throwable: Throwable? ->
                throwable?.let {
                    Logger.error("Publish to topic: {} failed, ", topic, throwable.message)
                } ?: Logger.info("Publish msg '{}' to topic: {}/command", payload, topic)
            }
        }
    }

    companion object {
        private const val CLIMATE_CONTROLLER_BACKEND = "ClimateControllerBackend"
        private const val TOPIC_ROOMS_TEMP = "room/+/temperature"
        private const val DECREASE = "DOWN"
        private const val INCREASE = "UP"

        private fun temperatureControl(value: Int) = if (value < 15) INCREASE else if (value > 30) DECREASE else null
    }
}