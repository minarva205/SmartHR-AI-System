let employeeList = [];

function createEmployeeCard(emp) {
    const profileSrc = emp.profilePic ? `assets/images/${emp.profilePic}` : 'https://ui-avatars.com/api/?name=' + encodeURIComponent(emp.fullName || 'User') + '&background=random';
    const fullNameSafe = encodeURIComponent(emp.fullName || 'User');

    return `
        <div class="bg-white p-6 rounded-2xl shadow-sm border border-slate-200 feature-card text-center hover:shadow-md transition-all cursor-pointer"
             onmouseover="showHoverDetail(${emp.id})" onmouseout="hideHoverDetail()">
            <div class="relative inline-block mb-4">
                <img src="${profileSrc}"
                     class="w-24 h-24 rounded-full object-cover border-4 border-blue-50 shadow-lg mx-auto"
                     onerror="this.onerror=null; this.src='https://ui-avatars.com/api/?name=${fullNameSafe}&background=random'">
            </div>
            <h3 class="font-bold text-lg text-slate-800 mb-2">${emp.fullName}</h3>
            <p class="text-blue-600 text-sm font-semibold mb-3">${emp.designation || 'Designation not set'}</p>
            <div class="flex items-center justify-center space-x-2 text-xs text-slate-400">
                <i class="fas fa-building"></i>
                <span>${emp.department || 'Department not set'}</span>
            </div>
        </div>
    `;
}

function showEmployeeDetail(employeeId) {
    const detailPanel = document.getElementById('employee-detail-panel');
    const employee = employeeList.find(emp => emp.id === employeeId);

    if (!detailPanel || !employee) return;

    detailPanel.innerHTML = `
        <div class="bg-slate-50 border border-slate-200 rounded-3xl p-8 shadow-lg">
            <div class="flex flex-col lg:flex-row gap-8 items-start lg:items-center">
                <img src="${employee.profilePic ? `assets/images/${employee.profilePic}` : 'https://ui-avatars.com/api/?name=' + encodeURIComponent(employee.fullName) + '&background=random'}"
                     class="w-36 h-36 rounded-full object-cover border-4 border-blue-100 shadow-xl"
                     onerror="this.onerror=null; this.src='https://ui-avatars.com/api/?name=${encodeURIComponent(employee.fullName)}&background=random'">
                <div class="flex-1 text-slate-700">
                    <div class="flex flex-wrap items-center gap-3 mb-4">
                        <span class="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-blue-100 text-blue-700 text-sm font-semibold">
                            <i class="fas fa-user"></i> ${employee.fullName}
                        </span>
                        <span class="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-slate-100 text-slate-600 text-sm">
                            <i class="fas fa-briefcase"></i> ${employee.designation || 'N/A'}
                        </span>
                    </div>
                    <div class="grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm text-slate-600">
                        <div class="bg-white p-4 rounded-2xl border border-slate-200">
                            <p class="text-xs uppercase tracking-[0.2em] text-slate-400 mb-2">Employee ID</p>
                            <p class="font-semibold text-slate-800">${employee.id || 'N/A'}</p>
                        </div>
                        <div class="bg-white p-4 rounded-2xl border border-slate-200">
                            <p class="text-xs uppercase tracking-[0.2em] text-slate-400 mb-2">Department</p>
                            <p class="font-semibold text-slate-800">${employee.department || 'N/A'}</p>
                        </div>
                        <div class="bg-white p-4 rounded-2xl border border-slate-200">
                            <p class="text-xs uppercase tracking-[0.2em] text-slate-400 mb-2">Email</p>
                            <p class="font-semibold text-slate-800">${employee.email || 'N/A'}</p>
                        </div>
                        <div class="bg-white p-4 rounded-2xl border border-slate-200">
                            <p class="text-xs uppercase tracking-[0.2em] text-slate-400 mb-2">Designation</p>
                            <p class="font-semibold text-slate-800">${employee.designation || 'N/A'}</p>
                        </div>
                    </div>
                    <div class="mt-6">
                        <button onclick="hideEmployeeDetail()" 
                            class="inline-flex items-center justify-center px-6 py-3 bg-blue-600 text-white rounded-full font-semibold hover:bg-blue-700 transition">
                            <i class="fas fa-arrow-left mr-2"></i> Back to Directory
                        </button>
                    </div>
                </div>
            </div>
        </div>
    `;

    detailPanel.classList.remove('hidden');
    // Removed scrollIntoView to keep it inside the section
}

function showEmployeePlaceholder() {
    const detailPanel = document.getElementById('employee-detail-panel');
    if (!detailPanel) return;

    detailPanel.innerHTML = `
        <div class="bg-slate-50 border border-slate-200 rounded-3xl p-8 shadow-lg text-center text-slate-600">
            <p class="text-lg font-semibold text-slate-800 mb-4">Employee select karo</p>
            <p>Directory ke andar kisi employee pe click karo aur uska full data yahan dekho.</p>
        </div>
    `;
    detailPanel.classList.remove('hidden');
}

function showHoverDetail(employeeId) {
    const hoverPanel = document.getElementById('hover-detail-panel');
    const employee = employeeList.find(emp => emp.id === employeeId);

    if (!hoverPanel || !employee) return;

    hoverPanel.innerHTML = `
        <div class="bg-white border border-slate-200 rounded-3xl p-6 shadow-lg">
            <img src="${employee.profilePic ? `assets/images/${employee.profilePic}` : 'https://ui-avatars.com/api/?name=' + encodeURIComponent(employee.fullName) + '&background=random'}"
                 class="w-24 h-24 rounded-full object-cover border-4 border-blue-100 shadow-lg mx-auto mb-4"
                 onerror="this.onerror=null; this.src='https://ui-avatars.com/api/?name=${encodeURIComponent(employee.fullName)}&background=random'">
            <h3 class="font-bold text-lg text-slate-800 mb-2 text-center">${employee.fullName}</h3>
            <div class="space-y-2 text-sm text-slate-600">
                <div class="flex justify-between">
                    <span class="font-semibold">ID:</span>
                    <span>${employee.id || 'N/A'}</span>
                </div>
                <div class="flex justify-between">
                    <span class="font-semibold">Designation:</span>
                    <span>${employee.designation || 'N/A'}</span>
                </div>
                <div class="flex justify-between">
                    <span class="font-semibold">Department:</span>
                    <span>${employee.department || 'N/A'}</span>
                </div>
                <div class="flex justify-between">
                    <span class="font-semibold">Email:</span>
                    <span>${employee.email || 'N/A'}</span>
                </div>
            </div>
        </div>
    `;
}

function hideHoverDetail() {
    const hoverPanel = document.getElementById('hover-detail-panel');
    if (!hoverPanel) return;

    hoverPanel.innerHTML = `
        <div class="bg-blue-50 border-2 border-dashed border-blue-200 rounded-3xl p-8 text-center text-slate-500">
            <i class="fas fa-id-badge text-4xl mb-4 text-blue-300"></i>
            <p>Kisi bhi staff par mouse rakhein (Hover) uski details dekhne ke liye.</p>
        </div>
    `;
}

function filterEmployees() {
    const searchInput = document.getElementById('staffSearch');
    const filter = searchInput.value.toLowerCase();
    const container = document.getElementById('employee-container');

    if (!container) return;

    const filteredEmployees = employeeList.filter(emp =>
        emp.fullName.toLowerCase().includes(filter) ||
        (emp.department && emp.department.toLowerCase().includes(filter)) ||
        (emp.designation && emp.designation.toLowerCase().includes(filter))
    );

    container.innerHTML = filteredEmployees.map(emp => createEmployeeCard(emp)).join('');
}

async function loadEmployees() {
    const container = document.getElementById('employee-container');

    if (!container) return;

    try {
        const response = await fetch('/api/employees/all');

        if (!response.ok) {
            throw new Error('Database connection failed');
        }

        const employeesData = await response.json();
        // Map Employee entity to expected Employee format for UI
        employeeList = employeesData.map(e => ({
            id: e.id,
            fullName: e.fullName,
            designation: e.designation,
            department: e.department,
            email: e.email,
            profilePic: e.profilePic
        }));

        if (!employeeList || employeeList.length === 0) {
            container.innerHTML = `<p class="col-span-full text-center text-slate-500">No employees found in database.</p>`;
            hideHoverDetail();
            return;
        }

        container.innerHTML = employeeList.map(emp => createEmployeeCard(emp)).join('');
        hideHoverDetail();

    } catch (error) {
        console.error('Error details:', error);
        container.innerHTML = `
            <div class="col-span-full text-center py-10 text-red-500">
                <i class="fas fa-exclamation-triangle text-2xl mb-2"></i>
                <p>Backend connect nahi ho raha. Check if Java server is running on 9090.</p>
            </div>
        `;
    }
}


function showDirectory() {
    const directorySection = document.getElementById('employee-directory-display');
    if (directorySection) {
        directorySection.scrollIntoView({ behavior: 'smooth' });
    } else {
        alert("Directory section niche missing hai! Pehle wala naya HTML section add karein.");
    }
}


document.addEventListener('DOMContentLoaded', () => {
    // Logo sync check
    if (typeof syncLogo === "function") syncLogo();

    // Employees load karein
    loadEmployees();
});

function openAddEmployeeForm() {
    const modal = document.getElementById('addEmployeeModal');
    if (modal) {
        modal.classList.remove('hidden');
    }
}

function closeAddEmployeeForm() {
    const modal = document.getElementById('addEmployeeModal');
    if (modal) {
        modal.classList.add('hidden');
    }

    ['empName', 'empEmail', 'empDesignation', 'empDepartment', 'empProfilePicFile'].forEach(id => {
        const input = document.getElementById(id);
        if (input) input.value = '';
    });
}

async function submitEmployee(event) {
    if (event) event.preventDefault();

    let profilePicUrl = null;
    const fileInput = document.getElementById('empProfilePicFile');
    if (fileInput && fileInput.files.length > 0) {
        const formData = new FormData();
        formData.append("file", fileInput.files[0]);
        const uploadRes = await fetch('/api/employees/upload-profile-pic', {
            method: 'POST',
            body: formData
        });
        if (uploadRes.ok) {
            profilePicUrl = await uploadRes.text();
        }
    }

    const employee = {
        fullName: document.getElementById('empName').value,
        designation: document.getElementById('empDesignation').value,
        department: document.getElementById('empDepartment').value,
        email: document.getElementById('empEmail').value,
        profilePic: profilePicUrl
    };

    if (!employee.fullName || !employee.email) {
        alert("Please enter Name and Email");
        return;
    }

    try {
        const response = await fetch('http://localhost:9090/api/employees/create', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(employee)
        });

        if (response.ok) {
            alert('Employee saved successfully in Database!');
            closeAddEmployeeForm();
            window.location.reload();
        } else {
            const errorText = await response.text();
            console.error("Server Error:", errorText);
            alert('Failed to save. Server check karein.');
        }
    } catch (error) {
        console.error('Fetch Error:', error);
        alert('Could not connect to Java Backend on Port 9090');
    }
}