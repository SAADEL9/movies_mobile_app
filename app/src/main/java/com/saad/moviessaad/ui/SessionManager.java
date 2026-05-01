package com.saad.moviessaad.ui;

import com.saad.moviessaad.data.SupabaseService;

public class SessionManager {

    public String getCurrentUserId() {
        return SupabaseService.INSTANCE.getCurrentUserId();
    }

    public boolean isLoggedIn() {
        return SupabaseService.INSTANCE.isLoggedIn();
    }
}
