package com.campusrecycle.service;

import com.campusrecycle.dto.SubmissionRequest;
import com.campusrecycle.model.RecyclingSubmission;
import com.campusrecycle.model.User;
import com.campusrecycle.repository.RecyclingSubmissionRepository;
import com.campusrecycle.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class RecyclingSubmissionService {

    private static final Map<String, Integer> POINTS_PER_KG = Map.of(
        "PLASTIC",     10,
        "PAPER",        5,
        "GLASS",        8,
        "METAL",       12,
        "ELECTRONICS", 20,
        "ORGANIC",      3,
        "OTHER",        2
    );

    private final RecyclingSubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    public RecyclingSubmissionService(RecyclingSubmissionRepository submissionRepository,
                                      UserRepository userRepository,
                                      UserService userService) {
        this.submissionRepository = submissionRepository;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @Transactional
    public RecyclingSubmission submit(Long userId, SubmissionRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        String itemType = request.getItemType().toUpperCase();
        BigDecimal qty = request.getQuantityKg() != null ? request.getQuantityKg() : BigDecimal.ONE;
        int pointsPerKg = POINTS_PER_KG.getOrDefault(itemType, 2);
        int points = (int) Math.round(qty.doubleValue() * pointsPerKg);

        RecyclingSubmission submission = new RecyclingSubmission();
        submission.setUser(user);
        submission.setItemType(itemType);
        submission.setQuantityKg(qty);
        submission.setPointsEarned(points);
        submission.setLocation(request.getLocation());
        submission.setNotes(request.getNotes());
        submission.setStatus("APPROVED");

        RecyclingSubmission saved = submissionRepository.save(submission);

        userService.addPoints(userId, points);

        return saved;
    }

    public List<RecyclingSubmission> getUserSubmissions(Long userId) {
        return submissionRepository.findByUserIdOrderBySubmittedAtDesc(userId);
    }

    public List<RecyclingSubmission> getAllPending() {
        return submissionRepository.findByStatusOrderBySubmittedAtDesc("PENDING");
    }

    public List<RecyclingSubmission> getAllSubmissions() {
        return submissionRepository.findAll();
    }

    @Transactional
    public RecyclingSubmission review(Long submissionId, String status, Long reviewerId) {
        RecyclingSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found: " + submissionId));

        String normalised = status.toUpperCase();
        if (!List.of("APPROVED", "REJECTED").contains(normalised)) {
            throw new IllegalArgumentException("Status must be APPROVED or REJECTED");
        }

        boolean wasApproved = "APPROVED".equals(submission.getStatus());
        boolean nowApproved = "APPROVED".equals(normalised);

        if (!wasApproved && nowApproved) {
            userService.addPoints(submission.getUser().getId(), submission.getPointsEarned());
        } else if (wasApproved && !nowApproved) {
            userService.addPoints(submission.getUser().getId(), -submission.getPointsEarned());
        }

        submission.setStatus(normalised);
        submission.setReviewedAt(LocalDateTime.now());
        return submissionRepository.save(submission);
    }

    public Optional<RecyclingSubmission> findById(Long id) {
        return submissionRepository.findById(id);
    }

    public Map<String, Integer> getPointsTable() {
        return POINTS_PER_KG;
    }
}
