/**
 * Master Logic for Specialist Workspace
 */

const API_BASE = "http://localhost:8080";

let currentWeekOffset = 0;
let currentWeekDates = {};
let currentFetchedSchedule = [];

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

// 🌟 新增：启动页面顶部的实时时钟
function startRealTimeClock() {
    // 确保在标题下方插入时间容器
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
        .catch(err => console.error("加载排班失败:", err));
}

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

        if (slot.timeSlotStatus === 'BOOKED') {
            title = slot.customerUsername || 'Customer';
            displayStatus = slot.bookingStatus || 'BOOKED';
            if (displayStatus === 'PENDING') cardType = 'card-pending';
            if (displayStatus === 'CONFIRMED') cardType = 'card-confirmed';
            if (displayStatus === 'CANCELED') cardType = 'card-canceled';
        }

        const cardHtml = `
            <div class="apt-card ${cardType}" style="top: ${topPx}px; height: ${heightPx}px;" onclick="openAptDetails(${slot.id})">
                <div class="apt-title">${title}</div>
                <div class="apt-time">${startH}:00 - ${endH}:00</div>
                <div class="apt-status">${displayStatus}</div>
            </div>
        `;
        $(`.day-column[data-day="${dayKey}"]`).append(cardHtml);
    });
}

function openAptDetails(id) {
    alert(`详情功能开发中 (Slot ID: ${id})`);
}

let isDrag = false, startH = null, curDay = null;
let cacheData = { mon: [], tue: [], wed: [], thu: [], fri: [], sat: [], sun: [] };

function renderBlocks() {
    $('.drag-block').remove();
    for(let d in cacheData) {
        cacheData[d].forEach((b, i) => {
            const col = $(`.drag-col[data-day="${d}"]`);
            const h = (b.e - b.s) * 80;

            // 🌟 重点渲染：如果是锁定的块（24小时内），不给删除按钮，换灰色样式
            if (b.locked) {
                col.find(`[data-hour="${b.s}"]`).append(`
                    <div class="drag-block drag-block-locked" style="height:${h-4}px" title="Within 24 hours. Cannot modify.">
                        <i class="bi bi-lock-fill mb-1"></i>
                        Locked ${b.s}:00-${b.e}:00
                    </div>`);
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

    // 双保险：如果是锁定状态，坚决不准删
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

    startRealTimeClock(); // 启动时钟

    const token = localStorage.getItem('token');
    const role = localStorage.getItem('role');
    const name = localStorage.getItem('username');
    if(!token || role !== 'SPECIALIST') {
        alert("Authentication failed.");
        window.location.href = 'login.html';
        return;
    }

    $('#header-user-name').text(name);
    $('#nav-user-name').text(name);
    if(!$('#header-avatar').attr('src') || $('#header-avatar').attr('src') === 'images/beauty.png'){
        $('#header-avatar').attr('src', `https://api.dicebear.com/7.x/initials/svg?seed=${name}`);
    }

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

        // 🌟 规则 1：如果是周日，彻底锁死“本周”！
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
        $('#wb-status').text('Manage Mode: DRAG to set slots. (Slots within 24 hours are locked)');

        cacheData = { mon: [], tue: [], wed: [], thu: [], fri: [], sat: [], sun: [] };

        // 计算 24 小时后的绝对时间戳阈值
        const thresholdTime = now.getTime() + (24 * 60 * 60 * 1000);

        currentFetchedSchedule.forEach(slot => {
            if (slot.timeSlotStatus === 'AVAILABLE') {
                const dayKey = getDayFromDate(slot.slotDate, currentWeekDates);
                const startH = parseInt(slot.startTime.split(':')[0]);
                const endH = parseInt(slot.endTime.split(':')[0]);

                // 🌟 规则 2：判断这块时间是不是在 24 小时内
                const slotDateTime = new Date(`${slot.slotDate}T${slot.startTime}`).getTime();
                const isLocked = slotDateTime < thresholdTime;

                if(dayKey) {
                    cacheData[dayKey].push({ s: startH, e: endH, id: slot.id, locked: isLocked });
                }
            }
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
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
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
            .finally(() => {
                btn.prop('disabled', false).text('Save Changes');
            });
    });

    // --- 拖拽事件监听 (加入24小时阈值拦截) ---
    $(document).on('mousedown', '#drag-calendar-root .drag-slot', function(e) {
        if ($(e.target).closest('.drag-block').length > 0) return;

        // 计算当前点击格子的时间
        curDay = $(this).closest('.drag-col').data('day');
        startH = parseInt($(this).data('hour'));

        const dragTimeObj = new Date(`${currentWeekDates[curDay]}T${String(startH).padStart(2, '0')}:00:00`).getTime();
        const thresholdTime = Date.now() + (24 * 60 * 60 * 1000);

        // 🌟 如果点击的时间在 24 小时内，直接拦截拖拽行为！
        if (dragTimeObj < thresholdTime) return;

        isDrag = true;
        $(this).addClass('selecting');
    });

    $(document).on('mouseover', '#drag-calendar-root .drag-slot', function() {
        if(!isDrag) return;
        const col = $(this).closest('.drag-col');
        if(col.data('day') !== curDay) return;

        const endH = parseInt($(this).data('hour'));
        const [min, max] = [Math.min(startH, endH), Math.max(startH, endH)];

        const thresholdTime = Date.now() + (24 * 60 * 60 * 1000);

        col.find('.drag-slot').removeClass('selecting');
        for(let i=min; i<=max; i++) {
            const targetSlot = col.find(`[data-hour="${i}"]`);
            const slotTimeObj = new Date(`${currentWeekDates[curDay]}T${String(i).padStart(2, '0')}:00:00`).getTime();

            // 🌟 只有24小时后的格子，才允许变蓝被选中
            if (slotTimeObj >= thresholdTime && targetSlot.find('.drag-block').length === 0) {
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