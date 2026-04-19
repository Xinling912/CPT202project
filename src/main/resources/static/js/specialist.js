/**
 * Master Logic for Specialist Workspace
 */

// 1. Sidebar Toggle
$('#toggle-sidebar').on('click', function() {
    $('#sidebar').toggleClass('collapsed');
    $('.main-content').toggleClass('expanded');
});

// 2. Tab Switching Logic
function switchTab(tabId, btn = null) {
    $('.tab-content').removeClass('active-tab');
    $('.nav-link').removeClass('active');

    const target = $('#' + tabId);
    if (target.length) {
        target.addClass('active-tab');
    }

    if (btn) {
        $(btn).addClass('active');
    } else {
        const matchingLink = $(`.nav-link[onclick*="'${tabId}'"]`);
        if(matchingLink.length > 0) {
            matchingLink.addClass('active');
        }
    }

    window.scrollTo({ top: 0, behavior: 'instant' });
}

// 3. Logout Logic
function logout() {
    if(confirm("Are you sure you want to log out?")) {
        localStorage.clear();
        window.location.href = 'login.html';
    }
}

// --- Workbench Calendar Initial Logic ---
const mockAppointments = [
    { id: 1, title: 'XingJianWu', day: 'MON', start: 8, end: 11, status: 'Confirmed', type: 'card-confirmed' },
    { id: 2, title: 'Vacant', day: 'MON', start: 14, end: 16, status: 'Available', type: 'card-vacant' },
    { id: 3, title: 'Vacant', day: 'TUE', start: 9, end: 11, status: 'Available', type: 'card-vacant' },
    { id: 4, title: 'ShaohuiShen', day: 'TUE', start: 14, end: 18, status: 'Pending', type: 'card-pending' },
    { id: 5, title: 'Vacant', day: 'WED', start: 10, end: 14, status: 'Available', type: 'card-vacant' },
    { id: 6, title: 'LeweiZhou', day: 'THU', start: 11, end: 13, status: 'Canceled', type: 'card-canceled' },
    { id: 7, title: 'Vacant', day: 'THU', start: 14, end: 17, status: 'Available', type: 'card-vacant' },
    { id: 8, title: 'XingJianWu', day: 'FRI', start: 13, end: 16, status: 'Confirmed', type: 'card-confirmed' }
];

function initWorkbench() {
    const timeAxis = $('#wb-time-axis').empty();
    for(let h = 8; h <= 18; h++) {
        timeAxis.append(`<div class="time-slot-label">${h}:00</div>`);
    }

    const HOUR_HEIGHT = 80;
    const START_HOUR = 8;

    mockAppointments.forEach(apt => {
        const topPx = (apt.start - START_HOUR) * HOUR_HEIGHT;
        const heightPx = (apt.end - apt.start) * HOUR_HEIGHT;
        const cardHtml = `
            <div class="apt-card ${apt.type}" style="top: ${topPx}px; height: ${heightPx}px;" onclick="openAptDetails(${apt.id})">
                <div class="apt-title">${apt.title}</div>
                <div class="apt-time">${apt.start}:00 - ${apt.end}:00</div>
                <div class="apt-status">${apt.status}</div>
            </div>
        `;
        $(`.day-column[data-day="${apt.day.toLowerCase()}"]`).append(cardHtml);
    });
}

function openAptDetails(id) {
    const apt = mockAppointments.find(a => a.id === id);
    if(apt.status === 'Pending') {
        alert(`You clicked ${apt.title}. Time to Confirm or Cancel!`);
    } else {
        alert(`Viewing details for ${apt.title} (${apt.status})`);
    }
}

// --- 小徐的拖拽排班逻辑 (移植并清理) ---
let isDrag = false, startH = null, curDay = null;
let cacheData = { mon: [], tue: [], wed: [], thu: [], fri: [], sat: [], sun: [] };

function renderBlocks() {
    $('.drag-block').remove();
    for(let d in cacheData) {
        cacheData[d].forEach((b, i) => {
            const col = $(`.drag-col[data-day="${d}"]`);
            const h = (b.e - b.s) * 45;
            col.find(`[data-hour="${b.s}"]`).append(`
                <div class="drag-block" style="height:${h-4}px">
                    Vacant ${b.s}:00-${b.e}:00
                    <i class="bi bi-x-circle-fill drag-del-btn" onclick="removeB(event, '${d}', ${i})"></i>
                </div>`);
        });
    }
}

window.removeB = function(e, d, i) {
    e.stopPropagation();
    cacheData[d].splice(i, 1);
    renderBlocks();
};

// --- DOM Ready ---
$(document).ready(() => {
    initWorkbench();

    // 加载用户名
    const name = localStorage.getItem('username') || 'XinlingDu'; // 默认杜姐
    $('#header-user-name').text(name);
    $('#nav-user-name').text(name);
    if(!$('#header-avatar').attr('src')){
        $('#header-avatar').attr('src', `https://api.dicebear.com/7.x/initials/svg?seed=${name}`);
    }

    // 初始化小徐的拖拽网格
    for(let h=8; h<22; h++) {
        $('#drag-time-axis').append(`<div style="height:45px; display:flex; align-items:center; justify-content:center; border-bottom:1px solid #eee;">${h}:00</div>`);
        $('.drag-col').append(`<div class="drag-slot" data-hour="${h}"></div>`);
    }

    // 拖拽模式切换按钮
    $('#start-manage').click(function() {
        $(this).addClass('d-none');
        $('#wb-week-toggle').addClass('d-none');
        $('#save-all, #cancel-manage').removeClass('d-none');

        $('#wb-calendar-root').addClass('d-none');
        $('#drag-calendar-root').removeClass('d-none');
        $('#wb-status').text('Manage Mode: DRAG to set available slots. Click X to remove.');
    });

    $('#cancel-manage').click(function() {
        $('#save-all, #cancel-manage').addClass('d-none');
        $('#start-manage').removeClass('d-none');
        $('#wb-week-toggle').removeClass('d-none');

        $('#drag-calendar-root').addClass('d-none');
        $('#wb-calendar-root').removeClass('d-none');
        $('#wb-status').text('Manage your appointments and availability.');

        cacheData = { mon: [], tue: [], wed: [], thu: [], fri: [], sat: [], sun: [] };
        renderBlocks();
    });

    $('#save-all').click(function() {
        alert("Atomizing schedule and syncing with backend...");
        $('#cancel-manage').click();
    });

    // 拖拽事件监听
    $(document).on('mousedown', '#drag-calendar-root .drag-slot', function(e) {
        if ($(e.target).closest('.drag-block').length > 0) return;
        isDrag = true;
        startH = parseInt($(this).data('hour'));
        curDay = $(this).closest('.drag-col').data('day');
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
            if (targetSlot.find('.drag-block').length === 0) {
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

// --- Professional Profile Logic ---
$('#profile-photo-btn').on('click', function() { $('#profile-photo-input').click(); });

$('#profile-photo-input').on('change', function(e) {
    const file = e.target.files[0];
    if (file) {
        const reader = new FileReader();
        reader.onload = function(e) { $('#profile-photo-preview').attr('src', e.target.result); }
        reader.readAsDataURL(file);
    }
});

$('#cert-upload-box').on('click', function(e) { if(e.target.id !== 'file-list') $('#cert-input').click(); });

$('#cert-input').on('change', function(e) {
    const files = e.target.files;
    const fileList = $('#file-list').empty();
    if (files.length > 0) {
        let fileNames = [];
        for (let i = 0; i < files.length; i++) {
            fileNames.push(`<div class="mb-1 py-1 px-2 bg-white rounded border d-inline-block me-2">
                <i class="bi bi-check-circle-fill text-success me-1"></i>${files[i].name}
            </div>`);
        }
        fileList.html(fileNames.join(''));
    }
});

$('#bio-desc').on('input', function() {
    const currentLength = $(this).val().length;
    $('#bio-counter').text(`${currentLength} / 500 characters`);
    if(currentLength > 500) $('#bio-counter').addClass('text-danger');
    else $('#bio-counter').removeClass('text-danger');
});

$('#save-profile-btn').on('click', function() {
    const btn = $(this);
    const status = $('#profile-save-status');
    btn.prop('disabled', true).html('<span class="spinner-border spinner-border-sm me-2"></span>Saving...');
    setTimeout(() => {
        btn.prop('disabled', false).text('Save Profile');
        status.removeClass('d-none');
        setTimeout(() => status.addClass('d-none'), 3000);
    }, 1000);
});