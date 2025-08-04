package com.docvault.docvault_backend.repository;

import com.docvault.docvault_backend.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByOwnerId(String ownerId);
}
