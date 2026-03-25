package com.agenticcare.common.constants;

public final class AgentConstants {

    private AgentConstants() {}

    // Risk thresholds
    public static final double RP_OUTREACH_THRESHOLD = 0.5;
    public static final double RP_ESCALATION_THRESHOLD = 0.65;

    // Escalation intervals (minutes before appointment)
    public static final int TV_FIRST_ESCALATION_MIN = 90;
    public static final int TV_FINAL_ESCALATION_MIN = 30;

    // PCA assessment points (hours before appointment)
    public static final int PCA_ASSESS_T24H = 24;
    public static final int PCA_ASSESS_T4H = 4;
    public static final int PCA_ASSESS_T90MIN_H = 1; // ~90 min
}
