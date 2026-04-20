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



// 检查登录状态

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



function showJoinUs() {

    window.location.href = 'expert_apply.html';

}



// --- 2. 页面初始化 ---

$(document).ready(() => {

    if (checkLogin()) {

        initList();

    }

});



function handleSearch() {

    const keyword = $('#search-input').val().trim();

    initList(keyword);

}



// 专家头像逻辑 (booking.js)

function getAvatar(username) {

    if (username === 'ShenShaohui' || username === 'Shaohui Shen') {

        return `images/ssh.jpg?t=${new Date().getTime()}`;

    }

    if (username === 'XingjianWu' || username === 'Xingjian Wu') {

        return `images/specialist1.png`;

    }

    if (username === 'XinlingDu' || username === 'Xinling Du') {

        return `images/specialist3.png`;

    }

    if (username === 'Carrot') {

// 🌟 同样指向 Carrot 的照片

        return `images/carrot.png`;

    }

    return `https://api.dicebear.com/7.x/initials/svg?seed=${username}`;

}

// --- 3. 核心业务逻辑 ---



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

    });

}



function goToProfile(id) {

    selectedExpertId = id;

    $('#pBio').text("Loading profile...");

    $.get(`${API_BASE}/api/specialists/${id}`, (data) => {

        $('#pName').text(data.user.username);

        $('#pExpertise').text(data.expertise ? data.expertise.name : '领域专家');

        $('#pRate').text('$' + (data.hourlyFee || 0));

        $('#pLevel').text(data.level || 'EXPERT');



        let bioContent = data.resume || data.info || "No biography provided.";

        if (data.user.username === 'ShenShaohui' || data.user.username === 'Shaohui Shen') {

            bioContent = bioContent.length < 5 ? '资深情感博主，专治撩妹苦手。' : bioContent;

        }

        $('#pBio').text(bioContent);

        $('#pAvatar').attr('src', getAvatar(data.user.username));

        showPage('profile-page');

    });

}



// --- 4. 预约与日历逻辑 ---



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

    const daysOfWeek = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT'];

    daysOfWeek.forEach(day => body.append(`<div class="cal-header-day">${day}</div>`));



    const firstDayOffset = 3; // 4月1号是周三

    const daysInMonth = 30;



    if ($('.cal-legend').length === 0) {

        body.before(`<div class="cal-legend">

<span><div class="dot-available"></div> Open for Booking</span>

<span><div class="dot-empty"></div> Fully Booked / No Slots</span>

</div>`);

    }



    for (let i = 0; i < firstDayOffset; i++) body.append(`<div></div>`);



    for (let i = 1; i <= daysInMonth; i++) {

        const dateStr = `2026-04-${i.toString().padStart(2, '0')}`;

        const isAvailable = availableDates.includes(dateStr);

        if (isAvailable) {

            body.append(`<div class="cal-date available" onclick="showSlots('${dateStr}')">${i}</div>`);

        } else {

            body.append(`<div class="cal-date empty">${i}</div>`);

        }

    }

}



function showSlots(dateStr) {

    $('#display-date').text(dateStr);

    const list = $('#slot-list').empty();

    $('#confirm-btn').hide();

    currentSelectedSlotId = null;



    $.get(`${API_BASE}/api/timeslots/specialist/${selectedExpertId}/available-times?date=${dateStr}`, (res) => {

        const slots = Array.isArray(res) ? res : (res.timeSlots || []);

        if (slots.length === 0) {

            list.append(`<h5 class="text-muted mt-4">No slots available.</h5>`);

            showPage('timeslot-page');

            return;

        }

        slots.forEach(s => {

            let start = s.startTime ? s.startTime.substring(0, 5) : "--:--";

            let end = s.endTime ? s.endTime.substring(0, 5) : "--:--";

            list.append(`

<div class="slot-card" id="slot-card-${s.id}" onclick="selectSlot(${s.id})">

<div class="slot-time">${start} - ${end}</div>

<div class="slot-status"><i class="bi bi-check2-circle me-1"></i>Available</div>

</div>

`);

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



// --- 5. 绑定确认下单按钮 ---

$('#confirm-btn').off('click').on('click', function() {

    if (!currentSelectedSlotId || !selectedExpertId) return;

    const btn = $(this);

    btn.prop('disabled', true).html('Processing...');



    fetch(`${API_BASE}/api/bookings/create`, {

        method: 'POST',

        headers: {

            'Authorization': `Bearer ${localStorage.getItem('token')}`,

            'Content-Type': 'application/json'

        },

        body: JSON.stringify({

            specialistId: selectedExpertId,

            slotId: currentSelectedSlotId,

            notes: "I need consultation."

        })

    })

        .then(async res => {

            if (res.ok) {

                alert("Reservation Successful!");

                showPage('list-page');

            } else {

                const text = await res.text();

                alert("Failed to book: " + text);

            }

        })

        .finally(() => {

            btn.prop('disabled', false).text('Confirm Reservation');

        });

});



function showPage(id) {

    $('.page-view').hide();

    $(`#${id}`).fadeIn();

    window.scrollTo(0,0);

}