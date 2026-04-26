const API_BASE = "http://localhost:8080";

document.addEventListener('DOMContentLoaded', () => {

    // 1. 登录校验(杜姐啊杜姐何以味)
    const savedName = localStorage.getItem('username');
    if (savedName) {
        const userNameEl = document.querySelector('.user-name');
        if(userNameEl) userNameEl.textContent = savedName;
    } else {
        window.location.href = 'login.html';
    }

    // 2. 侧边栏折叠交互
    const toggleBtn = document.getElementById('toggle-sidebar');
    if (toggleBtn) {
        toggleBtn.onclick = function() {
            document.getElementById('sidebar').classList.toggle('collapsed');
            document.getElementById('main-content').classList.toggle('expanded');
        };
    }

    // 3. 修复版的子菜单展开逻辑
    function toggleSub(btnId, subId) {
        const btn = document.getElementById(btnId);
        const sub = document.getElementById(subId);
        if(btn && sub) {
            btn.classList.toggle('expanded');
            sub.classList.toggle('open');
        }
    }

    // 安全绑定点击事件，防止报错卡死 JS
    const btnSpecialistMgmt = document.getElementById('btnSpecialistMgmt');
    if (btnSpecialistMgmt) btnSpecialistMgmt.onclick = () => toggleSub('btnSpecialistMgmt', 'subSpecialistMgmt');

    const btnReview = document.getElementById('btnReview');
    if (btnReview) btnReview.onclick = () => toggleSub('btnReview', 'subReview');

    document.querySelectorAll('.menu-clickable').forEach(menu => {
        menu.onclick = function(e) {
            e.stopPropagation();

            // 移除所有的高亮
            document.querySelectorAll('.menu-clickable').forEach(m => m.classList.remove('active'));
            this.classList.add('active');

            // 只保留高亮逻辑，不再强行关闭 submenu！
            if (this.classList.contains('nav-btn')) {
                this.classList.add('active');
            }

            // 隐藏右侧所有页面
            document.querySelectorAll('.admin-page').forEach(page => page.style.display = 'none');

            // 显示目标页面
            const targetId = this.getAttribute('data-target');
            const targetPage = document.getElementById(targetId);
            if(targetPage) {
                targetPage.style.display = targetId === 'pageEmpty' ? 'flex' : 'block';
            }

            // 加载对应数据
            if (targetId === 'pageSpecialistStatus') loadExistingSpecialists();
            if (targetId === 'pageSpecialistApproval') loadPendingSpecialists();
            if (targetId === 'pageReport') loadComplaints('pending');
            if (targetId === 'pageExpertise') loadExpertise();
        };
    });
    // 默认打开
    loadExistingSpecialists();
});

// ==========================================
// 辅助函数：自动匹配头像
// ==========================================
function getAvatar(username, realName) {
    let avatarUrl = `https://api.dicebear.com/7.x/initials/svg?seed=${username}`;
    if (username === 'ShenShaohui' || realName === 'Shaohui Shen') avatarUrl = 'images/ssh.jpg';
    else if (username === 'XingjianWu' || realName === 'Xingjian Wu') avatarUrl = 'images/specialist1.png';
    else if (username === 'XinlingDu' || realName === 'Xinling Du') avatarUrl = 'images/specialist3.png';
    else if (username === 'Carrot' || realName === 'Carrot') avatarUrl = 'images/carrot.png';
    return avatarUrl;
}

// ==========================================
// 页面 1A：专家状态管理 (带搜索)
// ==========================================
window.loadExistingSpecialists = function() {
    const token = localStorage.getItem('token');
    const container = document.getElementById('existing-list-body');
    const keywordInput = document.getElementById('search-keyword');
    const keyword = keywordInput ? keywordInput.value : '';

    container.innerHTML = '<div style="text-align: center; padding: 40px; color: #94a3b8;">Loading...</div>';

    fetch(`${API_BASE}/api/admin/specialists?keyword=${encodeURIComponent(keyword)}`, {
        method: 'GET',
        headers: { 'Authorization': `Bearer ${token}` }
    })
        .then(res => res.json())
        .then(resData => {
            let list = Array.isArray(resData) ? resData : (resData.data || []);
            container.innerHTML = '';

            if(list.length === 0) {
                container.innerHTML = '<div style="text-align: center; padding: 40px; color: #94a3b8;">No specialists found.</div>';
                return;
            }

            list.forEach(item => {
                const username = item.user ? item.user.username : 'Unknown';
                const realName = item.realName || username;
                const currentStatus = item.status || 'ACTIVE';
                const isActive = (currentStatus === 'ACTIVE');
                const avatarUrl = getAvatar(username, realName);

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
                    <div class="desc text-muted mb-1" style="font-size:12px;">ID: ${item.id} | Email: ${item.user ? item.user.email : 'N/A'}</div>
                    <div class="desc">${item.resume || 'No description provided.'}</div>
                </div>
                ${btnHtml}
            `;
                container.appendChild(div);
            });
        });
}

window.toggleStatus = function(id, action) {
    if (!confirm(`Are you sure you want to ${action} this specialist?`)) return;
    fetch(`${API_BASE}/api/admin/specialists/${id}/${action}`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
    }).then(res => {
        if(res.ok) { alert("Status updated!"); loadExistingSpecialists(); }
    });
}

// ==========================================
// 页面 1B：新申请审批
// ==========================================
window.loadPendingSpecialists = function() {
    const token = localStorage.getItem('token');
    const tbody = document.getElementById('pending-list-body');
    tbody.innerHTML = '<tr><td colspan="6" style="text-align:center; padding:30px; color:#94a3b8;">Loading...</td></tr>';

    fetch(`${API_BASE}/api/admin/applications/pending`, {
        method: 'GET',
        headers: { 'Authorization': `Bearer ${token}` }
    })
        .then(res => res.json())
        .then(resData => {
            let list = resData.data || [];
            tbody.innerHTML = list.length ? '' : '<tr><td colspan="6" style="text-align:center; padding:30px; color:#94a3b8;">No pending applications.</td></tr>';
            list.forEach(item => {
                const tr = document.createElement('tr');
                tr.innerHTML = `
                <td><input type="checkbox"></td>
                <td>${item.id}</td>
                <td style="color:var(--brand); font-weight:700;">${item.realName || (item.user?item.user.username:'Unknown')}</td>
                <td>${item.proposedExpertiseName || 'N/A'}</td>
                <td>$${item.hourlyFee}/hr</td>
                <td>
                    <button class="btn btn-approve" onclick="approveApp(${item.id}, event)">Approve</button>
                    <button class="btn btn-reject" onclick="rejectApp(${item.id}, event)">Reject</button>
                </td>
            `;
                tbody.appendChild(tr);
            });
        }).catch(err => {
        tbody.innerHTML = '<tr><td colspan="6" style="text-align:center; padding:30px; color:#ef4444;">No data or API error.</td></tr>';
    });
}

window.approveApp = function(id, e) {
    e.stopPropagation();
    if(confirm("Approve this application?")) {
        fetch(`${API_BASE}/api/admin/applications/${id}/approve`, {
            method:'POST',
            headers:{'Authorization':`Bearer ${localStorage.getItem('token')}`}
        }).then(res => { if(res.ok) loadPendingSpecialists(); });
    }
}

window.rejectApp = function(id, e) {
    e.stopPropagation();
    if(confirm("Reject this application?")) {
        fetch(`${API_BASE}/api/admin/applications/${id}/reject`, {
            method:'POST',
            headers:{'Authorization':`Bearer ${localStorage.getItem('token')}`}
        }).then(res => { if(res.ok) loadPendingSpecialists(); });
    }
}

// ==========================================
//  4：举报管理 (Report)
// ==========================================
let currentComplaintMode = 'pending';

window.toggleComplaintTab = function(mode, btn) {
    document.querySelectorAll('#complaint-tabs button').forEach(b => {
        b.classList.remove('btn-primary', 'text-white');
        b.classList.add('text-muted');
    });
    btn.classList.remove('text-muted');
    btn.classList.add('btn-primary', 'text-white');
    loadComplaints(mode);
}
// ==========================================

window.loadComplaints = function(mode) {
    currentComplaintMode = mode;
    const url = mode === 'pending' ? '/api/complaints/pending' : '/api/complaints/all';
    const container = document.getElementById('complaints-list-container');
    container.innerHTML = '<div class="text-center py-5">Loading reports...</div>';

    fetch(`${API_BASE}${url}`, {
        headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
    })
        .then(res => {
            if (!res.ok) throw new Error("Server crashed (500). 数据关联可能已损坏，请重启 IDEA。");
            return res.json();
        })
        .then(resData => {
            const data = resData.data || [];
            container.innerHTML = '';

            if(data.length === 0) {
                container.innerHTML = `<div class="text-center py-5 text-muted">No ${mode} reports found.</div>`;
                return;
            }

            data.forEach(c => {
                // 🕵️‍♂️ 终极兜底：即便 booking 或 specialist 是 null，也不让 JS 崩掉
                const booking = c.booking || {};
                const specialist = booking.specialist || {};
                const user = specialist.user || {};

                const sUsername = user.username || 'BannedAccount';
                const sRealName = specialist.realName || c.specialistName || sUsername;
                const sId = specialist.id || c.specialistId || 'N/A';
                const sEmail = user.email || 'N/A';
                const bId = c.bookingId || booking.id || 'N/A';
                const rName = c.reporterName || (c.reporter ? c.reporter.username : 'User');

                let statusBadge = `<span class="badge ${c.status === 'PENDING' ? 'bg-warning' : 'bg-secondary'} px-3 rounded-pill">${c.status}</span>`;

                let actionHtml = (mode === 'pending' && c.status === 'PENDING') ? `
                <div class="d-flex gap-3 justify-content-end mt-3 border-top pt-3">
                    <button class="btn btn-sm btn-light border" onclick="handleComplaint(${c.id}, 'dismiss')">Dismiss</button>
                    <button class="btn btn-sm btn-danger shadow-sm" onclick="handleComplaint(${c.id}, 'ban')">Ban Specialist</button>
                </div>` : '';

                const div = document.createElement('div');
                div.className = 'border rounded-4 p-4 mb-4 bg-white shadow-sm';
                div.innerHTML = `
                <div class="d-flex justify-content-between mb-3">
                    <h6 class="fw-800 text-danger mb-0">Report ID: #${c.id}</h6>
                    ${statusBadge}
                </div>
                <div class="d-flex align-items-center gap-4 p-3 mb-3 rounded-4" style="background: #fff5f5; border: 1px dashed #fecaca;">
                    <img src="${getAvatar(sUsername, sRealName)}" class="rounded-circle shadow-sm" width="65" height="65" style="object-fit:cover; border: 3px solid white; background: white;">
                    <div class="flex-grow-1">
                        <div class="fw-900 text-dark">${sRealName} <span class="badge bg-white text-danger border small">ID: ${sId}</span></div>
                        <div class="text-muted small">${sEmail} | Booking: #${bId} | Reporter: ${rName}</div>
                    </div>
                </div>
                <div class="bg-light p-3 rounded-3 small"><b>Reason:</b> ${c.reason || 'N/A'}</div>
                ${actionHtml}
            `;
                container.appendChild(div);
            });
        })
        .catch(err => {
            container.innerHTML = `<div class="alert alert-danger mx-5 mt-4"><i class="bi bi-bug me-2"></i>${err.message}</div>`;
        });
};

// 🌟 管理员处理：Ban 之后自动跳到 All Records 查看结果
window.handleComplaint = function(id, action) {
    if(action === 'ban' && !confirm("Permanently ban this expert?")) return;

    fetch(`${API_BASE}/api/complaints/${id}/${action}`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
    })
        .then(async res => {
            if(res.ok) {
                alert("Success!");
                // 重点：处理完立马刷新“所有记录”页 [cite: 1141]
                toggleComplaintTab('all', document.querySelectorAll('#complaint-tabs button')[1]);
            } else {
                const err = await res.json();
                alert("Failed: " + err.error);
            }
        });
};


window.handleComplaint = function(id, action) {
    if(action === 'ban' && !confirm("Warning: Permanently BAN this specialist?")) return;

    fetch(`${API_BASE}/api/complaints/${id}/${action}`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
    })
        .then(async res => {
            if(res.ok) {
                alert(`Report has been ${action === 'ban' ? 'handled and expert banned' : 'dismissed'}.`);

                toggleComplaintTab('all', document.querySelectorAll('#complaint-tabs button')[1]);
            } else {
                const err = await res.json();
                alert("Action failed: " + (err.error || "Internal Error"));
            }
        });
}

window.handleComplaint = async function(complaintId, action, specialistId = null) {
    if(action === 'ban') {
        if(!confirm("Warning: This will permanently BAN the specialist and resolve the report. Continue?")) return;

        // 第一步：先封掉专家的账号 (调用原本专家管理的 suspend 接口)
        if (specialistId && specialistId !== 'N/A') {
            try {
                const banRes = await fetch(`${API_BASE}/api/admin/specialists/${specialistId}/suspend`, {
                    method: 'POST',
                    headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
                });
                if (!banRes.ok) console.warn("Expert account suspension failed, but proceeding to resolve report.");
            } catch (e) {
                console.error("Critical: Could not reach specialist API.");
            }
        }
    }

    // 第二步：处理举报单状态 (Ban 或 Dismiss)
    fetch(`${API_BASE}/api/complaints/${complaintId}/${action}`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
    })
        .then(res => {
            if(res.ok) {
                alert(`Report has been ${action === 'ban' ? 'handled and expert banned' : 'dismissed'}!`);

                toggleComplaintTab('all', document.querySelectorAll('#complaint-tabs button')[1]);
            } else {
                alert("Action failed.");
            }
        });
}

// ==========================================
// 页面 3：专业领域管理
// ==========================================
window.loadExpertise = function() {
    const tbody = document.getElementById('expertise-table-body');
    tbody.innerHTML = '<tr><td colspan="4" class="text-center py-5 text-muted">Loading expertise data...</td></tr>';

    fetch(`${API_BASE}/api/expertise/list`, {
        headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
    })
        .then(res => res.json())
        .then(resData => {
            tbody.innerHTML = '';
            const data = resData.data || [];
            if(data.length === 0) {
                tbody.innerHTML = '<tr><td colspan="4" class="text-center py-5 text-muted">No expertise found.</td></tr>';
                return;
            }

            data.forEach((exp, index) => {
                const tr = document.createElement('tr');
                tr.innerHTML = `
                <td class="text-muted fw-bold">#${exp.id}</td>
                <td class="fw-bold text-dark fs-6">${exp.name}</td>
                <td class="text-muted">${exp.description}</td>
                <td class="text-end">
                    <button class="action-btn btn-edit" onclick="editExpertise(${exp.id})"><i class="bi bi-pencil-fill me-1"></i>Edit</button>
                    <button class="action-btn btn-del" onclick="deleteExpertise(${exp.id}, '${exp.name}')"><i class="bi bi-trash3-fill me-1"></i>Delete</button>
                </td>
            `;
                tbody.appendChild(tr);
            });
        }).catch(err => {
        tbody.innerHTML = '<tr><td colspan="4" class="text-center py-5 text-danger">Backend disconnected. Showing Mock Data failed.</td></tr>';
    });
}

window.addExpertise = function() {
    const name = document.getElementById('new-exp-name').value;
    const desc = document.getElementById('new-exp-desc').value;

    if(!name || !desc) {
        alert("Please fill in both Name and Description!");
        return;
    }

    fetch(`${API_BASE}/api/expertise/add`, {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${localStorage.getItem('token')}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ name: name, description: desc })
    })
        .then(async res => {
            if(res.ok) {
                alert("New expertise added successfully!");
                document.getElementById('new-exp-name').value = '';
                document.getElementById('new-exp-desc').value = '';
                loadExpertise();
            } else {
                const err = await res.json();
                alert("Failed to add: " + (err.error || "Unknown error"));
            }
        });
}

window.deleteExpertise = function(id, name) {
    if(!confirm(`Are you sure you want to permanently delete [ ${name} ]?`)) return;

    fetch(`${API_BASE}/api/expertise/${id}`, {
        method: 'DELETE',
        headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
    }).then(async res => {
        if(res.ok) {
            alert(`Expertise [ ${name} ] has been deleted.`);
            loadExpertise(); // 刷新表格
        } else {
            const err = await res.json();
            // 对接文档里的失败响应："error":"专业【xxx】正在被使用,无法删除"
            alert("Action Failed: " + (err.error || "Cannot delete this expertise."));
        }
    });
}

window.editExpertise = function(id) {
    // 防发呆 不调用借口
    alert("The Edit API endpoint is not yet provided by the backend. Coming soon!");
}
// ==========================================
//  Content Review 页面专属审核逻辑 (从 HTML 抽离)
// ==========================================

function hideAllPagesForReview() {
    document.querySelectorAll('.admin-page').forEach(page => page.style.display = 'none');
}

// 强制接管 Review 按钮点击事件，防止和上方路由冲突
window.showReviewPage = function(event) {
    if (event) {
        event.preventDefault();
        event.stopPropagation();
    }

    setTimeout(() => {
        hideAllPagesForReview();
        document.getElementById('pageSpecialistReview').style.display = 'block';
        loadPendingRequests();
        cleanAndLoadHistory();
    }, 50);
}

function generateDiffHTML(oldP, newP) {
    if (!oldP && !newP) {
        return '<div style="text-align:center; padding: 20px; color:#94a3b8;"><i class="bi bi-info-circle me-2"></i>This is an old record without detailed snapshot data.</div>';
    }

    oldP = oldP || {};
    newP = newP || {};
    let diffHtml = '';

    const generateRow = (label, oldVal, newVal, isImage = false) => {
        if (oldVal === newVal) return '';
        if (isImage) {
            return `
            <div class="diff-box">
                <div class="diff-label">${label}</div>
                <div class="diff-content">
                    <img src="${oldVal || 'images/beauty.png'}" class="diff-img" title="Old">
                    <i class="bi bi-arrow-right fs-4 diff-arrow"></i>
                    <img src="${newVal || 'images/beauty.png'}" class="diff-img" style="border-color:#10b981;" title="New">
                </div>
            </div>`;
        }
        return `
        <div class="diff-box">
            <div class="diff-label">${label}</div>
            <div class="diff-content">
                <span class="diff-old">${oldVal || '(Empty)'}</span>
                <i class="bi bi-arrow-right fs-5 diff-arrow"></i>
                <span class="diff-new">${newVal || '(Empty)'}</span>
            </div>
        </div>`;
    };

    diffHtml += generateRow('Profile Photo', oldP.photo, newP.photo, true);
    diffHtml += generateRow('Full Name', oldP.name, newP.name);
    diffHtml += generateRow('Category', oldP.category, newP.category);
    diffHtml += generateRow('Hourly Fee ($)', oldP.fee, newP.fee);
    diffHtml += generateRow('Resume', oldP.resume, newP.resume);

    if(diffHtml === '') diffHtml = '<div style="text-align:center; padding: 20px; color:#94a3b8; font-weight: bold;">No modifications detected (Data is identical).</div>';
    return diffHtml;
}

window.openAdminModal = function(htmlContent) {
    document.getElementById('modal-content').innerHTML = htmlContent;
    document.getElementById('modalOverlay').style.display = 'flex';
}

window.closeAdminModal = function() {
    document.getElementById('modalOverlay').style.display = 'none';
}

window.viewPendingDetail = function() {
    const historyData = JSON.parse(localStorage.getItem('sas_profile_history')) || [];
    const pendingRecord = historyData.find(r => r.status === 'Pending Review');
    if (pendingRecord) {
        openAdminModal(generateDiffHTML(pendingRecord.oldProfile, pendingRecord.newProfile));
    } else {
        alert("No details available for this pending request.");
    }
};

window.viewHistoryDetail = function(index) {
    const historyData = JSON.parse(localStorage.getItem('sas_profile_history')) || [];
    const record = historyData[index];
    if(record) {
        openAdminModal(generateDiffHTML(record.oldProfile, record.newProfile));
    }
};

window.loadPendingRequests = function() {
    const pendingData = JSON.parse(localStorage.getItem('sas_pending_profile'));
    const tbody = document.getElementById('update-review-list-body');

    if (!pendingData) {
        tbody.innerHTML = '<tr><td colspan="4" style="text-align:center; padding:30px; color:#94a3b8;">No pending updates to review.</td></tr>';
        return;
    }

    tbody.innerHTML = `
        <tr style="border-top: 1px solid #e2e8f0; transition: background 0.2s;">
            <td style="padding: 15px 20px;">
                <div style="display: flex; align-items: center; gap: 12px;">
                    <img src="${pendingData.photo || 'images/beauty.png'}" width="36" height="36" style="border-radius: 50%; object-fit: cover;">
                    <span style="font-weight: 700; color: #1e293b;">${pendingData.name || 'N/A'}</span>
                </div>
            </td>
            <td style="padding: 15px 20px; font-weight:600; color:#475569;">${pendingData.category || 'N/A'}</td>
            <td style="padding: 15px 20px; font-weight:600; color:#3b82f6;">$${pendingData.fee || 'N/A'}</td>
            <td style="padding: 15px 20px; text-align: center;">
                <button onclick="viewPendingDetail()" style="color: #3b82f6; border: none; background: none; cursor:pointer; margin-right:12px; font-weight: 600;" title="Compare Diffs"><i class="bi bi-layout-split me-1"></i> Compare</button>
                <button onclick="handleApproval(true)" style="color: #10b981; border: none; background: none; cursor:pointer; margin-right:12px; font-weight: 600;"><i class="bi bi-check-circle-fill me-1"></i> Approve</button>
                <button onclick="handleApproval(false)" style="color: #ef4444; border: none; background: none; cursor:pointer; font-weight: 600;"><i class="bi bi-x-circle-fill me-1"></i> Reject</button>
            </td>
        </tr>
    `;
}

window.cleanAndLoadHistory = function() {
    let historyData = JSON.parse(localStorage.getItem('sas_profile_history')) || [];
    const now = new Date();

    const filteredHistory = historyData.filter(record => {
        const recordDate = new Date(record.time.replace(/-/g, '/'));
        const diffDays = Math.ceil(Math.abs(now - recordDate) / (1000 * 60 * 60 * 24));
        return diffDays <= 30;
    });

    if (filteredHistory.length !== historyData.length) {
        localStorage.setItem('sas_profile_history', JSON.stringify(filteredHistory));
        historyData = filteredHistory;
    }

    const tbody = document.getElementById('admin-history-list-body');
    const completedRecords = historyData.map((record, index) => ({ record, realIndex: index }))
        .filter(item => item.record.status === 'Approved' || item.record.status === 'Rejected');

    if (completedRecords.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4" style="text-align:center; padding:30px; color:#94a3b8;">No approval history found for the last 30 days.</td></tr>';
        return;
    }

    let html = '';
    completedRecords.reverse().forEach(item => {
        const record = item.record;
        let badgeColor = record.status === 'Approved' ? '#10b981' : '#ef4444';
        let badgeBg = record.status === 'Approved' ? '#d1fae5' : '#fee2e2';

        html += `
        <tr style="border-top: 1px solid #e2e8f0;">
            <td style="padding: 15px 20px; color: #64748b;">${record.time}</td>
            <td style="padding: 15px 20px; font-weight:600; color:#475569;">Profile Update</td>
            <td style="padding: 15px 20px;">
                <span style="background-color: ${badgeBg}; color: ${badgeColor}; padding: 4px 10px; border-radius: 999px; font-size: 0.75rem; font-weight: 700;">
                    ${record.status}
                </span>
            </td>
            <td style="padding: 15px 20px; text-align: center;">
                <button style="border:1px solid #cbd5e1; background:white; color:#475569; padding:4px 12px; border-radius:6px; cursor:pointer; font-weight:600; font-size:0.85rem;" onclick="viewHistoryDetail(${item.realIndex})">
                    <i class="bi bi-eye me-1"></i> View Details
                </button>
            </td>
        </tr>
        `;
    });
    tbody.innerHTML = html;
}

window.handleApproval = function(isApproved) {
    const pendingData = JSON.parse(localStorage.getItem('sas_pending_profile'));
    const historyData = JSON.parse(localStorage.getItem('sas_profile_history')) || [];

    if (isApproved) {
        localStorage.setItem('sas_active_profile', JSON.stringify(pendingData));
    }

    localStorage.removeItem('sas_pending_profile');

    for (let i = historyData.length - 1; i >= 0; i--) {
        if (historyData[i].status === 'Pending Review') {
            historyData[i].status = isApproved ? 'Approved' : 'Rejected';
            break;
        }
    }
    localStorage.setItem('sas_profile_history', JSON.stringify(historyData));

    loadPendingRequests();
    cleanAndLoadHistory();
}