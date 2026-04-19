const API_BASE = "http://localhost:8080";
let selectedExpertId = null;

// 全局 AJAX 设置，确保每次请求都带上 Token
$.ajaxSetup({
    beforeSend: function(xhr) {
        const token = localStorage.getItem('token');
        if (token) xhr.setRequestHeader('Authorization', `Bearer ${token}`);
    }
});

function checkLogin() {
    const token = localStorage.getItem('token');
    if (!token) {
        alert("请先登录！");
        window.location.href = 'login.html';
        return false;
    }
    return true;
}

function handleProtectedView(pageId) {
    if (checkLogin()) showPage(pageId);
}

$(document).ready(() => initList());

function handleSearch() {
    const keyword = $('#search-input').val().trim();
    initList(keyword);
}

// 专家头像逻辑修复
function getAvatar(username) {
    if (username === 'ShenShaohui' || username === '申少辉') {
        // 增加时间戳防止浏览器缓存不更新图片
        return `/images/ssh.jpg?t=${new Date().getTime()}`;
    }
    return `https://api.dicebear.com/7.x/initials/svg?seed=${username}`;
}

// 初始化列表
function initList(keyword = '') {
    let url = `${API_BASE}/api/specialists`;
    if (keyword) url += `?keyword=${encodeURIComponent(keyword)}`;

    $.get(url, (res) => {
        const grid = $('#expert-grid').empty();
        const content = res.content || res || [];

        if (content.length === 0) {
            grid.append('<div class="col-12 text-center text-muted py-5"><h4>未找到匹配的专家</h4></div>');
            return;
        }

        content.forEach(item => {
            const avatar = getAvatar(item.user.username);
            grid.append(`
                <div class="col-md-6">
                    <div class="expert-card" onclick="goToProfile(${item.id})">
                        <span class="card-level-tag">${item.level || 'EXPERT'}</span>
                        <div class="d-flex align-items-center gap-3 mb-4">
                            <img src="${avatar}" style="width:60px; height:60px; border-radius:50%; object-fit: cover; border: 2px solid #0d6efd;">
                            <div>
                                <h3 class="fw-800 mb-0">${item.user.username}</h3>
                                <div class="text-muted small">${item.expertise ? item.expertise.name : '全栈开发'}</div>
                            </div>
                        </div>
                        <div class="price-info d-flex justify-content-between align-items-center">
                            <span class="fw-800">$${item.hourlyFee || 0}/hr</span>
                            <span class="text-primary fw-bold">查看详情 &raquo;</span>
                        </div>
                    </div>
                </div>`);
        });
    }).fail(() => console.error("后端连接失败"));
}

// 进入详情页
function goToProfile(id) {
    selectedExpertId = id;

    // 清空旧内容，显示加载中
    $('#pBio').text("正在加载专家履历...");

    $.get(`${API_BASE}/api/specialists/${id}`, (data) => {
        $('#pName').text(data.user.username);
        $('#pExpertise').text(data.expertise ? data.expertise.name : '领域专家');
        $('#pRate').text('$' + (data.hourlyFee || 0));
        $('#pLevel').text(data.level || 'EXPERT');

        // --- 逻辑修复重点 ---
        // 1. 优先尝试获取数据库里的简历字段
        let bioContent = data.resume || data.info || data.description;

        // 2. 如果是申少辉，且数据库没字，才给他的专属金句
        if (!bioContent || bioContent.length < 5) {
            if (data.user.username === 'ShenShaohui' || data.user.username === '申少辉') {
                bioContent = '“如果不曾见过极致的温柔，又怎会甘心在平庸里沉沦？” 申少辉，代号 SSH。他不仅是精通底层协议的架构大师，更是游走在理性与感性边缘的“灵魂咨询师”。';
            } else {
                bioContent = '该专家暂未填写个人简介。作为行业资深人士，他/她将在预约时段内为您提供最专业的指导。';
            }
        }

        $('#pBio').text(bioContent);
        $('#pAvatar').attr('src', getAvatar(data.user.username));
        showPage('profile-page');
    }).fail(() => {
        $('#pBio').text("加载失败，请重试。");
    });
}
// 预约日历加载
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
    const days = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT'];
    days.forEach(d => body.append(`<div class="p-2 fw-800 small text-muted text-center">${d}</div>`));

    for(let i=0; i<3; i++) body.append('<div></div>');

    for(let i=1; i<=30; i++) {
        const dateStr = `2026-04-${i.toString().padStart(2, '0')}`;
        const isAvailable = availableDates.includes(dateStr);
        body.append(`<div class="cal-date ${isAvailable?'has-data':'text-muted'}" ${isAvailable?`onclick="showSlots('${dateStr}')"`:'style="opacity:0.3"'}>${i}</div>`);
    }
}

// 核心修复：防止 Substring 报错的 showSlots
function showSlots(dateStr) {
    $('#display-date').text(dateStr);
    const list = $('#slot-list').empty();
    $('#confirm-btn').hide();

    const token = localStorage.getItem('token');

    $.ajax({
        url: `${API_BASE}/api/timeslots/specialist/${selectedExpertId}/available-times?date=${dateStr}`,
        type: 'GET',
        headers: { 'Authorization': `Bearer ${token}` },
        success: function(res) {
            // 兼容性提取数组
            const slots = Array.isArray(res) ? res : (res.timeSlots || res.content || []);

            if(!slots || slots.length === 0) {
                list.append('<p class="text-muted py-3">该日期暂无可用时段</p>');
            } else {
                slots.forEach(s => {
                    let timeDisplay = "可用";
                    // 核心修复：更健壮的时间字符串截取逻辑
                    if (s.startTime) {
                        if (s.startTime.includes(' ')) {
                            // 处理 "2026-04-20 09:00:00" 格式
                            timeDisplay = s.startTime.split(' ')[1].substring(0, 5);
                        } else {
                            // 处理 "09:00:00" 或 "09:00" 格式
                            timeDisplay = s.startTime.substring(0, 5);
                        }
                    }

                    list.append(`
                        <div class="time-btn m-2 d-inline-block"
                             onclick="$(this).addClass('selected').siblings().removeClass('selected');$('#confirm-btn').fadeIn().data('slot-id', ${s.id})">
                            ${timeDisplay}
                        </div>`);
                });
            }
            showPage('timeslot-page');
        },
        error: function(err) {
            console.error("加载时段失败:", err);
            alert("无法获取可用时段，请检查网络或重新登录。");
        }
    });
}

function showPage(id) {
    $('.page-view').removeClass('page-active');
    $(`#${id}`).addClass('page-active');
    window.scrollTo(0,0);
}