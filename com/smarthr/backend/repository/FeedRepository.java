package com.smarthr.backend.repository;

import com.smarthr.backend.model.Feed;
import com.smarthr.backend.dto.FeedDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FeedRepository extends JpaRepository<Feed, Long> {

    @Query("SELECT new com.smarthr.backend.dto.FeedDTO(f.id, e.fullName, e.designation, f.content, f.likesCount, f.createdAt) "
            +
            "FROM Feed f JOIN Employee e ON f.employeeId = e.id " +
            "ORDER BY f.createdAt DESC")
    List<FeedDTO> getAllFeedsWithEmployeeDetails();
}