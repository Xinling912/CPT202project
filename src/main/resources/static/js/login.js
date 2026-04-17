$(document).ready(function() {
    $('#loginForm').on('submit', function(e) {
        e.preventDefault(); 
        
        const btn = $('#loginBtn');
        const inputAccount = $('#log-email').val(); // 现在这个框既能填邮箱，也能填用户名
        const inputPwd = $('#log-password').val();
        
        btn.text('Verifying...')
           .prop('disabled', true)
           .addClass('opacity-70 cursor-not-allowed');

        // 严格按照杜姐的 LoginRequest 要求来写字段名
        const loginData = {
            usernameOrEmail: inputAccount, 
            password: inputPwd
        };

        // 呼叫杜姐的新版登录接口
        fetch('http://localhost:8080/api/users/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(loginData)
        })
        .then(async response => {
            const data = await response.json(); // 现在后端返回的是规范的 JSON 啦！

            if (response.ok) {
                // ==========================================
                // 🌟 核心高光时刻：拿到并戴上 Token 手环！
                // ==========================================
                // 假设杜姐的返回值里带有 token 字段 (如果没有，你得提醒杜姐在返回值里加上 token 字段)
                if(data.token) {
                    localStorage.setItem('token', data.token); 
                }
                // 顺便把用户名也存下来，以后网页右上角能显示 "欢迎您，XXX"
                localStorage.setItem('username', data.username); 

                btn.text('Log In').prop('disabled', false).removeClass('opacity-70 cursor-not-allowed');
                alert(data.message); // 弹出杜姐写的 "登录成功"
                window.location.href = 'home.html'; // 丝滑跳转
            } else {
                // 登录失败 (密码错误、账号不存在等)
                alert("Login Failed: " + data.message);
                btn.text('Log In').prop('disabled', false).removeClass('opacity-70 cursor-not-allowed');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            alert("Server connection failed! Is your Spring Boot running?");
            btn.text('Log In').prop('disabled', false).removeClass('opacity-70 cursor-not-allowed');
        });
    });

    // 页面跳转逻辑
    $('#go-to-register').on('click', function() {
        window.location.href = 'register.html';
    });
});