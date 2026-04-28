const API_BASE = "http://localhost:8080";

$(document).ready(function() {

    const token = localStorage.getItem('token');
    if (!token) {
        alert("Please log in first!");
        window.location.href = 'login.html';
        return;
    }

    //  1. 进页面第一件事：向少辉请示当前申请状态！
    checkApplyStatus();

    //  2. 动态加载数据库里真实的专业列表！
    loadExpertiseCategories();


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

    $('#exp-level').on('change', function() {
        $(this).css('color', '#1f2937');
    });

    $('#go-back').on('click', function() {
        window.history.back();
    });

    // 处理表单提交
    $('#expertForm').on('submit', function(e) {
        e.preventDefault();

        const $btn = $('#applyBtn');
        const originalText = $btn.text();
        $btn.prop('disabled', true).text('Submitting...').css('opacity', '0.7');

        const domainSelectValue = $('#exp-domain-select').val();
        const isOther = domainSelectValue === 'other';

        const expertData = {
            realName: $('#exp-name').val().trim(),
            level: $('#exp-level').val(),
            hourlyFee: Number($('#exp-fee').val()),
            resume: $('#exp-resume').val().trim(),
            expertiseId: isOther ? null : Number(domainSelectValue),
            // 🌟 核心：如果是Other，就把输入的文本发过去，后端会自动创建新专业
            newExpertiseName: isOther ? $('#exp-domain-input').val().trim() : null
        };

        fetch(`${API_BASE}/api/specialists/apply`, {
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
                    // 提交成功后，直接刷新当前页面，页面会自动变成黄色的“审核中”状态！
                    window.location.reload();
                } else if (response.status === 401) {
                    alert("Session expired. Please log in again.");
                    localStorage.clear();
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
                alert("Network error. Please make sure the backend server is running.");
            })
            .finally(() => {
                $btn.prop('disabled', false).text(originalText).css('opacity', '1');
            });
    });
});

// ==========================================
// 核心功能 1：获取真实专业列表
// ==========================================
function loadExpertiseCategories() {
    fetch(`${API_BASE}/api/expertise/list`, {
        headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
    })
        .then(res => res.json())
        .then(resData => {
            const list = resData.data || [];
            const select = $('#exp-domain-select');

            select.empty();
            select.append('<option value="" disabled selected>Please select your professional field.</option>');

            // 动态把数据库里的专业塞进去
            list.forEach(exp => {
                select.append(`<option value="${exp.id}">${exp.name}</option>`);
            });

            // 最后加上 Other 选项
            select.append('<option value="other">Other (自定义新专业)...</option>');
        })
        .catch(err => console.error("Failed to load expertise:", err));
}

// ==========================================
// 核心功能 2：追踪用户申请状态
// ==========================================
function checkApplyStatus() {
    fetch(`${API_BASE}/api/specialists/apply-status`, {
        headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` },
        cache: 'no-store'
    })
        .then(res => res.json())
        .then(data => {
            const status = data.status;
            const msg = data.message;
            const banner = $('#status-banner');
            const form = $('#expertForm');

            if (status === 'APPLY_PENDING') {
                // 审核中：显示黄条，隐藏表单
                banner.css({'display': 'block', 'background': '#fef3c7', 'color': '#92400e', 'border': '1px solid #f59e0b'});
                banner.html(`⏳ Update Under Review: ${msg}`);
                form.hide();

                //  修复点：这里必须用 else if 隔开！！！
            } else if (status === 'APPLY_REJECTED') {
                // 被驳回：显示红条，保留表单
                banner.css({'display': 'block', 'background': '#fee2e2', 'color': '#b91c1c', 'border': '1px solid #ef4444'});
                banner.html(`
                <div style="display:flex; align-items:center;">
                    <i class="bi bi-x-circle-fill" style="font-size:1.5rem; margin-right:10px;"></i>
                    <div>
                        <div style="font-size:1.1rem;">Application Rejected (申请被驳回)</div>
                        <div style="font-weight:normal; font-size:0.85rem; margin-top:3px;">Reason: ${msg}。Please modify your information below and submit again. (请修改下方信息后重新提交)</div>
                    </div>
                </div>
                `);
                form.show(); // 确保表单让他重新填

            } else if (status === 'IS_ACTIVE_SPECIALIST' || status.startsWith('EDIT_')) {
                window.location.href = 'specialist.html';
            }
        })
        .catch(err => console.error("Failed to check status:", err));
}