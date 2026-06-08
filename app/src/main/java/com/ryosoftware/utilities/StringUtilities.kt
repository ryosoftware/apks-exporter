package com.ryosoftware.utilities

object StringUtilities {

    private const val UNICODE_DIACRITIC_CHARACTERS =
        "ĄŁĽŚŠŞŤŹŽŻąłľśšşťźžżŔÁAĂÄĹĆÇČÉĘËĚÍÎĎĐŃŇÓÔŐÖ×ŘŮÚŰÜÝŢßŕáâăäĺćçčéęëěíîďđńňóôőöřůúűüýţ"
    private const val ASCII_DIACRITIC_REPLACEMENTS =
        "ALLSSSZZZalllssstzzzRAAAAAAALCCCEEEEIIDDNNOOOOxRUUUUYTsraaaalccceeeeiiDDnnoooorUUUUyt"

    fun removeAccents(string: String?): String? {
        if (string == null) return null
        val sb = StringBuilder(string.length)
        var found = false
        for (letter in string) {
            val pos = if (letter <= '\u007E') -1 else UNICODE_DIACRITIC_CHARACTERS.indexOf(letter)
            if (pos > -1) { found = true; sb.append(ASCII_DIACRITIC_REPLACEMENTS[pos]) }
            else sb.append(letter)
        }
        return if (!found) string else sb.toString()
    }

    fun join(values: Array<String?>?, separator: String, lastSeparator: String, from: Int, to: Int): String {
        var string = ""; var end = to
        if (values != null) {
            for (i in to downTo from) if (values[i] != null) { end = i; break }
            if (end >= from) for (i in from..end) if (values[i] != null)
                string += (if (string.isEmpty()) "" else if (i == end) lastSeparator else separator) + values[i]
        }
        return string
    }

    fun join(values: Array<String?>?, separator: String, lastSeparator: String): String {
        if (values == null) return ""
        return join(values, separator, lastSeparator, 0, values.size - 1)
    }

    fun join(values: Array<String?>?, separator: String, from: Int, to: Int) = join(values, separator, separator, from, to)
    fun join(values: Array<String?>?, separator: String) = join(values, separator, separator)

    fun join(values: Set<String>?, separator: String, lastSeparator: String): String {
        if (values != null) return join(values.toTypedArray(), separator, lastSeparator)
        return ""
    }

    fun join(values: Set<String>?, separator: String) = join(values, separator, separator)

    fun <T> join(values: Array<T>?, separator: String, lastSeparator: String): String {
        var string = ""
        if (values != null) {
            val lastValid = values.indices.lastOrNull { values[it] != null } ?: -1
            if (lastValid >= 0) for (i in 0..lastValid) if (values[i] != null)
                string += (if (string.isEmpty()) "" else if (i == lastValid) lastSeparator else separator) + values[i].toString()
        }
        return string
    }

    fun <T> join(values: Array<T>?, separator: String) = join(values, separator, separator)

    fun <T> join(values: List<T>?, separator: String, lastSeparator: String, from: Int, to: Int): String {
        var string = ""; var end = to
        if (values != null) {
            for (i in to downTo from) if (values[i] != null) { end = i; break }
            if (end >= from) for (i in from..end) if (values[i] != null)
                string += (if (string.isEmpty()) "" else if (i == end) lastSeparator else separator) + values[i].toString()
        }
        return string
    }

    fun <T> join(values: List<T>?, separator: String, lastSeparator: String): String {
        if (values == null) return ""
        return join(values, separator, lastSeparator, 0, values.size - 1)
    }

    fun <T> join(values: List<T>?, separator: String) = join(values, separator, separator)

    fun join(values: IntArray?, separator: String, lastSeparator: String): String {
        var string = ""; var actualSep = ""
        if (values != null) for (i in values.indices) {
            string += actualSep + values[i].toString()
            actualSep = if (i == values.size - 2) lastSeparator else separator
        }
        return string
    }

    fun join(values: IntArray?, separator: String) = join(values, separator, separator)

    fun join(values: LongArray?, separator: String): String {
        var string = ""; var actualSep = ""
        if (values != null) for (v in values) { string += actualSep + v.toString(); actualSep = separator }
        return string
    }

    fun split(string: String?, separator: String): ArrayList<String> {
        val result = ArrayList<String>()
        if (!string.isNullOrEmpty()) string.split(separator.toRegex()).forEach { result.add(it) }
        return result
    }

    fun toSingleLine(values: List<String>?): String? {
        if (values.isNullOrEmpty()) return null
        return values.joinToString("")
    }

    fun getPostfix(string: String, prefix: String): String? {
        return if (string.startsWith(prefix)) string.substring(prefix.length) else null
    }

    fun getBytes(string: String): ByteArray = string.toByteArray()

    fun indexOf(values: Array<String>, string: String): Int = values.indexOfFirst { it == string }

    fun getList(strings: Array<String>): List<String> = strings.toList()

    fun reduce(string: String?, maxLength: Int): String {
        if (string.isNullOrEmpty()) return ""
        if (string.length <= maxLength) return string
        return string.substring(0, maxLength).trim() + "..."
    }

    fun equals(list1: Array<String>?, list2: Array<String>?): Boolean {
        if (list1 == null) return list2 == null
        if (list2 == null) return false
        if (list1.size != list2.size) return false
        return list1.all { v1 -> list2.any { v2 -> v1 == v2 } }
    }

    fun quote(string: String?): String? = if (string != null) "\"$string\"" else null

    fun quote(strings: List<String>?): Array<String>? = strings?.map { quote(it) ?: "" }?.toTypedArray()

    fun quote(strings: Array<String>?): Array<String>? = strings?.map { quote(it) ?: "" }?.toTypedArray()

    fun ucFirst(string: String?): String? {
        if (!string.isNullOrEmpty()) return string[0].uppercaseChar() + if (string.length > 1) string.substring(1) else ""
        return null
    }

    fun toLongs(strings: Array<String>?): LongArray? = strings?.map { it.trim().toLong() }?.toLongArray()

    fun isLong(value: String): Boolean = value.toLongOrNull() != null

    fun setMinStringLines(string: String?, lines: Int): String {
        val sb = StringBuilder()
        if (string != null) sb.append(string)
        repeat(lines) { sb.append('\n') }
        return sb.toString()
    }

    fun isNullOrEmpty(string: String?): Boolean = string.isNullOrEmpty()

    fun getFirstNotNullOrEmptyValue(vararg values: String?): String? = values.firstOrNull { !it.isNullOrEmpty() }

    fun equals(left: String?, right: String?): Boolean = left == right

    fun <T> fromSet(values: Set<T>?): List<T> = values?.toList() ?: emptyList()

    fun <T> toListOfObjects(values: Set<T>?): List<Any> = values?.map { it as Any } ?: emptyList()

    fun difference(complete: Array<String>, partial: Array<String>): Array<String> =
        complete.filter { cv -> partial.none { equals(cv, it) } }.toTypedArray()

    fun getStringByIndex(strings: Array<String>, index: Int, defaultValue: String): String =
        if (index >= 0 && index < strings.size) strings[index] else defaultValue
}
