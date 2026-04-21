$(document).ready(function() {


    const token = localStorage.getItem('token');
    if (!token) {
        alert("Please log in first!");
        window.location.href = 'login.html';
        return;
    }


    // 处理专业领域下拉框变化
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
            newExpertiseName: isOther ? $('#exp-domain-input').val().trim() : null,
        };

        fetch('http://localhost:8080/api/specialists/apply', {
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
                window.location.href = 'booking.html';
            } else if (response.status === 401) {
                alert("Session expired. Please log in again.");
                localStorage.removeItem('token');
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
            console.error('Fetch error:', error);
            alert("Network error. Please make sure the backend server is running.");
        })
        .finally(() => {
            $btn.prop('disabled', false).text(originalText).css('opacity', '1');
        });
    });
});