package com.evolution.dropfilecli;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ProgressIndicator {

    private final String[] frames;

    private int currentFrameIndex = 0;

    public String getProgressIndicator() {
        String currentFrame = frames[currentFrameIndex];

        currentFrameIndex = (currentFrameIndex + 1) % frames.length;

        return currentFrame;
    }

    public static ProgressIndicator of(String message) {
        String[] frames = {"%s /".formatted(message), "%s -".formatted(message), "%s \\".formatted(message), "%s |".formatted(message)};
        return new ProgressIndicator(frames);
    }

    public static ProgressIndicator processing() {
        return ProgressIndicator.of("Processing");
    }

    public static ProgressIndicator live() {
        return ProgressIndicator.of("Live");
    }
}
