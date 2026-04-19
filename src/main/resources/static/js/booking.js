const API_BASE = "http://localhost:8080";
let selectedExpertId = null;

// --- 1. 全局配置 ---
$.ajaxSetup({
    beforeSend: function(xhr) {
        const token = localStorage.getItem('token');
        if (token) xhr.setRequestHeader('Authorization', `Bearer ${token}`);
    }
});

// 检查登录状态：如果没 Token，直接踢回登录页，不给任何乱跑的机会
function checkLogin() {
    const token = localStorage.getItem('token');
    if (!token) {
        alert("Session expired, please log in.");
        window.location.href = 'login.html';
        return false;
    }
    return true;
}

function handleProtectedView(pageId) {
    if (checkLogin()) showPage(pageId);
}

// --- 2. 页面初始化 ---
$(document).ready(() => {
    // 启动时检查：没登录直接踢走
    if (checkLogin()) {
        initList();
    }
});

function handleSearch() {
    const keyword = $('#search-input').val().trim();
    initList(keyword);
}

// 专家头像逻辑：ssh.jpg 放在 images 文件夹下
function getAvatar(username) {
    if (username === 'ShenShaohui' || username === '申少辉') {
        return `images/ssh.jpg?t=${new Date().getTime()}`;
    }
    return `https://api.dicebear.com/7.x/initials/svg?seed=${username}`;
}

// --- 3. 核心业务逻辑 ---

// 加载专家列表
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
            const avatar = getAvatar(item.user.username);
            grid.append(`
                <div class="col-md-6">
                    <div class="expert-card p-4 bg-white rounded-5 border mb-4 shadow-sm" style="cursor:pointer" onclick="goToProfile(${item.id})">
                        <span class="badge bg-primary mb-3">${item.level || 'EXPERT'}</span>
                        <div class="d-flex align-items-center gap-3 mb-4">
                            <img src="${avatar}" style="width:60px; height:60px; border-radius:50%; object-fit: cover; border: 2px solid #0d6efd;">
                            <div>
                                <h3 class="fw-800 mb-0">${item.user.username}</h3>
                                <div class="text-muted small">${item.expertise ? item.expertise.name : '领域专家'}</div>
                            </div>
                        </div>
                        <div class="d-flex justify-content-between align-items-center">
                            <span class="fw-800">$${item.hourlyFee || 0}/hr</span>
                            <span class="text-primary fw-bold">View Detail &raquo;</span>
                        </div>
                    </div>
                </div>`);
        });
    }).fail((err) => {
        // 🌟 重点：如果后端回 401，说明 Token 挂了，强制登录
        if (err.status === 401) {
            alert("Please log in first!");
            window.location.href = 'login.html';
        } else {
            console.error("Connection failed");
        }
    });
}

// 加载详情页
function goToProfile(id) {
    selectedExpertId = id;
    $('#pBio').text("Loading profile...");

    $.get(`${API_BASE}/api/specialists/${id}`, (data) => {
        $('#pName').text(data.user.username);
        $('#pExpertise').text(data.expertise ? data.expertise.name : '领域专家');
        $('#pRate').text('$' + (data.hourlyFee || 0));
        $('#pLevel').text(data.level || 'EXPERT');

        // 读取简历
        let bioContent = data.resume || data.info || "No biography provided.";
        // 申少辉金句
        if (data.user.username === 'ShenShaohui' || data.user.username === '申少辉') {
            bioContent = bioContent.length < 5 ? '“如果不曾见过极致的温柔，又怎会甘心在平庸里沉沦？”' : bioContent;
        }

        $('#pBio').text(bioContent);
        $('#pAvatar').attr('src', getAvatar(data.user.username));
        showPage('profile-page');
    });
}

// 预约相关逻辑
function goToCalendar() {
    if (!selectedExpertId) return;
    $.get(`${API_BASE}/api/timeslots/specialist/${selectedExpertId}/available-dates`, (data) => {
        const dates = Array.isArray(data) ? data : (data.availableDates || []);
        renderUserCalendar(dates);
        showPage('calendar-page');
    });
}

function renderUserCalendar(availableDates) {
    const body = $('#calendar-body').empty();
    // 渲染简单的 30 天日历 (示意逻辑)
    for(let i=1; i<=30; i++) {
        const dateStr = `2026-04-${i.toString().padStart(2, '0')}`;
        const isAvailable = availableDates.includes(dateStr);
        body.append(`<div class="p-3 border text-center ${isAvailable?'bg-primary text-white':'opacity-25'}" 
            style="cursor:${isAvailable?'pointer':'not-allowed'}"
            ${isAvailable?`onclick="showSlots('${dateStr}')"`:''}>${i}</div>`);
    }
}

function showSlots(dateStr) {
    $('#display-date').text(dateStr);
    const list = $('#slot-list').empty();
    const token = localStorage.getItem('token');

    $.ajax({
        url: `${API_BASE}/api/timeslots/specialist/${selectedExpertId}/available-times?date=${dateStr}`,
        type: 'GET',
        headers: { 'Authorization': `Bearer ${token}` },
        success: function(res) {
            const slots = Array.isArray(res) ? res : (res.timeSlots || []);
            slots.forEach(s => {
                let time = s.startTime ? s.startTime.substring(11, 16) : "Available";
                list.append(`<button class="btn btn-outline-primary m-2" onclick="$('#confirm-btn').show()">${time}</button>`);
            });
            showPage('timeslot-page');
        }
    });
}

// 切页通用函数
function showPage(id) {
    $('.page-view').hide();
    $(`#${id}`).fadeIn();
    window.scrollTo(0,0);
}