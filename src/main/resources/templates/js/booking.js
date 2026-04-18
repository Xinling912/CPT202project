const API_BASE = "http://localhost:8080";
let selectedExpertId = null;
let allSlots = [];

// ==========================================
// 🌟 JWT Token 拦截器配置 (必须保留！)
// ==========================================
$.ajaxSetup({
    beforeSend: function(xhr) {
        const token = localStorage.getItem('token');
        if (token) {
            xhr.setRequestHeader('Authorization', `Bearer ${token}`);
        }
    }
});

// 辅助函数：检查登录状态
function checkLogin() {
    const token = localStorage.getItem('token');
    if (!token) {
        alert("Please log in first!");
        window.location.href = 'login.html'; 
        return false;
    }
    return true;
}

// 处理需要登录才能查看的页面
function handleProtectedView(pageId, specialistId = null) {
    if (checkLogin()) {
        if (pageId === 'specialist-dashboard') {
            loadSpecialistDashboard(specialistId);
        } else {
            showPage(pageId);
        }
    }
}

// ==========================================
// 🌟 【新增核心代码】：前端门卫 (RBAC 权限隔离)
// ==========================================
function setupRoleBasedUI() {
    // 进这个页面必须先过安检，没登录直接踢回登录页
    if (!checkLogin()) return; 

    const userRole = localStorage.getItem('role');

    if (userRole === 'CUSTOMER') {
        // 1. 如果是普通用户：隐藏顶部导航栏的【专家视图】按钮
        $('button:contains("Specialist View")').hide();
        // 2. 默认让他看到专家列表
        showPage('list-page');
        // 3. 去后端拉取真实专家数据画卡片
        initList();
    } 
    else if (userRole === 'SPECIALIST') {
        // 1. 如果是专家：隐藏【普通用户大厅】和【我的预约】按钮
        $('button:contains("User Portal")').hide();
        $('button:contains("My Bookings")').hide();
        // 2. 专家一进来，强制直接跳到他自己的日历工作台！
        const mySpecialistId = localStorage.getItem('userId'); 
        loadSpecialistDashboard(mySpecialistId);
    } else {
        // 兜底防御，万一没取到角色，默认当普通用户处理
        showPage('list-page');
        initList();
    }
}

// 页面加载完成后，立刻呼叫门卫！(替换掉了小徐原来无脑执行的 initList)
$(document).ready(() => {
    setupRoleBasedUI();
});

// ==========================================
// 下面全是小徐原本的渲染逻辑，原封不动保留
// ==========================================

function initList() {
    $.get(`${API_BASE}/api/specialists`, (res) => {
        const grid = $('#expert-grid').empty();
        // 兼容后端的 Page 返回格式
        const experts = res.content ? res.content : res; 
        if(!experts || experts.length === 0) return;

        experts.forEach(item => {
            grid.append(`
                <div class="col-md-6">
                    <div class="expert-card" onclick="goToProfile(${item.id},'${item.user?.username || 'Expert'}','${item.expertise?.name || 'General'}','${item.hourlyFee || '100'}','${item.level || 'Pro'}')">
                        <span class="card-level-tag">${item.level || 'Pro'}</span>
                        <div class="d-flex align-items-center gap-3 mb-4">
                            <img src="https://api.dicebear.com/7.x/initials/svg?seed=${item.user?.username || 'E'}" style="width:60px; border-radius:50%">
                            <div>
                                <h3 class="fw-800 mb-0">${item.user?.username || 'Expert'}</h3>
                                <div class="text-muted small">${item.expertise?.name || 'General'}</div>
                            </div>
                        </div>
                        <div class="price-info d-flex justify-content-between align-items-center">
                            <span class="fw-800">$${item.hourlyFee || '100'}/hr</span>
                            <span class="text-primary fw-bold">View Profile &raquo;</span>
                        </div>
                    </div>
                </div>
            `);
        });
    });
}

function goToProfile(id, name, exp, rate, lvl) { selectedExpertId = id; $('#pName').text(name); $('#pExpertise').text(exp); $('#pRate').text('$'+rate); $('#pLevel').text(lvl); $('#pAvatar').attr('src', `https://api.dicebear.com/7.x/initials/svg?seed=${name}`); showPage('profile-page'); }

function goToCalendar() {
    $.get(`${API_BASE}/api/specialists/${selectedExpertId}/schedules`, (data) => {
        allSlots = data || []; renderUserCalendar(); showPage('calendar-page');
    });
}

function renderUserCalendar() {
    const body = $('#calendar-body').empty();
    const days = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT'];
    days.forEach(d => body.append(`<div class="p-2 fw-800 small text-muted">${d}</div>`));
    for(let i=0; i<3; i++) body.append('<div></div>');
    for(let i=1; i<=30; i++) {
        const dateStr = `2026-04-${i.toString().padStart(2, '0')}`;
        const hasData = allSlots.some(s => s.slotDate === dateStr && !s.booked);
        body.append(`<div class="cal-date ${hasData ? 'has-data' : ''}" onclick="showSlots('${dateStr}')">${i}</div>`);
    }
}

function showSlots(dateStr) {
    $('#display-date').text(dateStr);
    const list = $('#slot-list').empty(); $('#confirm-btn').hide();
    const todaySlots = allSlots.filter(s => s.slotDate === dateStr && !s.booked);
    todaySlots.forEach(s => {
        let timeStr = s.startTime ? s.startTime.split(' ').pop().substring(0, 5) : '10:00';
        list.append(`<div class="time-btn" onclick="$(this).addClass('selected').siblings().removeClass('selected');$('#confirm-btn').fadeIn()">${timeStr}</div>`);
    });
    showPage('timeslot-page');
}

function showOrderDetail(ex, ti, st, stat) { $('#mEx').text(ex); $('#mSt').text(stat); new bootstrap.Modal('#orderModal').show(); }

function loadSpecialistDashboard(id) {
    showPage('specialist-dashboard');
    const body = $('#specialist-calendar-body').empty();
    for (let h = 8; h < 22; h++) {
        body.append(`<tr id="row-${h}"><td class="time-col">${h}:00</td><td data-day="MON"></td><td data-day="TUE"></td><td data-day="WED"></td><td data-day="THU"></td><td data-day="FRI"></td><td data-day="SAT"></td><td data-day="SUN"></td></tr>`);
    }

    setTimeout(() => {
        renderBlock("MON", 8, 3, "XingjianWu", "Confirmed", "08:00 - 11:00");
        renderBlock("MON", 14, 2, "Vacant", "Available", "14:00 - 16:00");
        renderBlock("TUE", 9, 2, "Vacant", "Available", "09:00 - 11:00");
        renderBlock("TUE", 15, 3, "ShaohuiShen", "Pending", "15:00 - 18:00");
        renderBlock("WED", 10, 4, "Vacant", "Available", "10:00 - 14:00");
        renderBlock("THU", 11, 2, "LeweiZhou", "Canceled", "11:00 - 13:00");
        renderBlock("THU", 15, 2, "Vacant", "Available", "15:00 - 17:00");
        renderBlock("FRI", 13, 3, "XingjianWu", "Confirmed", "13:00 - 16:00");
    }, 100);
}

function renderBlock(day, start, dur, user, status, range) {
    const cell = $(`#row-${start} td[data-day="${day}"]`);
    const block = $(`
        <div class="booking-block status-${status}" style="height:${dur*45-6}px" 
             onclick="${status !== 'Available' ? 'new bootstrap.Modal(\'#decisionModal\').show()' : ''}">
            <div>${user}</div>
            <div style="font-size:0.55rem">${range}</div>
            <div class="mt-auto" style="font-size:0.5rem">${status}</div>
        </div>
    `);
    cell.append(block);
}

function showPage(id) { $('.page-view').removeClass('page-active'); $(`#${id}`).addClass('page-active'); window.scrollTo(0,0); }