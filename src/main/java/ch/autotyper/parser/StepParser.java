package ch.autotyper.parser;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Java files with // @step N markers and produces an ordered list of
 * typing instructions (StepBlocks) that define WHAT to type and WHERE to insert it.
 *
 * <p>Usage example in a .java file:</p>
 * <pre>
 * // @step 1
 * public class Foo {
 *
 * // @step 2
 *     private int x;
 *
 * // @step 3
 *     public Foo() { }
 *
 * // @step 1
 * }
 * </pre>
 *
 * <p>This produces:
 * <ol>
 *   <li>Step 1: "public class Foo {\n\n}" (skeleton)</li>
 *   <li>Step 2: "    private int x;\n" (inserted between line 2 and closing brace)</li>
 *   <li>Step 3: "    public Foo() { }\n" (inserted after step 2 content)</li>
 * </ol>
 */
public class StepParser {

    private static final Pattern STEP_PATTERN = Pattern.compile("^\\s*//\\s*@step\\s+(\\d+)\\s*$");

    /**
     * Represents a block of code belonging to a step.
     */
    public static class StepBlock {
        private final int stepNumber;
        private final String content;
        private final int originalLineStart; // line index in source where this block starts

        public StepBlock(int stepNumber, String content, int originalLineStart) {
            this.stepNumber = stepNumber;
            this.content = content;
            this.originalLineStart = originalLineStart;
        }

        public int getStepNumber() {
            return stepNumber;
        }

        public String getContent() {
            return content;
        }

        public int getOriginalLineStart() {
            return originalLineStart;
        }

        @Override
        public String toString() {
            return "StepBlock{step=" + stepNumber + ", lines=" + content.split("\n").length + "}";
        }
    }

    /**
     * Represents the full typing plan: an ordered sequence of blocks to type.
     */
    public static class TypingPlan {
        private final List<StepBlock> blocks;
        private final boolean hasStepMarkers;

        public TypingPlan(List<StepBlock> blocks, boolean hasStepMarkers) {
            this.blocks = blocks;
            this.hasStepMarkers = hasStepMarkers;
        }

        public List<StepBlock> getBlocks() {
            return blocks;
        }

        public boolean hasStepMarkers() {
            return hasStepMarkers;
        }

        public int getTotalSteps() {
            if (!hasStepMarkers) return 1;
            return (int) blocks.stream().mapToInt(StepBlock::getStepNumber).distinct().count();
        }

        /**
         * Returns the full final code (all blocks combined in original order).
         */
        public String getFinalCode() {
            // Sort by original line position to reconstruct final file
            List<StepBlock> sorted = new ArrayList<>(blocks);
            sorted.sort(Comparator.comparingInt(StepBlock::getOriginalLineStart));
            StringBuilder sb = new StringBuilder();
            for (StepBlock block : sorted) {
                sb.append(block.getContent());
            }
            return sb.toString();
        }
    }

    /**
     * Parses the source code and returns a TypingPlan.
     * If no @step markers are found, returns a single-block plan (sequential typing).
     */
    public static TypingPlan parse(String source) {
        String[] lines = source.split("\n", -1);
        List<StepBlock> blocks = new ArrayList<>();

        int currentStep = -1;
        int blockStartLine = 0;
        StringBuilder currentContent = new StringBuilder();
        boolean foundMarkers = false;

        for (int i = 0; i < lines.length; i++) {
            Matcher matcher = STEP_PATTERN.matcher(lines[i]);
            if (matcher.matches()) {
                foundMarkers = true;
                // Save previous block if exists
                if (currentStep >= 0 && currentContent.length() > 0) {
                    blocks.add(new StepBlock(currentStep, currentContent.toString(), blockStartLine));
                }
                currentStep = Integer.parseInt(matcher.group(1));
                currentContent = new StringBuilder();
                blockStartLine = i + 1;
            } else {
                if (currentStep >= 0) {
                    currentContent.append(lines[i]);
                    if (i < lines.length - 1) {
                        currentContent.append("\n");
                    }
                } else if (!foundMarkers) {
                    // Content before any marker (e.g. package/imports without a step)
                    currentContent.append(lines[i]);
                    if (i < lines.length - 1) {
                        currentContent.append("\n");
                    }
                }
            }
        }

        // Save last block
        if (currentStep >= 0 && currentContent.length() > 0) {
            blocks.add(new StepBlock(currentStep, currentContent.toString(), blockStartLine));
        }

        // If no markers found, return entire file as one block
        if (!foundMarkers) {
            blocks.clear();
            blocks.add(new StepBlock(1, source, 0));
            return new TypingPlan(blocks, false);
        }

        // Sort blocks by step number (stable sort preserves order within same step)
        blocks.sort(Comparator.comparingInt(StepBlock::getStepNumber));

        return new TypingPlan(blocks, true);
    }

    /**
     * Calculates the insertion offset for a block, given the text already in the editor.
     * Blocks of the same step are appended at the end of the previous same-step content.
     * Higher step numbers are inserted based on their original line position relative
     * to already-inserted content.
     */
    public static int calculateInsertionOffset(String currentEditorContent, StepBlock block,
                                                List<StepBlock> alreadyInserted) {
        if (alreadyInserted.isEmpty()) {
            return 0; // First block always at start
        }

        // Find the correct position based on original line ordering
        // Strategy: Insert after the last block that has a lower original line start
        // and is already in the editor

        int insertAfterLine = -1;
        int offsetAccumulator = 0;

        // Build a map of what's already in the editor by original position
        List<StepBlock> sortedInserted = new ArrayList<>(alreadyInserted);
        sortedInserted.sort(Comparator.comparingInt(StepBlock::getOriginalLineStart));

        for (StepBlock inserted : sortedInserted) {
            if (inserted.getOriginalLineStart() < block.getOriginalLineStart()) {
                offsetAccumulator += inserted.getContent().length();
            }
        }

        // Ensure we don't exceed editor content length
        return Math.min(offsetAccumulator, currentEditorContent.length());
    }
}
