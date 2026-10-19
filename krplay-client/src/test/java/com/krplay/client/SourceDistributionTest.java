package com.krplay.client;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.krplay.distribution.SourceDistribution;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 29)
public class SourceDistributionTest {
    @Test public void remoteSourceUsesReceiverRevisionAndRejectsArbitraryUrls() throws Exception {
        String revision = "0123456789abcdef0123456789abcdef01234567";
        JSONObject remote = new JSONObject().put("sourceDistribution", new JSONObject()
            .put("revision", revision).put("sourceUrl", "https://untrusted.example/source"));
        assertEquals(SourceDistribution.REPOSITORY + "/tree/" + revision, SourceDistribution.receiverSourceUrl(remote));
        remote.getJSONObject("sourceDistribution").put("revision", "../../another-project");
        assertEquals(SourceDistribution.REPOSITORY, SourceDistribution.receiverSourceUrl(remote));
        assertEquals(SourceDistribution.REPOSITORY, SourceDistribution.receiverSourceUrl(new JSONObject()));
    }

    @Test public void sourceOfferOpensProjectAndLicenseTextIsAvailableOffline() {
        org.robolectric.android.controller.ActivityController<Activity> controller = Robolectric.buildActivity(Activity.class).setup();
        try {
            Activity activity = controller.get();
            SourceDistribution.open(activity, SourceDistribution.REPOSITORY);
            assertEquals(SourceDistribution.REPOSITORY, Shadows.shadowOf(activity).getNextStartedActivity().getDataString());
            SourceDistribution.showLicenses(activity);
            AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
            String text = allText(dialog.getWindow().getDecorView());
            assertTrue(text.contains("GNU AFFERO GENERAL PUBLIC LICENSE"));
            assertTrue(text.contains("GNU GENERAL PUBLIC LICENSE"));
            assertTrue(text.contains("THIRD_PARTY_NOTICES.md"));
            assertEquals(SourceDistribution.REPOSITORY,
                SourceDistribution.info(RuntimeEnvironment.getApplication()).optString("repository"));
            dialog.dismiss();
        } finally { controller.pause().stop().destroy(); }
    }

    private String allText(View view) {
        StringBuilder result = new StringBuilder();
        if (view instanceof TextView) result.append(((TextView)view).getText());
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup)view;
            for (int i = 0; i < group.getChildCount(); i++) result.append(allText(group.getChildAt(i)));
        }
        return result.toString();
    }
}
