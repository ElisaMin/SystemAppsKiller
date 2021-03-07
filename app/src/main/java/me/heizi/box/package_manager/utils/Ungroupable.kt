package me.heizi.box.package_manager.utils

inline fun StringBuilder.line(crossinline block:()->String) { appendLine(block()) }
