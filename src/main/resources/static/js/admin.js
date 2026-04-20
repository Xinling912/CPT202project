const API_BASE = "http://localhost:8080";

document.addEventListener('DOMContentLoaded', () => {

    // 🌟 1. 登录校验
    const savedName = localStorage.getItem('username');
    if (savedName) {
        document.querySelector('.user-name').textContent = savedName;
    } else {
        window.location.href = 'login.html';
    }

    // 🌟 2. 侧边栏交互
    document.getElementById('toggle-sidebar').onclick = function() {
        document.getElementById('sidebar').classList.toggle('collapsed');
        document.getElementById('main-content').classList.toggle('expanded');
    };

    function toggleSub(btnId, subId) {
        const btn = document.getElementById(btnId);
        const sub = document.getElementById(subId);
        if(btn && sub) {
            btn.classList.toggle('expanded');
            sub.classList.toggle('open');
        }
    }
    document.getElementById('btnUser').onclick = () => toggleSub('btnUser', 'subUser');
    document.getElementById('btnSpecialist').onclick = (e) => {
        e.stopPropagation();
        toggleSub('btnSpecialist', 'subSpecialist');
    };
    document.getElementById('btnReview').onclick = () => toggleSub('btnReview', 'subReview');

    // 🌟 3. 路由与高亮切换
    const pageExisting = document.getElementById('pageExisting');
    const pagePending = document.getElementById('pagePending');
    const pageEmpty = document.getElementById('pageEmpty');

    function hideAll() {
        [pageExisting, pagePending, pageEmpty].forEach(p => p.style.display = 'none');
    }

    document.querySelectorAll('.menu-clickable').forEach(menu => {
        menu.onclick = function(e) {
            e.stopPropagation();
            // 切换高亮
            document.querySelectorAll('.menu-clickable').forEach(m => m.classList.remove('active'));
            this.classList.add('active');

            hideAll();
            if (this.id === 'menuStatus') {
                pageExisting.style.display = 'block';
                loadExistingSpecialists();
            } else if (this.id === 'menuApproval') {
                pagePending.style.display = 'block';
                loadPendingSpecialists();
            } else {
                pageEmpty.style.display = 'flex';
            }
        };
    });

    // 默认打开审批页
    const defaultMenu = document.getElementById('menuApproval');
    if(defaultMenu) defaultMenu.click();

    // 🌟 4. 模态框控制
    const modalOverlay = document.getElementById('modalOverlay');
    const closeModal = () => modalOverlay.style.display = 'none';
    document.getElementById('closeModalBtn').onclick = closeModal;
    modalOverlay.onclick = (e) => { if(e.target === modalOverlay) closeModal(); };
});

// ==========================================
// 🌟 核心 A：管理已有专家 (Status Adjustment)
// ==========================================
function loadExistingSpecialists() {
    const token = localStorage.getItem('token');
    const container = document.getElementById('existing-list-body');
    container.innerHTML = '<div style="text-align: center; padding: 40px; color: #94a3b8;">Loading...</div>';

    fetch(`${API_BASE}/api/admin/specialists`, {
        method: 'GET',
        headers: { 'Authorization': `Bearer ${token}` }
    })
        .then(res => res.json())
        .then(resData => {
            let list = Array.isArray(resData) ? resData : (resData.data || []);
            container.innerHTML = '';

            list.forEach(item => {
                const username = item.user ? item.user.username : 'Unknown';
                const realName = item.realName || username;
                const currentStatus = item.status || 'ACTIVE';
                const isActive = (currentStatus === 'ACTIVE');

                // --- 🌟 全明星头像自动匹配逻辑 (admin.js) ---
                let avatarUrl = `https://api.dicebear.com/7.x/initials/svg?seed=${username}`;

                if (username === 'ShenShaohui' || realName === 'Shaohui Shen') {
                    avatarUrl = 'images/ssh.jpg';
                } else if (username === 'XingjianWu' || realName === 'Xingjian Wu') {
                    avatarUrl = 'images/specialist1.png';
                } else if (username === 'XinlingDu' || realName === 'Xinling Du') {
                    avatarUrl = 'images/specialist3.png';
                } else if (username === 'Carrot' || realName === 'Carrot') {
                    // 🌟 这里加上 Carrot 的照片路径，假设你把新上传的图改名为 carrot.png
                    avatarUrl = 'images/carrot.png';
                }

                const badgeHtml = isActive
                    ? `<span class="badge bg-success text-white ms-2 px-2 py-1 rounded-pill" style="font-size:0.7rem;">ACTIVE</span>`
                    : `<span class="badge bg-secondary text-white ms-2 px-2 py-1 rounded-pill" style="font-size:0.7rem;">INACTIVE</span>`;

                const btnHtml = isActive
                    ? `<button class="modify-btn" style="color: #ef4444; border-color: #fca5a5;" onclick="toggleStatus(${item.id}, 'suspend')">Suspend</button>`
                    : `<button class="modify-btn" style="color: var(--brand); border-color: var(--brand);" onclick="toggleStatus(${item.id}, 'activate')">Activate</button>`;

                const div = document.createElement('div');
                div.className = 'specialist-item';
                div.innerHTML = `
                <img src="${avatarUrl}" alt="Avatar">
                <div class="info">
                    <div class="name">${realName} ${badgeHtml}</div>
                    <div class="desc">${item.resume || 'No description.'}</div>
                </div>
                ${btnHtml}
            `;
                container.appendChild(div);
            });
        });
}

function toggleStatus(id, action) {
    if (!confirm(`Are you sure you want to ${action} this specialist?`)) return;
    const token = localStorage.getItem('token');
    fetch(`${API_BASE}/api/admin/specialists/${id}/${action}`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` }
    }).then(res => {
        if(res.ok) { alert("Status updated!"); loadExistingSpecialists(); }
    });
}

// ==========================================
// 🌟 核心 B：新申请审批 (Information Approval)
// ==========================================
function loadPendingSpecialists() {
    const token = localStorage.getItem('token');
    const tbody = document.getElementById('pending-list-body');
    fetch(`${API_BASE}/api/admin/applications/pending`, {
        method: 'GET',
        headers: { 'Authorization': `Bearer ${token}` }
    })
        .then(res => res.json())
        .then(resData => {
            let list = resData.data || [];
            tbody.innerHTML = list.length ? '' : '<tr><td colspan="6" style="text-align:center; padding:30px;">Empty.</td></tr>';
            list.forEach(item => {
                const tr = document.createElement('tr');
                tr.innerHTML = `
                <td><input type="checkbox"></td>
                <td>${item.id}</td>
                <td style="color:var(--brand); font-weight:700;">${item.realName || item.user.username}</td>
                <td>${item.proposedExpertiseName || 'N/A'}</td>
                <td>$${item.hourlyFee}/hr</td>
                <td>
                    <button class="btn btn-approve" onclick="approveApp(${item.id}, event)">Approve</button>
                    <button class="btn btn-reject" onclick="rejectApp(${item.id}, event)">Reject</button>
                </td>
            `;
                tbody.appendChild(tr);
            });
        });
}

function approveApp(id, e) {
    e.stopPropagation();
    if(confirm("Approve?")) {
        fetch(`${API_BASE}/api/admin/applications/${id}/approve`, {
            method:'POST',
            headers:{'Authorization':`Bearer ${localStorage.getItem('token')}`}
        }).then(res => { if(res.ok) loadPendingSpecialists(); });
    }
}

function rejectApp(id, e) {
    e.stopPropagation();
    if(confirm("Reject?")) {
        fetch(`${API_BASE}/api/admin/applications/${id}/reject`, {
            method:'POST',
            headers:{'Authorization':`Bearer ${localStorage.getItem('token')}`}
        }).then(res => { if(res.ok) loadPendingSpecialists(); });
    }
}