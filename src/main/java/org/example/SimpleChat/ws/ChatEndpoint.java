package org.example.SimpleChat.ws;

import com.alibaba.fastjson.JSON;
import jakarta.servlet.http.HttpSession;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import org.example.SimpleChat.config.WebSocketConfigurator;
import org.example.SimpleChat.utils.JwtUtil;
import org.example.SimpleChat.utils.MessageUtils;
import org.example.SimpleChat.ws.pojo.Message;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint(value = "/chat",configurator = WebSocketConfigurator.class)
@Component
public class ChatEndpoint {
    private static final Map<String,Session> onlineUsers=new ConcurrentHashMap<>();//thread safe map

    private String username;

    @OnOpen
    public void onOpen(Session session, EndpointConfig config) throws IOException {
        //get token from url
        String token = (String) config.getUserProperties().get("token");

        // 从 token 参数中获取用户名
        Map<String, Object> params = JwtUtil.parseJwt(token);
        this.username = (String)params.get("username");

        if (username == null || username.isEmpty()) {
            session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, "Username is required"));
            return;
        }
        onlineUsers.put(username, session);
        String msg = MessageUtils.getMessage(true,null,onlineUsers.keySet());
        boardcast(msg);
    }

    @OnMessage
    public void onMessage(String message){

        Message msg = JSON.parseObject(message, Message.class);
        String toName = msg.getToName();
        Session session = onlineUsers.get(toName);
        String msgStr = MessageUtils.getMessage(false,username,msg.getMessage());
        try{
            session.getBasicRemote().sendText(msgStr);
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    @OnClose
    public void onClose(){
        onlineUsers.remove(username);
        String msg = MessageUtils.getMessage(true,null,onlineUsers.keySet());
        boardcast(msg);
    }

    private void boardcast(String message){
        try {
            for (Session session : onlineUsers.values()) {
                session.getBasicRemote().sendText(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null) return map;
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length == 2) {
                map.put(pair[0], URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
            }
        }
        return map;
    }
}
