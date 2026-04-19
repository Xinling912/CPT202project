$(document).ready(function() {
    $('#loginForm').on('submit', function(e) {
        e.preventDefault();

        const btn = $('#loginBtn');
        const account = $('#log-email').val().trim();
        const pwd = $('#log-password').val().trim();

        // 前端强制检查
        if (!account || !pwd) {
            alert("前端检查：账号或密码不能为空！");
            return;
        }

        btn.text('Verifying...').prop('disabled', true);

        // 🌟 终极核心：一字不差地对齐后端的 Record 属性名
        const loginData = {
            usernameOrEmail: account,
            password: pwd
        };

        console.log("正在发送数据到后端:", loginData);

        fetch('/api/users/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(loginData)
        })
        .then(async response => {
            const data = await response.json();
            console.log("后端返回原始数据:", data);

            if (response.ok) {
                localStorage.setItem('token', data.token);
                localStorage.setItem('username', data.username || account);
                localStorage.setItem('role', data.role || 'CLIENT');

                alert("登录成功！");

                // 身份跳转
                if (data.role === 'SPECIALIST') {
                    window.location.href = 'specialist.html';
                } else {
                    window.location.href = 'home.html';
                }
            } else {
                alert("登录失败: " + (data.message || "账号或密码错误"));
                btn.text('Log In').prop('disabled', false);
            }
        })
        .catch(error => {
            console.error('Fetch Error:', error);
            alert("网络错误，请检查后端服务是否正常。");
            btn.text('Log In').prop('disabled', false);
        });
    });

    $('#go-to-register').on('click', function() {
        window.location.href = 'register.html';
    });
});