package org.example.SimpleChat.controller;


import org.example.SimpleChat.pojo.User;
import org.example.SimpleChat.utils.JwtUtil;
import org.example.SimpleChat.utils.Result;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("user")
public class UserController {

    @PostMapping("/login")
    public Result<User> loginController(@RequestBody Map<String, Object> params) {
        // 解析参数
        String account = (String) params.get("username");
        String password = (String) params.get("password");

        // 参数校验
        if ( account == null || password == null) {
            return Result.error("2", "请求参数错误！");
        }
        User user= new User(account,password);

        if (user != null) {
            Map<String,Object> claims = new HashMap<>();
            claims.put("type","user");
            claims.put("username",user.getUsername());
            user.setToken(JwtUtil.generateJwt(claims));
            return Result.success(user, "登录成功！");
        } else {
            return Result.error("2", "账号或密码错误！");
        }
    }

}
