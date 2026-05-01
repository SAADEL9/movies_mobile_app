package com.saad.moviessaad.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

object SupabaseClientProvider {

    // Public anon/publishable key only. Do not place service-role keys in the app.
    private const val SUPABASE_URL = "https://xurhbqyamxmjcqlpwvmg.supabase.co"
    private const val SUPABASE_PUBLISHABLE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inh1cmhicXlhbXhtamNxbHB3dm1nIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzcxNDMyMTQsImV4cCI6MjA5MjcxOTIxNH0.rfvq4RL9CEycUOnAHnObL6A2uBGuEM-csWsyQkFJdjg"

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_PUBLISHABLE_KEY
    ) {
        install(Auth)
        install(Postgrest)
        install(Storage)
    }
}
