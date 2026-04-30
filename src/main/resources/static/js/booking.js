/**
 * User Booking Logic
 * Specialist Administration System (SAS)
 */

const API_BASE = "http://localhost:8080";
let selectedExpertId = null;
let currentSelectedSlotId = null;
// ==========================================
//报错弹窗武器库 因为报错弹窗太多了 懒得一个一个找了 带给他们梅花一下吧
// ==========================================


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

// 1. 加上 async
async function logout() {
    // 2. 换成高级弹窗并 await
    const isConfirmed = await showConfirm("Are you sure you want to log out?");

    // 3. 如果确认了，执行清理和跳转
    if (isConfirmed) {
        localStorage.clear(); // 清除 Token
        window.location.href = 'landingpage.html';
    }
}

// --- 2. 页面初始化 ---
$(document).ready(() => {
    if (checkLogin()) {
        initList();
        loadFilterData();//新增：页面一加载，就去后端拉取真实的专业和等级数据！
        //  核心：页面一加载，就把右上角的用户名和头像替换成当前登录人的信息
        const currentUsername = localStorage.getItem('username') || 'User';
        $('#current-user-display').text(currentUsername);
        $('#nav-user-avatar').attr('src', getAvatar(currentUsername));
    }
});
function loadFilterData() {
    $.get(`${API_BASE}/api/specialists/filters`, (res) => {
        // 1. 动态渲染 Category 下拉框
        const categorySelect = $('#search-category');
        if (res.expertises && res.expertises.length > 0) {
            res.expertises.forEach(exp => {
                categorySelect.append(`<option value="${exp.id}">${exp.name}</option>`);
            });
        }

        // 2. 动态渲染 Level 下拉框
        const levelSelect = $('#search-level');
        if (res.levels && res.levels.length > 0) {
            res.levels.forEach(lvl => {
                // 做一个人性化的文本转换
                let displayLvl = lvl === 'JUNIOR' ? 'Junior Specialist' :
                    lvl === 'SENIOR' ? 'Senior Specialist' :
                        lvl === 'EXPERT' ? 'Expert Specialist' : lvl;
                levelSelect.append(`<option value="${lvl}">${displayLvl}</option>`);
            });
        }
    }).fail(() => {
        console.error("Failed to load filter options from database.");
    });
}

// --- 搜索触发逻辑 ---
function handleSearch() {
    const keyword = $('#search-input').val().trim();
    const categoryId = $('#search-category').val(); // 拿到的绝对是真实的 ID
    const level = $('#search-level').val();         // 拿到的绝对是 JUNIOR/SENIOR/EXPERT

    initList(keyword, categoryId, level);
}

// --- 带参数发送给的后端接口 ---
function initList(keyword = '', categoryId = '', level = '') {
    let url = `${API_BASE}/api/specialists`;
    let queryParams = [];

    if (keyword) queryParams.push(`keyword=${encodeURIComponent(keyword)}`);
    if (categoryId) queryParams.push(`expertiseId=${categoryId}`);
    if (level) queryParams.push(`level=${level}`);

    if (queryParams.length > 0) {
        url += '?' + queryParams.join('&');
    }

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
                         <span class="fw-800 text-primary">${item.hourlyFee || 0} Yuan/hour</span>
                            <span class="text-primary fw-bold">View Detail &raquo;</span>
                        </div>
                    </div>
                </div>`);
        });
    });
}
function handleProtectedView(pageId) {
    if (checkLogin()) {
        showPage(pageId);
        if (pageId === 'user-orders-page') loadMyOrders(); // 切换到订单页时拉取数据
    }
}

function showJoinUs() {
    window.location.href = 'expert_apply.html';
}

//  修复：搜索功能触发逻辑
function handleSearch() {
    // 抓取输入框的名字
    const keyword = $('#search-input').val().trim();

    // 抓取下拉框的分类 ID (确保你 HTML 里分类下拉框的 ID 叫 search-category)
    const categoryId = $('#search-category').val();

    // 抓取下拉框的等级 (确保你 HTML 里等级下拉框的 ID 叫 search-level)
    const level = $('#search-level').val();

    // 把这三个参数一起传给 initList
    initList(keyword, categoryId, level);
}
function initList(keyword = '', categoryId = '', level = '') {
    let url = `${API_BASE}/api/specialists`;
    let queryParams = [];

    // 1. 如果有关键字，加进去
    if (keyword) {
        queryParams.push(`keyword=${encodeURIComponent(keyword)}`);
    }
    // 2. 如果选了分类且不是“全部分类/空”，把分类 ID 传给 expertiseId
    if (categoryId && categoryId !== '' && categoryId !== 'ALL') {
        queryParams.push(`expertiseId=${categoryId}`);
    }
    // 3. 如果选了等级且不是“全部等级/空”，把等级传给 level
    if (level && level !== '' && level !== 'ALL') {
        queryParams.push(`level=${level}`);
    }

    // 智能拼接 URL
    if (queryParams.length > 0) {
        url += '?' + queryParams.join('&');
    }

    console.log("打印表演一下小杜的超强搜索功能 哈哈 调侃一下 URL:", url); // 你可以按 F12 看看这句打印

    // 下面的 $.get 渲染逻辑完全不用动！保持你原来的样子！
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
                         <span class="fw-800 text-primary">${item.hourlyFee || 0} Yuan/hour</span>
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
        $('#pRate').text((data.hourlyFee || 0) + ' Yuan/hour');
        $('#pLevel').text(data.level || 'EXPERT');
        $('#pBio').text(data.resume || "No biography provided.");
        $('#pAvatar').attr('src', getAvatar(data.user.username));
        showPage('profile-page');
    });
}

// --- 4. 预约与日历逻辑 (全新动态双月升级版 + 防穿透提取) ---
function goToCalendar() {
    $.get(`${API_BASE}/api/timeslots/specialist/${selectedExpertId}/available-dates`, (data) => {

        //  核心破案 1：完美兼容后端 {"data": [...]} 的包装盒！
        let dates = [];
        if (Array.isArray(data)) dates = data;
        else if (data.data && Array.isArray(data.data)) dates = data.data;
        else if (data.availableDates) dates = data.availableDates;

        const body = $('#calendar-body').empty();

        // 1. 渲染星期表头
        ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT'].forEach(day => body.append(`<div class="cal-header-day">${day}</div>`));

        // 2. 获取当前系统真实的年和月
        const today = new Date();
        let currentYear = today.getFullYear();
        let currentMonth = today.getMonth();

        //  3. 核心魔法：循环渲染 2 个月（本月 m=0，下个月 m=1）
        for (let m = 0; m < 2; m++) {
            let renderMonth = currentMonth + m;
            let renderYear = currentYear;

            // 自动进位跨年逻辑
            if (renderMonth > 11) {
                renderMonth -= 12;
                renderYear += 1;
            }

            const firstDay = new Date(renderYear, renderMonth, 1).getDay();
            const daysInMonth = new Date(renderYear, renderMonth + 1, 0).getDate();

            // 插入绚丽的月份分割线
            body.append(`
                <div style="grid-column: span 7; text-align: center; font-weight: 800; color: #3b82f6; margin-top: 15px; margin-bottom: 5px; font-size: 1.1rem;">
                    ${renderYear} - ${String(renderMonth + 1).padStart(2, '0')}
                </div>
            `);

            // 渲染1号之前的空白占位符
            for (let i = 0; i < firstDay; i++) {
                body.append(`<div></div>`);
            }

            // 渲染这一个月的真实格子
            for (let i = 1; i <= daysInMonth; i++) {
                const monthStr = String(renderMonth + 1).padStart(2, '0');
                const dayStr = String(i).padStart(2, '0');
                const dateStr = `${renderYear}-${monthStr}-${dayStr}`;

                //  模糊匹配：不管后端给的日期带不带时间戳，只要开头匹配就亮起！
                const isAvail = JSON.stringify(dates).includes(dateStr);

                body.append(`
                    <div class="cal-date ${isAvail ? 'available text-primary fw-bolder shadow-sm border border-primary' : 'empty'}" 
                         ${isAvail ? `onclick="showSlots('${dateStr}')"` : ''}>
                        ${i}
                    </div>
                `);
            }
        }

        showPage('calendar-page');
    });
}

function showSlots(dateStr) {
    $('#display-date').text(dateStr);
    const list = $('#slot-list').empty();
    $('#confirm-btn').hide();

    $.get(`${API_BASE}/api/timeslots/specialist/${selectedExpertId}/available-times?date=${dateStr}`, (res) => {

        //  核心破案 2：同样兼容时间段接口的 JSON 包装盒！
        let slots = [];
        if (Array.isArray(res)) slots = res;
        else if (res.data && Array.isArray(res.data)) slots = res.data;
        else if (res.timeSlots) slots = res.timeSlots;

        //  核心破案 3：智能时间提取器 (专治 "2026-05-05 09:00:00" 切割乱码)
        const safeExtractTime = (timeStr) => {
            if (!timeStr) return '--:--';
            if (timeStr.includes('T')) return timeStr.split('T')[1].substring(0, 5);
            if (timeStr.includes(' ')) return timeStr.split(' ')[1].substring(0, 5);
            return timeStr.substring(0, 5); // 兜底正常的 "09:00:00"
        };

        if (slots.length === 0) {
            list.append('<div class="text-muted text-center w-100 py-3 fw-bold">No available slots for this date.</div>');
            return;
        }

        slots.forEach(s => {
            const startTime = safeExtractTime(s.startTime);
            const endTime = safeExtractTime(s.endTime);

            list.append(`
                <div class="slot-card" id="slot-card-${s.id}" onclick="selectSlot(${s.id})">
                    <div class="slot-time fw-bold">${startTime} - ${endTime}</div>
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
    const hourlyFee = parseFloat($('#pRate').text().replace(/[^0-9.]/g, '')) || 0;

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

    //  这里把真正的 Token 和数据带上，后端绝对马上放行！
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
            // 获取后端的报错文本
            const errText = await res.text();

            if (res.status === 409) {
                alert("Action Failed:Sorry, this schedule has been reserved by others.");
                $('#customerConfirmModal').modal('hide');
                showSlots($('#display-date').text());
            }
            //  核心拦截：捕获 500 数据库重复报错，进行人性化翻译！
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

window.loadMyOrders = function() {
    const list = $('#orders-list').empty().append('<p class="text-center py-5 text-muted">Fetching your bookings...</p>');

    fetch(`${API_BASE}/api/bookings/myOrders`, {
        method: 'GET',
        headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
    })
        .then(res => res.json())
        .then(orders => {
            list.empty();
            if (!orders || orders.length === 0) {
                list.append('<div class="text-center py-5"><h4>No bookings found.</h4></div>');
                return;
            }

            orders.forEach(order => {
                const expName = order.specialistName || 'Specialist';
                const dateStr = order.slotDate || order.date || 'Unknown Date';
                const timeString = `${order.startTime ? order.startTime.substring(0,5) : '--'} - ${order.endTime ? order.endTime.substring(0,5) : '--'}`;

                let statusColor = 'text-warning';
                if(order.status === 'CONFIRMED' || order.status === 'COMPLETED') statusColor = 'text-success';
                if(order.status === 'CANCELLED' || order.status === 'CANCELED') statusColor = 'text-danger';


                let actionHtml = `<div class="d-flex gap-2 mt-2">`;


                if (order.status === 'PENDING' || order.status === 'CONFIRMED') {
                    actionHtml += `<button class="btn btn-sm btn-outline-danger rounded-pill px-3 fw-bold" onclick="cancelCustomerOrder(${order.id})">Cancel Order</button>`;
                }


                if (order.status === 'COMPLETED') {
                    actionHtml += `<button class="btn btn-sm btn-outline-danger rounded-pill px-3 fw-bold" onclick="openReportModal(${order.id}, '${expName}')">Report</button>`;
                }


                actionHtml += `<button class="btn btn-sm btn-outline-primary rounded-pill px-3 fw-bold" onclick="openOrderDetail(${order.id})">View Detail</button>`;
                actionHtml += `</div>`;

                list.append(`
                <div class="booking-item-card shadow-sm d-flex justify-content-between align-items-start p-4 mb-3 border rounded-4 bg-white">
                    <div class="d-flex align-items-start gap-3">
                        <img src="${getAvatar(expName)}" class="rounded-circle shadow-sm" width="55" height="55" style="object-fit:cover; border: 2px solid var(--brand-light);">
                        <div>
                            <h5 class="fw-800 mb-1">${expName}</h5>
                            <div class="mb-1 small text-muted">
                                <i class="bi bi-calendar-event me-1"></i>${dateStr}
                                <i class="bi bi-clock ms-3 me-1"></i>${timeString}
                            </div>
                            <div class="small text-secondary fst-italic mt-1">Web Booking.</div>
                        </div>
                    </div>
                    <div class="text-end d-flex flex-column align-items-end">
                        <div class="fw-900 ${statusColor} mb-1" style="font-size: 1.1rem;">${order.status}</div>
                        <small class="text-muted mb-1">Order ID: #${order.id}</small>
                        ${actionHtml}
                    </div>
                </div>`);
            });
        });
};

// --- 举报逻辑：弹出 Modal ---
let currentReportBookingId = null;
window.openReportModal = function(bookingId, specName) {
    currentReportBookingId = bookingId;
    $('#report-order-id').text('#' + bookingId);
    $('#report-spec-name').text(specName);
    $('#report-reason').val('');
    $('#reportModal').modal('show');
}

window.submitFinalReport = function() {
    const reason = $('#report-reason').val().trim();
    if (!reason) return alert("Please enter a reason.");

    const btn = $('#submit-report-btn');
    //  记录原始状态，防止卡死
    btn.prop('disabled', true).text('Submitting...');

    fetch(`${API_BASE}/api/complaints/report?bookingId=${currentReportBookingId}&reason=${encodeURIComponent(reason)}`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
    })
        .then(async res => {
            const resData = await res.json();
            if (res.ok) {
                alert("Report submitted! Admin will review it soon.");
                $('#reportModal').modal('hide');
                loadMyOrders();
            } else {
                //  重点：如果是 400 (如重复举报)，直接显示后端给的中文错误
                alert("Action Failed: " + (resData.error || "Submit error"));
            }
        })
        .catch(err => {
            console.error(err);
            alert("Network Error: Backend is down or unreachable.");
        })
        .finally(() => {
            //  无论成功失败，都把按钮还给人家
            btn.prop('disabled', false).text('Submit Report');
        });
};
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

                //  核心修改：不调 logout() 询问，直接清空 Token 并强制踢回 login.html
                localStorage.clear();
                window.location.href = 'login.html';
            } else {
                alert("Failed to update: " + await res.text());
            }
        }).catch(err => alert("Network error! Make sure the backend is running."))
            .finally(() => btn.prop('disabled', false).text('Update Password'));
    });
}