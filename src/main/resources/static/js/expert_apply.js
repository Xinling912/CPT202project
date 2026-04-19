$(document).ready(function() {
    // 1. Utility to get the latest Authorization header
    // This ensures that even if the token changes, we always grab the current one
    const getAuthHeaders = () => {
        const token = localStorage.getItem('token');
        if (!token) {
            alert("Please log in first!");
            window.location.href = 'login.html';
            return {};
        }
        return {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + token
        };
    };

    // 2. Logic to control the "Other" input field visibility
    $('#exp-domain-select').change(function() {
        if ($(this).val() === 'other') {
            $('#exp-domain-input').show().attr('required', true);
        } else {
            $('#exp-domain-input').hide().attr('required', false).val('');
        }
    });

    // 3. Form Submission Logic
    $('#expertForm').on('submit', function(e) {
        e.preventDefault();

        const btn = $('#applyBtn');
        btn.text('Submitting...').prop('disabled', true);

        // --- Data Extraction with Type Casting ---
        const isOther = $('#exp-domain-select').val() === 'other';

        const expertData = {
            // Field mapping strictly aligned with Specialist API
            realName: $('#exp-name').val(),                      // String
            level: $('#exp-level').val(),                        // Enum: JUNIOR, SENIOR, EXPERT
            hourlyFee: Number($('#exp-fee').val()),              // Must be Number for Decimal(10,2)
            resume: $('#exp-resume').val(),                      // String

            // Mutually exclusive logic for Professional Field [cite: 124]
            // If selecting existing: provide expertiseId (Number)
            expertiseId: isOther ? null : Number($('#exp-domain-select').val()),

            // If selecting other: provide newExpertiseName (String)
            // This is the field Admin will see in the review list [cite: 123, 155]
            newExpertiseName: isOther ? $('#exp-domain-input').val() : null
        };

        // 4. Send Request to Backend
        fetch('http://localhost:8080/api/specialists/apply', { // Using corrected path [cite: 120]
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify(expertData)
        })
        .then(async response => {
            if (response.ok) {
                // Success: Information stored in PENDING status for Admin review [cite: 135, 153]
                alert("Application submitted! Please wait for admin review.");
                window.location.href = 'home.html';
            } else if (response.status === 401) {
                alert("Session expired. Please log in again.");
                window.location.href = 'login.html';
            } else {
                const errorData = await response.text();
                alert("Submission failed: " + errorData);
            }
        })
        .catch(error => {
            console.error('Connection Error:', error);
            alert("Could not connect to the server. Please ensure the backend is running.");
        })
        .finally(() => {
            btn.text('Come up for review').prop('disabled', false);
        });
    });

    // Back button logic
    $('#go-back').on('click', function() {
        window.location.href = 'home.html';
    });
});