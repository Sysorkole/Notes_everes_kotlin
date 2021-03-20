package com.example.notes.database

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.media.UnsupportedSchemeException
import android.net.Uri
import android.os.Build
import android.provider.BaseColumns._ID
import androidx.annotation.RequiresApi
import com.example.notes.database.NotesDatabaseHelper.Companion.TABLE_NOTES
import java.nio.file.PathMatcher

class NotesProvider : ContentProvider() {

    private lateinit var mUriMatcher: UriMatcher
    private lateinit var dbHelper: NotesDatabaseHelper

    override fun onCreate(): Boolean {
        mUriMatcher = UriMatcher(UriMatcher.NO_MATCH)

        //Definindo endereços e identificação do Content Provider
        mUriMatcher.addURI(AUTHORITY, "notes", NOTES)
        mUriMatcher.addURI(AUTHORITY, "notes/#", NOTES_BY_ID) //É para requisições de IDs
        if(context != null){ dbHelper = NotesDatabaseHelper(context as Context) }
        // Garante que o context vai ser do tipo Context através de um casting
        return true
    }

    // 1- Verificamos se é uma uri do tipo ID
    // 2 - Fazemos a operação de delete
    // 3 - Notificamos o Content Provider que foi feita uma operação nessa uri
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        if(mUriMatcher.match(uri) == NOTES_BY_ID){
            val db: SQLiteDatabase = dbHelper.writableDatabase
            val linesAffect: Int = db.delete(TABLE_NOTES, "$_ID =?", arrayOf(uri.lastPathSegment)) //Será o elemento do id que será deletado
            db.close()
            context?.contentResolver?.notifyChange(uri, null) // Notifica pro Content Provider tudo que foi alterado, interna ou externamente
            return linesAffect
        }else{
            throw UnsupportedSchemeException("Uri Inválida para exclusão!")
        }
    }

    // Não implementado
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    override fun getType(uri: Uri): String? = throw UnsupportedSchemeException("Uri não implementado")


    // 1- Verificamos se o uri é um valor
    // 2 - Fizemos a operação dele
    // 3 - Notificamos ao content provider que foi feita uma alteração
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        if(mUriMatcher.match(uri) == NOTES){
            val db:SQLiteDatabase = dbHelper.writableDatabase
            val id:Long = db.insert(TABLE_NOTES, null, values)
            val insertUri: Uri = Uri.withAppendedPath(BASE_URI, id.toString())
            db.close()
            context?.contentResolver?.notifyChange(uri, null)
            return insertUri
        }else{
            throw UnsupportedSchemeException("Uri Inválida para inserção!")
        }
    }

    // 1- Verificamos se é uma uri do tipo ID ou valor
    // 2 - Passamos os dados para um cursor
    // 3 - Cursor notifica que recebeu os dados
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    override fun query(
        uri: Uri, projection: Array<String>?, selection: String?,
        selectionArgs: Array<String>?, sortOrder: String?
    ): Cursor? {
        return when {
            mUriMatcher.match(uri) == NOTES -> {
                val db: SQLiteDatabase = dbHelper.writableDatabase
                val cursor = db.query(TABLE_NOTES, projection, selection, selectionArgs, null, null, sortOrder)
                cursor.setNotificationUri(context?.contentResolver, uri)
                cursor
            }
            mUriMatcher.match(uri) == NOTES_BY_ID ->{
                val db: SQLiteDatabase = dbHelper.writableDatabase
                val cursor = db.query(TABLE_NOTES, projection, "$_ID = ?", arrayOf(uri.lastPathSegment), null,null, sortOrder)
                cursor.setNotificationUri(context?.contentResolver,uri)
                cursor
            }
            else -> {
                throw UnsupportedSchemeException("Uri não implementada!")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    override fun update(
        uri: Uri, values: ContentValues?, selection: String?,
        selectionArgs: Array<String>?
    ): Int {
        if(mUriMatcher.match(uri) == NOTES_BY_ID){
            val db:SQLiteDatabase =  dbHelper.writableDatabase
            val linesAffect: Int = db.update(TABLE_NOTES, values, "$_ID = ?", arrayOf(uri.lastPathSegment))
            db.close()
            context?.contentResolver?.notifyChange(uri, null)
            return linesAffect
        }else{
            throw UnsupportedSchemeException("Uri não implementada!")
        }
    }

    companion object{
        const val AUTHORITY = "com.example.notes.provider"
        val BASE_URI = Uri.parse("content://$AUTHORITY") // Converte a String em URI
        val URI_NOTES = Uri.withAppendedPath(BASE_URI, "notes") // Resultado: "content://com.example.notes.provider/notes"

        const val NOTES = 1
        const val NOTES_BY_ID = 2
    }
}