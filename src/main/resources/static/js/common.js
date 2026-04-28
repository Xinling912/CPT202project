// 1. 智能提取后端报错信息 (专治各种花式 JSON 报错)

async function parseError(response) {

    const text = await response.text();

    try {

// 尝试把后端的报错解析成 JSON 对象

        const json = JSON.parse(text);

// 如果后端传了 error 字段就用 error，传了 message 就用 message，都没有就原样返回

        return json.error || json.message || text;

    } catch (e) {

// 如果后端传的根本不是 JSON（比如 500 页面源码），就直接返回纯文本

        return text;

    }

}



// 2. 现代化悬浮提示框 (彻底替代丑陋的 window.alert)

window.showToast = function(message, type = 'error') {

// 检查页面上有没有装提示框的容器，没有就建一个

    let container = document.getElementById('toast-container');

    if (!container) {

        container = document.createElement('div');

        container.id = 'toast-container';

        container.style.cssText = 'position: fixed; top: 24px; left: 50%; transform: translateX(-50%); z-index: 9999; display: flex; flex-direction: column; gap: 12px; pointer-events: none;';

        document.body.appendChild(container);

    }



// 设置不同状态的颜色和图标

    const bgColor = type === 'success' ? '#10b981' : (type === 'warning' ? '#f59e0b' : '#ef4444');

    const icon = type === 'success' ? 'bi-check-circle-fill' : (type === 'warning' ? 'bi-exclamation-triangle-fill' : 'bi-x-circle-fill');



// 创建一条提示

    const toast = document.createElement('div');

    toast.style.cssText = `background: ${bgColor}; color: white; padding: 12px 24px; border-radius: 50px; box-shadow: 0 10px 25px rgba(0,0,0,0.15); display: flex; align-items: center; gap: 10px; font-weight: 600; font-size: 0.95rem; opacity: 0; transform: translateY(-20px); transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);`;

    toast.innerHTML = `<i class="bi ${icon} fs-5"></i> <span>${message}</span>`;



    container.appendChild(toast);



// 动画：滑入显示

    requestAnimationFrame(() => {

        toast.style.opacity = '1';

        toast.style.transform = 'translateY(0)';

    });



// 动画：3秒后自动消失

    setTimeout(() => {

        toast.style.opacity = '0';

        toast.style.transform = 'translateY(-20px)';

        setTimeout(() => toast.remove(), 300);

    }, 3000);

}

window.showConfirm = function(message) {
    return new Promise((resolve) => {
        // 1. 创建全屏半透明遮罩
        const overlay = document.createElement('div');
        overlay.style.cssText = 'position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; background: rgba(0, 0, 0, 0.4); backdrop-filter: blur(2px); display: flex; justify-content: center; align-items: center; z-index: 10000; opacity: 0; transition: opacity 0.2s ease;';

        // 2. 创建弹窗主体
        const box = document.createElement('div');
        box.style.cssText = 'background: white; width: 360px; padding: 24px; border-radius: 12px; box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04); transform: scale(0.95); transition: transform 0.2s cubic-bezier(0.175, 0.885, 0.32, 1.275);';

        // 3. 填充 HTML 结构 (标题、内容、两个按钮)
        box.innerHTML = `
            <div style="display: flex; align-items: center; gap: 10px; margin-bottom: 12px; color: #1f2937;">
                <i class="bi bi-question-circle-fill text-primary" style="font-size: 1.5rem;"></i>
                <h5 style="margin: 0; font-weight: 700; font-size: 1.15rem;">Action Required</h5>
            </div>
            <p style="color: #4b5563; font-size: 0.95rem; margin-bottom: 24px; line-height: 1.5;">${message}</p>
            <div style="display: flex; justify-content: flex-end; gap: 12px;">
                <button id="custom-confirm-cancel" style="padding: 8px 16px; border: 1px solid #d1d5db; background: white; color: #4b5563; border-radius: 6px; font-weight: 600; cursor: pointer; transition: background 0.2s;">Cancel</button>
                <button id="custom-confirm-ok" style="padding: 8px 16px; border: none; background: #3b82f6; color: white; border-radius: 6px; font-weight: 600; cursor: pointer; box-shadow: 0 1px 2px rgba(0,0,0,0.05); transition: background 0.2s;">Confirm</button>
            </div>
        `;

        overlay.appendChild(box);
        document.body.appendChild(overlay);

        // 4. 进场动画
        requestAnimationFrame(() => {
            overlay.style.opacity = '1';
            box.style.transform = 'scale(1)';
        });

        // 5. 绑定点击事件 (核心：点击后把 Promise resolve 掉)
        const btnCancel = box.querySelector('#custom-confirm-cancel');
        const btnOk = box.querySelector('#custom-confirm-ok');

        // 悬浮变色小交互
        btnCancel.onmouseover = () => btnCancel.style.background = '#f3f4f6';
        btnCancel.onmouseout = () => btnCancel.style.background = 'white';
        btnOk.onmouseover = () => btnOk.style.background = '#2563eb';
        btnOk.onmouseout = () => btnOk.style.background = '#3b82f6';

        // 点击取消 -> 返回 false，并销毁弹窗
        btnCancel.onclick = () => {
            overlay.style.opacity = '0';
            setTimeout(() => overlay.remove(), 200);
            resolve(false);
        };

        // 点击确认 -> 返回 true，并销毁弹窗
        btnOk.onclick = () => {
            overlay.style.opacity = '0';
            setTimeout(() => overlay.remove(), 200);
            resolve(true);
        };
    });
};
window.originalAlert = window.alert; // 把丑陋的老 alert 备份一下（以防万一）

window.alert = function(message) {
    // 智能判断应该用什么颜色
    let type = 'error'; // 默认当报错处理
    let msgStr = String(message).toLowerCase();

    if (msgStr.includes('success') || msgStr.includes('updated') || msgStr.includes('approved') || msgStr.includes('成功')) {
        type = 'success';
    } else if (msgStr.includes('please') || msgStr.includes('expired') || msgStr.includes('请')) {
        type = 'warning';
    }

    // 调用我们自己写的神级弹窗！
    showToast(message, type);
};