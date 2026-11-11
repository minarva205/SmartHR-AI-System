package com.smarthr.backend.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.smarthr.backend.model.Attendance;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    // 1. Employee ki attendance history date wise order mein nikalne ke liye
    List<Attendance> findByEmployeeIdOrderByAttendanceDateDesc(Long employeeId);

    // 2. Admin dashboard par aaj ki date ke hisab se live logs dekhne ke liye
    List<Attendance> findByAttendanceDateOrderByCheckInDesc(LocalDate attendanceDate);
}