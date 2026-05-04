package dev.payment.paymentservice.repository;

import dev.payment.paymentservice.entity.Journal;
import dev.payment.paymentservice.entity.Journal.JournalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface JournalRepository extends JpaRepository<Journal, UUID> {
    
    @Query("SELECT j FROM Journal j WHERE j.status = :status ORDER BY j.createdAt ASC")
    List<Journal> findByStatus(JournalStatus status);
    
    @Query("SELECT j FROM Journal j WHERE j.referenceId = :referenceId AND j.referenceType = :referenceType")
    List<Journal> findByReference(String referenceId, String referenceType);
    
    @Query("SELECT j FROM Journal j WHERE j.status = 'POSTED' AND j.postedAt >= :fromDate AND j.postedAt < :toDate")
    List<Journal> findPostedJournalsBetween(java.time.Instant fromDate, java.time.Instant toDate);
}