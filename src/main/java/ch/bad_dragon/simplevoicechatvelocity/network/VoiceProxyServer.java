package ch.bad_dragon.simplevoicechatvelocity.network;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * The VoiceProxyServer implements the publicly facing UDP server which then proxies
 * the UDP traffic to the appropriate backend server's Simple Voice Chat UDP server.
 * <p>
 * This server lives for the duration of the runtime of the Velocity proxy.
 */
public class VoiceProxyServer extends Thread {
    /**
     * Dedicated logger for VoiceProxyServer
     */
    private static final Logger LOGGER = LoggerFactory.getLogger("VoiceProxyServer");

    /**
     * A map of all currently connected players and their respective PlayerVoiceBridge
     * Not all players connected to the Velocity proxy necessarily have a PlayerVoiceBridge.
     */
    private final Map<UUID, PlayerVoiceBridge> bridgeMap = new ConcurrentHashMap<>();

    /**
     * The asynchronous processor for the datagram queue.
     */
    private final DatagramProcessor datagramProcessor = new DatagramProcessor();

    /**
     * A queue of incoming datagrams on the public UDP socket of the Velocity proxy server.
     */
    private final BlockingQueue<DatagramPacket> datagramQueue = new LinkedBlockingQueue<>();

    /**
     * The currently active instance of the Velocity ProxyServer used for Player & Server lookup.
     */
    private final ProxyServer proxyServer;

    /**
     * The public UDP socket of the Velocity proxy server. This is where Minecraft clients will connect to.
     */
    private DatagramSocket socket;

    public VoiceProxyServer(ProxyServer proxyServer) {
        setDaemon(true);
        setName("VoiceProxyServer");

        this.proxyServer = proxyServer;
        this.datagramProcessor.start();
    }

    public void interruptBridgeForPlayer(Player player) {
        PlayerVoiceBridge bridge = this.bridgeMap.getOrDefault(player.getUniqueId(), null);
        if (bridge != null) {
            bridge.interrupt();
            LOGGER.info("Disconnected PlayerVoiceBridge for client {}", player.getUsername());
            this.bridgeMap.remove(player.getUniqueId());
        }
    }

    @Override
    public void run() {
        try {
            // Ensure we start with a fresh UDP socket, if for some reason there is already a socket, we have to ensure it's closed
            if (this.socket != null) this.socket.close();
            this.socket = new DatagramSocket(proxyServer.getBoundAddress().getPort(), proxyServer.getBoundAddress().getAddress());
            LOGGER.info("Voice chat server started at {}:{}", proxyServer.getBoundAddress().getAddress(), socket.getLocalPort());

            while (!socket.isClosed() && !this.isInterrupted()) {
                try {
                    DatagramPacket packet = new DatagramPacket(new byte[4096], 4096);
                    this.socket.receive(packet);
                    this.datagramQueue.add(packet);
                } catch (Exception e) {
                    LOGGER.debug("An exception occurred while attempting to read & queue an incoming datagram", e);
                }
            }
        } catch (Exception e) {
            LOGGER.error("The VoiceProxyServer encountered a fatal error and has been shut down", e);
        }
        this.socket.close();
        this.datagramProcessor.interrupt();
        this.bridgeMap.values().forEach(PlayerVoiceBridge::interrupt);
        this.bridgeMap.clear();
    }

    /**
     * DatagramProcessor implements the internal asynchronous datagram queue processing thread
     * which is responsible for handling incoming datagrams, figuring out which player they belong
     * to and ultimately relaying the data to the appropriate backend server.
     * <p>
     * Any invalid datagram packets will be discarded silently.
     */
    private class DatagramProcessor extends Thread {
        public DatagramProcessor() {
            setDaemon(true);
            setName("VoiceProxyServerDatagramProcessor");
        }

        @Override
        public void run() {
            while (!this.isInterrupted()) {
                try {
                    DatagramPacket packet = datagramQueue.poll(10, TimeUnit.MILLISECONDS);
                    if (packet == null) continue;

                    // The first byte in the datagram must match the magic byte, else this is not a valid SimpleVoiceChat packet
                    ByteBuffer bb = ByteBuffer.wrap(packet.getData());
                    if (bb.get() != (byte) 0b11111111) continue;

                    // The Player UUID comes right after the magic byte in the form of two longs
                    UUID playerUuid = new UUID(bb.getLong(), bb.getLong());

                    // At this point we are sure this seems to be a valid SimpleVoiceChat packet
                    // Get or establish the bridge connection between the player and the backend server
                    PlayerVoiceBridge bridge = bridgeMap.computeIfAbsent(playerUuid, uuid -> {
                        Optional<Player> player = proxyServer.getPlayer(playerUuid);
                        if (player.isEmpty()) return null;

                        Optional<ServerConnection> server = player.get().getCurrentServer();
                        if (server.isEmpty()) return null;

                        try {
                            PlayerVoiceBridge newBridge = new PlayerVoiceBridge(socket, packet.getSocketAddress(), server.get().getServerInfo().getAddress());
                            newBridge.start();
                            LOGGER.info("Established new PlayerVoiceBridge for client {}", player.get().getUsername());
                            return newBridge;
                        } catch (SocketException e) {
                            throw new RuntimeException(e);
                        }
                    });

                    if (bridge == null) continue;

                    // We found or created a bridge, we can now relay this packet
                    bridge.relay(packet);
                } catch (Exception e) {
                    LOGGER.error("An exception occurred while processing an incoming datagram, continuing loop...", e);
                }
            }
        }
    }
}
