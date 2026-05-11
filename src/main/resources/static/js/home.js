$(document).ready(function() {
    // 1. Core Fix: Read username from the browser; if not found, default to 'Guest'
    let displayName = localStorage.getItem('username') || 'Guest';

    // 2. Render directly as @ + username
    $('#user-email').text('@' + displayName);

    // 3. Click any "Search More" button to redirect to the booking page
    $('#searchMoreTopBtn, #searchMoreBottomBtn').on('click', function() {
        window.location.href = 'booking.html';
    });

    // 4. Click "Join us" to redirect to the expert application page
    $('#joinUsBtn').on('click', function() {
        window.location.href = 'expert_apply.html';
    });

    // 5. Logout button logic (Upgraded: Complete state cleanup)
    // 1. Added async
    async function logout() {
        // 2. Replaced with advanced modal and await
        const isConfirmed = await showConfirm("Are you sure you want to log out?");

        // 3. If confirmed, perform cleanup and redirection
        if (isConfirmed) {
            localStorage.clear(); // Clear Token
            window.location.href = 'landingpage.html';
        }
    }
});