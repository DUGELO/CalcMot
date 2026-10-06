package br.com.calcmot.analytics

/** Route templates group visits consistently without leaking dynamic session IDs in tags. */
internal fun productScreenName(namespace: String, route: String): String =
    "${namespace}_${route.substringBefore('/')}".replace(Regex("[^a-zA-Z0-9_]"), "_").take(80)
