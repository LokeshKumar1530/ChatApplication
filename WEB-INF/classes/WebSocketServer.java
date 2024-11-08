import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

@ServerEndpoint(value = "/websocketendpoint")
public class WebSocketServer {

    
    private static Map<String, Set<Session>> chatRooms = new ConcurrentHashMap<String, Set<Session>>();

    @OnOpen
    public void onOpen(Session session) {
        System.out.println("Connection opened : " + session.getId());
      }
     
     @OnMessage
    public void onMessage(String message, Session session) throws IOException {

      System.out.println("Message from : " + session.getId() + ": " + message); 

      System.out.println("-----------------------------------");
      System.out.println(session.getUserProperties());
      System.out.println("______________________________________");
        JSONParser parser = new JSONParser();
        try {
            JSONObject data = (JSONObject) parser.parse(message);
            String type = (String) data.get("type");
            String code = (String) data.get("code");

            if ("create".equals(type) || "join".equals(type)) {
                session.getUserProperties().put("chatRoomCode", code);
                chatRooms.computeIfAbsent(code, k -> ConcurrentHashMap.newKeySet()).add(session);
                JSONObject response = new JSONObject();
                response.put("type", "system");
                response.put("message", "Connected to chat room: " + code);
                session.getBasicRemote().sendText(response.toJSONString());
            } else if ("chat".equals(type)) {
                String chatRoomCode = (String) session.getUserProperties().get("chatRoomCode");
                if (chatRoomCode != null) {
                    Set<Session> chatRoomSessions = chatRooms.get(chatRoomCode);
                    if (chatRoomSessions != null) {
                        String chatMessage = (String) data.get("message");
                       
                        JSONObject chatResponse = new JSONObject();
                        chatResponse.put("type", "chat");
                        chatResponse.put("message", chatMessage);
                        for (Session client : chatRoomSessions) {
                            chatResponse.put("isSelf", client.equals(session));
                            client.getBasicRemote().sendText(chatResponse.toJSONString());
                        }
                    }
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @OnClose 
    public void onClose(Session session) {
        String chatRoomCode = (String) session.getUserProperties().get("chatRoomCode");
        if (chatRoomCode != null) {
            Set<Session> chatRoomSessions = chatRooms.get(chatRoomCode);
            if (chatRoomSessions != null) {
                chatRoomSessions.remove(session);
                if (chatRoomSessions.isEmpty()) {
                    chatRooms.remove(chatRoomCode);
                }
            }
        }
    }

   @OnError
    public void onError(Session session, Throwable error) {
        error.printStackTrace();
        onClose(session);
    }
}
