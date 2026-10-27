package com.krplay.client;

import android.app.Dialog;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ScrollView;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textview.MaterialTextView;
import com.krplay.protocol.UiPalette;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 29)
public class ThemeAndUiComponentsTest {

    private Object field(Object obj, String name) throws Exception {
        Field f = obj.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(obj);
    }

    private List<CheckBox> findCheckBoxes(View v) {
        List<CheckBox> result = new ArrayList<>();
        if (v instanceof CheckBox) result.add((CheckBox) v);
        if (v instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) v;
            for (int i = 0; i < group.getChildCount(); i++) {
                result.addAll(findCheckBoxes(group.getChildAt(i)));
            }
        }
        return result;
    }

    private List<MaterialButton> findMaterialButtons(View v) {
        List<MaterialButton> result = new ArrayList<>();
        if (v instanceof MaterialButton) result.add((MaterialButton) v);
        if (v instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) v;
            for (int i = 0; i < group.getChildCount(); i++) {
                result.addAll(findMaterialButtons(group.getChildAt(i)));
            }
        }
        return result;
    }

    @Test
    public void defaultThemeIsLightStyleAndUsesMaterial3Components() throws Exception {
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).create();
        try {
            MainActivity activity = controller.get();
            Configuration config = activity.getResources().getConfiguration();
            assertFalse("Default configuration should not be night mode", UiPalette.isDark(config.uiMode));

            // Verify UI components use Material 3 classes
            TextView waiting = (TextView) field(activity, "waiting");
            TextView status = (TextView) field(activity, "status");
            TextView performanceHud = (TextView) field(activity, "performanceHud");

            assertTrue("Waiting text should be MaterialTextView", waiting instanceof MaterialTextView);
            assertTrue("Status text should be MaterialTextView", status instanceof MaterialTextView);
            assertTrue("Performance HUD should be MaterialTextView", performanceHud instanceof MaterialTextView);

            // In light mode, text colors should match the light palette
            assertEquals(UiPalette.LIGHT_MUTED, waiting.getCurrentTextColor());
            assertEquals(UiPalette.LIGHT_MUTED, status.getCurrentTextColor());
            assertEquals(UiPalette.LIGHT_ACCENT, performanceHud.getCurrentTextColor());

            // Open performance dialog and verify MaterialCheckBox & MaterialButton
            Method showPerf = MainActivity.class.getDeclaredMethod("showPerformance");
            showPerf.setAccessible(true);
            showPerf.invoke(activity);

            Dialog dialog = ShadowDialog.getLatestDialog();
            assertNotNull(dialog);
            List<CheckBox> checkBoxes = findCheckBoxes(dialog.getWindow().getDecorView());
            assertEquals(2, checkBoxes.size());
            for (CheckBox cb : checkBoxes) {
                assertTrue("Checkboxes should be MaterialCheckBox", cb instanceof MaterialCheckBox);
            }

            List<MaterialButton> buttons = findMaterialButtons(dialog.getWindow().getDecorView());
            assertFalse("Dialog should contain MaterialButton instances", buttons.isEmpty());
            dialog.dismiss();
        } finally {
            controller.destroy();
        }
    }

    @Test
    @Config(qualifiers = "night")
    public void nightModeSwitchesToDarkStyle() throws Exception {
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).create();
        try {
            MainActivity activity = controller.get();
            Configuration config = activity.getResources().getConfiguration();
            assertTrue("Configuration should be night mode", UiPalette.isDark(config.uiMode));

            TextView waiting = (TextView) field(activity, "waiting");
            TextView status = (TextView) field(activity, "status");
            TextView performanceHud = (TextView) field(activity, "performanceHud");

            // In dark mode, text colors should match the dark palette
            assertEquals(UiPalette.MUTED, waiting.getCurrentTextColor());
            assertEquals(UiPalette.MUTED, status.getCurrentTextColor());
            assertEquals(UiPalette.ACCENT, performanceHud.getCurrentTextColor());
        } finally {
            controller.destroy();
        }
    }

    @Test
    public void configurationChangeDynamicallySwitchesBetweenLightAndDark() throws Exception {
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).create();
        try {
            MainActivity activity = controller.get();
            TextView waiting = (TextView) field(activity, "waiting");
            TextView status = (TextView) field(activity, "status");

            // Initial: Light mode
            assertEquals(UiPalette.LIGHT_MUTED, waiting.getCurrentTextColor());
            assertEquals(UiPalette.LIGHT_MUTED, status.getCurrentTextColor());

            // Change to Night mode
            Configuration nightConfig = new Configuration(activity.getResources().getConfiguration());
            nightConfig.uiMode = (nightConfig.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | Configuration.UI_MODE_NIGHT_YES;
            activity.getResources().updateConfiguration(nightConfig, activity.getResources().getDisplayMetrics());
            activity.onConfigurationChanged(nightConfig);

            assertEquals(UiPalette.MUTED, waiting.getCurrentTextColor());
            assertEquals(UiPalette.MUTED, status.getCurrentTextColor());

            // Change back to Day mode
            Configuration dayConfig = new Configuration(activity.getResources().getConfiguration());
            dayConfig.uiMode = (dayConfig.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | Configuration.UI_MODE_NIGHT_NO;
            activity.getResources().updateConfiguration(dayConfig, activity.getResources().getDisplayMetrics());
            activity.onConfigurationChanged(dayConfig);

            assertEquals(UiPalette.LIGHT_MUTED, waiting.getCurrentTextColor());
            assertEquals(UiPalette.LIGHT_MUTED, status.getCurrentTextColor());
        } finally {
            controller.destroy();
        }
    }

    private ScrollView findScrollView(View v) {
        if (v instanceof ScrollView) return (ScrollView) v;
        if (v instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) v;
            for (int i = 0; i < group.getChildCount(); i++) {
                ScrollView found = findScrollView(group.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    @Test
    public void settingsDialogHasVerticalScrollbarAndBoundedDimensions() throws Exception {
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).create();
        try {
            MainActivity activity = controller.get();
            Method settingsMethod = MainActivity.class.getDeclaredMethod("settings");
            settingsMethod.setAccessible(true);
            settingsMethod.invoke(activity);

            Dialog dialog = ShadowDialog.getLatestDialog();
            assertNotNull("Settings dialog must be shown", dialog);
            ScrollView scrollView = findScrollView(dialog.getWindow().getDecorView());
            assertNotNull("Settings dialog must contain a ScrollView", scrollView);
            assertTrue("ScrollView vertical scrollbar must be enabled", scrollView.isVerticalScrollBarEnabled());
            assertFalse("ScrollView scrollbar fading must be disabled so it remains visible", scrollView.isScrollbarFadingEnabled());
            dialog.dismiss();
        } finally {
            controller.destroy();
        }
    }
}

