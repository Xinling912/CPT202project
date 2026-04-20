$(document).ready(function() {

    // ==========================================
    // 1. UI Navigation (Login <-> Forgot Password)
    // ==========================================

    $('#go-to-forgot').on('click', function() {
        $('#login-section').hide();
        $('#forgot-section').fadeIn();
    });

    $('#back-to-login').on('click', function() {
        $('#forgot-section').hide();
        $('#login-section').fadeIn();
        $('#forgotForm')[0].reset(); // Clear form when navigating back
    });

    $('#go-to-register').on('click', function() {
        window.location.href = 'register.html';
    });


    // ==========================================
    // 2. Login Logic
    // ==========================================
    $('#loginForm').on('submit', function(e) {
        e.preventDefault();

        const btn = $('#loginBtn');
        const inputAccount = $('#log-email').val();
        const inputPwd = $('#log-password').val();

        btn.text('Verifying...')
           .prop('disabled', true)
           .addClass('opacity-70 cursor-not-allowed');

        const loginData = {
            usernameOrEmail: inputAccount,
            password: inputPwd
        };

        fetch('http://localhost:8080/api/users/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(loginData)
        })
        .then(async response => {
            const data = await response.json();
            console.log("[Debug] Full backend response:", data);

            if (response.ok) {
                const actualToken = data.token;
                if (actualToken) {
                    localStorage.setItem('token', actualToken);
                    console.log("[Debug] Token saved to local storage:", actualToken);
                } else {
                    console.warn("Warning: Backend returned success, but no Token was found!");
                }

                const actualUsername = data.username || (data.data && data.data.username) || inputAccount;
                localStorage.setItem('username', actualUsername);

                if (data.role) {
                    localStorage.setItem('role', data.role);
                    console.log("[Debug] User role saved:", data.role);
                }
                if (data.userId) {
                    localStorage.setItem('userId', data.userId);
                    console.log("[Debug] User ID saved:", data.userId);
                }

                alert(data.message || "Login successful!");

                // Redirect based on role
                if (data.role === 'SPECIALIST') {
                    window.location.href = 'specialist.html';
                } else if (data.role === 'ADMIN') {
                    window.location.href = 'admin.html';
                } else {
                    window.location.href = 'booking.html';
                }
            } else {
                alert("Login failed: " + (data.message || "Unknown error, please check your credentials."));
            }
        })
        .catch(error => {
            console.error('Error:', error);
            alert("Server connection failed! Please check if your backend is running.");
        })
        .finally(() => {
            btn.text('Log In').prop('disabled', false).removeClass('opacity-70 cursor-not-allowed');
        });
    });


    // ==========================================
    // 3. Send Verification Code Logic
    // ==========================================
    $('#sendCodeBtn').on('click', function() {
        const email = $('#forgot-email').val();
        if (!email) {
            alert('Please enter your email address first.');
            return;
        }

        const btn = $(this);
        btn.prop('disabled', true).text('Sending...');

        fetch('http://localhost:8080/api/users/verify-code', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email: email })
        })
        .then(async response => {
            const data = await response.json();
            if (response.ok) {
                alert(data.message || 'Verification code sent to your email!');
                // 60-second cooldown timer
                let timeLeft = 60;
                const timer = setInterval(() => {
                    if (timeLeft <= 0) {
                        clearInterval(timer);
                        btn.prop('disabled', false).text('Send');
                    } else {
                        btn.text(`${timeLeft}s`);
                        timeLeft -= 1;
                    }
                }, 1000);
            } else {
                alert('Failed to send: ' + (data.message || 'Unknown error'));
                btn.prop('disabled', false).text('Send');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            alert("Network error, unable to send the verification code.");
            btn.prop('disabled', false).text('Send');
        });
    });


    // ==========================================
    // 4. Submit Reset Password Logic
    // ==========================================
    $('#forgotForm').on('submit', function(e) {
        e.preventDefault();

        const email = $('#forgot-email').val();
        const code = $('#forgot-code').val();
        const newPassword = $('#forgot-new-password').val();
        const btn = $('#resetBtn');

        btn.text('Resetting...').prop('disabled', true);

        const requestData = {
            email: email,
            verifyCode: code,
            newPassword: newPassword
        };

        fetch('http://localhost:8080/api/users/forgot-password', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(requestData)
        })
        .then(async response => {
            const data = await response.json();
            if (response.ok) {
                alert(data.message || 'Password reset successfully! Please log in with your new password.');
                // Switch back to login page on success
                $('#forgotForm')[0].reset();
                $('#forgot-section').hide();
                $('#login-section').fadeIn();
            } else {
                alert('Reset failed: ' + (data.message || 'Please check if your verification code is correct.'));
            }
        })
        .catch(error => {
            console.error('Error:', error);
            alert("Server connection failed!");
        })
        .finally(() => {
            btn.text('Reset Password').prop('disabled', false);
        });
    });

});