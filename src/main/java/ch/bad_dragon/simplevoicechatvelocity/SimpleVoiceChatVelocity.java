package ch.bad_dragon.simplevoicechatvelocity;

import ch.bad_dragon.simplevoicechatvelocity.network.VoiceProxyServer;
import com.google.inject.Inject;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

@Plugin(
        id = "simplevoicechat-velocity",
        name = "SimpleVoiceChatVelocity",
        version = BuildConstants.VERSION,
        authors = "NilaTheDragon",
        url = "https://github.com/nilathedragon/simple-voice-chat-velocity",
        description = "Run multiple servers with Simple Voice Chat behind a single public port"
)
public class SimpleVoiceChatVelocity {

    @Inject
    private Logger logger;
    @Inject
    private ProxyServer proxyServer;
    private VoiceProxyServer voiceProxyServer;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        this.voiceProxyServer = new VoiceProxyServer(this.proxyServer);
        this.voiceProxyServer.start();
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (this.voiceProxyServer != null) this.voiceProxyServer.interrupt();
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        if (this.voiceProxyServer == null) return;

        this.voiceProxyServer.interruptBridgeForPlayer(event.getPlayer());
        this.logger.debug("Player {} is switching servers, interrupting bridge if it exists", event.getPlayer().getUsername());
    }

    @Subscribe
    public void onServerConnected(DisconnectEvent event) {
        if (this.voiceProxyServer == null) return;

        this.voiceProxyServer.interruptBridgeForPlayer(event.getPlayer());
        this.logger.debug("Player {} is has disconnected from Velocity, interrupting bridge if it exists", event.getPlayer().getUsername());
    }
}
