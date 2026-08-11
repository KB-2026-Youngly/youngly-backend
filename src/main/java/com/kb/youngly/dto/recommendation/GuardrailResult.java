package com.kb.youngly.dto.recommendation;

public record GuardrailResult(boolean passed, String reason) {
    public static GuardrailResult success() {
        return new GuardrailResult(true, null);
    }

    public static GuardrailResult failed(String reason) {
        return new GuardrailResult(false, reason);
    }
}
