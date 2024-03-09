package ch.bad_dragon.simplevoicechatvelocity.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;

/**
 * The PlayerVoiceBridge implements a single proxy connection from a velocity-connected player
 * to one of the velocity registered backend servers. The bridge lives for the duration of the
 * connection between the velocity player and the specific backend server.
 */
public class PlayerVoiceBridge extends Thread {
    /**
     * Dedicated logger for PlayerVoiceBridge
     */
    private static final Logger LOGGER = LoggerFactory.getLogger("PlayerVoiceBridge");

    /**
     * The connection between the Velocity proxy, acting as a player, to the backend server's UDP server
     */
    private final DatagramSocket backendServerSocket = new DatagramSocket();

    /**
     * The connection between the minecraft clients and the Velocity UDP proxy.
     */
    private final DatagramSocket publicSocket;

    /**
     * The SocketAddress used by the player to connect to the Velocity UDP proxy.
     */
    private final SocketAddress playerAddress;

    /**
     * The SocketAddress used by the velocity proxy to write to the backend server's UDP server.
     */
    private final SocketAddress serverAddress;

    public PlayerVoiceBridge(DatagramSocket socket, SocketAddress playerAddress, SocketAddress serverAddress) throws SocketException {
        this.publicSocket = socket;
        this.playerAddress = playerAddress;
        this.serverAddress = serverAddress;
    }

    /**
     * Notify the bridge to exit the forwarding loop and close
     * the UDP socket to the backend server.
     */
    @Override
    public void interrupt() {
        LOGGER.debug("Bridge received interrupt and is being shut down");
        super.interrupt();
    }

    @Override
    public void run() {
        try {
            while(!this.isInterrupted()) {
                DatagramPacket packet = new DatagramPacket(new byte[4096], 4096);
                this.backendServerSocket.receive(packet);
                this.publicSocket.send(new DatagramPacket(packet.getData(), packet.getLength(), this.playerAddress));
            }
        } catch (Exception e) {
            LOGGER.error("Failed to bridge packet from backend server to player", e);
        }
        this.backendServerSocket.close();
        LOGGER.debug("Bridge closed socket to backend service, exiting...");
    }

    /**
     * Relay a DatagramPacket to the backend UDP server
     * @param packet The DatagramPacket to relay to the backend server
     */
    public void relay(DatagramPacket packet) throws IOException {
        this.backendServerSocket.send(new DatagramPacket(packet.getData(), packet.getLength(), this.serverAddress));
    }

}
