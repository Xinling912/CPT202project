package com.cpt202.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Autowired
    private ApplicationContext context;

    @PostMapping("/verify-code")
    public Map<String, String> verifyCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        Map<String, String> response = new HashMap<>();

        // 尝试获取邮件发送器，如果 @Autowired 没拿到，我们从上下文里再试一次
        JavaMailSender activeSender = (mailSender != null) ? mailSender :
                (context.containsBean("javaMailSender") ? context.getBean(JavaMailSender.class) : null);

        if (activeSender == null) {
            response.put("message", "发送失败：邮件服务未就绪，请检查 application.properties 是否填写了正确的 mail 配置");
            return response;
        }

        try {
            String code = String.valueOf((int)((Math.random() * 9 + 1) * 100000)); // 生成 6 位随机码
            SimpleMailMessage message = new SimpleMailMessage();

            // 注意：这里必须和你的 application.properties 里的 username 一致
            message.setFrom("597918354@qq.com");
            message.setTo(email);
            message.setSubject("Group 13 - 验证码集成测试");
            message.setText("少辉你好，你的 6 位验证码是：" + code + "。这封邮件证明了 Notification 模块集成成功！");

            activeSender.send(message);
            response.put("message", "验证码已发送到邮箱");

        } catch (Exception e) {
            response.put("message", "发送异常: " + e.getMessage());
            e.printStackTrace();
        }
        return response;
    }
}