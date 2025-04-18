package org.example.SimpleChat.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table(name = "chat_msg")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMsg {
  //  private Integer sessionId;
    private String msg;
    private String sender;
    private String receiver;
    private LocalDateTime time;
}
