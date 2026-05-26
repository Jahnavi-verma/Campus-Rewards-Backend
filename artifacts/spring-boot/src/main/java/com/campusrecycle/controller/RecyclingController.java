package com.campusrecycle.controller;

import com.campusrecycle.dto.SubmissionDto;
import com.campusrecycle.dto.SubmissionRequest;
import com.campusrecycle.model.RecyclingSubmission;
import com.campusrecycle.service.RecyclingSubmissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/recycling")
public class RecyclingController {

    private final RecyclingSubmissionService submissionService;

    public RecyclingController(RecyclingSubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @PostMapping("/submit")
    public ResponseEntity<SubmissionDto> submit(@RequestBody SubmissionRequest request,
                                                 Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        RecyclingSubmission saved = submissionService.submit(userId, request);
        return ResponseEntity.ok(SubmissionDto.from(saved));
    }

    @GetMapping("/my")
    public ResponseEntity<List<SubmissionDto>> mySubmissions(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        List<SubmissionDto> list = submissionService.getUserSubmissions(userId)
                .stream().map(SubmissionDto::from).toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/points-table")
    public ResponseEntity<Map<String, Integer>> pointsTable() {
        return ResponseEntity.ok(submissionService.getPointsTable());
    }

    @GetMapping("/all")
    public ResponseEntity<List<SubmissionDto>> allSubmissions(Authentication authentication) {
        List<SubmissionDto> list = submissionService.getAllSubmissions()
                .stream().map(SubmissionDto::from).toList();
        return ResponseEntity.ok(list);
    }

    @PatchMapping("/{id}/review")
    public ResponseEntity<SubmissionDto> review(@PathVariable Long id,
                                                 @RequestBody Map<String, String> body,
                                                 Authentication authentication) {
        String status = body.get("status");
        if (status == null) return ResponseEntity.badRequest().build();
        Long reviewerId = Long.parseLong(authentication.getName());
        RecyclingSubmission updated = submissionService.review(id, status, reviewerId);
        return ResponseEntity.ok(SubmissionDto.from(updated));
    }
}
