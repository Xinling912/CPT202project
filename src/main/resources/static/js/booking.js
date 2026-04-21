/**
 * User Booking Logic
 * Specialist Administration System (SAS)
 */

const API_BASE = "http://localhost:8080";
let selectedExpertId = null;
let currentSelectedSlotId = null;

// --- 1. 全局配置 ---
$.ajaxSetup({
    beforeSend: function(xhr) {
        const token = localStorage.getItem('token');
        if (token) xhr.setRequestHeader('Authorization', `Bearer ${token}`);
    }
});

function checkLogin() {
    const token = localStorage.getItem('token');
    if (!token) {
        window.location.href = 'login.html';
        return false;
    }
    return true;
}

function logout() {
    if(confirm("Are you sure you want to log out?")) {
        localStorage.clear(); // 清除 Token
        // 🌟 跳转回落地页
        window.location.href = 'landingpage.html';
    }
}

// 专家/用户全明星头像逻辑
function getAvatar(username) {
    if (username === 'ShenShaohui' || username === 'Shaohui Shen') return `images/ssh.jpg`;
    if (username === 'XingjianWu' || username === 'Xingjian Wu') return `images/specialist1.png`;
    if (username === 'XinlingDu' || username === 'Xinling Du') return `images/specialist3.png`;
    if (username === 'Carrot') return `images/carrot.png`;
    return `https://api.dicebear.com/7.x/initials/svg?seed=${username}`;
}

// --- 2. 页面初始化 ---
$(document).ready(() => {
    if (checkLogin()) {
        initList();

        // 🌟 核心：页面一加载，就把右上角的用户名和头像替换成当前登录人的信息
        const currentUsername = localStorage.getItem('username') || 'User';
        $('#current-user-display').text(currentUsername);
        $('#nav-user-avatar').attr('src', getAvatar(currentUsername));
    }
});

function handleProtectedView(pageId) {
    if (checkLogin()) {
        showPage(pageId);
        if (pageId === 'user-orders-page') loadMyOrders(); // 切换到订单页时拉取数据
    }
}

function showJoinUs() {
    window.location.href = 'expert_apply.html';
}

// 🌟 修复：搜索功能触发逻辑
function handleSearch() {
    const keyword = $('#search-input').val().trim();
    initList(keyword);
}
// --- 3. 专家列表与详情 ---
function initList(keyword = '') {
    let url = `${API_BASE}/api/specialists`;
    if (keyword) url += `?keyword=${encodeURIComponent(keyword)}`;

    $.get(url, (res) => {
        const grid = $('#expert-grid').empty();
        const content = res.content || res || [];

        if (content.length === 0) {
            grid.append('<div class="col-12 text-center text-muted py-5"><h4>No experts found.</h4></div>');
            return;
        }

        content.forEach(item => {
            grid.append(`
                <div class="col-md-6">
                    <div class="expert-card shadow-sm" onclick="goToProfile(${item.id})">
                        <span class="badge bg-light text-primary rounded-pill mb-3 border" style="width: fit-content;">${item.level || 'EXPERT'}</span>
                        <div class="d-flex align-items-center gap-3 mb-4">
                            <img src="${getAvatar(item.user.username)}" style="width:60px; height:60px; border-radius:50%; object-fit: cover; border: 2px solid #0d6efd;">
                            <div>
                                <h3 class="fw-800 mb-0">${item.user.username}</h3>
                                <div class="text-muted small">${item.expertise ? item.expertise.name : 'Professional'}</div>
                            </div>
                        </div>
                        <div class="d-flex justify-content-between align-items-center">
                            <span class="fw-800">$${item.hourlyFee || 0}/hr</span>
                            <span class="text-primary fw-bold">View Detail &raquo;</span>
                        </div>
                    </div>
                </div>`);
        });
    });
}

function goToProfile(id) {
    selectedExpertId = id;
    $.get(`${API_BASE}/api/specialists/${id}`, (data) => {
        $('#pName').text(data.user.username);
        $('#pExpertise').text(data.expertise ? data.expertise.name : 'Consultant');
        $('#pRate').text('$' + (data.hourlyFee || 0));
        $('#pLevel').text(data.level || 'EXPERT');
        $('#pBio').text(data.resume || "No biography provided.");
        $('#pAvatar').attr('src', getAvatar(data.user.username));
        showPage('profile-page');
    });
}

// --- 4. 预约与日历逻辑 ---
function goToCalendar() {
    $.get(`${API_BASE}/api/timeslots/specialist/${selectedExpertId}/available-dates`, (data) => {
        const dates = Array.isArray(data) ? data : (data.availableDates || []);
        const body = $('#calendar-body').empty();

        ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT'].forEach(day => body.append(`<div class="cal-header-day">${day}</div>`));
        for (let i = 0; i < 3; i++) body.append(`<div></div>`); // 4月便宜量

        for (let i = 1; i <= 30; i++) {
            const dateStr = `2026-04-${i.toString().padStart(2, '0')}`;
            const isAvail = dates.includes(dateStr);
            body.append(`<div class="cal-date ${isAvail?'available':'empty'}" ${isAvail?`onclick="showSlots('${dateStr}')"`:''}>${i}</div>`);
        }
        showPage('calendar-page');
    });
}

function showSlots(dateStr) {
    $('#display-date').text(dateStr);
    const list = $('#slot-list').empty();
    $('#confirm-btn').hide();

    $.get(`${API_BASE}/api/timeslots/specialist/${selectedExpertId}/available-times?date=${dateStr}`, (res) => {
        const slots = Array.isArray(res) ? res : (res.timeSlots || []);
        slots.forEach(s => {
            list.append(`
                <div class="slot-card" id="slot-card-${s.id}" onclick="selectSlot(${s.id})">
                    <div class="slot-time">${s.startTime.substring(0, 5)} - ${s.endTime.substring(0, 5)}</div>
                    <div class="slot-status"><i class="bi bi-check2-circle me-1"></i>Available</div>
                </div>`);
        });
        showPage('timeslot-page');
    });
}

window.selectSlot = function(slotId) {
    currentSelectedSlotId = slotId;
    $('.slot-card').removeClass('selected');
    $(`#slot-card-${slotId}`).addClass('selected');
    $('#confirm-btn').fadeIn();
}

$('#confirm-btn').off('click').on('click', function() {
    // 1. 从详情页抓取当前专家的信息
    const specName = $('#pName').text();
    const specProf = $('#pExpertise').text();
    const avatarSrc = $('#pAvatar').attr('src');
    const dateStr = $('#display-date').text();

    // 抓取选中的时间段 (比如从 "12:00 - 15:00" 这个卡片上拿文本)
    const timeStr = $(`#slot-card-${currentSelectedSlotId} .slot-time`).text();

    // 2. 获取费率并计算总价 (假设按你参考图里的 Charon Coin 结算)
    // 这里的 replace 是为了把 "$888" 提取成数字 888
    const hourlyFee = parseFloat($('#pRate').text().replace('$', '')) || 0;

    // 简单计算一下时长：提取 12 和 15 算出差值 3 小时
    let hours = 1;
    try {
        const startH = parseInt(timeStr.split('-')[0].trim().split(':')[0]);
        const endH = parseInt(timeStr.split('-')[1].trim().split(':')[0]);
        hours = endH - startH;
    } catch(e) {}

    const totalFee = hourlyFee * hours;

    // 3. 把数据填入杜姐要求的弹窗里
    $('#confirm-avatar').attr('src', avatarSrc);
    $('#confirm-spec-name').text(specName);
    $('#confirm-spec-prof').text(specProf);
    $('#confirm-date').text(dateStr);
    $('#confirm-time').text(timeStr);
    $('#confirm-fee-rate').text(`${hourlyFee} Yuan`);
    $('#confirm-total-fee').text(`${totalFee} Yuan`);

    // 清空上次留下的备注
    $('#booking-notes').val('');

    // 4. 弹出收银台！
    $('#customerConfirmModal').modal('show');
});

// ==========================================
// 🌟 第二步：点击橘色按钮，真正发送请求给后端 (含并发处理)
// ==========================================
$('#final-submit-booking-btn').off('click').on('click', function() {
    const btn = $(this);
    const notes = $('#booking-notes').val().trim() || "Web Booking.";

    // 按钮变灰防止连点
    btn.prop('disabled', true).html('<span class="spinner-border spinner-border-sm me-2"></span>Processing...');

    fetch(`${API_BASE}/api/bookings/create`, {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${localStorage.getItem('token')}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            specialistId: selectedExpertId,
            slotId: currentSelectedSlotId,
            notes: notes // 把用户填写的备注传给杜姐
        })
    }).then(async res => {
        if (res.ok) {
            // ✅ 杜姐要求：成功后正中央提示，并跳回界面
            alert("Booking Confirmed Successfully!");
            $('#customerConfirmModal').modal('hide');
            handleProtectedView('user-orders-page'); // 跳去订单列表页
        } else {
            // ❌ 杜姐要求：并发情况，时段被抢走 (状态码 409)
            if (res.status === 409) {
                alert("该时段已被预约 (This time slot is already booked by someone else).");
                $('#customerConfirmModal').modal('hide');
                // 刷新时间块，变灰被抢走的格子
                showSlots($('#display-date').text());
            } else {
                // 其他后端错误（比如达到了每月限制等）
                const errData = await res.json().catch(()=>({}));
                alert(`Booking Failed: ${errData.message || 'Unknown error'}`);
            }
        }
    }).catch(err => {
        alert("Network Error: Could not connect to the server.");
    }).finally(() => {
        // 恢复橘色按钮状态
        btn.prop('disabled', false).text('Confirm Booking');
    });
});

// --- 5. 加载订单历史 ---
function loadMyOrders() {
    const list = $('#orders-list').empty().append('<p class="text-center py-5 text-muted">Fetching your bookings...</p>');

    $.get(`${API_BASE}/api/bookings/myOrders`, (orders) => {
        list.empty();
        if (!orders || orders.length === 0) {
            list.append('<div class="text-center py-5"><i class="bi bi-inbox fs-1 opacity-25"></i><h4 class="mt-3">No bookings found.</h4></div>');
            return;
        }

        orders.forEach(order => {
            let statusColor = order.status === 'CONFIRMED' ? 'text-success' : (order.status === 'CANCELLED' ? 'text-danger' : 'text-primary');
            const expName = order.specialist.user.username;

            // 🌟 细节 1：拼接完整的起止时间
            const timeString = `${order.timeSlot.startTime.substring(0,5)} - ${order.timeSlot.endTime.substring(0,5)}`;

            list.append(`
                <div class="booking-item-card shadow-sm d-flex justify-content-between align-items-center">
                    <div class="d-flex align-items-center gap-3">
                        <img src="${getAvatar(expName)}" class="rounded-circle" width="50" height="50" style="object-fit:cover;">
                        <div>
                            <h5 class="fw-800 mb-0">${expName}</h5>
                            <small class="text-muted"><i class="bi bi-calendar-event me-1"></i>${order.timeSlot.slotDate}</small>
                            <small class="text-muted ms-3"><i class="bi bi-clock me-1"></i>${timeString}</small>
                        </div>
                    </div>
                    
                    <div class="text-end d-flex flex-column align-items-end">
                        <div class="fw-bold ${statusColor} mb-1">${order.status}</div>
                        <small class="text-muted mb-2">Order ID: #${order.id}</small>
                        <button class="btn btn-sm btn-outline-dark rounded-pill px-3" style="font-size: 0.8rem; font-weight: 700;" onclick="openOrderDetail(${order.id})">
                            View Detail
                        </button>
                    </div>
                </div>`);
        });
    });
}

// 🌟 细节 3：详情功能的占位函数 (先弹个窗，以后咱们再慢慢往里面加具体的模态框)
window.openOrderDetail = function(orderId) {
    alert(`Loading details for Order #${orderId}...\n\n(Detail feature is under development!)`);
}

function showPage(id) {
    $('.page-view').hide();
    $(`#${id}`).fadeIn();
    window.scrollTo(0,0);
}
window.openChangePasswordModal = function() {
    // 1. 如果页面没这个弹窗，就动态插进去 (加入了第三个密码框)
    if ($('#changePasswordModal').length === 0) {
        $('body').append(`
            <div class="modal fade" id="changePasswordModal" tabindex="-1">
              <div class="modal-dialog modal-dialog-centered">
                <div class="modal-content rounded-4 border-0 shadow-lg">
                  <div class="modal-header border-bottom-0 pb-0 mt-3 px-4">
                    <h5 class="modal-title fw-800"><i class="bi bi-shield-lock text-primary me-2"></i>Change Password</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                  </div>
                  <div class="modal-body p-4">
                    <div class="form-floating mb-3">
                      <input type="password" class="form-control rounded-4" id="oldPassword" placeholder="Old Password">
                      <label for="oldPassword">Old Password</label>
                    </div>
                    <div class="form-floating mb-3">
                      <input type="password" class="form-control rounded-4" id="newPassword" placeholder="New Password">
                      <label for="newPassword">New Password</label>
                    </div>
                    <div class="form-floating mb-4">
                      <input type="password" class="form-control rounded-4" id="confirmNewPassword" placeholder="Confirm New Password">
                      <label for="confirmNewPassword">Confirm New Password</label>
                    </div>
                    <button class="btn btn-primary w-100 rounded-pill py-3 fw-bold shadow-sm" id="btn-submit-password">Update Password</button>
                  </div>
                </div>
              </div>
            </div>
        `);
    }

    // 每次打开弹窗前清空输入框
    $('#oldPassword').val('');
    $('#newPassword').val('');
    $('#confirmNewPassword').val(''); // 清空确认框
    $('#changePasswordModal').modal('show');

    // 绑定提交事件
    $('#btn-submit-password').off('click').on('click', function() {
        const oldPw = $('#oldPassword').val();
        const newPw = $('#newPassword').val();
        const confirmPw = $('#confirmNewPassword').val();

        // 🌟 校验 1：是否填完
        if(!oldPw || !newPw || !confirmPw) {
            alert("Please fill in all fields!");
            return;
        }

        // 🌟 校验 2：两次新密码是否一致
        if(newPw !== confirmPw) {
            alert("The new passwords do not match. Please try again!");
            return;
        }

        const btn = $(this);
        btn.prop('disabled', true).text('Updating...');

        fetch(`${API_BASE}/api/users/change-password`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${localStorage.getItem('token')}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ oldPassword: oldPw, newPassword: newPw })
        }).then(async res => {
            if(res.ok) {
                alert("Password updated successfully! Please login again with your new password.");
                $('#changePasswordModal').modal('hide');

                // 🌟 核心修改：不调 logout() 询问，直接清空 Token 并强制踢回 login.html
                localStorage.clear();
                window.location.href = 'login.html';
            } else {
                alert("Failed to update: " + await res.text());
            }
        }).catch(err => alert("Network error! Make sure the backend is running."))
            .finally(() => btn.prop('disabled', false).text('Update Password'));
    });
}