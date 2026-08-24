package dev.gaphunter.rabbitmqchannelreusecompanion.detect

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class KotlinChannelPerPublishFinderTest : BasePlatformTestCase() {

    fun `test channel created and published on in the same method is flagged`() {
        val file = myFixture.configureByText(
            "OrderPublisher.kt",
            """
            class OrderPublisher {
                fun publishOrder(body: ByteArray) {
                    val channel = connection.createChannel()
                    channel.basicPublish("", "orders", null, body)
                }
            }
            """.trimIndent(),
        )
        assertEquals(1, KotlinChannelPerPublishFinder.findAll(file).size)
    }

    fun `test channel created but not published on is not flagged`() {
        val file = myFixture.configureByText(
            "OrderPublisher.kt",
            """
            class OrderPublisher {
                fun inspectChannel(): Any {
                    val channel = connection.createChannel()
                    return channel.isOpen
                }
            }
            """.trimIndent(),
        )
        assertTrue(KotlinChannelPerPublishFinder.findAll(file).isEmpty())
    }

    fun `test channel stored as instance property is not flagged`() {
        val file = myFixture.configureByText(
            "OrderPublisher.kt",
            """
            class OrderPublisher(connection: Connection) {
                private val sharedChannel = connection.createChannel()

                fun publishOrder(body: ByteArray) {
                    sharedChannel.basicPublish("", "orders", null, body)
                }
            }
            """.trimIndent(),
        )
        assertTrue(KotlinChannelPerPublishFinder.findAll(file).isEmpty())
    }
}
