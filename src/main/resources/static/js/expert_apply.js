$(document).ready(function() {
    // ==========================================
    // 1. 新增：专业领域“其他”选项的显隐控制
    // ==========================================
    $('#exp-domain').change(function() {
        if ($(this).val() === 'other') {
            // 如果选择了 other，显示输入框并设为必填
            $('#exp-domain-other').show().attr('required', true);
        } else {
            // 否则隐藏输入框并取消必填
            $('#exp-domain-other').hide().attr('required', false);
        }
    });

    // ==========================================
    // 2. 表单提交逻辑
    // ==========================================
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

        // --- 数据提取逻辑开始 ---
        // 处理“其他”专业领域的值
        let finalDomain = $('#exp-domain').val();
        if (finalDomain === 'other') {
            finalDomain = $('#exp-domain-other').val();
        }

        const expertData = {
            name: $('#exp-name').val(),
            domain: finalDomain,
            level: $('#exp-level').val(), // 对应 HTML 中的 JUNIOR, SENIOR, EXPERT
            fee: $('#exp-fee').val(),
            resume: $('#exp-resume').val()
            // 注意：此处已根据你的需求去掉了 qualification certificates
        };
        // --- 数据提取逻辑结束 ---

        // 发送真实请求去申请专家
        fetch('http://localhost:8080/api/experts/apply', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                // 🌟 核心高光时刻：向保安亮出手环！
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(expertData)
        })
        .then(async response => {
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