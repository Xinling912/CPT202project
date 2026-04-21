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
    if(confirm("Logout from SAS?")) {
        localStorage.clear();
        window.location.href = 'login.html';
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
    const btn = $(this);
    btn.prop('disabled', true).html('Processing...');

    fetch(`${API_BASE}/api/bookings/create`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}`, 'Content-Type': 'application/json' },
        body: JSON.stringify({ specialistId: selectedExpertId, slotId: currentSelectedSlotId, notes: "Web Booking." })
    })
        .then(async res => {
            if (res.ok) {
                alert("Reservation Successful!");
                handleProtectedView('user-orders-page');
            } else {
                const text = await res.text();
                alert("Failed to book: " + text);
            }
        })
        .finally(() => btn.prop('disabled', false).text('Confirm Reservation'));
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