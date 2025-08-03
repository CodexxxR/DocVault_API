package com.docvault.docvault_backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String name; // original uploaded name
    private String fileName; // stored file name (with timestamp)
    private String docType;

    private String filePath;

    private String ownerId;
    private String uploadedBy;

    private String tags; // comma-separated string

    private boolean adminOnly;

    private LocalDateTime uploadedAt;
}
