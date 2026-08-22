package kotlinx.coroutines

/**
 * Compatibility bridge for code that calls kotlinx.coroutines.async without an
 * explicit CoroutineScope. It keeps the child job attached to the current
 * coroutine context instead of using GlobalScope.
 */
suspend fun <T> async(block: suspend CoroutineScope.() -> T): Deferred<T> {
    return CoroutineScope(currentCoroutineContext()).async(block = block)
}
