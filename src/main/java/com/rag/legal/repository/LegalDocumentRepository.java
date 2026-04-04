package com.rag.legal.repository;

import com.rag.legal.domain.LegalDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LegalDocumentRepository extends JpaRepository<LegalDocument, Long> {

    Optional<LegalDocument> findByDocumentNumber(String documentNumber);

    List<LegalDocument> findByTribunal(String tribunal);

    List<LegalDocument> findByLegalArea(String legalArea);

    List<LegalDocument> findByStatus(String status);

    List<LegalDocument> findByDocumentType(String documentType);

    @Query("SELECT d FROM LegalDocument d WHERE d.tribunal = :tribunal AND d.status = 'VIGENTE' ORDER BY d.publicationDate DESC")
    List<LegalDocument> findVigenteByTribunal(@Param("tribunal") String tribunal);

    @Query("SELECT d FROM LegalDocument d WHERE d.legalArea = :legalArea AND d.status = 'VIGENTE' ORDER BY d.publicationDate DESC")
    List<LegalDocument> findVigenteByLegalArea(@Param("legalArea") String legalArea);

    @Query("SELECT d FROM LegalDocument d WHERE d.status = 'VIGENTE' AND d.publicationDate >= :startDate ORDER BY d.publicationDate DESC")
    List<LegalDocument> findVigenteAfterDate(@Param("startDate") LocalDate startDate);

    @Query("SELECT d FROM LegalDocument d WHERE d.tribunal = :tribunal AND d.legalArea = :legalArea AND d.status = 'VIGENTE'")
    List<LegalDocument> findByTribunalAndLegalAreaVigente(
        @Param("tribunal") String tribunal,
        @Param("legalArea") String legalArea
    );

    @Query(value = "SELECT * FROM legal_documents WHERE LOWER(content) LIKE LOWER(CONCAT('%', :keyword, '%')) AND status = 'VIGENTE'", nativeQuery = true)
    List<LegalDocument> findByContentContainingIgnoreCaseAndVigente(@Param("keyword") String keyword);
}
