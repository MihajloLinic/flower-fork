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
                println("[GRPC] onNext: Received ServerMessage: type=${'$'}{msg.msgCase}")
                sendResponse(msg)
            } catch (e: Exception) {
                println("[GRPC][ERROR] Exception while handling onNext: ${'$'}{e.message}")
                e.printStackTrace()
            }
        }

        override fun onError(t: Throwable) {
            println("[GRPC][ERROR] Stream error: ${'$'}{t::class.qualifiedName}: ${'$'}{t.message}")
            t.printStackTrace()
            finishLatch.countDown()
        }

        override fun onCompleted() {
            println("[GRPC] Stream completed by server")
            finishLatch.countDown()
        }
    })!!

    fun sendResponse(msg: ServerMessage) {
        val response = handleLegacyMessage(client, msg)
        println("[GRPC] Sending ClientMessage: type=${'$'}{response.first.msgCase}")
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
        println("[GRPC] Using plaintext (no TLS)")
        channelBuilder.usePlaintext()
    } else {
        println("[GRPC] Using TLS")
    }
    println("[GRPC] Building channel to address='${address}', maxInboundMessageSize=${HUNDRED_MEBIBYTE}")
    return withContext(Dispatchers.IO) {
        val ch = channelBuilder.build()
        println("[GRPC] Channel built: isTerminated=${ch.isTerminated}, isShutdown=${ch.isShutdown}")
        ch
    }
}

const val HUNDRED_MEBIBYTE = 100 * 1024 * 1024
