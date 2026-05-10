package com.saad.moviessaad.ui;

import android.view.View;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

final class SystemBarInsets {

    private SystemBarInsets() {
    }

    static void applyToRoot(View root) {
        applyToRoot(root, true);
    }

    static void applyToRootWithoutBottom(View root) {
        applyToRoot(root, false);
    }

    private static void applyToRoot(View root, boolean includeBottom) {
        if (root == null) return;

        int start = root.getPaddingStart();
        int top = root.getPaddingTop();
        int end = root.getPaddingEnd();
        int bottom = root.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPaddingRelative(
                    start + bars.left,
                    top + bars.top,
                    end + bars.right,
                    bottom + (includeBottom ? bars.bottom : 0)
            );
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
    }
}
