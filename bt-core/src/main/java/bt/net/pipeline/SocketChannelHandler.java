/*
 * Copyright (c) 2016—2017 Andrei Tomashpolskiy and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package bt.net.pipeline;

import bt.net.DataReceiver;
import bt.net.Peer;
import bt.net.buffer.BorrowedBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.function.Function;

public class SocketChannelHandler implements ChannelHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SocketChannelHandler.class);

    private final Peer peer;
    private final SocketChannel channel;
    private final BorrowedBuffer<ByteBuffer> inboundBuffer;
    private final BorrowedBuffer<ByteBuffer> outboundBuffer;
    private final ChannelHandlerContext context;
    private final DataReceiver dataReceiver;

    public SocketChannelHandler(
            Peer peer,
            SocketChannel channel,
            BorrowedBuffer<ByteBuffer> inboundBuffer,
            BorrowedBuffer<ByteBuffer> outboundBuffer,
            Function<ChannelHandler, ChannelHandlerContext> contextFactory,
            DataReceiver dataReceiver) {

        this.peer = peer;
        this.channel = channel;
        this.inboundBuffer = inboundBuffer;
        this.outboundBuffer = outboundBuffer;
        this.context = contextFactory.apply(this);
        this.dataReceiver = dataReceiver;
    }

    @Override
    public Peer peer() {
        return peer;
    }

    @Override
    public void register() {
        dataReceiver.registerChannel(channel, this);
        context.fireChannelRegistered();
    }

    @Override
    public void unregister() {
        dataReceiver.unregisterChannel(channel);
        context.fireChannelUnregistered();
    }

    @Override
    public void activate() {
        dataReceiver.activateChannel(channel);
        context.fireChannelActive();
    }

    @Override
    public void deactivate() {
        dataReceiver.deactivateChannel(channel);
        context.fireChannelInactive();
    }

    @Override
    public void fireChannelReady() {
        try {
            processInboundData();
        } catch (IOException e) {
            shutdown();
            throw new RuntimeException("Unexpected I/O error", e);
        }
    }

    private void processInboundData() throws IOException {
        ByteBuffer buffer = inboundBuffer.lockAndGet();

        try {
            int readLast, readTotal = 0;
            boolean processed = false;
            while ((readLast = channel.read(buffer)) > 0) {
                processed = false;
                readTotal += readLast;
                if (!buffer.hasRemaining()) {
                    // TODO: currently this will be executed in the same thread,
                    // but still would be nice to unlock the buffer prior to firing the event,
                    // so that in future we would not need to rewrite this part of code
                    context.fireDataReceived();
                    processed = true;
                    if (!buffer.hasRemaining()) {
                        throw new IOException("Can't receive data: insufficient space in the incoming buffer");
                    }
                }
            }
            if (readTotal > 0 && !processed) {
                context.fireDataReceived();
            }
            if (readLast == -1) {
                throw new EOFException();
            }
        } finally {
            inboundBuffer.unlock();
        }
    }

    @Override
    public void tryFlush() {
        ByteBuffer buffer = outboundBuffer.lockAndGet();
        try {
            while (buffer.hasRemaining() && channel.write(buffer) > 0)
                ;
            outboundBuffer.unlock();
        } catch (IOException e) {
            outboundBuffer.unlock(); // can't use finally block due to possibility of double-unlock
            shutdown();
            throw new RuntimeException("Unexpected I/O error", e);
        }
    }

    private void shutdown() {
        try {
            unregister();
        } catch (Exception e) {
            LOGGER.error("Failed to unregister channel", e);
        }
        closeChannel();
        releaseBuffers();
    }

    private void closeChannel() {
        try {
            channel.close();
        } catch (IOException e) {
            LOGGER.error("Failed to close channel for peer: " + peer, e);
        }
    }

    private void releaseBuffers() {
        releaseBuffer(inboundBuffer);
        releaseBuffer(outboundBuffer);
    }

    private void releaseBuffer(BorrowedBuffer<ByteBuffer> buffer) {
        try {
            buffer.release();
        } catch (Exception e) {
            LOGGER.error("Failed to release buffer", e);
        }
    }
}
