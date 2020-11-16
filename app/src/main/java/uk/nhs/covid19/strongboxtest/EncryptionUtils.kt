package uk.nhs.covid19.strongboxtest

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Build.VERSION_CODES
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.EncryptedFile.Builder
import androidx.security.crypto.EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.io.File
import java.io.OutputStreamWriter
import java.security.SecureRandom

data class EncryptedFileInfo(
    val file: File,
    val encryptedFile: EncryptedFile
)

object EncryptionUtils {
    const val TAG = "EncryptionUtils"
    private const val STRONG_BOX_BACKED_MASTER_KEY = "_master_key_strongbox_"
    internal const val KEYSET_PREF_NAME = "__androidx_security_crypto_encrypted_file_pref__"
    private const val STRONGBOX_KEYSET_PREF_NAME =
        "__androidx_security_crypto_encrypted_file_strongbox_pref__"
    private val secureRandom = SecureRandom()

    @SuppressLint("NewApi")
    fun createEncryptedFile(
        context: Context,
        name: String,
        useStrongBox: Boolean
    ): EncryptedFileInfo {
        val (file, encryptedFile) = if (useStrongBox) {
            val file = File(context.filesDir, name + "_strongbox")
            Log.d(TAG, "Has StrongBox")
            val strongBoxMasterKeyAlias = getStrongBoxBackedMasterKey()
            val strongBoxBackedEncryptedFile =
                getEncryptedFile(context, file, strongBoxMasterKeyAlias, STRONGBOX_KEYSET_PREF_NAME)

            file to strongBoxBackedEncryptedFile
        } else {
            val file = File(context.filesDir, name)
            val masterKeyAlias = getDefaultMasterKey()
            file to getEncryptedFile(context, file, masterKeyAlias, KEYSET_PREF_NAME)
        }

        return EncryptedFileInfo(
            file,
            encryptedFile
        )
    }

    fun getEncryptedFile(
        context: Context,
        file: File,
        masterKeyAlias: String,
        keySetPrefName: String
    ): EncryptedFile {
        return Builder(file, context, masterKeyAlias, AES256_GCM_HKDF_4KB)
            .setKeysetPrefName(keySetPrefName)
            .build()
    }

    internal fun getDefaultMasterKey(): String {
        // We can’t use UserAuthenticationRequired because that limits our background access to encrypted data
        return MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    }

    @VisibleForTesting
    @RequiresApi(VERSION_CODES.P)
    internal fun getStrongBoxBackedMasterKey(): String {
        // We can’t use UserAuthenticationRequired because that limits our background access to encrypted data
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            STRONG_BOX_BACKED_MASTER_KEY,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setIsStrongBoxBacked(true)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        return MasterKeys.getOrCreate(keyGenParameterSpec)
    }

    @VisibleForTesting
    internal fun hasStrongBox(context: Context) = Build.VERSION.SDK_INT >= 28 &&
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)

    @SuppressLint("NewApi")
    fun createEncryptedSharedPreferences(
        context: Context, id: Int,
        useStrongBox: Boolean
    ): SharedPreferences {
        return if (useStrongBox) {
            createStrongBoxBackedEncryptedSharedPreferences(context, id)
        } else {
            createEncryptedSharedPreferences(context, getDefaultMasterKey(), "prefs$id")
        }
    }

    @RequiresApi(VERSION_CODES.P)
    private fun createStrongBoxBackedEncryptedSharedPreferences(
        context: Context,
        id: Int
    ): SharedPreferences {
        val strongBoxMasterKeyAlias = getStrongBoxBackedMasterKey()
        return createEncryptedSharedPreferences(
            context,
            strongBoxMasterKeyAlias,
            "strongBoxBackedPrefs$id"
        )
    }

    internal fun createEncryptedSharedPreferences(
        context: Context,
        masterKeyAlias: String,
        fileName: String
    ) =
        EncryptedSharedPreferences.create(
            fileName,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
}

fun EncryptedFile.readText(): String {
    val inputStream = openFileInput()
    return inputStream.bufferedReader().use { it.readText() }
}

fun EncryptedFile.writeText(content: String) {
    val writer = OutputStreamWriter(openFileOutput(), Charsets.UTF_8)
    writer.use { it.write(content) }
}
