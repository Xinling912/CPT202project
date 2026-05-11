// 1. Smart extraction of backend error messages (handles various JSON error formats)

async function parseError(response) {

    const text = await response.text();

    try {

// Try to parse the backend error into a JSON object

        const json = JSON.parse(text);

// If the backend provided an "error" field, use it; if "message", use that; otherwise return original text

        return json.error || json.message || text;

    } catch (e) {

// If the backend response is not JSON (e.g., 500 page source code), return as plain text

        return text;

    }

}



// 2. Modern floating toast notification (completely replaces the ugly window.alert)

window.showToast = function(message, type = 'error') {

// Check if a container for toast notifications exists; if not, create one

    let container = document.getElementById('toast-container');

    if (!container) {

        container = document.createElement('div');

        container.id = 'toast-container';

        container.style.cssText = 'position: fixed; top: 24px; left: 50%; transform: translateX(-50%); z-index: 9999; display: flex; flex-direction: column; gap: 12px; pointer-events: none;';

        document.body.appendChild(container);

    }



// Set colors and icons for different status types

    const bgColor = type === 'success' ? '#10b981' : (type === 'warning' ? '#f59e0b' : '#ef4444');

    const icon = type === 'success' ? 'bi-check-circle-fill' : (type === 'warning' ? 'bi-exclamation-triangle-fill' : 'bi-x-circle-fill');



// Create a toast notification element

    const toast = document.createElement('div');

    toast.style.cssText = `background: ${bgColor}; color: white; padding: 12px 24px; border-radius: 50px; box-shadow: 0 10px 25px rgba(0,0,0,0.15); display: flex; align-items: center; gap: 10px; font-weight: 600; font-size: 0.95rem; opacity: 0; transform: translateY(-20px); transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);`;

    toast.innerHTML = `<i class="bi ${icon} fs-5"></i> <span>${message}</span>`;



    container.appendChild(toast);



// Animation: Slide in and display

    requestAnimationFrame(() => {

        toast.style.opacity = '1';

        toast.style.transform = 'translateY(0)';

    });



// Animation: Automatically disappear after 3 seconds

    setTimeout(() => {

        toast.style.opacity = '0';

        toast.style.transform = 'translateY(-20px)';

        setTimeout(() => toast.remove(), 300);

    }, 3000);

}

window.showConfirm = function(message) {
    return new Promise((resolve) => {
        // 1. Create a full-screen semi-transparent overlay
        const overlay = document.createElement('div');
        overlay.style.cssText = 'position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; background: rgba(0, 0, 0, 0.4); backdrop-filter: blur(2px); display: flex; justify-content: center; align-items: center; z-index: 10000; opacity: 0; transition: opacity 0.2s ease;';

        // 2. Create the modal body
        const box = document.createElement('div');
        box.style.cssText = 'background: white; width: 360px; padding: 24px; border-radius: 12px; box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04); transform: scale(0.95); transition: transform 0.2s cubic-bezier(0.175, 0.885, 0.32, 1.275);';

        // 3. Fill HTML structure (Title, Content, two Buttons)
        box.innerHTML = `
            <div style="display: flex; align-items: center; gap: 10px; margin-bottom: 12px; color: #1f2937;">
                <i class="bi bi-question-circle-fill text-primary" style="font-size: 1.5rem;"></i>
                <h5 style="margin: 0; font-weight: 700; font-size: 1.15rem;">Action Required</h5>
            </div>
            <p style="color: #4b5563; font-size: 0.95rem; margin-bottom: 24px; line-height: 1.5;">${message}</p>
            <div style="display: flex; justify-content: flex-end; gap: 12px;">
                <button id="custom-confirm-cancel" style="padding: 8px 16px; border: 1px solid #d1d5db; background: white; color: #4b5563; border-radius: 6px; font-weight: 600; cursor: pointer; transition: background 0.2s;">Cancel</button>
                <button id="custom-confirm-ok" style="padding: 8px 16px; border: none; background: #3b82f6; color: white; border-radius: 6px; font-weight: 600; cursor: pointer; box-shadow: 0 1px 2px rgba(0,0,0,0.05); transition: background 0.2s;">Confirm</button>
            </div>
        `;

        overlay.appendChild(box);
        document.body.appendChild(overlay);

        // 4. Entrance animation
        requestAnimationFrame(() => {
            overlay.style.opacity = '1';
            box.style.transform = 'scale(1)';
        });

        // 5. Bind click events (Core: resolve the Promise after clicking)
        const btnCancel = box.querySelector('#custom-confirm-cancel');
        const btnOk = box.querySelector('#custom-confirm-ok');

        // Hover effect interactions
        btnCancel.onmouseover = () => btnCancel.style.background = '#f3f4f6';
        btnCancel.onmouseout = () => btnCancel.style.background = 'white';
        btnOk.onmouseover = () => btnOk.style.background = '#2563eb';
        btnOk.onmouseout = () => btnOk.style.background = '#3b82f6';

        // Click Cancel -> return false, and destroy modal
        btnCancel.onclick = () => {
            overlay.style.opacity = '0';
            setTimeout(() => overlay.remove(), 200);
            resolve(false);
        };

        // Click Confirm -> return true, and destroy modal
        btnOk.onclick = () => {
            overlay.style.opacity = '0';
            setTimeout(() => overlay.remove(), 200);
            resolve(true);
        };
    });
};
window.originalAlert = window.alert; // Backup the native alert just in case

window.alert = function(message) {
    // Intelligently determine which color/status to use
    let type = 'error'; // Default to error
    let msgStr = String(message).toLowerCase();

    if (msgStr.includes('success') || msgStr.includes('updated') || msgStr.includes('approved') || msgStr.includes('成功')) {
        type = 'success';
    } else if (msgStr.includes('please') || msgStr.includes('expired') || msgStr.includes('请')) {
        type = 'warning';
    }

    // Call our custom high-end toast notification!
    showToast(message, type);
};
// Avatar matching logic
function getAvatar(username, realName = '') {
// 1. First, check local storage: has this account uploaded a custom avatar?
    const customAvatar = localStorage.getItem(`custom_avatar_${username}`);
    if (customAvatar) {
        return customAvatar; // If yes, return the user-uploaded avatar (Base64 encoded)
    }


    if (username === 'ShenShaohui' ) return `images/ssh.jpg`;
    if (username === 'XingjianWu' ) return `images/specialist1.png`;
    if (username === 'XinlingDu' ) return `images/specialist3.png`;
    if (username === 'Carrot') return `images/carrot.png`;

    if (username === 'Tudou' ) return `images/potato.jpg`;
    if (username === 'zhangjuyi' ) return `images/zhangjuyi.jpg`;
    if (username === 'Faker' ) return `images/faker.jpg`;
    if (username === 'Qlin') return `images/qlin.jpg`;

    // Fallback: If no matches, use an automatically generated initials avatar
    return `https://api.dicebear.com/7.x/initials/svg?seed=${username}`;
}