package com.shilapi.xcertplay
import android.app.Service
import android.content.Intent
import java.io.File
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[29])
class AuthenticationActivationTest {
    @Test fun missingBuiltInResourcesCannotStartCarPlayService(){
        val controller=Robolectric.buildService(DiPlaySessionService::class.java).create()
        try{val s=controller.get();assertEquals(Service.START_NOT_STICKY,s.onStartCommand(null,0,1));assertTrue(shadowOf(s).isStoppedBySelf);assertNull(ReflectionHelpers.getField<Any?>(s,"wakeLock"));assertFalse(CarPlayBackgroundSession.hasSession())}finally{controller.destroy()}
    }
    @Test fun invalidPrivateIdentityIsReportedRatherThanSilentlyAccepted(){
        val app=RuntimeEnvironment.getApplication();val directory=File(app.noBackupFilesDir,"offline-mfi").apply{mkdirs()}
        val a=SyntheticMfiIdentity.create();val b=SyntheticMfiIdentity.create()
        File(directory,"identity.pk8").writeBytes(a.key);File(directory,"certificate.p7b").writeBytes(b.certificate)
        assertTrue(runCatching{DiPlayBootstrap.ensure(app)}.isFailure)
    }
    @Test fun validLocalProvisioningLoadsWithoutAnImportScreen(){
        val app=RuntimeEnvironment.getApplication();val identity=SyntheticMfiIdentity.create()
        MfiAssetStore(app.noBackupFilesDir).installFiles({identity.key.inputStream()},{identity.certificate.inputStream()})
        DiPlayBootstrap.ensure(app)
        assertArrayEquals(identity.certificate,MfiAssetStore(app.noBackupFilesDir).load().readCertificate())
    }
}
