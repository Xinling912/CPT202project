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
        window.location.href = 'login.html';
    }
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

// 启动实时时钟
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

// 🌟 替换：画出日历实体方块，并注入订单数据供弹窗使用
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
        let bookingId = slot.bookingId || slot.id; // 获取订单ID

        if (slot.timeSlotStatus === 'BOOKED') {
            title = slot.customerUsername || 'Customer';
            displayStatus = slot.bookingStatus || 'BOOKED';
            if (displayStatus === 'PENDING') cardType = 'card-pending';
            if (displayStatus === 'CONFIRMED') cardType = 'card-confirmed';
            if (displayStatus === 'CANCELED' || displayStatus === 'CANCELLED') cardType = 'card-canceled';
        }

        const cardHtml = `
            <div class="apt-card ${cardType}" style="top: ${topPx}px; height: ${heightPx}px;" 
                 onclick="openAptDetails(${bookingId}, '${displayStatus}', '${title}', '${startH}:00 - ${endH}:00')">
                <div class="apt-title">${title}</div>
                <div class="apt-time">${startH}:00 - ${endH}:00</div>
                <div class="apt-status">${displayStatus}</div>
            </div>
        `;
        $(`.day-column[data-day="${dayKey}"]`).append(cardHtml);
    });
}

// 🌟 替换：点击卡片触发高级审批弹窗
window.openAptDetails = function(bookingId, status, customerName, timeStr) {
    if (status === 'PENDING') {
        if ($('#aptActionModal').length === 0) {
            $('body').append(`
                <div class="modal fade" id="aptActionModal" tabindex="-1">
                  <div class="modal-dialog modal-dialog-centered modal-sm">
                    <div class="modal-content rounded-4 border-0 shadow-lg">
                      <div class="modal-header border-0 pb-0">
                        <h5 class="modal-title fw-800">Booking Request</h5>
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
                        <div class="d-flex gap-3 mt-4 px-2">
                            <button class="btn btn-light flex-grow-1 rounded-pill fw-bold text-danger border" id="btn-reject-apt">Reject</button>
                            <button class="btn btn-primary flex-grow-1 rounded-pill fw-bold" id="btn-confirm-apt">Confirm</button>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
            `);
        }

        $('#modal-customer-name').text(customerName);
        $('#modal-apt-time').text(timeStr);
        $('#aptActionModal').modal('show');

        $('#btn-confirm-apt').off('click').on('click', function() {
            processApt(bookingId, 'confirm');
        });

        $('#btn-reject-apt').off('click').on('click', function() {
            const reason = prompt("Please enter a reason for rejecting this booking:");
            if (reason !== null) {
                processApt(bookingId, 'cancel', reason || "Specialist unavailable");
            }
        });

    } else {
        alert(`This time slot is currently [ ${status} ].`);
    }
}

// 🌟 新增：处理确认或拒绝的网络请求
function processApt(bookingId, action, reason = "") {
    const token = localStorage.getItem('token');
    const btn = action === 'confirm' ? $('#btn-confirm-apt') : $('#btn-reject-apt');
    const originalText = btn.text();
    btn.prop('disabled', true).text('Processing...');

    let url = `${API_BASE}/api/bookings/${action}/${bookingId}`;
    if (action === 'cancel') url += `?reason=${encodeURIComponent(reason)}`;

    fetch(url, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` }
    }).then(async res => {
        if(res.ok) {
            alert(`Booking successfully ${action === 'confirm' ? 'Confirmed' : 'Rejected'}!`);
            $('#aptActionModal').modal('hide');
            loadMySchedule();
        } else {
            alert("Action failed: " + await res.text());
        }
    }).catch(err => alert("Network Error: Make sure backend is running."))
        .finally(() => btn.prop('disabled', false).text(originalText));
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
                    // 🌟 已经被预约的，根据状态涂上橙色(Pending)或绿色(Confirmed)
                    let bgClass = 'bg-secondary';
                    if (b.statusTitle === 'PENDING') bgClass = 'bg-warning';
                    if (b.statusTitle === 'CONFIRMED') bgClass = 'bg-success';

                    col.find(`[data-hour="${b.s}"]`).append(`
                        <div class="drag-block ${bgClass} text-white border-0 shadow-sm" style="height:${h-4}px; opacity: 0.95; cursor: not-allowed;" title="Already Booked">
                            <i class="bi bi-person-check-fill mb-1"></i>
                            ${b.statusTitle} ${b.s}:00-${b.e}:00
                        </div>`);
                } else {
                    // 过期锁定（浅灰色）
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

    // 🌟 动态渲染名字和真实头像
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

        // 🌟 提示语
        $('#wb-status').html('Manage Mode: DRAG to set slots. <br><span class="text-primary fw-bold mt-1 d-inline-block" style="font-size: 0.9rem;"><i class="bi bi-lightbulb-fill me-1"></i>Hint: You can only modify the time slots in the white areas.</span>');

        cacheData = { mon: [], tue: [], wed: [], thu: [], fri: [], sat: [], sun: [] };

        const thresholdTime = now.getTime() + (24 * 60 * 60 * 1000);

        // 给过期/24小时内的底图加上灰色类名
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

        // 把数据库里已经有的排班加载成卡片 (包含预约过的状态)
        currentFetchedSchedule.forEach(slot => {
            const dayKey = getDayFromDate(slot.slotDate, currentWeekDates);
            if (!dayKey) return;

            const startH = parseInt(slot.startTime.split(':')[0]);
            const endH = parseInt(slot.endTime.split(':')[0]);
            const slotDateTime = new Date(`${slot.slotDate}T${slot.startTime}`).getTime();

            const isTimeLocked = slotDateTime < thresholdTime;
            const isBooked = slot.timeSlotStatus === 'BOOKED';

            // 只要时间过期了，或者已经被顾客预约了，就必须死死锁定！
            const isLocked = isTimeLocked || isBooked;

            let statusTitle = 'VACANT';
            if (isBooked) {
                statusTitle = slot.bookingStatus || 'BOOKED';
            }

            cacheData[dayKey].push({
                s: startH,
                e: endH,
                id: slot.id,
                locked: isLocked,
                isBooked: isBooked,
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
        // 🌟 防错：如果是灰色格子，直接不让点
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
            // 🌟 防错：划过灰色格子时，不能被选中
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