package com.zeekr.bridge
import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.*

/** Small launcher that stays on the root-created secondary display. */
class BoxLauncherActivity:Activity(){
    override fun onCreate(state:Bundle?){super.onCreate(state)
        val column=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(32,24,32,24);setBackgroundColor(0xff101820.toInt())}
        column.addView(TextView(this).apply{text="KrPlay · 盒子应用";textSize=28f;setTextColor(-1)})
        val scroll=ScrollView(this);val grid=GridLayout(this).apply{columnCount=4}
        val apps=packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0).sortedBy{it.loadLabel(packageManager).toString()}
        for(app in apps){if(app.activityInfo.packageName==packageName)continue
            grid.addView(Button(this).apply{text=app.loadLabel(packageManager);textSize=18f;minWidth=220;minimumHeight=100;gravity=Gravity.CENTER
                setOnClickListener{val intent=Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setClassName(app.activityInfo.packageName,app.activityInfo.name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    val displayId=windowManager.defaultDisplay.displayId
                    try{startActivity(intent,ActivityOptions.makeBasic().setLaunchDisplayId(displayId).toBundle())}catch(e:Exception){Toast.makeText(this@BoxLauncherActivity,"应用不支持副屏：${e.message}",Toast.LENGTH_LONG).show()}}
            })
        };scroll.addView(grid);column.addView(scroll);setContentView(column)
    }
}
