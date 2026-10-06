package com.tapshare.app

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.nfc.NfcAdapter
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.View
import android.view.WindowManager
import android.widget.*

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var nfcBtn: Button
    private lateinit var user: EditText
    private lateinit var custom: EditText
    private lateinit var instaRb: RadioButton
    private lateinit var customRb: RadioButton

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val d = resources.displayMetrics.density
        val pad = (20*d).toInt()
        val p = Store.prefs(this)

        val root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(pad,pad*2,pad,pad) }
        fun tv(t:String,sz:Float,bold:Boolean=false)=TextView(this).apply { text=t;textSize=sz;if(bold)setTypeface(typeface,android.graphics.Typeface.BOLD);setPadding(0,0,0,(8*d).toInt()) }

        root.addView(tv("TapShare",30f,true))
        root.addView(tv("Tap phones. Open Instagram. No copying links.",17f,true))
        root.addView(tv("Keep TapShare open on this phone. When another NFC phone taps yours, Android can open the shared Instagram profile directly when Instagram is installed; otherwise it can fall back to the web.",14f))

        val group=RadioGroup(this)
        instaRb=RadioButton(this).apply{text="Instagram profile";id=1}
        customRb=RadioButton(this).apply{text="Custom link";id=2}
        group.addView(instaRb);group.addView(customRb);root.addView(group)

        user=EditText(this).apply{hint="Instagram username";setText(p.getString("user",Store.DEFAULT_USER));inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI;isSingleLine=true}
        custom=EditText(this).apply{hint="https://...";setText(p.getString("custom",""));inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI;isSingleLine=true}
        root.addView(user);root.addView(custom)
        if(p.getString("mode","insta")=="custom")customRb.isChecked=true else instaRb.isChecked=true
        group.setOnCheckedChangeListener{_,_->updateFields()}

        val save=Button(this).apply{text="Save & Ready";setOnClickListener{save()}}
        val test=Button(this).apply{text="Test on this phone";setOnClickListener{openLink()}}
        root.addView(LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;addView(save,LinearLayout.LayoutParams(0,-2,1f));addView(test,LinearLayout.LayoutParams(0,-2,1f))})

        status=tv("",15f).apply{setPadding(0,(16*d).toInt(),0,(8*d).toInt())};root.addView(status)
        nfcBtn=Button(this).apply{text="Turn on NFC";setOnClickListener{startActivity(Intent(Settings.ACTION_NFC_SETTINGS))}};root.addView(nfcBtn)
        root.addView(tv("READY state: screen on + NFC on. Bring the backs of the phones together for about one second.",13f))
        setContentView(ScrollView(this).apply{addView(root)})
        updateFields()
    }

    private fun updateFields(){user.visibility=if(customRb.isChecked)View.GONE else View.VISIBLE;custom.visibility=if(customRb.isChecked)View.VISIBLE else View.GONE}

    private fun save(){
        val mode=if(customRb.isChecked)"custom" else "insta"
        val c=custom.text.toString().trim()
        val u=Store.cleanUser(user.text.toString())
        if(mode=="custom" && !(c.startsWith("http://")||c.startsWith("https://"))){toast("Use an http:// or https:// link");return}
        if(mode=="insta" && u.isEmpty()){toast("Enter your Instagram username");return}
        Store.prefs(this).edit().putString("mode",mode).putString("user",u).putString("custom",c).apply()
        if(!Store.fits(this))toast("Link is too long for the NFC payload") else toast("TapShare is ready")
        refresh()
    }

    private fun openLink(){
        val u=Store.url(this);if(u.isEmpty())return
        try{startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(u)))}catch(_:ActivityNotFoundException){toast("No app can open this link")}
    }

    private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show()

    override fun onResume(){super.onResume();refresh()}

    private fun refresh(){
        val nfc=NfcAdapter.getDefaultAdapter(this)
        nfcBtn.visibility=View.GONE
        status.text=when{
            nfc==null->"This phone does not support NFC."
            !nfc.isEnabled->{nfcBtn.visibility=View.VISIBLE;"NFC is OFF. Turn it on, then return here."}
            Store.url(this).isEmpty()->"Enter your Instagram username and tap Save & Ready."
            !Store.fits(this)->"This link is too long for the NFC payload."
            else->"✓ READY — tap another NFC phone.\nInstagram opens on the receiver when Android resolves the shared profile link."
        }
    }
}
