$(document).ready(function() {
    $('#expertForm').on('submit', function(e) {
        e.preventDefault();
        
        const btn = $('#applyBtn');
        btn.text('Submitting...')
           .prop('disabled', true)
           .addClass('opacity-70 cursor-not-allowed');

        // 从浏览器的记事本里，把登录时存的手环拿出来
        const token = localStorage.getItem('token');

        // 如果连手环都没有，说明根本没登录，直接赶去登录页
        if (!token) {
            alert("Please log in first!");
            window.location.href = 'login.html';
            return;
        }

        // 假设你要提交的数据
        const expertData = {
            // 这里填入你们专家表单里提取出来的数据
            // name: $('#expert-name').val(),
            // field: $('#expert-field').val()
        };

        // 发送真实请求去申请专家
        fetch('http://localhost:8080/api/experts/apply', { // 注意：等后端写好后，改成真实的接口地址
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                // ==========================================
                // 🌟 核心高光时刻：向保安亮出手环！
                // ==========================================
                'Authorization': `Bearer ${token}` 
            },
            body: JSON.stringify(expertData)
        })
        .then(async response => {
            // 这里为了防止后端还没写好接口报错，先做个兼容处理
            if(response.ok) {
                alert("Application submitted successfully for review!");
                window.location.href = 'home.html'; // 提交成功回主页
            } else {
                alert("Submission failed. Please try again later.");
            }
        })
        .catch(error => {
            console.error('Error:', error);
            // 这里保留你以前的模拟逻辑，万一后端没开，至少前端还能弹个框展示一下
            setTimeout(() => {
                alert("Application submitted successfully for review! (Simulated)");
                window.location.href = 'home.html';
            }, 1000);
        })
        .finally(() => {
            btn.text('Come up for review')
               .prop('disabled', false)
               .removeClass('opacity-70 cursor-not-allowed');
        });
    });

    // 返回按钮逻辑
    $('#go-back').on('click', function() {
        window.location.href = 'home.html';
    });
});