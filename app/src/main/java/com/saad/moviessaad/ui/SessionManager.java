package com.saad.moviessaad.ui;

import com.saad.moviessaad.data.SupabaseService;

public class SessionManager {

    public String getCurrentUserId() {
        return SupabaseService.INSTANCE.getCurrentUserId();
    }

    public String getToken() {
        return SupabaseService.INSTANCE.getAccessToken();
    }

    public boolean isLoggedIn() {
        return SupabaseService.INSTANCE.isLoggedIn();
    }
}
