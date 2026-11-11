async function syncSystemBranding() {
    const nameTags = document.querySelectorAll('[id="displayCompanyName"]');

    try {
        const response = await fetch('http://localhost:9090/api/system/config');

        if (!response.ok) throw new Error("Backend Offline");

        const data = await response.json();

        nameTags.forEach(tag => {

            tag.innerHTML = `SmartHR <span class="text-blue-500">${data.company_name.split(' ')[1] || 'AI'}</span>`;
        });

    } catch (err) {
        console.warn("Using local fallback branding.");
        nameTags.forEach(tag => {
            tag.innerHTML = `SmartHR <span class="text-blue-500">AI</span>`;
        });
    }
}
document.addEventListener('DOMContentLoaded', syncSystemBranding);