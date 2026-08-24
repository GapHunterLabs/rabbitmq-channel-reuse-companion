package com.acmecorp.orders;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

/**
 * Demo data for RabbitMQ Channel Reuse Companion — used with
 * `./gradlew runIde` to capture the real Marketplace screenshot. Open
 * this file, the warning icon should appear on the call inside
 * `publishOrder`.
 */
public class OrderPublisher {

    private final Channel sharedChannel;

    public OrderPublisher(Connection connection) throws Exception {
        // Built once, in the constructor -- NOT flagged.
        this.sharedChannel = connection.createChannel();
    }

    public void publishOrder(Connection connection, byte[] body) throws Exception {
        // A new channel per message -- a network round-trip on every
        // publish. FLAGGED.
        Channel channel = connection.createChannel();
        channel.basicPublish("", "orders", null, body);
    }
}
