$(document).ready(function() {
    // 成功后点击去登录
    $('#back-to-login').on('click', function() {
        window.location.href = 'login.html';
    });
});
// --- 🌟 Earnings & Finances Logic ---

// 点击“添加新卡”按钮，隐藏列表，显示表单
$('#show-add-card-btn').on('click', function() {
    $('#add-card-form').removeClass('d-none').addClass('animation-fade-in');
    $('#linked-cards-list').addClass('d-none');
    $(this).addClass('d-none'); // 隐藏添加按钮自己
});

// 点击“取消”或“保存”按钮，隐藏表单，恢复列表
$('#cancel-card-btn, #save-card-btn').on('click', function(e) {
    e.preventDefault(); // 防止表单默认提交刷新页面
    
    $('#add-card-form').addClass('d-none').removeClass('animation-fade-in');
    $('#linked-cards-list').removeClass('d-none');
    $('#show-add-card-btn').removeClass('d-none');
    
    // 如果点的是保存，弹个提示
    if(e.target.id === 'save-card-btn') {
        alert("New card linked successfully!");
    }
});