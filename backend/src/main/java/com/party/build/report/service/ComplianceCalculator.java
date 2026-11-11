package com.party.build.report.service;

public final class ComplianceCalculator {
    private ComplianceCalculator() {
    }

    public static Result calculate(int required, int passed,
                                   int overdue, int urgent, int near, int info,
                                   int conflict, int doubt, int semanticMissing, int lowConfidence,
                                   int semanticTotal) {
        double m = required == 0 ? 100.0 : (passed * 100.0 / required);
        int timeTotal = overdue + urgent + near + info;
        double t = timeTotal == 0 ? 100.0
                : 100.0 * (1 - (overdue * 1.0 + urgent * 0.6 + near * 0.3 + info * 0.1) / timeTotal);
        double q = semanticTotal == 0 ? 100.0
                : 100.0 * (1 - (conflict * 1.0 + doubt * 0.7 + semanticMissing * 0.5 + lowConfidence * 0.3)
                / Math.max(1, semanticTotal));
        m = clamp(m);
        t = clamp(t);
        q = clamp(q);
        double total = clamp(0.40 * m + 0.35 * t + 0.25 * q);
        String risk = total < 60 ? "RED" : total < 80 ? "YELLOW" : "GREEN";
        return new Result(round(m), round(t), round(q), round(total), risk);
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(100, v));
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    public record Result(double materialScore, double timeScore, double contentScore,
                         double totalScore, String riskLevel) {
    }
}