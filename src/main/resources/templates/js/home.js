$(document).ready(function() {
    // 1. 核心修复：从浏览器读取 username，如果没有拿到，默认显示 Guest
    let displayName = localStorage.getItem('username') || 'Guest';

    // 2. 直接渲染为 @ + 用户名
    $('#user-email').text('@' + displayName);

    // 3. 点击任何一个 Search More 按钮，都跳转到 booking 页面
    $('#searchMoreTopBtn, #searchMoreBottomBtn').on('click', function() {
        window.location.href = 'booking.html';
    });

    // 4. 点击 Join us 跳转到专家申请页面
    $('#joinUsBtn').on('click', function() {
        window.location.href = 'expert_apply.html'; 
    });

    // 5. 登出按钮逻辑 (升级版：彻底清空状态)
    $('#logoutBtn').on('click', function() {
        if (confirm("Are you sure you want to log out?")) {
            localStorage.clear(); // 退出时直接清空所有状态 (包括 token, role, username 等)
            window.location.href = 'login.html';  // 踢回登录页
        }
    });
});