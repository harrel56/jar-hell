package dev.harrel.jarhell.model;

public record ArtifactVersion(String version, State state) {
    public static State state(boolean unresolved, String unresolvedReason) {
        if (!unresolved) {
            return State.ANALYZED;
        } else {
            return "initial-indexing".equals(unresolvedReason) ? State.NOT_ANALYZED : State.FAILED;
        }
    }

    public enum State {
        NOT_ANALYZED, ANALYZED, FAILED
    }
}
