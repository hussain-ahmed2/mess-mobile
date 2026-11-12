package com.messmate.bd.domain.model

/**
 * Creates the stable, monotonically increasing member ids (`M001`, `M002` …).
 *
 * The ids are **never** reused. That is what keeps the `Meals` grid and the
 * `Summary` rows aligned across all devices: a member that joins later is
 * simply appended, no existing row ever moves.
 */
object MemberIdGenerator {

    private const val PREFIX = "M"
    private const val DIGITS = 3

    /**
     * Next free id for the given set of already used ids.
     *
     * @param existingIds every id that is already in use.
     */
    fun next(existingIds: Collection<String>): String {
        val highest = existingIds
            .mapNotNull { id -> id.removePrefix(PREFIX).toIntOrNull() }
            .maxOrNull() ?: 0
        return PREFIX + (highest + 1).toString().padStart(DIGITS, '0')
    }
}
