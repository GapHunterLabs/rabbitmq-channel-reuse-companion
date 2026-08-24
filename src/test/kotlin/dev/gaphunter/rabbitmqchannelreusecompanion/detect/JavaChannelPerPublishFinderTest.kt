package dev.gaphunter.rabbitmqchannelreusecompanion.detect

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class JavaChannelPerPublishFinderTest : BasePlatformTestCase() {

    fun `test channel created and published on in the same method is flagged`() {
        val file = myFixture.configureByText(
            "OrderPublisher.java",
            """
            class OrderPublisher {
                void publishOrder(byte[] body) {
                    Channel channel = connection.createChannel();
                    channel.basicPublish("", "orders", null, body);
                }
            }
            """.trimIndent(),
        )
        assertEquals(1, JavaChannelPerPublishFinder.findAll(file).size)
    }

    fun `test channel created but not published on is not flagged`() {
        val file = myFixture.configureByText(
            "OrderPublisher.java",
            """
            class OrderPublisher {
                Object inspectChannel() {
                    Channel channel = connection.createChannel();
                    return channel.isOpen();
                }
            }
            """.trimIndent(),
        )
        assertTrue(JavaChannelPerPublishFinder.findAll(file).isEmpty())
    }

    fun `test channel stored as instance field is not flagged`() {
        val file = myFixture.configureByText(
            "OrderPublisher.java",
            """
            class OrderPublisher {
                private final Channel sharedChannel;

                OrderPublisher(Connection connection) throws Exception {
                    this.sharedChannel = connection.createChannel();
                }

                void publishOrder(byte[] body) throws Exception {
                    sharedChannel.basicPublish("", "orders", null, body);
                }
            }
            """.trimIndent(),
        )
        assertTrue(JavaChannelPerPublishFinder.findAll(file).isEmpty())
    }
}
