package com.quizgen.repository;

import com.quizgen.model.StudyMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudyMaterialRepository extends JpaRepository<StudyMaterial, Long> {
    List<StudyMaterial> findByUploadedBy(String uploadedBy);

    List<StudyMaterial> findByFileType(String fileType);
}
