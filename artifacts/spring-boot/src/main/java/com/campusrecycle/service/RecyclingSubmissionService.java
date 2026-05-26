package com.campusrecycle.service;

import com.campusrecycle.dto.SubmissionRequest;
import com.campusrecycle.model.RecyclingSubmission;
import com.campusrecycle.model.User;
import com.campusrecycle.repository.RecyclingSubmissionRepository;
import com.campusrecycle.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class RecyclingSubmissionService {

    private static final Set<String> VALID_ITEMS = Set.of("BOTTLE", "CAN");
    private static final int POINTS_PER_ITEM = 1;

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

        String itemType = request.getItemType() != null
                ? request.getItemType().toUpperCase() : "";

        if (!VALID_ITEMS.contains(itemType)) {
            throw new IllegalArgumentException(
                "Invalid item type '" + itemType + "'. Must be BOTTLE or CAN.");
        }

        int qty = Math.max(1, request.getQuantity());
        int points = qty * POINTS_PER_ITEM;

        RecyclingSubmission submission = new RecyclingSubmission();
        submission.setUser(user);
        submission.setItemType(itemType);
        submission.setQuantity(qty);
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

    public Map<String, Object> getItemInfo() {
        return Map.of(
            "items", List.of(
                Map.of("type", "BOTTLE", "pointsPerItem", POINTS_PER_ITEM, "description", "Plastic bottle"),
                Map.of("type", "CAN",    "pointsPerItem", POINTS_PER_ITEM, "description", "Aluminium can")
            ),
            "welcomeBonus", 20
        );
    }
}
