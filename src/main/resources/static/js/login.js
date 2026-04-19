$(document).ready(function() {
    $('#loginForm').on('submit', function(e) {
        e.preventDefault(); 
        
        const btn = $('#loginBtn');
        const inputAccount = $('#log-email').val(); 
        const inputPwd = $('#log-password').val();
        
        btn.text('Verifying...')
           .prop('disabled', true)
           .addClass('opacity-70 cursor-not-allowed');

        const loginData = {
            usernameOrEmail: inputAccount, 
            password: inputPwd
        };

        fetch('http://localhost:8080/api/users/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(loginData)
        })
        .then(async response => {
            const data = await response.json(); 

            console.log("【调试】后端返回的完整数据：", data); 

            if (response.ok) {
                const actualToken = data.token;
                
                if (actualToken) {
                    localStorage.setItem('token', actualToken); 
                    console.log("【调试】Token 成功存入浏览器：", actualToken);
                } else {
                    console.warn("⚠️ 警告：后端返回成功，但没有找到 Token 字段！");
                    alert("登录成功，但未获取到 Token，请按 F12 查看控制台并联系后端！");
                }
                
                const actualUsername = data.username || (data.data && data.data.username) || inputAccount;
                localStorage.setItem('username', actualUsername); 
localStorage.setItem('username', data.username || inputAccount);
                // ==========================================
                // 🌟 【新增核心代码】：保存身份牌，供后面的页面做权限隔离
                // ==========================================
                if (data.role) {
                    localStorage.setItem('role', data.role); // 存入角色 (CUSTOMER 或 SPECIALIST)
                    console.log("【调试】用户身份(Role)已存入：", data.role);
                }
                if (data.userId) {
                    localStorage.setItem('userId', data.userId); // 存入用户ID，方便查询属于他的订单
                    console.log("【调试】用户ID已存入：", data.userId);
                }
                // ==========================================

                btn.text('Log In').prop('disabled', false).removeClass('opacity-70 cursor-not-allowed');
               alert(data.message || "登录成功！"); 

// 🌟 终极分流口：不同身份进不同的门
if (data.role === 'SPECIALIST') {

    window.location.href = 'specialist.html'; 
}
else  if (data.role === 'ADMIN')

{window.location.href = 'admin.html';}

else
{
    window.location.href = 'booking.html';
}
            } else {
                alert("Login Failed: " + (data.message || "未知错误，请检查账号密码"));
                btn.text('Log In').prop('disabled', false).removeClass('opacity-70 cursor-not-allowed');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            alert("Server connection failed! Is your Spring Boot running?");
            btn.text('Log In').prop('disabled', false).removeClass('opacity-70 cursor-not-allowed');
        });
    });

    $('#go-to-register').on('click', function() {
        window.location.href = 'register.html';
    });
});