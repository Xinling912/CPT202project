$(document).ready(function() {
    // Redirect to login page after success
    $('#back-to-login').on('click', function() {
        window.location.href = 'login.html';
    });
});
// ---  Earnings & Finances Logic ---

// Click "Add New Card" button to hide the list and show the form
$('#show-add-card-btn').on('click', function() {
    $('#add-card-form').removeClass('d-none').addClass('animation-fade-in');
    $('#linked-cards-list').addClass('d-none');
    $(this).addClass('d-none'); // Hide the "Add" button itself
});

// Click "Cancel" or "Save" button to hide the form and restore the list
$('#cancel-card-btn, #save-card-btn').on('click', function(e) {
    e.preventDefault(); // Prevent default form submission from refreshing the page

    $('#add-card-form').addClass('d-none').removeClass('animation-fade-in');
    $('#linked-cards-list').removeClass('d-none');
    $('#show-add-card-btn').removeClass('d-none');

    // If the "Save" button was clicked, show an alert
    if(e.target.id === 'save-card-btn') {
        alert("New card linked successfully!");
    }
});