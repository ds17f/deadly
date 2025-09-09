package com.grateful.deadly

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform