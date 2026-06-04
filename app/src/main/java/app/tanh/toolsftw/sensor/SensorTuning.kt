package app.tanh.toolsftw.sensor

/**
 * Shared sensor smoothing and publish-throttling parameters, kept in one place so the compass and
 * level faces stay consistent if these are tuned. Per-reading thresholds (epsilon) live with each
 * controller because they're scaled to that signal's units.
 */
internal object SensorTuning {
    /** Low-pass filter weight applied to each new sample (higher = more responsive, less smooth). */
    const val FILTER_ALPHA = 0.18f

    /** Force a publish at least this often so the UI keeps updating even below the epsilon threshold. */
    const val PUBLISH_MAX_INTERVAL_MILLIS = 80L
}
