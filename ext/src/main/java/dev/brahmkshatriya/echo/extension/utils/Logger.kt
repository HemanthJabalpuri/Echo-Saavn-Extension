package dev.brahmkshatriya.echo.extension.utils

enum class LogLevel(val priority: Int) {
    DEBUG(0), INFO(1), WARN(2), ERROR(3), NONE(4)
}

object Logger {
    private const val PREFIX = "[JioSaavn]"
    
    var minLevel: LogLevel = LogLevel.DEBUG
    
    fun d(tag: String, message: String) {
        if (minLevel.priority <= LogLevel.DEBUG.priority) {
            println("$PREFIX [D] [$tag] $message")
        }
    }
    
    fun i(tag: String, message: String) {
        if (minLevel.priority <= LogLevel.INFO.priority) {
            println("$PREFIX [I] [$tag] $message")
        }
    }
    
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (minLevel.priority <= LogLevel.WARN.priority) {
            println("$PREFIX [W] [$tag] $message")
            throwable?.printStackTrace()
        }
    }
    
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (minLevel.priority <= LogLevel.ERROR.priority) {
            println("$PREFIX [E] [$tag] $message")
            throwable?.printStackTrace()
        }
    }
}
