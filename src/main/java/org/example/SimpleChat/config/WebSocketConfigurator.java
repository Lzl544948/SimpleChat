package org.example.SimpleChat.config;

import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class WebSocketConfigurator extends ServerEndpointConfig.Configurator {

    private static AutowireCapableBeanFactory factory;

    @Autowired
    public void setApplicationContext(ApplicationContext applicationContext) {
        WebSocketConfigurator.factory = applicationContext.getAutowireCapableBeanFactory();
    }

    @Override
    public <T> T getEndpointInstance(Class<T> clazz) throws InstantiationException {
        // 正确方式：每次连接都创建新实例
        return factory.createBean(clazz);
    }
    @Override
    public void modifyHandshake(ServerEndpointConfig sec,
                                HandshakeRequest request,
                                HandshakeResponse response) {
        // 读取请求参数
        Map<String, List<String>> parameterMap = request.getParameterMap();

        // 将参数存储到 UserProperties 中，以便在 @OnOpen 中使用
      //  String user = parameterMap.get("user").get(0);
        String token = parameterMap.get("token").get(0);
        String sessionId = parameterMap.get("sessionId").get(0);

      //  sec.getUserProperties().put("user", user);
        sec.getUserProperties().put("token", token);
        sec.getUserProperties().put("sessionId", sessionId);
    }

}
