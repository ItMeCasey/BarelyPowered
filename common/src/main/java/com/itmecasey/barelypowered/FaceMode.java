package com.itmecasey.barelypowered;

/** What one face of a battery does. Right-clicking a face with an empty hand steps through these in order. */
public enum FaceMode {
    BOTH("Input and output"),
    INPUT("Input only"),
    OUTPUT("Output only"),
    OFF("Off");

    private final String label;

    FaceMode(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public boolean canInput() {
        return this == BOTH || this == INPUT;
    }

    public boolean canOutput() {
        return this == BOTH || this == OUTPUT;
    }

    /** The next mode in the cycle: both, input, output, off, and round again. */
    public FaceMode next() {
        return values()[(ordinal() + 1) % values().length];
    }
}
