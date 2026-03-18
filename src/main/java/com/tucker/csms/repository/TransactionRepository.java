package com.tucker.csms.repository;

import com.tucker.csms.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    List<Transaction> findByStatus(Transaction.TransactionStatus status);

    List<Transaction> findByStationId(String stationId);

    @Query("SELECT t FROM Transaction t WHERE t.stationId = :stationId ORDER BY t.startTime DESC")
    List<Transaction> findByStationIdOrderByStartTimeDesc(@Param("stationId") String stationId);

    @Query("SELECT t FROM Transaction t WHERE t.startTime >= :since AND t.status = 'COMPLETED'")
    List<Transaction> findCompletedTransactionsSince(@Param("since") Instant since);
}
