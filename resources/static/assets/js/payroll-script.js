const API_BASE_URL = 'http://localhost:9090/api';

document.addEventListener('DOMContentLoaded', () => {
    loadEmployees();
    loadPayrolls();
});

async function loadEmployees() {
    try {
        const response = await fetch(`${API_BASE_URL}/employees/all`);
        if (response.ok) {
            const employees = await response.json();
            const dropdown = document.getElementById('empDropdown');
            dropdown.innerHTML = '<option value="">Select Employee</option>';
            employees.forEach(emp => {
                const option = document.createElement('option');
                option.value = emp.id;
                option.textContent = `${emp.fullName} (ID: ${emp.id})`;
                dropdown.appendChild(option);
            });
        }
    } catch (error) {
        console.error("Error fetching employees:", error);
    }
}

async function loadPayrolls() {
    try {
        const response = await fetch(`${API_BASE_URL}/payroll/all`);
        if (response.ok) {
            const payrolls = await response.json();
            const table = document.getElementById('payrollTable');
            table.innerHTML = '';
            payrolls.forEach(payroll => {
                const tr = document.createElement('tr');
                tr.className = 'border-b hover:bg-slate-50 transition-colors';
                const empName = payroll.employee && payroll.employee.fullName ? payroll.employee.fullName : 'Unknown';
                tr.innerHTML = `
                    <td class="py-4 flex items-center gap-3">
                        <div class="w-8 h-8 rounded-full bg-blue-100 text-blue-600 flex items-center justify-center font-bold">
                            ${empName.charAt(0)}
                        </div>
                        <span class="font-bold">${empName}</span>
                    </td>
                    <td class="py-4">${payroll.salaryMonth || '-'}</td>
                    <td class="py-4 font-bold text-slate-800">₹${payroll.netSalary || 0}</td>
                    <td class="py-4">
                        <span class="px-3 py-1 bg-green-100 text-green-700 rounded-full text-xs font-bold">${payroll.paymentStatus || 'Paid'}</span>
                    </td>
                    <td class="py-4 flex gap-3">
                        <button onclick='viewInvoice(${JSON.stringify({ name: empName, month: payroll.salaryMonth || "-", base: payroll.calculatedSalary || 0, bonus: payroll.bonus || 0, net: payroll.netSalary || 0 }).replace(/'/g, "\\'")})' class="text-blue-600 hover:text-blue-800" title="View Invoice"><i class="fas fa-eye"></i></button>
                        <button onclick='downloadSlip(${JSON.stringify({ name: empName, month: payroll.salaryMonth || "-", base: payroll.calculatedSalary || 0, bonus: payroll.bonus || 0, net: payroll.netSalary || 0 }).replace(/'/g, "\\'")})' class="text-green-600 hover:text-green-800" title="Download Slip"><i class="fas fa-download"></i></button>
                        <button onclick='deletePayroll(${payroll.id})' class="text-red-600 hover:text-red-800" title="Delete"><i class="fas fa-trash"></i></button>
                    </td>
                `;
                table.appendChild(tr);
            });
        }
    } catch (error) {
        console.error("Error fetching payrolls:", error);
    }
}

async function deletePayroll(id) {
    if (confirm("Are you sure you want to delete this payroll record?")) {
        try {
            const response = await fetch(`${API_BASE_URL}/payroll/delete/${id}`, {
                method: 'DELETE'
            });
            if (response.ok) {
                alert("Payroll deleted successfully.");
                loadPayrolls();
            } else {
                alert("Failed to delete payroll.");
            }
        } catch (error) {
            console.error("Error deleting payroll:", error);
        }
    }
}

function viewInvoice(data) {
    const element = document.getElementById('slipTemplate');
    element.style.display = 'block';
    document.getElementById('slipContent').innerHTML = `
        <p><strong>Name:</strong> ${data.name}</p>
        <p><strong>Month:</strong> ${data.month}</p>
        <hr>
        <p><strong>Base Salary:</strong> ₹${data.base}</p>
        <p><strong>Bonus:</strong> ₹${data.bonus}</p>
        <h3 class="text-xl font-bold">Net Paid: ₹${data.net}</h3>
        <button onclick="document.getElementById('slipTemplate').style.display='none'" class="mt-4 px-4 py-2 bg-red-500 text-white rounded">Close</button>
    `;
}

async function calculateSalary() {
    const empId = document.getElementById('empDropdown').value;
    const month = document.getElementById('salMonth').value;
    const bonus = parseFloat(document.getElementById('bonusAmt').value || 0);

    if (!empId || !month) {
        alert("Please select employee and month");
        return;
    }

    try {
        const employee = { id: empId };

        // In a real scenario, attendance needs to be fetched for totalPresentDays.
        // For now, we will create a basic payroll record.
        const baseSalaryPerDay = 1000;
        const presentDays = 22; // Placeholder
        const calculated = presentDays * baseSalaryPerDay;
        const net = calculated + bonus;

        const payload = {
            employee: employee,
            salaryMonth: month,
            totalPresentDays: presentDays,
            calculatedSalary: calculated,
            bonus: bonus,
            deductions: 0,
            netSalary: net,
            paymentStatus: 'Paid',
            paymentDate: new Date().toISOString().split('T')[0]
        };

        const response = await fetch(`${API_BASE_URL}/payroll/create`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(payload)
        });

        if (response.ok) {
            alert("Salary Calculated and Saved for Employee ID: " + empId + " for " + month);
            loadPayrolls(); // Refresh table
        } else {
            alert("Error saving payroll");
        }
    } catch (error) {
        console.error("Calculation error:", error);
        alert("Error connecting to server");
    }
}

function downloadSlip(data) {
    const element = document.getElementById('slipTemplate');
    element.style.display = 'block';
    document.getElementById('slipContent').innerHTML = `
        <p><strong>Name:</strong> ${data.name}</p>
        <p><strong>Month:</strong> ${data.month}</p>
        <hr>
        <p><strong>Base Salary:</strong> ₹${data.base}</p>
        <p><strong>Bonus:</strong> ₹${data.bonus}</p>
        <h3 class="text-xl font-bold">Net Paid: ₹${data.net}</h3>
    `;

    html2pdf().from(element).save(`Slip_${data.name}.pdf`);
    setTimeout(() => element.style.display = 'none', 100);
}