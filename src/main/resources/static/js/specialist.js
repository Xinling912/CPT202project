/**
 * Master Logic for Specialist Workspace
 */

let scheduleData = {
    this: { MON: [], TUE: [], WED: [], THU: [], FRI: [], SAT: [], SUN: [] },
    next: { MON: [], TUE: [], WED: [], THU: [], FRI: [], SAT: [], SUN: [] }
};

let currentWeek = 'this';
let currentDay = 'MON';
let isEditMode = false;

// 1. Sidebar Toggle
$('#toggle-sidebar').on('click', function() {
    $('#sidebar').toggleClass('collapsed');
    $('.main-content').toggleClass('expanded');
});

// 2. 🌟 Tab Switching Logic (支持侧边栏和下拉菜单)
function switchTab(tabId, btn = null) {
    if (isEditMode) {
        alert("⚠️ Please exit 'Edit Mode' (Save & Exit) before leaving this page.");
        return;
    }

    $('.tab-content').removeClass('active-tab');
    $('.nav-link').removeClass('active');

    const target = $('#' + tabId);
    if (target.length) {
        target.addClass('active-tab');
    }

    if (btn) {
        $(btn).addClass('active');
    } else {
        // 如果是点击右上角下拉菜单进来的，自动高亮侧边栏对应的按钮
        const matchingLink = $(`.nav-link[onclick*="'${tabId}'"]`);
        if(matchingLink.length > 0) {
            matchingLink.addClass('active');
        }
    }

    window.scrollTo({ top: 0, behavior: 'instant' });
}

// ==========================================
// 🌟 Logout Logic
// ==========================================
function logout() {
    if(confirm("Are you sure you want to log out?")) {
        localStorage.clear();
        window.location.href = 'login.html'; // 踢回登录页
    }
}

// 3. Init Schedule Grid
function initSchedule() {
    const editor = $('#slot-editor-container').empty();
    for (let h = 8; h <= 18; h++) {
        editor.append(`<div class="slot-btn" data-time="${h}:00">${h}:00</div>`);
    }

    const tableBody = $('#preview-table-body').empty();
    for (let h = 8; h <= 18; h++) {
        let row = `<tr data-time="${h}:00"><td class="fw-bold text-muted small">${h}:00</td>`;
        ['MON','TUE','WED','THU','FRI','SAT','SUN'].forEach(day => {
            row += `<td data-day="${day}"></td>`;
        });
        row += `</tr>`;
        tableBody.append(row);
    }
}

// 4. Edit Mode Controller
$('#edit-mode-btn').on('click', function() {
    isEditMode = !isEditMode;
    if (isEditMode) {
        $(this).html('<i class="bi bi-check-lg"></i> Exit & Save').addClass('btn-danger').removeClass('btn-outline-secondary');
        $('#edit-panel').addClass('editing-active');
        $('#slot-editor-container').removeClass('disabled-grid');
        $('#save-status').text('UNSAVED').removeClass('bg-secondary').addClass('bg-warning text-dark');
    } else {
        $(this).html('<i class="bi bi-pencil-square"></i> Enter Edit Mode').addClass('btn-outline-secondary').removeClass('btn-danger');
        $('#edit-panel').removeClass('editing-active');
        $('#slot-editor-container').addClass('disabled-grid');
        $('#save-status').text('SAVED').removeClass('bg-warning text-dark').addClass('bg-success');
        setTimeout(() => $('#save-status').text('LOCKED').removeClass('bg-success').addClass('bg-secondary'), 2000);
    }
});

// 5. Selection Logic
$(document).on('click', '.slot-btn', function() {
    const time = $(this).data('time');
    const dayData = scheduleData[currentWeek][currentDay];
    if ($(this).hasClass('selected')) {
        $(this).removeClass('selected');
        const index = dayData.indexOf(time);
        if (index > -1) dayData.splice(index, 1);
    } else {
        $(this).addClass('selected');
        dayData.push(time);
    }
    updatePreviewTable();
});

function updatePreviewTable() {
    $('#preview-table-body td[data-day]').removeClass('preview-active');
    const weekData = scheduleData[currentWeek];
    for (let day in weekData) {
        weekData[day].forEach(time => {
            $(`#preview-table-body tr[data-time="${time}"] td[data-day="${day}"]`).addClass('preview-active');
        });
    }
}

function refreshEditorButtons() {
    $('.slot-btn').removeClass('selected');
    const activeSlots = scheduleData[currentWeek][currentDay];
    activeSlots.forEach(time => $(`.slot-btn[data-time="${time}"]`).addClass('selected'));
    $('#current-day-label').text(currentDay);
}

// 6. 🌟 Week Switcher
$('#week-selector button').on('click', function() {
    if (isEditMode) {
        alert("⚠️ Please Exit Edit Mode before switching weeks to avoid data loss.");
        return;
    }
    $('#week-selector button').removeClass('active');
    $(this).addClass('active');
    currentWeek = $(this).data('week');
    refreshEditorButtons();
    updatePreviewTable();
});

$('#day-selector button').on('click', function() {
    $(this).addClass('active').siblings().removeClass('active');
    currentDay = $(this).data('day');
    refreshEditorButtons();
});


// --- 🌟 Workbench Calendar Logic ---
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

    const days = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN'];
    const colsContainer = $('#wb-day-columns').empty();
    days.forEach(day => { colsContainer.append(`<div class="wb-day-col" id="col-${day}"></div>`); });

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
        $(`#col-${apt.day}`).append(cardHtml);
    });
}

function openAptDetails(id) {
    const apt = mockAppointments.find(a => a.id === id);
    if(apt.status === 'Pending') {
        alert(`You clicked ${apt.title}. In the next step, we will pop up the Confirm/Cancel modal for this!`);
    } else {
        alert(`Viewing details for ${apt.title} (${apt.status})`);
    }
}

// --- 🌟 DOM Ready ---
$(document).ready(() => {
    initSchedule();
    initWorkbench(); 
    
    // 动态显示用户名
    $(document).ready(() => {
        initSchedule();
        initWorkbench();

        // 🌟 1. 把默认名字改成 Wanfeng
        const name = localStorage.getItem('username') || 'Wanfeng';
        $('#header-user-name').text(name);
        $('#nav-user-name').text(name);
    });

    $('#header-user-name').text(name);
    $('#nav-user-name').text(name);
    if(!$('#header-avatar').attr('src')){
        $('#header-avatar').attr('src', `https://api.dicebear.com/7.x/initials/svg?seed=${name}`);
    }
});

// --- 🌟 Professional Profile Logic ---
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