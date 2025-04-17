package org.example.SimpleChat.pojo;

import lombok.Data;

import java.util.Date;


@Data
public class User {

    private Integer userId;

    private String username;
    private String password;


    private String token;

    public User() {
    }

    public User(String username, String password){
        this.username = username;
        this.password = password;
    }

    public User(Integer user_id, String username, String password, String salt, String email, String phone, Date register_date) {
        this.userId = user_id;
        this.username = username;
        this.password = password;

    }

}
