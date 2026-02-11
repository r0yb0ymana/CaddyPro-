package com.caddypro.app.data.remote

import com.caddypro.app.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage

/**
 * Supabase Client Configuration
 *
 * Provides access to Supabase services:
 * - Auth: Google Sign-In + Email/Password authentication
 * - Postgrest: PostgreSQL database access with PostGIS support
 * - Storage: File storage for course data and user uploads
 */
object SupabaseClientProvider {

    private var client: SupabaseClient? = null

    fun getClient(): SupabaseClient {
        if (client == null) {
            client = createSupabaseClient(
                supabaseUrl = BuildConfig.SUPABASE_URL,
                supabaseKey = BuildConfig.SUPABASE_ANON_KEY
            ) {
                install(Auth)
                install(Postgrest)
                install(Storage)
            }
        }
        return client!!
    }

    /**
     * Get Auth module for authentication operations
     */
    val auth: Auth
        get() = getClient().auth

    /**
     * Get Postgrest module for database operations
     */
    val postgrest: Postgrest
        get() = getClient().postgrest

    /**
     * Get Storage module for file operations
     */
    val storage: Storage
        get() = getClient().storage
}
