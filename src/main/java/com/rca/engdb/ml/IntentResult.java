package com.rca.engdb.ml;

public class IntentResult {

    private final IntentType intent;
    private final double confidence;

    public IntentResult(IntentType intent, double confidence) {
        this.intent = intent;
        this.confidence = confidence;
    }

    public IntentType getIntent() {
        return intent;
    }

    public double getConfidence() {
        return confidence;
    }
}

