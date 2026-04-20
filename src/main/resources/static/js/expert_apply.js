$(document).ready(function() {

    // ==========================================
    // 1. 页面初始化检查
    // ==========================================
    const token = localStorage.getItem('token');
    if (!token) {
        alert("Please log in first!");
        window.location.href = 'login.html';
        return;
    }

    // ==========================================
    // 2. UI 交互逻辑
    // ==========================================

    // 处理专业领域下拉框变化
    $('#exp-domain-select').on('change', function() {
        const selectedValue = $(this).val();
        const $otherInput = $('#exp-domain-input');

        $(this).css('color', '#1f2937');

        if (selectedValue === 'other') {
            $otherInput.fadeIn().attr('required', true).focus();
        } else {
            $otherInput.hide().attr('required', false).val('');
        }
    });

    // 处理 Level 下拉框颜色
    $('#exp-level').on('change', function() {
        $(this).css('color', '#1f2937');
    });

    // 返回按钮
    $('#go-back').on('click', function() {
        window.history.back();
    });

    // ==========================================
    // 3. 表单提交逻辑
    // ==========================================
    $('#expertForm').on('submit', function(e) {
        e.preventDefault();

        const $btn = $('#applyBtn');
        const originalText = $btn.text();
        $btn.prop('disabled', true).text('Submitting...').css('opacity', '0.7');

        const domainSelectValue = $('#exp-domain-select').val();
        const isOther = domainSelectValue === 'other';

        // ========================================================
        // 核心修复点：字段名必须严格对应后端 SpecialistApplyRequest Record 的属性名
        // 这样后端 request.realName() 才不会为 null，从而通过校验并执行 save()
        // ========================================================
        const expertData = {
            realName: $('#exp-name').val().trim(),
            level: $('#exp-level').val(),
            hourlyFee: Number($('#exp-fee').val()),
            resume: $('#exp-resume').val().trim(),
            expertiseId: isOther ? null : Number(domainSelectValue),
            newExpertiseName: isOther ? $('#exp-domain-input').val().trim() : null,
        };

        fetch('http://localhost:8080/api/specialists/apply', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ' + token
            },
            body: JSON.stringify(expertData)
        })
        .then(async response => {
            if (response.ok) {
                alert("Application submitted successfully! Please wait for admin review.");
                window.location.href = 'booking.html';
            } else if (response.status === 401) {
                alert("Session expired. Please log in again.");
                localStorage.removeItem('token');
                window.location.href = 'login.html';
            } else {
                const errorText = await response.text();
                try {
                    const errorObj = JSON.parse(errorText);
                    alert("Submission failed: " + (errorObj.error || errorObj.message || errorText));
                } catch(e) {
                    alert("Submission failed: " + errorText);
                }
            }
        })
        .catch(error => {
            console.error('Fetch error:', error);
            alert("Network error. Please make sure the backend server is running.");
        })
        .finally(() => {
            $btn.prop('disabled', false).text(originalText).css('opacity', '1');
        });
    });
});