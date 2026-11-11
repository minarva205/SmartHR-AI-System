let attendanceRecords = [];

window.onload = () => {
    updateClock();
    setInterval(updateClock, 1000);
    fetchEmployees();
    fetchAttendance();
};

function updateClock() {
    const clock = document.getElementById('digitalClock');
    if (clock) clock.innerText = new Date().toLocaleTimeString();
}

async function fetchEmployees() {
    try {
        const res = await fetch('http://localhost:9090/api/employees/all');
        const emps = await res.json();
        document.getElementById('employeeSelect').innerHTML =
            '<option value="">-- Select Employee --</option>' +
            emps.map(e => `<option value="${e.id}">${e.fullName}</option>`).join('');
    } catch (e) { console.error("Emp load error"); }
}

async function fetchAttendance() {
    try {
        const res = await fetch('http://localhost:9090/api/attendance/all');
        attendanceRecords = await res.json();
        const tbody = document.getElementById('attendanceData');
        tbody.innerHTML = attendanceRecords.map((r, index) => `
            <tr class="border-b hover:bg-blue-50 transition cursor-pointer" onclick="showDetails(${index})">
                <td class="py-4 font-bold text-blue-600 underline">${r.employee?.fullName || 'N/A'}</td>
                <td class="py-4">${r.attendanceDate}</td>
                <td class="py-4 text-green-600">${r.checkIn || '--:--'}</td>
                <td class="py-4 text-red-600">${r.checkOut || '--:--'}</td>
                <td class="py-4"><span class="px-2 py-1 rounded bg-slate-100 text-xs">${r.status}</span></td>
            </tr>
        `).join('');
    } catch (e) { console.error("Att fetch error"); }
}

function showDetails(idx) {
    const r = attendanceRecords[idx];
    alert(`
        EMPLOYEE DETAILS
        ----------------
        Name: ${r.employee?.fullName}
        Designation: ${r.employee?.designation || 'N/A'}
        Date: ${r.attendanceDate}
        Check In: ${r.checkIn || 'N/A'}
        Check Out: ${r.checkOut || 'N/A'}
        Work Hours: ${r.workHours || 'Calculating...'}
        Status: ${r.status}
    `);
}

async function logAttendance(type) {
    const id = document.getElementById('employeeSelect').value;
    if (!id) return alert("Select employee!");

    const data = {
        employee: { id: parseInt(id) },
        attendanceDate: new Date().toISOString().split('T')[0],
        status: "Present"
    };
    type === 'IN' ? data.checkIn = new Date().toLocaleTimeString('it-IT') : data.checkOut = new Date().toLocaleTimeString('it-IT');

    const res = await fetch('http://localhost:9090/api/attendance/mark', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
    });
    if (res.ok) { alert("Done!"); fetchAttendance(); }
    else { alert("Failed to log attendance!"); }
}