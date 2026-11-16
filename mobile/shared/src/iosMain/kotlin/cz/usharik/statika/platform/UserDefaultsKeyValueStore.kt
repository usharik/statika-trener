package cz.usharik.statika.platform

import platform.Foundation.NSUserDefaults

/** [KeyValueStore] nad NSUserDefaults (celá čísla se tam ukládají jako NSInteger). */
class UserDefaultsKeyValueStore(private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults) : KeyValueStore {
    private fun k(key: String) = "statika.$key"

    override fun getString(key: String): String? = defaults.stringForKey(k(key))
    override fun getInt(key: String, default: Int): Int = if (defaults.objectForKey(k(key)) != null) defaults.integerForKey(k(key)).toInt() else default
    override fun putString(key: String, value: String?) {
        if (value == null) defaults.removeObjectForKey(k(key)) else defaults.setObject(value, k(key))
    }
    override fun putInt(key: String, value: Int) = defaults.setInteger(value.toLong(), k(key))
}
