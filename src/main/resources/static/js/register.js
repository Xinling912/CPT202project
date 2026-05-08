$(document).ready(function() {

    // ==========================================
    // Feature 1: Send Verification Code with 60s Countdown
    // ==========================================
    $('#sendCodeBtn').on('click', function() {
        const emailInput = $('#reg-email').val();

        if (!emailInput) {
            alert("Please enter your email first!");
            return;
        }

        const btn = $(this);
        let timeLeft = 60;

        // Disable button and start countdown
        btn.prop('disabled', true).addClass('opacity-70 cursor-not-allowed');

        const timer = setInterval(() => {
            btn.text(timeLeft + 's');
            timeLeft--;
            if (timeLeft < 0) {
                clearInterval(timer);
                btn.prop('disabled', false).removeClass('opacity-70 cursor-not-allowed');
                btn.text('Send');
            }
        }, 1000);

        // Call the verification code interface
        fetch('http://localhost:8080/api/users/verify-code', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ email: emailInput })
        })
            .then(response => response.json())
            .then(data => {
                // Backend return message (e.g., "Verification code sent to email")
                console.log(data.message);
                // alert(data.message); could be used here to notify the user
            })
            .catch(error => {
                console.error('Failed to send verification code:', error);
                alert("Failed to send verification code. Check server.");
            });
    });


    // ==========================================
    // Feature 2: Submit Complete Registration Info
    // ==========================================
    $('#registerForm').on('submit', function(e) {
        e.preventDefault();

        const btn = $('#registerBtn');
        const username = $('#reg-username').val(); // Added: get username
        const email = $('#reg-email').val();
        const pwd = $('#reg-password').val();
        const confirm = $('#reg-confirm').val();
        const verifyCode = $('#reg-code').val();   // Added: get verification code

        if (pwd !== confirm) {
            alert("Passwords do not match!");
            $('#reg-confirm').trigger('focus');
            return;
        }

        btn.text('Processing...')
            .prop('disabled', true)
            .addClass('opacity-70 cursor-not-allowed');

        // Assemble data strictly according to the RegisterRequest defined in the backend
        const userData = {
            username: username,
            password: pwd,
            email: email,
            role: "CUSTOMER", // Role hardcoded as normal customer
            verifyCode: verifyCode
        };

        // Call the formal registration interface
        fetch('http://localhost:8080/api/users/register', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(userData)
        })
            .then(async response => {
                const data = await response.json(); // Parse the returned JSON info

                if (response.ok) {
                    // HTTP status code 200 or 201, registration successful!
                    btn.text('Register Account').prop('disabled', false).removeClass('opacity-70 cursor-not-allowed');
                    alert(data.message); // Pop up "Registration successful"
                    window.location.href = 'login.html'; // Redirect to login page immediately
                } else {
                    // Registration failed (e.g., incorrect code, email already taken)
                    alert("Registration failed: " + data.message);
                    btn.text('Register Account').prop('disabled', false).removeClass('opacity-70 cursor-not-allowed');
                }
            })
            .catch(error => {
                console.error('Error:', error);
                alert("Server connection failed! Make sure your Spring Boot app is running.");
                btn.text('Register Account').prop('disabled', false).removeClass('opacity-70 cursor-not-allowed');
            });
    });

    // Back to login page button logic
    $('#go-to-login').on('click', function() {
        window.location.href = 'login.html';
    });
});