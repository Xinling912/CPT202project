const API_BASE = "http://localhost:8080";

$(document).ready(function() {

    const token = localStorage.getItem('token');
    if (!token) {
        alert("Please log in first!");
        window.location.href = 'login.html';
        return;
    }

    // 1. First thing after entering the page: Request current application status from Shaohui!
    checkApplyStatus();

    // 2. Dynamically load the real expertise list from the database!
    loadExpertiseCategories();


    // Handle changes in the expertise domain dropdown
    $('#exp-domain-select').on('change', function() {
        const selectedValue = $(this).val();
        const $otherInput = $('#exp-domain-input');

        $(this).css('color', '#1f2937');

        if (selectedValue === 'other') {
            $otherInput.fadeIn().attr('required', true).focus();
        } else {
            $otherInput.hide().attr('required', false).val('');
        }
    });

    $('#exp-level').on('change', function() {
        $(this).css('color', '#1f2937');
    });

    $('#go-back').on('click', function() {
        window.history.back();
    });

    // Handle form submission
    $('#expertForm').on('submit', function(e) {
        e.preventDefault();

        const $btn = $('#applyBtn');
        const originalText = $btn.text();
        $btn.prop('disabled', true).text('Submitting...').css('opacity', '0.7');

        const domainSelectValue = $('#exp-domain-select').val();
        const isOther = domainSelectValue === 'other';

        const expertData = {
            realName: $('#exp-name').val().trim(),
            level: $('#exp-level').val(),
            hourlyFee: Number($('#exp-fee').val()),
            resume: $('#exp-resume').val().trim(),
            expertiseId: isOther ? null : Number(domainSelectValue),
            // Core: If "Other", send the input text; the backend will automatically create a new expertise (not automatic; only created after successful enrollment)
            newExpertiseName: isOther ? $('#exp-domain-input').val().trim() : null
        };

        fetch(`${API_BASE}/api/specialists/apply`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ' + token
            },
            body: JSON.stringify(expertData)
        })
            .then(async response => {
                if (response.ok) {
                    alert("Application submitted successfully! Please wait for admin review.");
                    // After successful submission, refresh current page; it will automatically turn into the yellow "Under Review" state!
                    window.location.reload();
                } else if (response.status === 401) {
                    alert("Session expired. Please log in again.");
                    localStorage.clear();
                    window.location.href = 'login.html';
                } else {
                    const errorText = await response.text();
                    try {
                        const errorObj = JSON.parse(errorText);
                        alert("Submission failed: " + (errorObj.error || errorObj.message || errorText));
                    } catch(e) {
                        alert("Submission failed: " + errorText);
                    }
                }
            })
            .catch(error => {
                alert("Network error. Please make sure the backend server is running.");
            })
            .finally(() => {
                $btn.prop('disabled', false).text(originalText).css('opacity', '1');
            });
    });
});

// ==========================================
// Core Function 1: Fetch Real Expertise List
// ==========================================
function loadExpertiseCategories() {
    fetch(`${API_BASE}/api/expertise/list`, {
        headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
    })
        .then(res => res.json())
        .then(resData => {
            const list = resData.data || [];
            const select = $('#exp-domain-select');

            select.empty();
            select.append('<option value="" disabled selected>Please select your professional field.</option>');

            // Dynamically inject expertise from the database
            list.forEach(exp => {
                select.append(`<option value="${exp.id}">${exp.name}</option>`);
            });

            // Finally, add the "Other" option
            select.append('<option value="other">Other (Custom New Expertise)...</option>');
        })
        .catch(err => console.error("Failed to load expertise:", err));
}

// ==========================================
// Core Function 2: Track User Application Status
// ==========================================
function checkApplyStatus() {
    fetch(`${API_BASE}/api/specialists/apply-status`, {
        headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` },
        cache: 'no-store'
    })
        .then(res => res.json())
        .then(data => {
            const status = data.status;
            const msg = data.message;
            const banner = $('#status-banner');
            const form = $('#expertForm');

            if (status === 'APPLY_PENDING') {
                // Under Review: Show yellow banner, hide form
                banner.css({'display': 'block', 'background': '#fef3c7', 'color': '#92400e', 'border': '1px solid #f59e0b'});
                banner.html(`⏳ Update Under Review: ${msg}`);
                form.hide();

                // Fix point: Must use "else if" here!!!
            } else if (status === 'APPLY_REJECTED') {
                // Rejected: Show red banner, keep form
                banner.css({'display': 'block', 'background': '#fee2e2', 'color': '#b91c1c', 'border': '1px solid #ef4444'});
                banner.html(`
                <div style="display:flex; align-items:center;">
                    <i class="bi bi-x-circle-fill" style="font-size:1.5rem; margin-right:10px;"></i>
                    <div>
                        <div style="font-size:1.1rem;">Application Rejected</div>
                        <div style="font-weight:normal; font-size:0.85rem; margin-top:3px;">Reason: ${msg}. Please modify your information below and submit again.</div>
                    </div>
                </div>
                `);
                form.show(); // Ensure the form is shown for re-filling

            } else if (status === 'IS_ACTIVE_SPECIALIST' || status.startsWith('EDIT_')) {
                window.location.href = 'specialist.html';
            }
        })
        .catch(err => console.error("Failed to check status:", err));
}