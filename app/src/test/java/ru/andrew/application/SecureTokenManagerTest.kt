package ru.andrew.application

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.andrew.application.notifications.SecureTokenManager
import java.util.concurrent.TimeUnit

class SecureTokenManagerTest {

    private val prefs = FakeSharedPreferences()
    private var nowMillis = 1_700_000_000_000L
    private val clock: () -> Long = { nowMillis }

    /** Новый экземпляр поверх того же хранилища — имитация нового процесса после убийства старого. */
    private fun newProcessManager() = SecureTokenManager(prefs, clock)

    @Test
    fun tokenIssuedInOneProcessIsValidAfterProcessDeath() {
        val token = newProcessManager().generateAndSaveToken(42L)

        assertTrue(newProcessManager().validateAndConsumeToken(42L, token))
    }

    @Test
    fun tokenIsSingleUse() {
        val manager = newProcessManager()
        val token = manager.generateAndSaveToken(42L)

        assertTrue(manager.validateAndConsumeToken(42L, token))
        assertFalse(manager.validateAndConsumeToken(42L, token))
        assertFalse(newProcessManager().validateAndConsumeToken(42L, token))
    }

    @Test
    fun nullTokenIsRejectedAndDoesNotBurnRealToken() {
        val manager = newProcessManager()
        val token = manager.generateAndSaveToken(42L)

        assertFalse(manager.validateAndConsumeToken(42L, null))
        assertTrue(manager.validateAndConsumeToken(42L, token))
    }

    @Test
    fun wrongTokenIsRejectedAndBurnsRealToken() {
        val manager = newProcessManager()
        val token = manager.generateAndSaveToken(42L)

        assertFalse(manager.validateAndConsumeToken(42L, "forged"))
        assertFalse(manager.validateAndConsumeToken(42L, token))
    }

    @Test
    fun tokenIsBoundToItsRequestId() {
        val manager = newProcessManager()
        val token = manager.generateAndSaveToken(42L)

        assertFalse(manager.validateAndConsumeToken(43L, token))
        assertTrue(manager.validateAndConsumeToken(42L, token))
    }

    @Test
    fun requestWithoutIssuedTokenIsRejected() {
        assertFalse(newProcessManager().validateAndConsumeToken(42L, "anything"))
    }

    @Test
    fun regeneratingReplacesPreviousTokenForSameRequest() {
        val manager = newProcessManager()
        val first = manager.generateAndSaveToken(42L)
        val second = manager.generateAndSaveToken(42L)

        assertNotEquals(first, second)
        assertFalse(manager.validateAndConsumeToken(42L, first))

        val third = manager.generateAndSaveToken(42L)
        assertTrue(manager.validateAndConsumeToken(42L, third))
    }

    @Test
    fun tokensForDifferentRequestsAreIndependent() {
        val manager = newProcessManager()
        val tokenA = manager.generateAndSaveToken(1L)
        val tokenB = manager.generateAndSaveToken(2L)

        assertTrue(manager.validateAndConsumeToken(2L, tokenB))
        assertTrue(manager.validateAndConsumeToken(1L, tokenA))
    }

    @Test
    fun expiredTokenIsRejected() {
        val manager = newProcessManager()
        val token = manager.generateAndSaveToken(42L)

        nowMillis += SecureTokenManager.TOKEN_TTL_MILLIS + 1
        assertFalse(manager.validateAndConsumeToken(42L, token))
    }

    @Test
    fun tokenJustInsideTtlIsAccepted() {
        val manager = newProcessManager()
        val token = manager.generateAndSaveToken(42L)

        nowMillis += SecureTokenManager.TOKEN_TTL_MILLIS
        assertTrue(manager.validateAndConsumeToken(42L, token))
    }

    @Test
    fun expiredAndMalformedEntriesArePrunedOnGenerate() {
        val manager = newProcessManager()
        manager.generateAndSaveToken(1L)
        prefs.edit().putString("token_99", "garbage").putString("unrelated", "keep").commit()

        nowMillis += SecureTokenManager.TOKEN_TTL_MILLIS + TimeUnit.MINUTES.toMillis(1)
        manager.generateAndSaveToken(2L)

        assertEquals(setOf("token_2", "unrelated"), prefs.all.keys)
    }

    /** Минимальная in-memory реализация SharedPreferences с семантикой Android Editor. */
    private class FakeSharedPreferences : SharedPreferences {
        private val data = mutableMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = data.toMutableMap()
        override fun getString(key: String?, defValue: String?): String? = data[key] as? String ?: defValue
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
            @Suppress("UNCHECKED_CAST")
            return data[key] as? MutableSet<String> ?: defValues
        }
        override fun getInt(key: String?, defValue: Int): Int = data[key] as? Int ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = data[key] as? Long ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = data[key] as? Float ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = data[key] as? Boolean ?: defValue
        override fun contains(key: String?): Boolean = data.containsKey(key)
        override fun edit(): SharedPreferences.Editor = FakeEditor()
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit

        private inner class FakeEditor : SharedPreferences.Editor {
            private val REMOVED = Any()
            private val pending = linkedMapOf<String, Any?>()
            private var clear = false

            private fun stage(key: String, value: Any?): SharedPreferences.Editor {
                pending[key] = value ?: REMOVED
                return this
            }

            override fun putString(key: String, value: String?) = stage(key, value)
            override fun putStringSet(key: String, values: MutableSet<String>?) = stage(key, values)
            override fun putInt(key: String, value: Int) = stage(key, value)
            override fun putLong(key: String, value: Long) = stage(key, value)
            override fun putFloat(key: String, value: Float) = stage(key, value)
            override fun putBoolean(key: String, value: Boolean) = stage(key, value)
            override fun remove(key: String) = stage(key, REMOVED)
            override fun clear(): SharedPreferences.Editor {
                clear = true
                return this
            }
            override fun commit(): Boolean {
                if (clear) data.clear()
                pending.forEach { (key, value) -> if (value === REMOVED) data.remove(key) else data[key] = value }
                return true
            }
            override fun apply() {
                commit()
            }
        }
    }
}
