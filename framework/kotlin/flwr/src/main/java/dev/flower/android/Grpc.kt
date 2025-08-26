package dev.flower.android

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.CountDownLatch
import flwr.proto.FlowerServiceGrpc
import flwr.proto.Transport.ServerMessage

internal class FlowerGrpc
@Throws constructor(
    channel: ManagedChannel,
    private val client: Client,
) {
    private val finishLatch = CountDownLatch(1)

    private val asyncStub = FlowerServiceGrpc.newStub(channel)!!

    private val requestObserver = asyncStub.join(object : StreamObserver<ServerMessage> {
        override fun onNext(msg: ServerMessage) {
            try {
                sendResponse(msg)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun onError(t: Throwable) {
            t.printStackTrace()
            finishLatch.countDown()
        }

        override fun onCompleted() {
            finishLatch.countDown()
        }
    })!!

    fun sendResponse(msg: ServerMessage) {
        val response = handleLegacyMessage(client, msg)
        requestObserver.onNext(response.first)
    }
}

/**
 * Start a Flower client node which connects to a Flower server.
 *
 * @param serverAddress The IPv4 or IPv6 address of the server. If the Flower server runs on the
 * same machine on port 8080, then server_address would be “[::]:8080”.
 * @param useTLS Whether to use TLS to connect to the Flower server.
 * @param client The Flower client implementation.
 */
suspend fun startClient(
    serverAddress: String,
    useTls: Boolean,
    client: Client,
) {
    FlowerGrpc(createChannel(serverAddress, useTls), client)
}

internal suspend fun createChannel(address: String, useTLS: Boolean = false): ManagedChannel {
    val channelBuilder =
        ManagedChannelBuilder.forTarget(address).maxInboundMessageSize(HUNDRED_MEBIBYTE)
    if (!useTLS) {
        channelBuilder.usePlaintext()
    }
    return withContext(Dispatchers.IO) {
        channelBuilder.build()
    }
}

const val HUNDRED_MEBIBYTE = 100 * 1024 * 1024
