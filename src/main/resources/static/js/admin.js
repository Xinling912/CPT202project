const API_BASE = "http://localhost:8080";

window.logout = async function() {
    const isConfirmed = await showConfirm("Are you sure you want to log out of the Admin Dashboard?");
    if (isConfirmed) {
        localStorage.clear(); // 清理所有的 token 和本地数据
        window.location.href = 'landingpage.html'; // 跳回主页（或者 login.html）
    }
};
document.addEventListener('DOMContentLoaded', () => {
    // 1. 登录校验
    const savedName = localStorage.getItem('username');
    if (savedName) {
        const userNameEl = document.querySelector('.user-name');
        if(userNameEl) userNameEl.textContent = savedName;
    } else {
        window.location.href = 'login.html';
    }

    // 2. 侧边栏折叠
    const toggleBtn = document.getElementById('toggle-sidebar');
    if (toggleBtn) {
        toggleBtn.onclick = function() {
            document.getElementById('sidebar').classList.toggle('collapsed');
            document.getElementById('main-content').classList.toggle('expanded');
        };
    }

    // 3. 子菜单逻辑
    window.toggleSub = function(btnId, subId) {
        const btn = document.getElementById(btnId);
        const sub = document.getElementById(subId);
        if(btn && sub) {
            btn.classList.toggle('expanded');
            sub.classList.toggle('open');
        }
    }

    const btnSpecialistMgmt = document.getElementById('btnSpecialistMgmt');
    if (btnSpecialistMgmt) btnSpecialistMgmt.onclick = () => toggleSub('btnSpecialistMgmt', 'subSpecialistMgmt');

    const btnReview = document.getElementById('btnReview');
    if (btnReview) btnReview.onclick = () => toggleSub('btnReview', 'subReview');

    // 4. 路由分发器
    document.querySelectorAll('.menu-clickable').forEach(menu => {
        menu.onclick = function(e) {
            e.stopPropagation();
            document.querySelectorAll('.menu-clickable').forEach(m => m.classList.remove('active'));
            this.classList.add('active');

            if (this.classList.contains('nav-btn')) {
                this.classList.add('active');
            }

            document.querySelectorAll('.admin-page').forEach(page => page.style.display = 'none');

            const targetId = this.getAttribute('data-target');
            const targetPage = document.getElementById(targetId);
            if(targetPage) {
                targetPage.style.display = targetId === 'pageEmpty' ? 'flex' : 'block';
            }

            if (targetId === 'pageSpecialistStatus') loadExistingSpecialists();
            if (targetId === 'pageSpecialistApproval') loadPendingSpecialists();
            if (targetId === 'pageReport') loadComplaints('pending');
            if (targetId === 'pageExpertise') loadExpertise();

            if (targetId === 'pageSpecialistReview') {
                loadPendingRequests();
                cleanAndLoadHistory();
            }
        };
    });
    loadExistingSpecialists();
});


// ==========================================
// 页面 1A：专家状态管理
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

window.toggleStatus = async function(id, action) {
    const isConfirmed = await showConfirm(`Are you sure you want to ${action} this specialist?`);

    if (!isConfirmed) return; // 如果返回 false (点了 Cancel)，直接退出
    fetch(`${API_BASE}/api/admin/specialists/${id}/${action}`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
    }).then(res => {
        if(res.ok) { alert("Status updated!"); loadExistingSpecialists(); }
    });
}

//  页面 1B：新申请审批 (带详情查看版)
// ==========================================

// 1. 新建一个新申请的专用快递柜
window.pendingApplicationsCache = {};

window.loadPendingSpecialists = function() {
    const token = localStorage.getItem('token');
    const tbody = document.getElementById('pending-list-body');
    if(!tbody) return;
    tbody.innerHTML = '<tr><td colspan="6" style="text-align:center; padding:30px; color:#94a3b8;">Loading...</td></tr>';

    fetch(`${API_BASE}/api/admin/applications/pending`, {
        headers: { 'Authorization': `Bearer ${token}` }
    })
        .then(res => res.json())
        .then(resData => {
            let list = resData.data || [];
            tbody.innerHTML = list.length ? '' : '<tr><td colspan="6" style="text-align:center; padding:30px; color:#94a3b8;">No pending applications.</td></tr>';

            // 每次加载前清空快递柜
            window.pendingApplicationsCache = {};

            list.forEach(item => {
                // 把数据存进快递柜
                window.pendingApplicationsCache[item.id] = item;

                const realName = item.realName || (item.user?item.user.username:'Unknown');
                const category = item.proposedExpertiseName || (item.expertise ? item.expertise.name : 'N/A');
                const fee = item.hourlyFee || '0.00';

                const tr = document.createElement('tr');
                tr.innerHTML = `
            <td><input type="checkbox"></td>
            <td>${item.id}</td>
            <td style="color:var(--brand); font-weight:700;">${realName}</td>
            <td>${category}</td>
            <td>${fee} Yuan/hour</td>
            <td>
                <button class="btn btn-sm btn-outline-info rounded-pill px-3 me-2 fw-bold" onclick="viewApplicationDetail(${item.id})"><i class="bi bi-eye-fill me-1"></i> Details</button>
                <button class="btn btn-approve" onclick="approveApp(${item.id}, event)">Approve</button>
                <button class="btn btn-reject" onclick="rejectApp(${item.id}, event)">Reject</button>
            </td>
        `;
                tbody.appendChild(tr);
            });
        });
}

// 2.  新增函数：展示新申请的详细信息（复用咱们底部的那个漂亮弹窗）
window.viewApplicationDetail = function(reqId) {
    const item = window.pendingApplicationsCache[reqId];
    if (!item) {
        alert("无法读取该条数据，请刷新重试！");
        return;
    }

    const realName = item.realName || (item.user?.username || 'Unknown');
    const category = item.proposedExpertiseName || (item.expertise ? item.expertise.name : 'N/A');
    const fee = item.hourlyFee || '0.00';
    const resume = item.resume || 'No resume provided.';

    let detailHtml = `
        <div style="padding: 20px; background: #f0f9ff; border-radius: 8px; border: 1px solid #bae6fd;">
            <h5 class="fw-bold mb-4 border-bottom border-info pb-2 text-primary"><i class="bi bi-person-badge me-2"></i>New Specialist Application</h5>
            <div class="row mb-3">
                <div class="col-6">
                    <p class="text-muted small fw-bold text-uppercase mb-1">Applicant Name</p>
                    <p class="text-dark fw-bold fs-5">${realName}</p>
                </div>
                <div class="col-6">
                    <p class="text-muted small fw-bold text-uppercase mb-1">Requested Domain</p>
                    <p class="text-dark fw-bold fs-5">${category}</p>
                </div>
            </div>
            <div class="mb-3">
                <p class="text-muted small fw-bold text-uppercase mb-1">Proposed Hourly Fee</p>
                <p class="text-success fw-bold fs-5">${fee} Yuan/hour</p>
            </div>
            <div class="mt-4">
                <p class="text-muted small fw-bold text-uppercase mb-2">Individual Resume</p>
                <div style="background: white; padding: 15px; border: 1px solid #e2e8f0; border-radius: 8px; min-height: 120px; white-space: pre-wrap; font-size: 1.05rem; color: #334155;">${resume}</div>
            </div>
        </div>
    `;
    // 调用我们之前写好的开弹窗函数
    openAdminModal(detailHtml);
};

// ==========================================

window.approveApp = async function(id, event) {
    if (event) { event.preventDefault(); event.stopPropagation(); }

    //  替换 confirm
    const isConfirmed = await showConfirm("Are you sure you want to approve this new specialist application?");
    if (!isConfirmed) return;

    fetch(`${API_BASE}/api/admin/applications/${id}/approve`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
    })
        .then(async res => {
            if (res.ok) {
                alert("Application successfully Approved!");
                loadPendingSpecialists(); // 刷新列表
            } else {
                // 🌟 核心：捕获杜姐后端的真实报错并弹出来！
                const err = await res.json();
                alert("Failed to Approve: " + (err.error || err.message));
            }
        })
        .catch(err => alert("Network Error."));
};

window.rejectApp = async function(id, event) {
    if (event) { event.preventDefault(); event.stopPropagation(); }

    // 替换 confirm
    const isConfirmed = await showConfirm("Are you sure you want to REJECT this new specialist application?");
    if (!isConfirmed) return;
    fetch(`${API_BASE}/api/admin/applications/${id}/reject`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
    })
        .then(async res => {
            if (res.ok) {
                alert("Application successfully Rejected!");
                loadPendingSpecialists(); // 刷新列表
            } else {
                // 🌟 核心：捕获杜姐后端的真实报错并弹出来！
                const err = await res.json();
                alert("Failed to Reject: " + (err.error || err.message));
            }
        })
        .catch(err => alert("Network Error."));
};

// ==========================================
// 4：举报管理 (Report)
// ==========================================
window.toggleComplaintTab = function(mode, btn) {
    document.querySelectorAll('#complaint-tabs button').forEach(b => {
        b.classList.remove('btn-primary', 'text-white');
        b.classList.add('text-muted');
    });
    btn.classList.remove('text-muted');
    btn.classList.add('btn-primary', 'text-white');
    loadComplaints(mode);
}

window.loadComplaints = function(mode) {
    const url = mode === 'pending' ? '/api/complaints/pending' : '/api/complaints/all';
    const container = document.getElementById('complaints-list-container');
    container.innerHTML = '<div class="text-center py-5">Loading reports...</div>';

    fetch(`${API_BASE}${url}`, {
        headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
    })
        .then(res => res.json())
        .then(resData => {
            const data = resData.data || [];
            container.innerHTML = '';
            if(data.length === 0) {
                container.innerHTML = `<div class="text-center py-5 text-muted">No reports found.</div>`;
                return;
            }

            data.forEach(c => {
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
                <button class="btn btn-sm btn-danger shadow-sm" onclick="handleComplaint(${c.id}, 'ban', ${sId})">Ban Specialist</button>
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
        });
};

window.handleComplaint = async function(complaintId, action, specialistId = null) {
    if(action === 'ban') {
        if(!confirm("Warning: This will permanently BAN the specialist. Continue?")) return;
        if (specialistId && specialistId !== 'N/A') {
            try {
                await fetch(`${API_BASE}/api/admin/specialists/${specialistId}/suspend`, {
                    method: 'POST',
                    headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
                });
            } catch (e) {}
        }
    } else {
        const isConfirmed = await showConfirm("Dismiss this report?");
        if(!isConfirmed) return;
    }

    fetch(`${API_BASE}/api/complaints/${complaintId}/${action}`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
    }).then(res => {
        if(res.ok) {
            alert(`Report handled successfully!`);
            toggleComplaintTab('all', document.querySelectorAll('#complaint-tabs button')[1]);
        }
    });
}

// ==========================================
// 页面 3：专业领域管理
// ==========================================
window.expertiseCache = {};
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

            // 清空快递柜
            window.expertiseCache = {};

            data.forEach(exp => {
                // 把当前这一行的数据存进柜子，钥匙是 ID
                window.expertiseCache[exp.id] = exp;

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
        });
}

window.addExpertise = function() {
    const name = document.getElementById('new-exp-name').value;
    const desc = document.getElementById('new-exp-desc').value;
    if(!name || !desc) { alert("Please fill in both Name and Description!"); return; }

    fetch(`${API_BASE}/api/expertise/add`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}`, 'Content-Type': 'application/json' },
        body: JSON.stringify({ name: name, description: desc })
    }).then(async res => {
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
window.deleteExpertise = async function(id, name) {
    //  替换 confirm
    const isConfirmed = await showConfirm(`Are you sure you want to permanently delete [ ${name} ]?`);
    if(!isConfirmed) return;
    fetch(`${API_BASE}/api/expertise/${id}`, {
        method: 'DELETE',
        headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
    }).then(async res => {
        if(res.ok) {
            alert(`Expertise [ ${name} ] has been deleted.`);
            loadExpertise();
        } else {
            const err = await res.json();
            alert("Action Failed: " + (err.error || "Cannot delete this expertise."));
        }
    });
}
window.editExpertise = function(id) {
    // 从柜子里拿出旧数据
    const exp = window.expertiseCache[id];
    if (!exp) return;

    // 拼装一个带输入框的精美弹窗 HTML
    const htmlContent = `
        <div style="padding: 10px;">
            <h5 class="fw-bold mb-4 text-primary"><i class="bi bi-pencil-square me-2"></i>Edit Expertise Category</h5>
            
            <div class="mb-3 text-start">
                <label class="form-label fw-bold small text-muted text-uppercase">Category Name</label>
                <input type="text" id="edit-exp-name" class="form-control form-control-lg bg-light" value="${exp.name}">
            </div>
            
            <div class="mb-4 text-start">
                <label class="form-label fw-bold small text-muted text-uppercase">Description</label>
                <textarea id="edit-exp-desc" class="form-control bg-light" rows="3">${exp.description || ''}</textarea>
            </div>
            
            <div class="d-flex justify-content-end gap-2 border-top pt-3 mt-2">
                <button class="btn btn-light fw-bold px-4" onclick="closeAdminModal()">Cancel</button>
                <button class="btn btn-primary fw-bold px-4 shadow-sm" onclick="submitEditExpertise(${id})">Save Changes</button>
            </div>
        </div>
    `;
    // 呼叫你的全局模态框
    openAdminModal(htmlContent);
};

// 点击弹窗里的 Save Changes 按钮：发给后端保存
window.submitEditExpertise = function(id) {
    // 拿到输入框里的新值
    const newName = document.getElementById('edit-exp-name').value.trim();
    const newDesc = document.getElementById('edit-exp-desc').value.trim();

    if (!newName || !newDesc) {
        showToast("Please fill in both Name and Description!", "warning");
        return;
    }


    // 如果报 404，请确认一下后端 Controller 里修改专业的真实路径！
    fetch(`${API_BASE}/api/expertise/${id}`, {
        method: 'PUT',  // 修改通常用 PUT，也有可能杜姐用的是 POST，视情况而定
        headers: {
            'Authorization': `Bearer ${localStorage.getItem('token')}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ name: newName, description: newDesc })
    }).then(async res => {
        if(res.ok) {
            showToast("Expertise updated successfully!", "success");
            closeAdminModal(); // 关闭弹窗
            loadExpertise();   // 刷新列表
        } else {
            const err = await parseError(res);
            showToast("Failed to update: " + err, "error");
        }
    }).catch(err => showToast("Network Error", "error"));
};

// ==========================================
//  5：Content Review 页面真实对接逻辑 (终极防白屏版)
// ==========================================

// 建立一个全局快递柜，用来存复杂的简历数据，防止把 HTML 挤爆
window.pendingEditsCache = {};

function hideAllPagesForReview() {
    document.querySelectorAll('.admin-page').forEach(page => page.style.display = 'none');
}

window.showReviewPage = function(event) {
    if (event) {
        event.preventDefault();
        event.stopPropagation();
    }
    setTimeout(() => {
        hideAllPagesForReview();
        document.getElementById('pageSpecialistReview').style.display = 'block';
        loadPendingRequests();
    }, 50);
}

window.openAdminModal = function(htmlContent) {
    document.getElementById('modal-content').innerHTML = htmlContent;
    document.getElementById('modalOverlay').style.display = 'flex';
}

window.closeAdminModal = function() {
    document.getElementById('modalOverlay').style.display = 'none';
}

// 1. 获取所有待审核的 Profile 申请
window.loadPendingRequests = function() {
    const tbody = document.getElementById('update-review-list-body');
    if(!tbody) return;
    tbody.innerHTML = '<tr><td colspan="4" style="text-align:center; padding:30px; color:#94a3b8;">Loading pending applications...</td></tr>';

    fetch(`${API_BASE}/api/admin/edits/pending`, {
        method: 'GET',
        headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
    })
        .then(async res => {
            if(!res.ok) throw new Error("Failed to load");
            return res.json();
        })
        .then(resData => {
            const list = resData.data || resData || [];
            if (list.length === 0) {
                tbody.innerHTML = '<tr><td colspan="4" style="text-align:center; padding:30px; color:#94a3b8;">No pending updates to review.</td></tr>';
                return;
            }

            // 每次加载前清空快递柜
            window.pendingEditsCache = {};
            let html = '';

            list.forEach(item => {
                const reqId = item.id;
                //  把复杂的 item 数据存进快递柜，钥匙就是 reqId
                window.pendingEditsCache[reqId] = item;

                const realName = item.newRealName || item.specialistProfile?.realName || item.user?.username || 'Unknown';
                const category = item.newProposedExpertiseName || (item.newExpertise ? item.newExpertise.name : 'N/A');
                const fee = item.newHourlyFee || item.hourlyFee || 'N/A';
                const username = item.specialistProfile?.user?.username || item.user?.username || '';
                const avatarUrl = getAvatar(username, realName);

                //  注意：这里的 Compare 按钮，只传极其安全的纯数字 reqId！
                html += `
            <tr style="border-top: 1px solid #e2e8f0; transition: background 0.2s;">
                <td style="padding: 15px 20px;">
                    <div style="display: flex; align-items: center; gap: 12px;">
                        <img src="${avatarUrl}" width="36" height="36" style="border-radius: 50%; object-fit: cover;">
                        <span style="font-weight: 700; color: #1e293b;">${realName}</span>
                    </div>
                </td>
                <td style="padding: 15px 20px; font-weight:600; color:#475569;">${category}</td>
            <td style="padding: 15px 20px; font-weight:600; color:#3b82f6;">${fee} Yuan/hour</td>
                <td style="padding: 15px 20px; text-align: center;">
                    <button onclick="viewPendingDetail(${reqId})" style="color: #3b82f6; border: none; background: none; cursor:pointer; margin-right:12px; font-weight: 600;"><i class="bi bi-layout-split me-1"></i> Compare</button>
                    <button onclick="handleApproval(${reqId}, true)" style="color: #10b981; border: none; background: none; cursor:pointer; margin-right:12px; font-weight: 600;"><i class="bi bi-check-circle-fill me-1"></i> Approve</button>
                    <button onclick="handleApproval(${reqId}, false)" style="color: #ef4444; border: none; background: none; cursor:pointer; font-weight: 600;"><i class="bi bi-x-circle-fill me-1"></i> Reject</button>
                </td>
            </tr>
            `;
            });
            tbody.innerHTML = html;
        })
        .catch(err => {
            tbody.innerHTML = '<tr><td colspan="4" style="text-align:center; padding:30px; color:#ef4444;">API Error or No Data.</td></tr>';
        });
}

// 2. 填充详情弹窗
window.viewPendingDetail = function(reqId) {
    // 凭着传进来的 ID 钥匙，去柜子里把数据拿出来
    const item = window.pendingEditsCache[reqId];
    if (!item) {
        alert("无法读取该条数据，请刷新重试！");
        return;
    }

    // --- 第一步：提取旧数据 (Current Profile) ---
    const oldProfile = item.specialistProfile || {};
    const oldName = oldProfile.realName || oldProfile.user?.username || 'Unknown';
    const oldCategory = oldProfile.expertise ? oldProfile.expertise.name : 'N/A';

    const oldFee = oldProfile.hourlyFee !== undefined && oldProfile.hourlyFee !== null ? `${oldProfile.hourlyFee} Yuan/hour` : 'N/A';
    const oldResume = oldProfile.resume || 'No resume content.';

// --- 第二步：提取新数据 (Requested Updates) ---
// 如果新数据是空的，说明专家没改这一项，那就直接等于旧数据
    const newName = item.newRealName || oldName;
    const newCategory = item.newProposedExpertiseName || (item.newExpertise ? item.newExpertise.name : oldCategory);

    const newFee = item.newHourlyFee !== undefined && item.newHourlyFee !== null ? `${item.newHourlyFee} Yuan/hour` : oldFee;
    const newResume = item.newResume || oldResume;
    // --- 第三步：制造一个“智能对比生成器” ---
    const renderRow = (label, oldVal, newVal, isLongText = false) => {
        // 情况A：如果没改动
        if (oldVal === newVal) {
            return `
                <div class="mb-3">
                    <label class="text-muted small fw-bold text-uppercase">${label} <span class="badge bg-light text-secondary border fw-normal ms-2">No Change</span></label>
                    <div class="${isLongText ? 'p-3 bg-light rounded border text-muted' : 'fs-6 text-dark mt-1'}" style="${isLongText ? 'white-space: pre-wrap;' : ''}">
                        ${oldVal}
                    </div>
                </div>
            `;
        }

        // 情况B：如果有改动
        if (isLongText) {
            // 长文本（简历）：左右分栏对比
            return `
                <div class="mb-3">
                    <label class="text-muted small fw-bold text-uppercase">${label}</label>
                    <div class="row g-3 mt-1">
                        <div class="col-6">
                            <div class="badge bg-secondary mb-2">Current</div>
                            <div class="p-3 bg-light rounded border text-muted" style="white-space: pre-wrap; font-size: 0.9rem; height: 100%;">${oldVal}</div>
                        </div>
                        <div class="col-6">
                            <div class="badge bg-success mb-2">Requested</div>
                            <div class="p-3 rounded border" style="background: #f0fdf4; border-color: #bbf7d0 !important; color: #166534; white-space: pre-wrap; font-size: 0.9rem; height: 100%;">${newVal}</div>
                        </div>
                    </div>
                </div>
            `;
        } else {
            // 短文本：删除线 + 箭头
            return `
                <div class="mb-3">
                    <label class="text-muted small fw-bold text-uppercase">${label}</label>
                    <div class="d-flex align-items-center gap-3 mt-1 fs-6">
                        <span class="text-muted text-decoration-line-through">${oldVal}</span>
                        <i class="bi bi-arrow-right-circle-fill text-primary"></i>
                        <span class="fw-bold text-success">${newVal}</span>
                    </div>
                </div>
            `;
        }
    };

    // --- 第四步：拼装最终的弹窗 HTML ---
    let detailHtml = `
        <div style="padding: 10px;">
            ${renderRow('Specialist Name', oldName, newName)}
            <div class="row">
                <div class="col-6">${renderRow('Expertise Category', oldCategory, newCategory)}</div>
                <div class="col-6">${renderRow('Hourly Fee', oldFee, newFee)}</div>
            </div>
            <div class="mt-2 border-top pt-3">
                ${renderRow('Resume / About Me', oldResume, newResume, true)}
            </div>
        </div>
    `;
    openAdminModal(detailHtml);
};
// 3. 同意 / 驳回
window.handleApproval = async function(reqId, isApproved) {
    const action = isApproved ? 'approve' : 'reject';
    const actionText = isApproved ? 'Approve' : 'Reject';

    //  替换 confirm
    const isConfirmed = await showConfirm(`Are you sure you want to ${actionText} this request?`);
    if(!isConfirmed) return;
    fetch(`${API_BASE}/api/admin/edits/${reqId}/${action}`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
    })
        .then(async res => {
            if (res.ok) {
                alert(`Request successfully ${actionText}d!`);
                loadPendingRequests();
            } else {
                const err = await res.json();
                alert(`Failed to ${actionText}: ` + (err.error || err.message));
            }
        })
        .catch(err => alert("Network Error."));
}

window.cleanAndLoadHistory = function() {
    const tbody = document.getElementById('admin-history-list-body');
    if(tbody) tbody.innerHTML = '<tr><td colspan="4" style="text-align:center; padding:30px; color:#94a3b8;">History feature is currently managed by backend.</td></tr>';
}
// ==========================================
