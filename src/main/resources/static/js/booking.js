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

$('#final-submit-booking-btn').off('click').on('click', function() {
    const btn = $(this);
    const notes = $('#booking-notes').val().trim() || "Web Booking.";

    // 按钮变灰防止连点
    btn.prop('disabled', true).html('<span class="spinner-border spinner-border-sm me-2"></span>Processing...');

    // 🌟 这里把真正的 Token 和数据带上，后端绝对马上放行！
    fetch(`${API_BASE}/api/bookings/create`, {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${localStorage.getItem('token')}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            specialistId: selectedExpertId,
            slotId: currentSelectedSlotId,
            notes: notes
        })
    }).then(async res => {
        if (res.ok) {
            alert("Booking Confirmed Successfully!");
            $('#customerConfirmModal').modal('hide');
            handleProtectedView('user-orders-page');
        } else {
            // 🌟 获取后端的报错文本
            const errText = await res.text();

            if (res.status === 409) {
                alert("Action Failed: 你不能预定你取消的订单,谢谢.");
                $('#customerConfirmModal').modal('hide');
                showSlots($('#display-date').text());
            }
            // 🌟 核心拦截：捕获 500 数据库重复报错，进行人性化翻译！
            else if (res.status === 500 && (errText.includes('Duplicate entry') || errText.includes('Constraint'))) {
                alert("Action Failed: You have recently cancelled an appointment for this time slot. The system does not allow immediate re-booking of the same slot to prevent spam. Please choose another time!");
                $('#customerConfirmModal').modal('hide');
            }
            else {
                // 尝试解析其他正常的 JSON 报错 (比如每月限购)
                try {
                    const errData = JSON.parse(errText);
                    alert(`Booking Failed: ${errData.message}`);
                } catch(e) {
                    alert(`Booking Failed: ${errText}`);
                }
            }
        }
    }).catch(err => {
        alert("Network Error: Could not connect to the server.");
    }).finally(() => {
        btn.prop('disabled', false).text('Confirm Booking');
    });
});
// --- 5. 顾客端加载订单历史 ---
window.loadMyOrders = function() {
    const list = $('#orders-list').empty().append('<p class="text-center py-5 text-muted"><span class="spinner-border spinner-border-sm me-2"></span>Fetching your bookings...</p>');

    fetch(`${API_BASE}/api/bookings/myOrders`, {
        method: 'GET',
        headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
    })
        .then(async res => {
            if (!res.ok) throw new Error(await res.text());
            return res.json();
        })
        .then(orders => {
            list.empty();
            if (!orders || orders.length === 0) {
                list.append('<div class="text-center py-5"><i class="bi bi-inbox fs-1 opacity-25"></i><h4 class="mt-3">No bookings found.</h4></div>');
                return;
            }

            // 🌟 核心探雷针：打印后端传来的原始数据！ 
            console.log("后端返回的订单列表(没事帮会看f12的记录一下 不是报错)：", orders);
            window.currentOrders = orders;
            orders.forEach(order => {
                // 解析扁平化数据
                const expName = order.specialistName || 'Specialist';
                const dateStr = order.slotDate || order.date || order.timeSlotDate || order.bookingDate || 'Unknown Date';
                const startStr = order.startTime ? order.startTime.substring(0,5) : '--:--';
                const endStr = order.endTime ? order.endTime.substring(0,5) : '--:--';
                const timeString = `${startStr} - ${endStr}`;

                let statusColor = order.status === 'CONFIRMED' ? 'text-success' : (order.status === 'CANCELLED' || order.status === 'CANCELED' ? 'text-danger' : 'text-warning');



                // ==========================================
                // ==========================================
                let actionHtml = `<div class="mt-2 d-flex justify-content-end gap-2">`;

                // 1. 【先写 Cancel】：因为它在 Flex 容器里会靠左显示
                if (order.status === 'PENDING' || order.status === 'CONFIRMED') {
                    actionHtml += `<button class="btn btn-sm btn-outline-danger rounded-pill px-3 fw-bold" onclick="cancelCustomerOrder(${order.id})">Cancel Order</button>`;
                }

                // 2. 【后写 View Detail】：因为它在 Flex 容器里会排在右边，也就是最右侧
                actionHtml += `<button class="btn btn-sm btn-outline-primary rounded-pill px-3 fw-bold" onclick="openOrderDetail(${order.id})">View Detail</button>`;

                actionHtml += `</div>`;

                if (order.status === 'PENDING' || order.status === 'CONFIRMED') {
                    actionHtml = `<button class="btn btn-sm btn-outline-danger rounded-pill px-3 mt-2 fw-bold" onclick="cancelCustomerOrder(${order.id})">Cancel Order</button>`;
                }


                const notesHtml = order.notes
                    ? `<div class="small mt-2 bg-light p-2 rounded text-secondary" style="max-width: 400px;">
                     <i class="bi bi-chat-left-text me-1"></i><span class="fst-italic">${order.notes}</span>
                   </div>`
                    : '';

                list.append(`
                <div class="booking-item-card shadow-sm d-flex justify-content-between align-items-start p-4 mb-3 border rounded-4 bg-white">
                    <div class="d-flex align-items-start gap-3">
                        <img src="${getAvatar(expName)}" class="rounded-circle shadow-sm" width="55" height="55" style="object-fit:cover; border: 2px solid var(--brand-light);">
                        <div>
                            <h5 class="fw-800 mb-1">${expName}</h5>
                            <div class="mb-1">
                                <small class="text-muted"><i class="bi bi-calendar-event me-1"></i>${dateStr}</small>
                                <small class="text-muted ms-3"><i class="bi bi-clock me-1"></i>${timeString}</small>
                            </div>
                            ${notesHtml}
                        </div>
                    </div>
                    
                    <div class="text-end d-flex flex-column align-items-end">
                        <div class="fw-900 ${statusColor} mb-1" style="font-size: 1.1rem;">${order.status}</div>
                        <small class="text-muted mb-1">Order ID: #${order.id}</small>
                        ${actionHtml}
                    </div>
                </div>`);
            });
        })
        .catch(err => {
            list.html(`<div class="text-center text-danger py-5"><h5 class="mt-3">Failed to load orders</h5><p class="small text-muted">${err.message}</p></div>`);
        });
}

window.cancelCustomerOrder = function(orderId) {
    const reason = prompt("Please enter a reason for cancelling your appointment:");
    if (reason === null) return;

    fetch(`${API_BASE}/api/bookings/cancel/${orderId}?reason=${encodeURIComponent(reason || "Customer cancelled")}`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
    }).then(async res => {
        if(res.ok) {
            alert("Order cancelled successfully!");
            loadMyOrders();
        } else {
            alert("Failed to cancel: " + await res.text());
        }
    }).catch(err => alert("Network Error."));
}


window.openOrderDetail = function(orderId) {
    alert(`Loading details for Order #${orderId}...\n\n(Detail feature is under development!)`);
}

function showPage(id) {
    $('.page-view').hide();
    $(`#${id}`).fadeIn();
    window.scrollTo(0,0);
}
window.openChangePasswordModal = function() {

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


    $('#oldPassword').val('');
    $('#newPassword').val('');
    $('#confirmNewPassword').val('');
    $('#changePasswordModal').modal('show');


    $('#btn-submit-password').off('click').on('click', function() {
        const oldPw = $('#oldPassword').val();
        const newPw = $('#newPassword').val();
        const confirmPw = $('#confirmNewPassword').val();


        if(!oldPw || !newPw || !confirmPw) {
            alert("Please fill in all fields!");
            return;
        }


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