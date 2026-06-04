package br.com.calcmot.processor

data class DebugTreeWalkResult<T>(
    val visited: List<DebugTreeVisit<T>>,
    val maxDepthReached: Int,
    val truncated: Boolean
)

data class DebugTreeVisit<T>(
    val node: T,
    val path: String,
    val depth: Int,
    val indexInParent: Int,
    val childCount: Int,
    val siblingCount: Int
)

object DebugTreeWalker {
    fun <T : Any> walkDepthFirst(
        root: T,
        maxDepth: Int,
        maxNodes: Int,
        identity: (T) -> String,
        childCount: (T) -> Int,
        childAt: (T, Int) -> T?
    ): DebugTreeWalkResult<T> {
        val visited = mutableListOf<DebugTreeVisit<T>>()
        val seen = mutableSetOf<String>()
        var maxDepthReached = 0
        var truncated = false

        fun visit(
            node: T,
            path: String,
            depth: Int,
            indexInParent: Int,
            siblingCount: Int
        ) {
            if (visited.size >= maxNodes) {
                truncated = true
                return
            }
            if (depth > maxDepth) {
                truncated = true
                return
            }

            val safeChildCount = runCatching { childCount(node).coerceAtLeast(0) }.getOrDefault(0)
            val nodeIdentity = "${identity(node)}|$path|$depth|$safeChildCount"
            if (!seen.add(nodeIdentity)) return

            maxDepthReached = maxOf(maxDepthReached, depth)
            visited += DebugTreeVisit(
                node = node,
                path = path,
                depth = depth,
                indexInParent = indexInParent,
                childCount = safeChildCount,
                siblingCount = siblingCount
            )

            for (index in 0 until safeChildCount) {
                if (visited.size >= maxNodes) {
                    truncated = true
                    break
                }
                val child = runCatching { childAt(node, index) }.getOrNull() ?: continue
                visit(
                    node = child,
                    path = "$path/$index",
                    depth = depth + 1,
                    indexInParent = index,
                    siblingCount = safeChildCount
                )
            }
        }

        visit(
            node = root,
            path = "0",
            depth = 0,
            indexInParent = 0,
            siblingCount = 1
        )

        return DebugTreeWalkResult(
            visited = visited,
            maxDepthReached = maxDepthReached,
            truncated = truncated
        )
    }
}
