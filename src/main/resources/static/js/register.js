$(document).ready(function() {

    // ==========================================
    // 大招一：发送验证码与 60 秒倒计时
    // ==========================================
    $('#sendCodeBtn').on('click', function() {
        const emailInput = $('#reg-email').val();
        
        if (!emailInput) {
            alert("Please enter your email first!");
            return;
        }

        const btn = $(this);
        let timeLeft = 60;

        // 按钮变灰，开始倒计时
        btn.prop('disabled', true).addClass('opacity-70 cursor-not-allowed');
        
        const timer = setInterval(() => {
            btn.text(timeLeft + 's');
            timeLeft--;
            if (timeLeft < 0) {
                clearInterval(timer);
                btn.prop('disabled', false).removeClass('opacity-70 cursor-not-allowed');
                btn.text('Send');
            }
        }, 1000);

        // 呼叫杜姐的发送验证码接口！
        fetch('http://localhost:8080/api/users/verify-code', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ email: emailInput })
        })
        .then(response => response.json())
        .then(data => {
            // 杜姐后端返回的提示语 (比如: "验证码已发送到邮箱")
            console.log(data.message); 
            // 你也可以换成 alert(data.message); 提醒用户去查看邮箱
        })
        .catch(error => {
            console.error('发送验证码失败:', error);
            alert("Failed to send verification code. Check server.");
        });
    });


    // ==========================================
    // 大招二：提交完整的注册信息
    // ==========================================
    $('#registerForm').on('submit', function(e) {
        e.preventDefault(); 
        
        const btn = $('#registerBtn');
        const username = $('#reg-username').val(); // 新增：获取用户名
        const email = $('#reg-email').val();
        const pwd = $('#reg-password').val();
        const confirm = $('#reg-confirm').val();
        const verifyCode = $('#reg-code').val();   // 新增：获取验证码

        if (pwd !== confirm) {
            alert("Passwords do not match!");
            $('#reg-confirm').trigger('focus');
            return;
        }

        btn.text('Processing...')
           .prop('disabled', true)
           .addClass('opacity-70 cursor-not-allowed');

        // 严格按照杜姐定义的 RegisterRequest 组装数据
        const userData = {
            username: username,
            password: pwd,
            email: email,
            role: "CUSTOMER", // 角色写死为普通客户
            verifyCode: verifyCode
        };

        // 呼叫杜姐的正式注册接口！
        fetch('http://localhost:8080/api/users/register', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json' 
            },
            body: JSON.stringify(userData) 
        })
        .then(async response => {
            const data = await response.json(); // 解析后端传回的 JSON 信息

            if (response.ok) {
                // HTTP 状态码是 200 或 201，注册成功！
                btn.text('Register Account').prop('disabled', false).removeClass('opacity-70 cursor-not-allowed');
                alert(data.message); // 弹出 "注册成功"
                window.location.href = 'login.html'; // 注册完直接跳去登录页
            } else {
                // 注册失败（比如验证码错误、邮箱被占用）
                alert("Registration failed: " + data.message);
                btn.text('Register Account').prop('disabled', false).removeClass('opacity-70 cursor-not-allowed');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            alert("Server connection failed! Make sure your Spring Boot app is running.");
            btn.text('Register Account').prop('disabled', false).removeClass('opacity-70 cursor-not-allowed');
        });
    });

    // 返回登录页按钮逻辑
    $('#go-to-login').on('click', function() {
        window.location.href = 'login.html'; 
    });
});