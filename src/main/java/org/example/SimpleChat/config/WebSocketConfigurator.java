package org.example.SimpleChat.config;

import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;

import java.util.*;

public class WebSocketConfigurator extends ServerEndpointConfig.Configurator {

    @Override
    public void modifyHandshake(ServerEndpointConfig sec,
                                HandshakeRequest request,
                                HandshakeResponse response) {
        // 读取请求参数
        Map<String, List<String>> parameterMap = request.getParameterMap();

        // 将参数存储到 UserProperties 中，以便在 @OnOpen 中使用
      //  String user = parameterMap.get("user").get(0);
        String token = parameterMap.get("token").get(0);

      //  sec.getUserProperties().put("user", user);
        sec.getUserProperties().put("token", token);
    }

}
