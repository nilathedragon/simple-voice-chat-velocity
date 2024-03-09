
# Velocity Proxy for Simple Voice Chat

This project allows you to run multiple servers with Simple Voice Chat behind a Velocity proxy all while using only a single public UDP port! You don't need to expose every server individually anymore as the Voice will be proxied through Velocity.



## Simple Voice Chat

Simple Voice Chat was developed by https://github.com/henkelmax and is available here.

 - [Simple Voice Chat on GitHub](https://github.com/henkelmax/simple-voice-chat)

**Please note that this project is not affiliated with henkelmax and/or Simple Voice Chat.** If you have an issue with this proxy please do not bother the mod author with it.


## Installation (Proxy)

To install the Velocity Proxy for Simple Voice Chat, download the latest version from our GitHub Releases and put the file inside the plugins folder of your Velocity proxy.

The Voice Proxy will start-up on the same port and IP as your Velocity Proxy server. Make sure the port is publicly reachable for both TCP and UDP! Ask your hosting service for assistance if you don't know what this means.


## Installation (Backend Servers)

Follow the official instructions from Simple Voice Chat to install the mod/plugin on your Minecraft game server. After you have completed these steps, open the configuration file of Simple Voice Chat in the game server (usually named "voicechat-server.properties") and set the property "voice_host" to the public IP and port of your velocity server.

You also need to set the "port" property to "-1".

Example of how this could look when you are done:
```properties
...
# The port of the voice chat server
# Setting this to "-1" sets the port to the Minecraft servers port (Not recommended)
port=-1
...
# The host name that clients should use to connect to the voice chat
# This may also include a port, e.g. 'example.com:24454'
# Don't change this value if you don't know what you are doing
voice_host=123.123.123.123:25565
...
```

Thats it. You should now be able to use Simple Voice Chat on all the backend servers that have it installed.
