package com.shilapi.xcertplay
import android.view.View
import android.widget.TextView
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[29],qualifiers="zh-rCN-w360dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NativeFlowTest {
    @org.junit.Before fun preauthorizeFixedEntryForUiTests(){org.robolectric.shadows.ShadowVpnService.setPrepareResult(null)}
    @Test fun launcherUsesTheExistingThreeTabsWithoutAnotherDashboard(){
        val controller=Robolectric.buildActivity(DiPlayActivity::class.java).setup()
        try{val a=controller.get();val all=SettingsNavigation.views(a.window.decorView);val tabs=all.filterIsInstance<BottomNavigationView>().single()
            assertEquals(3,tabs.menu.size());assertEquals(1,tabs.selectedItemId)
            assertNull(shadowOf(a).nextStartedActivity)
            val text=all.filterIsInstance<TextView>().joinToString("\n"){it.text}
            assertTrue(text.contains("车机连接"));assertTrue(text.contains("车载借网"));assertFalse(text.contains("扫描二维码配对"));assertFalse(text.contains("从 DiPlay APK 导入"))
            tabs.selectedItemId=2;assertEquals(2,tabs.selectedItemId)
            assertNull(shadowOf(a).nextStartedActivity)
        }finally{controller.pause().stop().destroy()}
    }
    @Test fun connectionFailureIsVisibleOnIPhonePage(){
        val controller=Robolectric.buildActivity(DiPlayActivity::class.java).setup()
        try{val a=controller.get();val identity=SyntheticMfiIdentity.create();MfiAssetStore(a.noBackupFilesDir).installFiles({identity.key.inputStream()},{identity.certificate.inputStream()})
            ConnectionDiagnostics.failure("FIRST_TCP_TIMEOUT at wlan1")
            controller.pause().resume();val views=SettingsNavigation.views(a.window.decorView);views.filterIsInstance<BottomNavigationView>().single().selectedItemId=2
            assertTrue(SettingsNavigation.views(a.window.decorView).filterIsInstance<TextView>().any{it.text.contains("FIRST_TCP_TIMEOUT")})
        }finally{controller.pause().stop().destroy()}
    }

    @Test fun hotspotPreflightGuidesSetupButDoesNotBlockUsb(){
        val app=org.robolectric.RuntimeEnvironment.getApplication();val identity=SyntheticMfiIdentity.create()
        MfiAssetStore(app.noBackupFilesDir).installFiles({identity.key.inputStream()},{identity.certificate.inputStream()})
        val controller=Robolectric.buildActivity(DiPlayActivity::class.java).setup()
        try{val a=controller.get();val views=SettingsNavigation.views(a.window.decorView)
            views.filterIsInstance<android.widget.Button>().single{it.text=="连接 iPhone"}.performClick()
            val dialog=org.robolectric.shadows.ShadowDialog.getLatestDialog() as androidx.appcompat.app.AlertDialog
            assertTrue(dialog.isShowing)
            assertTrue(SettingsNavigation.views(dialog.window!!.decorView).filterIsInstance<TextView>().any{it.text.toString().contains("热点")})
            assertNull(shadowOf(a).nextStartedActivity)
            dialog.dismiss()
            views.filterIsInstance<android.widget.Button>().single{it.text=="通过 USB 连接"}.performClick()
            assertEquals(CarPlayHostActivity::class.java.name,shadowOf(a).nextStartedActivity.component!!.className)
            assertFalse(AirPlayPersistence.loadWirelessEnabled(a))
        }finally{controller.pause().stop().destroy()}
    }
}
