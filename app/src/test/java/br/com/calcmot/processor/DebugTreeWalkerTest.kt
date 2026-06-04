package br.com.calcmot.processor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DebugTreeWalkerTest {

    @Test
    fun `walks deep tree with child traversal and records paths`() {
        val leaf = FakeNode("leaf")
        val root = FakeNode(
            "root",
            listOf(
                FakeNode("a"),
                FakeNode("b", listOf(FakeNode("b0", listOf(leaf))))
            )
        )

        val result = DebugTreeWalker.walkDepthFirst(
            root = root,
            maxDepth = 80,
            maxNodes = 100,
            identity = { it.id },
            childCount = { it.children.size },
            childAt = { node, index -> node.children.getOrNull(index) }
        )

        assertEquals(listOf("0", "0/0", "0/1", "0/1/0", "0/1/0/0"), result.visited.map { it.path })
        assertEquals(3, result.maxDepthReached)
        assertEquals(false, result.truncated)
    }

    @Test
    fun `truncates safely when node limit is reached`() {
        val root = FakeNode("root", (0 until 10).map { FakeNode("child-$it") })

        val result = DebugTreeWalker.walkDepthFirst(
            root = root,
            maxDepth = 80,
            maxNodes = 3,
            identity = { it.id },
            childCount = { it.children.size },
            childAt = { node, index -> node.children.getOrNull(index) }
        )

        assertEquals(3, result.visited.size)
        assertTrue(result.truncated)
    }

    private data class FakeNode(
        val id: String,
        val children: List<FakeNode> = emptyList()
    )
}
