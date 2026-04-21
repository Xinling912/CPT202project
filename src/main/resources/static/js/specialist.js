/**
 * Master Logic for Specialist Workspace
 */

const API_BASE = "http://localhost:8080";

let currentWeekOffset = 0;
let currentWeekDates = {};
let currentFetchedSchedule = [];

// 🌟 全明星头像匹配逻辑
function getAvatar(username) {
    if (username === 'ShenShaohui' || username === 'Shaohui Shen') return `images/ssh.jpg`;
    if (username === 'XingjianWu' || username === 'Xingjian Wu') return `images/specialist1.png`;
    if (username === 'XinlingDu' || username === 'Xinling Du') return `images/specialist3.png`;
    if (username === 'Carrot') return `images/carrot.png`;
    return `https://api.dicebear.com/7.x/initials/svg?seed=${username}`;
}

$('#toggle-sidebar').on('click', function() {
    $('#sidebar').toggleClass('collapsed');
    $('.main-content').toggleClass('expanded');
});

function switchTab(tabId, btn = null) {
    $('.tab-content').removeClass('active-tab');
    $('.nav-link').removeClass('active');
    const target = $('#' + tabId);
    if (target.length) target.addClass('active-tab');
    if (btn) {
        $(btn).addClass('active');
    } else {
        const matchingLink = $(`.nav-link[onclick*="'${tabId}'"]`);
        if(matchingLink.length > 0) matchingLink.addClass('active');
    }
    window.scrollTo({ top: 0, behavior: 'instant' });
}

function logout() {
    if(confirm("Are you sure you want to log out?")) {
        localStorage.clear();
        window.location.href = 'landingpage.html';
    }
}

// 🌟 升级：给专家的订单列表加上 Confirm / Reject / Cancel / Complete 按钮 (扁平化 DTO 适配版)
window.loadSpecialistOrders = function() {
    const list = $('#specialist-orders-list').empty().append('<p class="text-muted py-4 text-center">Loading orders...</p>');

    fetch(`${API_BASE}/api/bookings/specialist/my-bookings`, {
        method: 'GET',
        headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
    })
        .then(async res => {
            if (!res.ok) throw new Error(await res.text());
            return res.json();
        })
        .then(orders => {
            list.empty();
            if(!orders || orders.length === 0) {
                list.append('<div class="text-center py-5 text-muted"><i class="bi bi-inbox fs-1"></i><p class="mt-2">No bookings found yet.</p></div>');
                return;
            }

            orders.forEach(o => {
                // 🌟 核心防雷：兼容所有后端可能返回的名字 (DTO 或 嵌套)
                const custName = o.customerName || (o.customer && o.customer.username) || 'Customer';
                const dateStr = o.slotDate || o.date || (o.timeSlot && o.timeSlot.slotDate) || 'Unknown Date';
                const startStr = o.startTime ? o.startTime.substring(0,5) : (o.timeSlot && o.timeSlot.startTime ? o.timeSlot.startTime.substring(0,5) : '--:--');
                const endStr = o.endTime ? o.endTime.substring(0,5) : (o.timeSlot && o.timeSlot.endTime ? o.timeSlot.endTime.substring(0,5) : '--:--');

                const timeStr = `${dateStr} | ${startStr} - ${endStr}`;
                let statusBadge = o.status === 'CONFIRMED' ? 'text-success' : (o.status === 'CANCELLED' || o.status === 'CANCELED' ? 'text-danger' : 'text-warning');

                // 🌟 动态渲染操作按钮
                let actionHtml = '';
                if (o.status === 'PENDING') {
                    actionHtml = `
                    <div class="mt-3 d-flex gap-2 justify-content-end border-top pt-3">
                        <button class="btn btn-sm btn-light text-danger border rounded-pill px-4 fw-bold" onclick="handleAptAction(${o.id}, 'cancel', true)">Reject</button>
                        <button class="btn btn-sm btn-primary rounded-pill px-4 fw-bold" onclick="handleAptAction(${o.id}, 'confirm')">Confirm</button>
                    </div>
                `;
                } else if (o.status === 'CONFIRMED') {
                    actionHtml = `
                    <div class="mt-3 d-flex gap-2 justify-content-end border-top pt-3">
                        <button class="btn btn-sm btn-light text-danger border rounded-pill px-4 fw-bold" onclick="handleAptAction(${o.id}, 'cancel', true)">Cancel</button>
                        <button class="btn btn-sm btn-success rounded-pill px-4 fw-bold" onclick="handleAptAction(${o.id}, 'complete')">Complete</button>
                    </div>
                `;
                }

                list.append(`
                <div class="border rounded-4 p-4 mb-3 shadow-sm bg-white">
                    <div class="d-flex justify-content-between align-items-start">
                        <div class="d-flex align-items-center gap-3">
                            <img src="${getAvatar(custName)}" class="rounded-circle shadow-sm" width="55" height="55" style="object-fit:cover; border: 2px solid var(--brand-light);">
                            <div>
                                <h5 class="fw-bold mb-1">Customer: <span class="text-primary">${custName}</span></h5>
                                <div class="text-muted small"><i class="bi bi-clock me-1"></i>${timeStr}</div>
                                ${o.notes ? `<div class="small mt-2 bg-light p-2 rounded text-secondary"><i class="bi bi-chat-left-text me-1"></i>Notes: ${o.notes}</div>` : ''}
                            </div>
                        </div>
                        <div class="text-end">
                            <div class="fw-900 ${statusBadge} mb-1" style="font-size: 1.1rem;">${o.status}</div>
                            <div class="small text-muted">Order ID: #${o.id}</div>
                        </div>
                    </div>
                    ${actionHtml}
                </div>
            `);
            });
        })
        .catch(err => {
            console.error(err);
            list.html('<p class="text-danger text-center py-4">Failed to load orders. Please try again.</p>');
        });
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

function getWeekDates(offset = 0) {
    const now = new Date();
    const dayOfWeek = now.getDay() === 0 ? 7 : now.getDay();
    const monday = new Date(now);
    monday.setDate(now.getDate() - dayOfWeek + 1 + (offset * 7));

    const days = ['mon', 'tue', 'wed', 'thu', 'fri', 'sat', 'sun'];
    const weekDates = {};
    days.forEach((day, index) => {
        const date = new Date(monday);
        date.setDate(monday.getDate() + index);
        const yyyy = date.getFullYear();
        const mm = String(date.getMonth() + 1).padStart(2, '0');
        const dd = String(date.getDate()).padStart(2, '0');
        weekDates[day] = `${yyyy}-${mm}-${dd}`;
    });
    return weekDates;
}

function getDayFromDate(dateString, weekDatesObj) {
    for(let day in weekDatesObj) {
        if(weekDatesObj[day] === dateString) return day;
    }
    return null;
}

function startRealTimeClock() {
    if ($('#realtime-clock-display').length === 0) {
        $('.stat-card').first().prepend(
            `<div id="realtime-clock-display" class="fw-900 text-primary mb-3" style="font-size:1.1rem; letter-spacing:0.5px;"></div>`
        );
    }
    setInterval(() => {
        const now = new Date();
        const yyyy = now.getFullYear();
        const mm = String(now.getMonth() + 1).padStart(2, '0');
        const dd = String(now.getDate()).padStart(2, '0');
        const hh = String(now.getHours()).padStart(2, '0');
        const min = String(now.getMinutes()).padStart(2, '0');
        const sec = String(now.getSeconds()).padStart(2, '0');
        $('#realtime-clock-display').html(`<i class="bi bi-clock-history me-2"></i>Current System Time: ${yyyy}-${mm}-${dd} ${hh}:${min}:${sec}`);
    }, 1000);
}

function updateCalendarHeaders() {
    const days = ['mon', 'tue', 'wed', 'thu', 'fri', 'sat', 'sun'];
    const dayNames = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN'];

    let headerHtml = `<div style="width: 80px;" class="d-flex align-items-center justify-content-center">TIME</div>`;
    days.forEach((day, index) => {
        const fullDate = currentWeekDates[day];
        const shortDate = fullDate.substring(5);
        headerHtml += `<div class="flex-grow-1">${dayNames[index]}<span class="header-date-sub">${shortDate}</span></div>`;
    });

    $('.calendar-header').html(headerHtml);
    const rangeText = `${currentWeekDates['mon']} ~ ${currentWeekDates['sun']}`;
    $('#wb-status').text(`Manage your appointments. Selected Range: [ ${rangeText} ]`);
}

function loadMySchedule() {
    const token = localStorage.getItem('token');
    currentWeekDates = getWeekDates(currentWeekOffset);
    const startDate = currentWeekDates['mon'];
    const endDate = currentWeekDates['sun'];

    updateCalendarHeaders();

    const timeAxis = $('#wb-time-axis').empty();
    for(let h = 8; h <= 18; h++) {
        timeAxis.append(`<div class="time-slot-label">${h}:00</div>`);
    }

    fetch(`${API_BASE}/api/timeslots/my-schedule?startDate=${startDate}&endDate=${endDate}`, {
        headers: { 'Authorization': `Bearer ${token}` }
    })
        .then(res => res.json())
        .then(data => {
            currentFetchedSchedule = data.flatSchedule || [];
            renderRealSchedule(currentFetchedSchedule);
        })
        .catch(err => console.error("Failed to load schedule:", err));
}

// 🌟 修复：渲染日历方块，增加对 DISABLED 状态的支持
// 🌟 满足需求：把 DISABLED 画成红色的 Cancelled
function renderRealSchedule(flatSchedule) {
    $('.day-column').empty();
    const START_HOUR = 8;
    const HOUR_HEIGHT = 80;

    flatSchedule.forEach(slot => {
        const startH = parseInt(slot.startTime.split(':')[0]);
        const endH = parseInt(slot.endTime.split(':')[0]);
        const dayKey = getDayFromDate(slot.slotDate, currentWeekDates);

        if(!dayKey || startH < 8 || endH > 18) return;

        const topPx = (startH - START_HOUR) * HOUR_HEIGHT;
        const heightPx = (endH - startH) * HOUR_HEIGHT;

        let cardType = 'card-vacant';
        let title = 'Vacant';
        let displayStatus = slot.timeSlotStatus;
        let bookingId = slot.bookingId || slot.id;

        if (slot.timeSlotStatus === 'BOOKED') {
            title = slot.customerUsername || 'Customer';
            displayStatus = slot.bookingStatus || 'BOOKED';
            if (displayStatus === 'PENDING') cardType = 'card-pending';
            if (displayStatus === 'CONFIRMED') cardType = 'card-confirmed';
            if (displayStatus === 'CANCELED' || displayStatus === 'CANCELLED') cardType = 'card-canceled';
        } else if (slot.timeSlotStatus === 'DISABLED') {
            // 🌟 核心魔法：只要是 DISABLED，统统穿上红色的衣服，标上 Cancelled！
            cardType = 'card-canceled';
            title = 'Cancelled';
            displayStatus = 'DISABLED';
        }

        const cardHtml = `
            <div class="apt-card ${cardType}" style="top: ${topPx}px; height: ${heightPx}px;" onclick="openAptDetails(${bookingId}, '${displayStatus}', '${title}', '${startH}:00 - ${endH}:00')">
                <div class="apt-title">${title}</div>
                <div class="apt-time">${startH}:00 - ${endH}:00</div>
                <div class="apt-status">${displayStatus}</div>
            </div>
        `;
        $(`.day-column[data-day="${dayKey}"]`).append(cardHtml);
    });
}
window.openAptDetails = function(bookingId, status, customerName, timeStr) {
    if (status === 'PENDING' || status === 'CONFIRMED') {
        if ($('#aptActionModal').length === 0) {
            $('body').append(`
                <div class="modal fade" id="aptActionModal" tabindex="-1">
                  <div class="modal-dialog modal-dialog-centered modal-sm">
                    <div class="modal-content rounded-4 border-0 shadow-lg">
                      <div class="modal-header border-0 pb-0">
                        <h5 class="modal-title fw-800">Booking Management</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                      </div>
                      <div class="modal-body text-center pb-4 pt-2">
                        <div class="mb-3 mt-2">
                            <div class="text-muted small text-uppercase fw-bold">Customer</div>
                            <h3 class="fw-900 text-primary" id="modal-customer-name">--</h3>
                        </div>
                        <div class="mb-4">
                            <div class="text-muted small text-uppercase fw-bold">Time Slot</div>
                            <h5 class="fw-bold" id="modal-apt-time">--</h5>
                        </div>
                        <div class="d-flex gap-3 mt-4 px-2" id="modal-action-buttons"></div>
                      </div>
                    </div>
                  </div>
                </div>
            `);
        }

        $('#modal-customer-name').text(customerName);
        $('#modal-apt-time').text(timeStr);

        let buttonsHtml = '';
        if (status === 'PENDING') {
            buttonsHtml = `
                <button class="btn btn-light flex-grow-1 rounded-pill fw-bold text-danger border" onclick="handleAptAction(${bookingId}, 'cancel', true)">Reject</button>
                <button class="btn btn-primary flex-grow-1 rounded-pill fw-bold" onclick="handleAptAction(${bookingId}, 'confirm')">Confirm</button>
            `;
        } else if (status === 'CONFIRMED') {
            buttonsHtml = `
                <button class="btn btn-light flex-grow-1 rounded-pill fw-bold text-danger border" onclick="handleAptAction(${bookingId}, 'cancel', true)">Cancel</button>
                <button class="btn btn-success flex-grow-1 rounded-pill fw-bold" onclick="handleAptAction(${bookingId}, 'complete')">Complete</button>
            `;
        }
        $('#modal-action-buttons').html(buttonsHtml);
        $('#aptActionModal').modal('show');

    } else {
        alert(`This time slot is currently [ ${status} ]. No further actions available.`);
    }
}

// 🌟 优化版：带报错翻译的订单操作逻辑
window.handleAptAction = function(bookingId, action, requiresReason = false) {
    let reason = "";
    if (requiresReason) {
        reason = prompt("Please enter a reason for this cancellation/rejection:");
        if (reason === null) return;
    }

    let url = `${API_BASE}/api/bookings/${action}/${bookingId}`;
    if (action === 'cancel') url += `?reason=${encodeURIComponent(reason || "Specialist action")}`;

    fetch(url, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
    }).then(async res => {
        if(res.ok) {
            alert(`Order successfully updated!`);
            $('#aptActionModal').modal('hide');
            loadMySchedule();
            if ($('#all-bookings').hasClass('active-tab')) {
                loadSpecialistOrders();
            }
        } else {
            // 🌟 核心优化：解析后端的 JSON 报错，并进行“人性化翻译”
            const errorText = await res.text();
            let errorMessage = errorText;

            try {
                // 尝试把后端的字符串解析成 JSON 对象
                const errorJson = JSON.parse(errorText);
                if (errorJson.message) {
                    errorMessage = errorJson.message;
                }
            } catch (e) {} // 如果解析失败，就用原字符串

            // 🎯 针对杜姐特定的错误码，给专家弹大白话提示！
            if (errorMessage === 'ERROR_BOOKING_NOT_STARTED_YET') {
                alert("Action Failed: You cannot mark this booking as complete because the appointment time hasn't started yet!");
            } else if (errorMessage.includes('24 hours')) {
                alert("Action Failed: Please cancel at least 24 hours in advance.");
            } else {
                alert("Action Failed: " + errorMessage); // 兜底提示
            }
        }
    }).catch(err => alert("Network Error."));
}

let isDrag = false, startH = null, curDay = null;
let cacheData = { mon: [], tue: [], wed: [], thu: [], fri: [], sat: [], sun: [] };
function renderBlocks() {
    $('.drag-block').remove();
    for(let d in cacheData) {
        cacheData[d].forEach((b, i) => {
            const col = $(`.drag-col[data-day="${d}"]`);
            const h = (b.e - b.s) * 80;

            if (b.locked) {
                if (b.isBooked) {
                    let bgClass = 'bg-secondary';
                    if (b.statusTitle === 'PENDING') bgClass = 'bg-warning';
                    if (b.statusTitle === 'CONFIRMED') bgClass = 'bg-success';

                    col.find(`[data-hour="${b.s}"]`).append(`
                        <div class="drag-block ${bgClass} text-white border-0 shadow-sm" style="height:${h-4}px; opacity: 0.95; cursor: not-allowed;" title="Already Booked">
                            <i class="bi bi-person-check-fill mb-1"></i>
                            ${b.statusTitle} ${b.s}:00-${b.e}:00
                        </div>`);
                } else if (b.isDisabled) {
                    // 🌟 核心魔法：使用 Bootstrap 自带的 bg-danger 让它变红！
                    col.find(`[data-hour="${b.s}"]`).append(`
                        <div class="drag-block bg-danger text-white border-0 shadow-sm" style="height:${h-4}px; opacity: 0.9; cursor: not-allowed;" title="Cancelled by Specialist">
                            <i class="bi bi-x-octagon-fill mb-1"></i>
                            Cancelled ${b.s}:00-${b.e}:00
                        </div>`);
                } else {
                    col.find(`[data-hour="${b.s}"]`).append(`
                        <div class="drag-block drag-block-locked" style="height:${h-4}px" title="Within 24 hours. Cannot modify.">
                            <i class="bi bi-lock-fill mb-1"></i>
                            Locked ${b.s}:00-${b.e}:00
                        </div>`);
                }
            } else {
                col.find(`[data-hour="${b.s}"]`).append(`
                    <div class="drag-block" style="height:${h-4}px">
                        Vacant ${b.s}:00-${b.e}:00
                        <i class="bi bi-x-circle-fill drag-del-btn" onclick="removeB(event, '${d}', ${i})"></i>
                    </div>`);
            }
        });
    }
}
window.removeB = function(e, d, i) {
    e.stopPropagation();
    const block = cacheData[d][i];

    if (block.locked) return;

    if (block.id) {
        if(!confirm("This slot is already saved. Delete it permanently from database?")) return;

        fetch(`${API_BASE}/api/timeslots/${block.id}`, {
            method: 'DELETE',
            headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
        }).then(res => {
            if(res.ok) {
                cacheData[d].splice(i, 1);
                renderBlocks();
                loadMySchedule();
            } else {
                alert("Cannot delete a booked slot.");
            }
        });
    } else {
        cacheData[d].splice(i, 1);
        renderBlocks();
    }
};

$(document).ready(() => {
    startRealTimeClock();

    const token = localStorage.getItem('token');
    const role = localStorage.getItem('role');
    const name = localStorage.getItem('username') || 'Specialist';

    if(!token || role !== 'SPECIALIST') {
        alert("Authentication failed.");
        window.location.href = 'login.html';
        return;
    }

    $('#header-user-name').text(name);
    $('#nav-user-name').text(name);
    $('#header-avatar').attr('src', getAvatar(name));
    $('#profile-photo-preview').attr('src', getAvatar(name));

    loadMySchedule();

    $('#wb-week-toggle button').on('click', function() {
        $('#wb-week-toggle button').removeClass('btn-primary text-white').addClass('text-muted');
        $(this).removeClass('text-muted').addClass('btn-primary text-white');
        currentWeekOffset = $(this).text().trim() === 'This Week' ? 0 : 1;
        loadMySchedule();
    });

    for(let h=8; h<=18; h++) {
        $('#drag-time-axis').append(`<div class="time-slot-label" style="height: 80px; display:flex; align-items:flex-start; justify-content:center; padding-top:10px;">${h}:00</div>`);
        $('.drag-col').append(`<div class="drag-slot" data-hour="${h}" style="height: 80px;"></div>`);
    }

    $('#start-manage').click(function() {
        const now = new Date();

        if (currentWeekOffset === 0 && now.getDay() === 0) {
            alert("Platform Rules: Today is Sunday. Modifications for this week are locked. Redirecting to Next Week.");
            $('#wb-week-toggle button:eq(1)').click();
            return;
        }

        $(this).addClass('d-none');
        $('#wb-week-toggle').addClass('d-none');
        $('#save-all, #cancel-manage').removeClass('d-none');

        $('#wb-calendar-root').addClass('d-none');
        $('#drag-calendar-root').removeClass('d-none');

        $('#wb-status').html('Manage Mode: DRAG to set slots. <br><div class="text-danger fw-bolder mt-2 d-inline-block px-3 py-2 bg-danger-subtle border border-danger rounded-3 shadow-sm" style="font-size: 1.15rem; letter-spacing: 0.5px;"><i class="bi bi-exclamation-triangle-fill me-2 fs-4 align-middle"></i>IMPORTANT HINT: You can only modify the time slots in the white areas.</div>');

        cacheData = { mon: [], tue: [], wed: [], thu: [], fri: [], sat: [], sun: [] };

        const thresholdTime = now.getTime() + (24 * 60 * 60 * 1000);

        $('.drag-col').each(function() {
            const dayKey = $(this).data('day');
            const targetDateStr = currentWeekDates[dayKey];

            $(this).find('.drag-slot').each(function() {
                const startH = parseInt($(this).data('hour'));
                const slotDateTime = new Date(`${targetDateStr}T${String(startH).padStart(2, '0')}:00:00`).getTime();

                if (slotDateTime < thresholdTime) {
                    $(this).addClass('locked-cell');
                } else {
                    $(this).removeClass('locked-cell');
                }
            });
        });

        currentFetchedSchedule.forEach(slot => {
            const dayKey = getDayFromDate(slot.slotDate, currentWeekDates);
            if (!dayKey) return;

            const startH = parseInt(slot.startTime.split(':')[0]);
            const endH = parseInt(slot.endTime.split(':')[0]);
            const slotDateTime = new Date(`${slot.slotDate}T${slot.startTime}`).getTime();

            const isTimeLocked = slotDateTime < thresholdTime;
            const isBooked = slot.timeSlotStatus === 'BOOKED';
            const isDisabled = slot.timeSlotStatus === 'DISABLED'; // 🌟 新增判断

            // 只要时间过期、被预约、或者被禁用，全部死死锁定！
            const isLocked = isTimeLocked || isBooked || isDisabled;

            let statusTitle = 'VACANT';
            if (isBooked) statusTitle = slot.bookingStatus || 'BOOKED';
            if (isDisabled) statusTitle = 'DISABLED'; // 🌟 名字改成 Disabled

            cacheData[dayKey].push({
                s: startH,
                e: endH,
                id: slot.id,
                locked: isLocked,
                isBooked: isBooked,
                isDisabled: isDisabled, // 🌟 传给前端
                statusTitle: statusTitle
            });
        });

        renderBlocks();
    });

    $('#cancel-manage').click(function() {
        $('#save-all, #cancel-manage').addClass('d-none');
        $('#start-manage').removeClass('d-none');
        $('#wb-week-toggle').removeClass('d-none');
        $('#drag-calendar-root').addClass('d-none');
        $('#wb-calendar-root').removeClass('d-none');
        loadMySchedule();
    });

    $('#save-all').click(function() {
        const btn = $(this);
        const slotsToPublish = [];

        for (let day in cacheData) {
            const targetDate = currentWeekDates[day];
            cacheData[day].forEach(block => {
                if (!block.id && !block.locked) {
                    slotsToPublish.push({
                        date: targetDate,
                        startTime: String(block.s).padStart(2, '0') + ":00:00",
                        endTime: String(block.e).padStart(2, '0') + ":00:00"
                    });
                }
            });
        }

        if (slotsToPublish.length === 0) {
            $('#cancel-manage').click();
            return;
        }

        btn.prop('disabled', true).text('Saving...');

        fetch(`${API_BASE}/api/timeslots/publish`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' },
            body: JSON.stringify({ slots: slotsToPublish })
        })
            .then(async res => {
                if (res.ok) {
                    alert("Schedule successfully published!");
                    $('#cancel-manage').click();
                } else {
                    const text = await res.text();
                    alert("Failed to save: " + text);
                }
            })
            .finally(() => btn.prop('disabled', false).text('Save Changes'));
    });

    $(document).on('mousedown', '#drag-calendar-root .drag-slot', function(e) {
        if ($(e.target).closest('.drag-block').length > 0) return;
        if ($(this).hasClass('locked-cell')) return;

        curDay = $(this).closest('.drag-col').data('day');
        startH = parseInt($(this).data('hour'));
        isDrag = true;
        $(this).addClass('selecting');
    });

    $(document).on('mouseover', '#drag-calendar-root .drag-slot', function() {
        if(!isDrag) return;
        const col = $(this).closest('.drag-col');
        if(col.data('day') !== curDay) return;

        const endH = parseInt($(this).data('hour'));
        const [min, max] = [Math.min(startH, endH), Math.max(startH, endH)];

        col.find('.drag-slot').removeClass('selecting');
        for(let i=min; i<=max; i++) {
            const targetSlot = col.find(`[data-hour="${i}"]`);
            if (!targetSlot.hasClass('locked-cell') && targetSlot.find('.drag-block').length === 0) {
                targetSlot.addClass('selecting');
            }
        }
    });

    $(document).on('mouseup', function() {
        if(!isDrag) return;
        isDrag = false;
        const col = $(`.drag-col[data-day="${curDay}"]`);
        const selected = col.find('.drag-slot.selecting');
        if(selected.length > 0) {
            const hours = selected.map((i,el) => $(el).data('hour')).get();
            const min = Math.min(...hours);
            const max = Math.max(...hours) + 1;
            if (!cacheData[curDay].some(b => (min < b.e && max > b.s))) {
                cacheData[curDay].push({s: min, e: max});
                renderBlocks();
            }
        }
        $('.drag-slot').removeClass('selecting');
    });
});