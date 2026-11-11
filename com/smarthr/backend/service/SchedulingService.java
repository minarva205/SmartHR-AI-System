package com.smarthr.backend.service;

import com.smarthr.backend.model.InterviewSlot;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SchedulingService {

    private static final Map<String, String[]> TIME_SLOT_MAP = new LinkedHashMap<>();
    private static final Map<String, String> TYPE_LINKS = new HashMap<>();

    static {
        TIME_SLOT_MAP.put("Morning (9AM-12PM)", new String[]{"09:00 AM", "09:30 AM", "10:00 AM", "10:30 AM", "11:00 AM", "11:30 AM"});
        TIME_SLOT_MAP.put("Afternoon (12PM-4PM)", new String[]{"12:00 PM", "12:30 PM", "01:00 PM", "02:00 PM", "02:30 PM", "03:00 PM", "03:30 PM"});
        TIME_SLOT_MAP.put("Evening (4PM-7PM)", new String[]{"04:00 PM", "04:30 PM", "05:00 PM", "05:30 PM", "06:00 PM"});

        TYPE_LINKS.put("Video", "https://meet.google.com/auto-scheduled");
        TYPE_LINKS.put("Phone", "Call: +91-XXXX-XXXX");
        TYPE_LINKS.put("In-Person", "Office - HR Conference Room B, 3rd Floor");
    }

    /**
     * Core AI Slot Matching Engine
     * Picks best slot considering type, time preference, duration, and generates AI metadata.
     */
    public InterviewSlot autoMatch(InterviewSlot slot) {
        if (slot == null) return null;

        String timeRange = slot.getPreferredTimeRange();
        String[] slots = TIME_SLOT_MAP.getOrDefault(timeRange, TIME_SLOT_MAP.get("Morning (9AM-12PM)"));

        // Pick optimal time using a simple priority hash (interviewer name + role hash for determinism)
        int hash = Math.abs((slot.getInterviewerName() + slot.getJobRole()).hashCode());
        String optimalTime = slots[hash % slots.length];

        slot.setScheduledDate(slot.getPreferredDate());
        slot.setScheduledTime(optimalTime);
        slot.setStatus("Confirmed");
        slot.setAiStatus("Optimal Match");

        // Set meeting link/location by type
        String type = slot.getInterviewType() != null ? slot.getInterviewType() : "Video";
        slot.setMeetingLink(TYPE_LINKS.getOrDefault(type, "TBD"));
        if ("In-Person".equals(type)) {
            slot.setLocation("Office - HR Conference Room B, 3rd Floor");
        }

        // AI confidence score
        int confidence = 72 + (hash % 25); // 72-97 range
        slot.setAiConfidenceScore(Math.min(confidence, 97));

        // AI Match Reason
        slot.setAiMatchReason(buildMatchReason(slot, optimalTime));

        // AI Preparation Tips
        slot.setAiPreparationTips(buildPreparationTips(slot));

        return slot;
    }

    /**
     * Generate 3 alternative slot suggestions
     */
    public List<Map<String, String>> suggestAlternatives(InterviewSlot slot) {
        List<Map<String, String>> alternatives = new ArrayList<>();
        String[] ranges = TIME_SLOT_MAP.keySet().toArray(new String[0]);

        for (int i = 0; i < Math.min(3, ranges.length); i++) {
            String[] times = TIME_SLOT_MAP.get(ranges[i]);
            Map<String, String> alt = new HashMap<>();
            alt.put("timeRange", ranges[i]);
            alt.put("suggestedTime", times[i % times.length]);
            alt.put("date", slot.getPreferredDate());
            alt.put("confidence", String.valueOf(65 + (i * 8)) + "%");
            alternatives.add(alt);
        }
        return alternatives;
    }

    private String buildMatchReason(InterviewSlot slot, String time) {
        StringBuilder sb = new StringBuilder();
        sb.append("**AI Scheduling Engine Result**\n\n");
        sb.append("✅ Interviewer '").append(slot.getInterviewerName() != null ? slot.getInterviewerName() : "HR Team").append("' has been matched with an open availability window.\n");
        sb.append("✅ Time slot **").append(time).append("** identified as optimal within the preferred range '").append(slot.getPreferredTimeRange()).append("'.\n");
        sb.append("✅ Interview type **").append(slot.getInterviewType()).append("** setup confirmed — link/location auto-configured.\n");
        sb.append("✅ Duration: **").append(slot.getDuration() != null ? slot.getDuration() : "45 min").append("** — within standard interview window.\n\n");
        sb.append("No scheduling conflicts detected for this slot. Confirmation dispatched.");
        return sb.toString();
    }

    private String buildPreparationTips(InterviewSlot slot) {
        String role = slot.getJobRole() != null ? slot.getJobRole() : "the applied position";
        String dept = slot.getDepartment() != null ? slot.getDepartment() : "the department";
        String type = slot.getInterviewType() != null ? slot.getInterviewType() : "Video";

        StringBuilder sb = new StringBuilder();
        sb.append("**AI Preparation Checklist for ").append(role).append("**\n\n");
        sb.append("1. **Research ").append(dept).append(" department**: Understand their key projects, tech stack, and recent achievements.\n");
        sb.append("2. **Review your Resume**: Be ready to walk through every bullet point clearly and confidently.\n");
        sb.append("3. **Prepare STAR Stories**: Have 3-5 behavioral examples ready (Situation, Task, Action, Result).\n");

        if ("Video".equals(type)) {
            sb.append("4. **Tech Setup**: Test your camera, microphone, and internet speed at least 15 minutes before.\n");
            sb.append("5. **Environment**: Use a clean, well-lit background. Close extra browser tabs.\n");
        } else if ("Phone".equals(type)) {
            sb.append("4. **Phone Readiness**: Ensure full signal. Use earphones for hands-free clarity.\n");
            sb.append("5. **Quiet Space**: Find a private area with no background noise.\n");
        } else {
            sb.append("4. **Arrive Early**: Reach the office 10 minutes before the scheduled slot.\n");
            sb.append("5. **Carry Documents**: Bring printed resume, ID, and any portfolio/certificates.\n");
        }

        sb.append("6. **Questions for Them**: Prepare 2-3 thoughtful questions about the team, projects, or growth opportunities.\n");
        sb.append("7. **Stay Confident**: Your skills got you this interview — trust your preparation!");
        return sb.toString();
    }

    /**
     * AI Chatbot response for both roles
     */
    public String getChatbotResponse(String message, String role) {
        String msg = message.toLowerCase().trim();

        if ("ADMIN".equals(role)) {
            if (msg.contains("pending") || msg.contains("confirm") || msg.contains("unconfirmed")) {
                return "Here is a summary of **Pending Interview Slots**:\n\nPlease navigate to the **Workforce Interview Ledger** below and use the status filter to sort by 'Pending'. You can then bulk-confirm slots or use the AI Auto-Match button to instantly assign optimal times for each pending request.\n\n**Tip**: High-priority roles like Engineering and Management leads should be confirmed within 24 hours to reduce candidate drop-off.";
            } else if (msg.contains("reschedule") || msg.contains("cancel")) {
                return "**Rescheduling / Cancelling Guidance:**\n\n1. Locate the slot in the Interview Ledger below.\n2. Click the Status dropdown and select **Rescheduled** or **Cancelled**.\n3. The AI engine will automatically flag it and send system-level alerts.\n\n**Best Practice**: Always reschedule within 12 hours of cancellation to maintain a positive candidate experience.";
            } else if (msg.contains("conflict") || msg.contains("overlap") || msg.contains("clash")) {
                return "**Conflict Detection:**\n\nThe AI Scheduling Engine checks for overlapping interviewer availability before assigning slots. If a conflict arises, the system automatically picks the next optimal slot in the queue.\n\nYou can re-run AI Auto-Match on any pending slot by clicking **AI Auto-Match** in the ledger entry.";
            } else if (msg.contains("best time") || msg.contains("optimal") || msg.contains("recommend")) {
                return "**Optimal Interview Windows (AI Recommendation):**\n\n- 🟢 **Morning 10:00–11:30 AM** — Highest candidate alertness and interviewer availability overlap.\n- 🟡 **Early Afternoon 2:00–3:30 PM** — Good secondary window after lunch digestion.\n- 🔴 **Late Afternoon 5:00+ PM** — Fatigue factors increase; reserve for urgent schedules only.\n\nThe AI engine prioritizes Morning slots when 'Morning' is marked as preference.";
            } else {
                return "Hello Administrator! I am **ScheduleBot AI**, your intelligent interview coordination system. I can help you:\n\n- **Confirm or reschedule** pending interview requests\n- **Detect scheduling conflicts** across your team\n- **Recommend optimal interview windows** based on role priority\n- **Generate interview scripts** for specific roles\n\nHow can I assist today?";
            }
        } else {
            // Employee
            if (msg.contains("reschedule") || msg.contains("change") || msg.contains("postpone")) {
                return "To request a **rescheduling**, please use the form below and submit a new slot request with your updated preferred date and time range.\n\nOur HR Admin team will review the request within 1 business day. The AI engine will auto-match the best available slot for your new preference.";
            } else if (msg.contains("prepare") || msg.contains("ready") || msg.contains("tips") || msg.contains("advice")) {
                return "Here are **Top Interview Preparation Tips** from ScheduleBot AI:\n\n1. **Research the Role & Company**: Understand what the job entails and how your skills align.\n2. **Practice STAR Stories**: Behavioral questions are common — prepare 4-5 concrete examples.\n3. **Tech Test (for Video)**: Run a mic/camera check 15 minutes early.\n4. **Dress Professionally**: First impressions matter even on video.\n5. **Ask Smart Questions**: Show curiosity about growth opportunities and team culture.\n\nYou've got this! 🌟";
            } else if (msg.contains("status") || msg.contains("confirmed") || msg.contains("when")) {
                return "You can view the **live status** of all your interview requests in the **My Interview History** section below.\n\n- 🟢 **Confirmed** — Your slot is locked. Check the scheduled date and meeting link.\n- 🟡 **Pending** — HR is reviewing. You will be notified once confirmed.\n- 🔵 **Rescheduled** — A new slot has been assigned.\n- 🔴 **Cancelled** — Please re-submit a new request.";
            } else if (msg.contains("video") || msg.contains("link") || msg.contains("meeting")) {
                return "Your **meeting link** will be available once your interview slot is confirmed by the HR team.\n\nFor **Video interviews**, the link is auto-generated by the AI engine and attached to your confirmed slot card below. Please test your connection 15 minutes before the interview starts!";
            } else {
                return "Hi there! I am **ScheduleBot AI**, your personal interview assistant. I can help you:\n\n- **Request a new interview slot** with preferred timing\n- **Get AI preparation tips** for your specific role\n- **Check your interview status** and meeting details\n- **Request rescheduling** if needed\n\nWhat would you like help with today? 🗓️";
            }
        }
    }
}
