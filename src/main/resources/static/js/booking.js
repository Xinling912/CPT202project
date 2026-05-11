/**
 * User Booking Logic
 * Specialist Administration System (SAS)
 */

const API_BASE = "http://localhost:8080";
let selectedExpertId = null;
let currentSelectedSlotId = null;
let currentMyOrders = [];
// ==========================================
// Error modal repository. Because there are too many error modals,
// I'm too lazy to find them one by one. Let's beautify them together.
// ==========================================


// --- 1. Global Configuration ---
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

// 1. Added async
async function logout() {
    // 2. Switched to advanced modal and await
    const isConfirmed = await showConfirm("Are you sure you want to log out?");

    // 3. If confirmed, perform cleanup and redirection
    if (isConfirmed) {
        localStorage.clear(); // Clear Token
        window.location.href = 'landingpage.html';
    }
}

// --- 2. Page Initialization ---
$(document).ready(() => {
    if (checkLogin()) {
        initList();
        loadFilterData();// New: Load real expertise and level data from backend upon page load!
        // Core: Replace username and avatar in the top right corner with current login user info
        const currentUsername = localStorage.getItem('username') || 'User';
        $('#current-user-display').text(currentUsername);
        $('#nav-user-avatar').attr('src', getAvatar(currentUsername));
    }
});
function loadFilterData() {
    $.get(`${API_BASE}/api/specialists/filters`, (res) => {
        // 1. Dynamically render Category dropdown
        const categorySelect = $('#search-category');
        if (res.expertises && res.expertises.length > 0) {
            res.expertises.forEach(exp => {
                categorySelect.append(`<option value="${exp.id}">${exp.name}</option>`);
            });
        }

        // 2. Dynamically render Level dropdown
        const levelSelect = $('#search-level');
        if (res.levels && res.levels.length > 0) {
            res.levels.forEach(lvl => {
                // User-friendly text conversion
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

// --- Search Trigger Logic ---
function handleSearch() {
    const keyword = $('#search-input').val().trim();
    const categoryId = $('#search-category').val(); // This will definitely be the real ID
    const level = $('#search-level').val();         // This will definitely be JUNIOR/SENIOR/EXPERT

    initList(keyword, categoryId, level);
}

// --- Send parameters to the backend interface ---
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
        if (pageId === 'user-orders-page') loadMyOrders(); // Fetch data when switching to orders page
    }
}

function showJoinUs() {
    window.location.href = 'expert_apply.html';
}

// Fix: Search function trigger logic
function handleSearch() {
    // Grab keyword from input
    const keyword = $('#search-input').val().trim();

    // Grab Category ID from dropdown (ensure HTML ID is search-category)
    const categoryId = $('#search-category').val();

    // Grab Level from dropdown (ensure HTML ID is search-level)
    const level = $('#search-level').val();

    // Pass all three parameters to initList
    initList(keyword, categoryId, level);
}
function initList(keyword = '', categoryId = '', level = '') {
    let url = `${API_BASE}/api/specialists`;
    let queryParams = [];

    // 1. If keyword exists, add it
    if (keyword) {
        queryParams.push(`keyword=${encodeURIComponent(keyword)}`);
    }
    // 2. If category is selected and not "All/Empty", pass category ID to expertiseId
    if (categoryId && categoryId !== '' && categoryId !== 'ALL') {
        queryParams.push(`expertiseId=${categoryId}`);
    }
    // 3. If level is selected and not "All/Empty", pass level to level
    if (level && level !== '' && level !== 'ALL') {
        queryParams.push(`level=${level}`);
    }

    // Smart URL concatenation
    if (queryParams.length > 0) {
        url += '?' + queryParams.join('&');
    }

    console.log("Demonstrating the powerful search URL:", url); // Check console (F12) to view this

    // The $.get rendering logic remains unchanged
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

// --- 4. Booking and Calendar Logic (New dynamic dual-month upgrade + penetration prevention) ---
function goToCalendar() {
    $.get(`${API_BASE}/api/timeslots/specialist/${selectedExpertId}/available-dates`, (data) => {

        // Core Fix 1: Compatible with backend's {"data": [...]} wrapper!
        let dates = [];
        if (Array.isArray(data)) dates = data;
        else if (data.data && Array.isArray(data.data)) dates = data.data;
        else if (data.availableDates) dates = data.availableDates;

        const body = $('#calendar-body').empty();

        // 1. Render day headers
        ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT'].forEach(day => body.append(`<div class="cal-header-day">${day}</div>`));

        // 2. Get current system Year and Month
        const today = new Date();
        let currentYear = today.getFullYear();
        let currentMonth = today.getMonth();

        // 3. Core Magic: Loop to render 2 months (current m=0, next month m=1)
        for (let m = 0; m < 2; m++) {
            let renderMonth = currentMonth + m;
            let renderYear = currentYear;

            // Handle year crossover logic
            if (renderMonth > 11) {
                renderMonth -= 12;
                renderYear += 1;
            }

            const firstDay = new Date(renderYear, renderMonth, 1).getDay();
            const daysInMonth = new Date(renderYear, renderMonth + 1, 0).getDate();

            // Insert month divider
            body.append(`
                <div style="grid-column: span 7; text-align: center; font-weight: 800; color: #3b82f6; margin-top: 15px; margin-bottom: 5px; font-size: 1.1rem;">
                    ${renderYear} - ${String(renderMonth + 1).padStart(2, '0')}
                </div>
            `);

            // Render empty slots before the 1st day
            for (let i = 0; i < firstDay; i++) {
                body.append(`<div></div>`);
            }

            // Render the actual days of the month
            for (let i = 1; i <= daysInMonth; i++) {
                const monthStr = String(renderMonth + 1).padStart(2, '0');
                const dayStr = String(i).padStart(2, '0');
                const dateStr = `${renderYear}-${monthStr}-${dayStr}`;

                // Fuzzy match: Regardless of timestamp, highlight if the start matches!
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

        // Core Fix 2: Compatible with timeslot interface JSON wrapper!
        let slots = [];
        if (Array.isArray(res)) slots = res;
        else if (res.data && Array.isArray(res.data)) slots = res.data;
        else if (res.timeSlots) slots = res.timeSlots;

        // Core Fix 3: Smart time extractor (prevents "2026-05-05 09:00:00" slicing errors)
        const safeExtractTime = (timeStr) => {
            if (!timeStr) return '--:--';
            if (timeStr.includes('T')) return timeStr.split('T')[1].substring(0, 5);
            if (timeStr.includes(' ')) return timeStr.split(' ')[1].substring(0, 5);
            return timeStr.substring(0, 5); // Fallback for normal "09:00:00"
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
    // 1. Grab current specialist info from detail page
    const specName = $('#pName').text();
    const specProf = $('#pExpertise').text();
    const avatarSrc = $('#pAvatar').attr('src');
    const dateStr = $('#display-date').text();

    // Grab selected time range text from card
    const timeStr = $(`#slot-card-${currentSelectedSlotId} .slot-time`).text();

    // 2. Get rate and calculate total price
    const hourlyFee = parseFloat($('#pRate').text().replace(/[^0-9.]/g, '')) || 0;

    // Simple duration calculation: extract difference between hours
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

    // Clear previous notes
    $('#booking-notes').val('');

    // 4. Pop up the checkout modal!
    $('#customerConfirmModal').modal('show');
});

$('#final-submit-booking-btn').off('click').on('click', function() {
    const btn = $(this);
    const notes = $('#booking-notes').val().trim() || "Web Booking.";

    // Disable button to prevent double clicks
    btn.prop('disabled', true).html('<span class="spinner-border spinner-border-sm me-2"></span>Processing...');

    // Carry real Token and data for backend authorization
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
            // Get error text from backend
            const errText = await res.text();

            if (res.status === 409) {
                alert("Action Failed: Sorry, this schedule has been reserved by others.");
                $('#customerConfirmModal').modal('hide');
                showSlots($('#display-date').text());
            }
            // Core intercept: Capture 500 DB duplicate errors for user-friendly translation
            else if (res.status === 500 && (errText.includes('Duplicate entry') || errText.includes('Constraint'))) {
                alert("Action Failed: You have recently cancelled an appointment for this time slot. The system does not allow immediate re-booking of the same slot to prevent spam. Please choose another time!");
                $('#customerConfirmModal').modal('hide');
            }
            else {
                // Try parsing other normal JSON errors
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

// --- Report Logic: Pop up Modal ---
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
    // Record original state to prevent freezing
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
                // Focus: If 400 (e.g. duplicate report), show error from backend
                alert("Action Failed: " + (resData.error || "Submit error"));
            }
        })
        .catch(err => {
            console.error(err);
            alert("Network Error: Backend is down or unreachable.");
        })
        .finally(() => {
            // Restore button status regardless of result
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
    $('#detail-spec-name').text('Loading...');
    $('#orderDetailModal').modal('show');

    fetch(`${API_BASE}/api/bookings/myOrders`, {
        method: 'GET',
        headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
    })
        .then(res => res.json())
        .then(data => {
            let orderList = [];
            if (Array.isArray(data)) {
                orderList = data;
            } else if (data.data && Array.isArray(data.data)) {
                orderList = data.data;
            } else if (data.content && Array.isArray(data.content)) {
                orderList = data.content;
            }

            const order = orderList.find(o => String(o.id) === String(orderId));

            if (!order) {
                $('#orderDetailModal').modal('hide');
                alert("Action Failed: Order data not found on server.");
                return;
            }

            $('#detail-order-id').text('#' + order.id);

            const statusEl = $('#detail-status');
            statusEl.text(order.status).removeClass('bg-success bg-warning bg-danger bg-secondary');

            if (order.status === 'COMPLETED' || order.status === 'CONFIRMED') {
                statusEl.addClass('bg-success');
            } else if (order.status === 'PENDING') {
                statusEl.addClass('bg-warning text-dark');
            } else if (order.status === 'CANCELLED' || order.status === 'CANCELED') {
                statusEl.addClass('bg-danger');
            } else {
                statusEl.addClass('bg-secondary');
            }

            const expName = order.specialistName || 'Specialist';
            $('#detail-spec-name').text(expName);
            $('#detail-avatar').attr('src', getAvatar(expName));

            $('#detail-date').text(order.slotDate || order.date || 'N/A');
            const startTime = order.startTime ? order.startTime.substring(0, 5) : '--:--';
            const endTime = order.endTime ? order.endTime.substring(0, 5) : '--:--';
            $('#detail-time').text(`${startTime} - ${endTime}`);

            $('#detail-total-fee').text(order.totalAmount || order.totalFee ? `${order.totalAmount || order.totalFee} Yuan` : 'Consult Rate');
            $('#detail-notes').text(order.notes || 'No special notes provided.');

        })
        .catch(err => {
            console.error("Fetch Error:", err);
            $('#orderDetailModal').modal('hide');
            alert("Network Error: Failed to fetch order details.");
        });
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

                // Core Modification: Don't ask via logout(), clear Token directly and force kick back to login.html
                localStorage.clear();
                window.location.href = 'login.html';
            } else {
                alert("Failed to update: " + await res.text());
            }
        }).catch(err => alert("Network error! Make sure the backend is running."))
            .finally(() => btn.prop('disabled', false).text('Update Password'));
    });
}