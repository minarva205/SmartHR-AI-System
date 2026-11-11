package com.smarthr.backend.service;

import com.smarthr.backend.model.Feedback;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class SentimentService {

    // Lists of lexical markers
    private static final List<String> POSITIVE_WORDS = Arrays.asList(
        "great", "love", "amazing", "excellent", "good", "best", "happy", "thankful", 
        "appreciate", "support", "fantastic", "wonderful", "awesome", "proud", "glad", 
        "nice", "comfortable", "enjoy", "perfect", "helpful", "motivate", "inspired"
    );

    private static final List<String> NEGATIVE_WORDS = Arrays.asList(
        "toxic", "bad", "hate", "stress", "underpaid", "exhaust", "burn", "burn out", 
        "tired", "micro-manage", "micro", "management", "slow", "delay", "ignore", 
        "pressure", "heavy", "load", "sad", "angry", "complain", "grievance", 
        "struggle", "poor", "unfair", "worst", "fail", "issue", "problem", "hard", "difficult", "resign"
    );

    public Feedback analyzeFeedback(Feedback feedback) {
        if (feedback == null) return null;
        String text = feedback.getMessage();
        if (text == null) text = "";
        String cleanText = text.toLowerCase().trim();
        String category = feedback.getCategory() != null ? feedback.getCategory() : "Suggestion";

        // 1. Scoring Logic
        double score = 50.0; // Baseline neutral score

        if ("Appreciation".equalsIgnoreCase(category)) {
            score = 70.0;
        } else if ("Grievance".equalsIgnoreCase(category)) {
            score = 30.0;
        }

        int posCount = 0;
        for (String word : POSITIVE_WORDS) {
            if (cleanText.contains(word)) posCount++;
        }

        int negCount = 0;
        for (String word : NEGATIVE_WORDS) {
            if (cleanText.contains(word)) negCount++;
        }

        score += (posCount * 8.0) - (negCount * 8.0);
        score = Math.max(0.0, Math.min(100.0, score));

        feedback.setSentimentScore(score);

        // 2. Sentiment Labeling
        if (score >= 65.0) {
            feedback.setSentiment("Positive");
        } else if (score <= 35.0) {
            feedback.setSentiment("Negative");
        } else {
            feedback.setSentiment("Neutral");
        }

        // 3. Topic Mapping & Key Takeaways / Recommendations Generation
        boolean hasCompensation = cleanText.contains("underpaid") || cleanText.contains("salary") || cleanText.contains("pay") || cleanText.contains("raise") || cleanText.contains("appraisal") || cleanText.contains("money") || cleanText.contains("increment");
        boolean hasStress = cleanText.contains("stress") || cleanText.contains("exhaust") || cleanText.contains("burn") || cleanText.contains("pressure") || cleanText.contains("workload") || cleanText.contains("heavy") || cleanText.contains("tired");
        boolean hasCulture = cleanText.contains("toxic") || cleanText.contains("micro") || cleanText.contains("manager") || cleanText.contains("management") || cleanText.contains("boss") || cleanText.contains("rude") || cleanText.contains("culture");
        boolean hasGrowth = cleanText.contains("learn") || cleanText.contains("course") || cleanText.contains("skill") || cleanText.contains("grow") || cleanText.contains("promote") || cleanText.contains("career") || cleanText.contains("lms");
        boolean hasRemote = cleanText.contains("remote") || cleanText.contains("hybrid") || cleanText.contains("home") || cleanText.contains("commute") || cleanText.contains("travel") || cleanText.contains("office days");
        boolean hasFacilities = cleanText.contains("gym") || cleanText.contains("cafeteria") || cleanText.contains("food") || cleanText.contains("coffee") || cleanText.contains("office") || cleanText.contains("wifi") || cleanText.contains("facilities");

        // 4. Emotional Tone Classification
        String tone = "Neutral";
        if ("Positive".equals(feedback.getSentiment())) {
            if (posCount >= 3 || cleanText.contains("appreciate") || cleanText.contains("thank") || cleanText.contains("love")) {
                tone = "Joyful / Appreciative";
            } else if (hasGrowth || cleanText.contains("inspired") || cleanText.contains("motivate")) {
                tone = "Optimistic / Inspired";
            } else {
                tone = "Satisfied";
            }
        } else if ("Negative".equals(feedback.getSentiment())) {
            if (hasStress) {
                tone = "Stressed / Burnt-out";
            } else if (hasCompensation) {
                tone = "Anxious / Discontented";
            } else if (hasCulture || cleanText.contains("angry") || cleanText.contains("toxic")) {
                tone = "Frustrated / Annoyed";
            } else {
                tone = "Concerned / Distressed";
            }
        } else {
            if (hasRemote || hasGrowth || cleanText.contains("suggest") || cleanText.contains("request") || cleanText.contains("hope")) {
                tone = "Constructive / Hopeful";
            } else {
                tone = "Neutral / Objective";
            }
        }
        feedback.setEmotionalTone(tone);

        // 5. Generate Key Takeaways List
        List<String> takeaways = new ArrayList<>();
        if (hasCompensation) {
            takeaways.add("- Expresses financial concerns or requests compensation reviews matching market indices.");
        }
        if (hasStress) {
            takeaways.add("- Reports experiencing elevated workload stress, fatigue, or operational burnout.");
        }
        if (hasCulture) {
            takeaways.add("- Raises concerns regarding leadership support, micromanagement, or peer collaboration culture.");
        }
        if (hasGrowth) {
            takeaways.add("- Requests more robust professional training tracks, skill paths, or promotional clarity.");
        }
        if (hasRemote) {
            takeaways.add("- Proposes hybrid/telecommute options to cut down commute strain and improve work output.");
        }
        if (hasFacilities) {
            takeaways.add("- Mentions office environment, workspace amenities, or local facilities explicitly.");
        }
        if (takeaways.isEmpty()) {
            takeaways.add("- Submits standard workplace administrative suggestion or operational query.");
        }
        feedback.setAiKeyTakeaways(String.join("\n", takeaways));

        // 6. Generate Actions & AI Recommendation
        StringBuilder rec = new StringBuilder();
        rec.append("### Recommended Action Checklist for HR Management:\n\n");
        int count = 1;
        if (hasCompensation) {
            rec.append(count++).append(". **Compensation Audit**: Review employee's base salary against their team averages and verify market benchmarks for their designation.\n");
            rec.append(count++).append(". **Retention Sync**: Keep a close channel. Arrange a proactive compensation assessment sync.\n\n");
        }
        if (hasStress) {
            rec.append(count++).append(". **Workload Redistribution**: Auditing task backlog distributions and capping overtime requests in their team immediately.\n");
            rec.append(count++).append(". **Wellness Support**: Suggest utilizing mental wellness leaves and encourage mindfulness resources.\n\n");
        }
        if (hasCulture) {
            rec.append(count++).append(". **Confidential 1-on-1 Session**: HR representative should schedule a completely private dialogue to hear details about culture challenges.\n");
            rec.append(count++).append(". **Manager Alignment Sync**: Provide leadership management mentoring or host departmental sensitivity training workshops.\n\n");
        }
        if (hasGrowth) {
            rec.append(count++).append(". **SmartHR LMS Custom Path**: Map their career roadmap with courses on SmartHR LMS to maintain engagement.\n");
            rec.append(count++).append(". **Promotion Roadmap**: Clarify milestones and key results (KPIs) needed to qualify for next-level promotions.\n\n");
        }
        if (hasRemote) {
            rec.append(count++).append(". **Hybrid Assessment**: Assess the team's feasibility to work from home for up to 3 days/week.\n");
            rec.append(count++).append(". **Commute Relief Options**: Offer flexible core-hours to let them bypass peak traffic times.\n\n");
        }
        if (hasFacilities) {
            rec.append(count++).append(". **Office Admin Sync**: Forward workspace comments to the administrative facilities team for checklist reviews.\n\n");
        }
        if (count == 1) {
            rec.append(count++).append(". **Admin Log**: Acknowledge receipt of suggestion and reply with standard policy guidelines.\n");
            rec.append(count++).append(". **Resolution State**: Mark resolved once initial feedback is categorized.");
        } else {
            rec.append("**Next Steps**: HR Partner should initiate contact within 5 business days.");
        }
        feedback.setAiRecommendation(rec.toString());

        return feedback;
    }
}
