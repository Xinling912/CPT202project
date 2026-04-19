document.addEventListener('DOMContentLoaded', () => {

    // ==========================================
    // 🌟 动态加载登录的管理员用户名
    // ==========================================
    const savedName = localStorage.getItem('username');
    if (savedName) {
        // 找到页面右上角显示名字的地方，把名字替换掉
        const userNameElement = document.querySelector('.user-name');
        if (userNameElement) {
            userNameElement.textContent = savedName;
        }
    } else {
        // 如果没有找到名字，说明他没登录直接通过改网址进来的，直接踢回登录页！(可选安全措施)
        alert("Please log in first!");
        window.location.href = 'login.html';
    }
    // ==========================================

    // 🌟 侧边栏折叠逻辑
    document.getElementById('toggle-sidebar').onclick = function() {
        document.getElementById('sidebar').classList.toggle('collapsed');
        document.getElementById('main-content').classList.toggle('expanded');
    };

    // 室友的菜单展开逻辑
    function toggle(btnId, subId) {
        const btn = document.getElementById(btnId);
        const sub = document.getElementById(subId);
        if(btn && sub) {
            btn.classList.toggle('expanded');
            sub.classList.toggle('open');
        }
    }

    document.getElementById('btnUser').onclick = () => toggle('btnUser', 'subUser');
    document.getElementById('btnSpecialist').onclick = (e) => {
        e.stopPropagation();
        toggle('btnSpecialist', 'subSpecialist');
    };
    document.getElementById('btnReview').onclick = () => toggle('btnReview', 'subReview');

    const pageApproval = document.getElementById('pageApproval');
    const pageReview   = document.getElementById('pageReview');
    const pageEmpty    = document.getElementById('pageEmpty');

    function showApproval() {
        if(pageApproval) pageApproval.style.display = 'block';
        if(pageReview) pageReview.style.display = 'none';
        if(pageEmpty) pageEmpty.style.display = 'none';
    }
    function showReview() {
        if(pageApproval) pageApproval.style.display = 'none';
        if(pageReview) pageReview.style.display = 'block';
        if(pageEmpty) pageEmpty.style.display = 'none';
    }
    function showEmpty() {
        if(pageApproval) pageApproval.style.display = 'none';
        if(pageReview) pageReview.style.display = 'none';
        if(pageEmpty) pageEmpty.style.display = 'block';
    }

    document.getElementById('menuApproval').onclick = showApproval;
    document.getElementById('menuSpecialistReview').onclick = showReview;
    document.getElementById('menuStatus').onclick = showEmpty;
    document.getElementById('menuUser').onclick = showEmpty;
    document.getElementById('menuPostReview').onclick = showEmpty;

    // Modal
    const modalOverlay = document.getElementById('modalOverlay');
    function openModal() { if(modalOverlay) modalOverlay.style.display = 'flex'; }
    function closeModal() { if(modalOverlay) modalOverlay.style.display = 'none'; }

    document.querySelectorAll('.review-row').forEach(row => {
        row.onclick = openModal;
    });

    // 关闭按钮事件绑定
    const closeBtn = document.getElementById('closeModalBtn');
    if (closeBtn) {
        closeBtn.onclick = function(e) {
            e.preventDefault();
            closeModal();
        }
    }

    if(modalOverlay) {
        modalOverlay.onclick = (e) => {
            if (e.target === modalOverlay) closeModal();
        };
    }
});