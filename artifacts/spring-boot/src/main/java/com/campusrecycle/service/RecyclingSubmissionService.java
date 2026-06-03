package com.campusrecycle.service;

import com.campusrecycle.dto.SubmissionRequest;
import com.campusrecycle.model.RecyclingSubmission;
import com.campusrecycle.model.User;
import com.campusrecycle.repository.RecyclingSubmissionRepository;
import com.campusrecycle.repository.UserRepository;
import com.google.firebase.database.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class RecyclingSubmissionService {

    private static final Set<String> VALID_ITEMS = Set.of("BOTTLE", "CAN");
    private static final int POINTS_PER_PLASTIC = 10; // Updated point values
    private static final int POINTS_PER_METAL = 15;

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

    /**
     * ⚡ NEW: Automated QR Verification Flow
     * Connects directly to Firebase via the scanned QR sessionId, extracts counts, 
     * saves logs to Supabase, and updates user points instantly.
     */
    @Transactional
    public String processQrClaim(Long userId, String sessionId) throws ExecutionException, InterruptedException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(sessionId);
        CompletableFuture<DataSnapshot> future = new CompletableFuture<>();

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) { future.complete(snapshot); }
            @Override
            public void onCancelled(DatabaseError error) { future.completeExceptionally(error.toException()); }
        });

        DataSnapshot snapshot = future.get();
        if (!snapshot.exists()) {
            throw new RuntimeException("Invalid QR Code: Session node not found in Firebase!");
        }

        // Extract real-time hardware counts from Firebase fields
        Integer plasticCountObj = snapshot.child("plasticCount").getValue(Integer.class);
        Integer metalCountObj = snapshot.child("metalCount").getValue(Integer.class);

        int plasticCount = plasticCountObj != null ? plasticCountObj : 0;
        int metalCount = metalCountObj != null ? metalCountObj : 0;

        int totalPoints = (plasticCount * POINTS_PER_PLASTIC) + (metalCount * POINTS_PER_METAL);

        if (totalPoints == 0) {
            throw new RuntimeException("No items detected in this recycling session.");
        }

        // Persist itemized automated entries to match database expectations
        if (plasticCount > 0) {
            saveAutomatedRecord(user, "BOTTLE", plasticCount, plasticCount * POINTS_PER_PLASTIC);
        }
        if (metalCount > 0) {
            saveAutomatedRecord(user, "CAN", metalCount, metalCount * POINTS_PER_METAL);
        }

        // Balance accumulation trigger
        userService.addPoints(userId, totalPoints);

        return "QR Verified! Processed " + plasticCount + " plastics and " + metalCount + " metals. +" + totalPoints + " Points!";
    }

    private void saveAutomatedRecord(User user, String itemType, int qty, int points) {
        RecyclingSubmission submission = new RecyclingSubmission();
        submission.setUser(user);
        submission.setItemType(itemType);
        submission.setQuantity(qty);
        submission.setPointsEarned(points);
        submission.setStatus("APPROVED");
        submission.setNotes("Scanned via Campus Hardware Bin QR Code");
        submissionRepository.save(submission);
    }

    // --- Kept Your Original Framework Methods Untouched ---

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
        int points = qty * (itemType.equals("BOTTLE") ? POINTS_PER_PLASTIC : POINTS_PER_METAL);

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

    public List<RecyclingSubmission> getRecentSubmissions() {
        return submissionRepository.findTop20ByOrderBySubmittedAtDesc();
    }

    public Map<String, Object> getCampusStats() {
        List<Object[]> itemStats = submissionRepository.getStatsByItemType();
        long totalUsers = userRepository.count();
        long totalSubmissions = submissionRepository.count();

        long totalBottles = 0, totalCans = 0, totalPoints = 0;
        for (Object[] row : itemStats) {
            String type = (String) row[0];
            long qty   = ((Number) row[2]).longValue();

            if ("BOTTLE".equals(type)) {
                totalBottles = qty;
                totalPoints += (qty * POINTS_PER_PLASTIC);
            } else if ("CAN".equals(type)) {
                totalCans = qty;
                totalPoints += (qty * POINTS_PER_METAL);
            }
        }

        return Map.of(
            "totalSubmissions", totalSubmissions,
            "totalPoints",      totalPoints,
            "totalBottles",     totalBottles,
            "totalCans",        totalCans,
            "totalUsers",       totalUsers
        );
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
                Map.of("type", "BOTTLE", "pointsPerItem", POINTS_PER_PLASTIC, "description", "Plastic bottle"),
                Map.of("type", "CAN",    "pointsPerItem", POINTS_PER_METAL, "description", "Aluminium can")
            ),
            "welcomeBonus", 20
        );
    }
}