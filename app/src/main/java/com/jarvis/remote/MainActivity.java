package com.jarvis.remote;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int SPEECH = 42;
    private static final int MIC_PERMISSION = 43;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService net = Executors.newCachedThreadPool();
    private SharedPreferences prefs;
    private EditText hostBox, pinBox, commandBox;
    private TextView status, response;
    private Button connect, mic;
    private String host = "", token = "";
    private SpeechRecognizer recognizer;

    int bg = Color.rgb(5,11,22), panel = Color.rgb(10,20,36), blue = Color.rgb(32,175,255), cyan = Color.rgb(114,231,255), text = Color.rgb(234,248,255), muted = Color.rgb(118,147,168), red = Color.rgb(255,82,119);

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(bg); getWindow().setNavigationBarColor(bg);
        prefs = getSharedPreferences("jarvis", MODE_PRIVATE);
        host = prefs.getString("host", ""); token = prefs.getString("token", "");
        buildUi();
        if (!host.isEmpty() && !token.isEmpty()) setStatus("● SAVED SESSION");
    }

    TextView tv(String s, float size, int color) { TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); return t; }
    GradientDrawable box(int color, int stroke, int radius) { GradientDrawable g=new GradientDrawable(); g.setColor(color); if(stroke!=0)g.setStroke(1,stroke); g.setCornerRadius(radius); return g; }
    Button btn(String label) { Button b=new Button(this); b.setText(label); b.setTextColor(text); b.setTextSize(12); b.setAllCaps(false); b.setTypeface(Typeface.DEFAULT,Typeface.BOLD); b.setBackground(box(Color.rgb(10,29,50), Color.rgb(24,105,153), 22)); b.setPadding(8,2,8,2); return b; }
    LinearLayout row() { LinearLayout r=new LinearLayout(this); r.setOrientation(LinearLayout.HORIZONTAL); r.setGravity(Gravity.CENTER_VERTICAL); r.setPadding(0,5,0,5); return r; }

    void buildUi() {
        ScrollView scroll=new ScrollView(this); scroll.setBackgroundColor(bg);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(20,18,20,24); scroll.addView(root);

        TextView logo=tv("◉  J A R V I S",25,cyan); logo.setTypeface(Typeface.DEFAULT,Typeface.BOLD); root.addView(logo);
        TextView sub=tv("MOBILE COMMAND INTERFACE  /  LAN LINK",10,muted); sub.setLetterSpacing(.12f); root.addView(sub);
        Space sp=new Space(this); root.addView(sp,new LinearLayout.LayoutParams(1,18));

        LinearLayout statusCard=new LinearLayout(this); statusCard.setOrientation(LinearLayout.VERTICAL); statusCard.setPadding(18,16,18,16); statusCard.setBackground(box(panel,Color.rgb(17,63,91),24));
        status=tv("● OFFLINE",13,red); status.setTypeface(Typeface.DEFAULT,Typeface.BOLD); statusCard.addView(status);
        response=tv("Pair this phone with your running JARVIS dashboard.",11,muted); response.setPadding(0,8,0,0); statusCard.addView(response);
        root.addView(statusCard);

        TextView h=tv("PAIRING",11,cyan); h.setTypeface(Typeface.DEFAULT,Typeface.BOLD); h.setPadding(3,20,0,7); root.addView(h);
        hostBox=new EditText(this); hostBox.setHint("PC address, e.g. 192.168.1.5:8000"); hostBox.setHintTextColor(muted); hostBox.setTextColor(text); hostBox.setText(host); hostBox.setSingleLine(); hostBox.setTextSize(14); hostBox.setPadding(16,0,16,0); hostBox.setBackground(box(Color.rgb(8,17,30),Color.rgb(19,67,95),18)); root.addView(hostBox,new LinearLayout.LayoutParams(-1,52));
        LinearLayout pr=row(); pinBox=new EditText(this); pinBox.setHint("6-digit PIN from JARVIS"); pinBox.setHintTextColor(muted); pinBox.setTextColor(text); pinBox.setInputType(2); pinBox.setTextSize(14); pinBox.setPadding(16,0,16,0); pinBox.setBackground(box(Color.rgb(8,17,30),Color.rgb(19,67,95),18)); pr.addView(pinBox,new LinearLayout.LayoutParams(0,52,1));
        connect=btn("PAIR / CONNECT"); connect.setOnClickListener(v->pair()); LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(150,52); cp.setMargins(8,0,0,0); pr.addView(connect,cp); root.addView(pr);

        TextView ch=tv("QUICK COMMANDS",11,cyan); ch.setTypeface(Typeface.DEFAULT,Typeface.BOLD); ch.setPadding(3,20,0,7); root.addView(ch);
        LinearLayout r1=row(); addQuick(r1,"WAKE", "wake"); addQuick(r1,"LIGHTS ON", "turn on lights"); addQuick(r1,"LIGHTS OFF", "turn off lights"); root.addView(r1);
        LinearLayout r2=row(); addQuick(r2,"SLEEP PC", "put the computer to sleep"); addQuick(r2,"YOUTUBE", "open YouTube"); addQuick(r2,"STOP", "stop"); root.addView(r2);

        TextView vh=tv("VOICE CONTROL",11,cyan); vh.setTypeface(Typeface.DEFAULT,Typeface.BOLD); vh.setPadding(3,20,0,7); root.addView(vh);
        mic=btn("🎙  TAP TO SPEAK TO JARVIS"); mic.setTextSize(14); mic.setTextColor(cyan); mic.setBackground(box(Color.rgb(7,32,52),blue,26)); mic.setOnClickListener(v->startVoice()); root.addView(mic,new LinearLayout.LayoutParams(-1,58));

        LinearLayout cr=row(); commandBox=new EditText(this); commandBox.setHint("Type a command..."); commandBox.setHintTextColor(muted); commandBox.setTextColor(text); commandBox.setSingleLine(); commandBox.setTextSize(14); commandBox.setPadding(16,0,16,0); commandBox.setBackground(box(Color.rgb(8,17,30),Color.rgb(19,67,95),18)); cr.addView(commandBox,new LinearLayout.LayoutParams(0,52,1)); Button send=btn("SEND"); send.setOnClickListener(v->sendText()); LinearLayout.LayoutParams sp2=new LinearLayout.LayoutParams(80,52); sp2.setMargins(8,0,0,0); cr.addView(send,sp2); root.addView(cr);

        TextView foot=tv("JARVIS stays on the PC. This app is a secure LAN remote.",10,muted); foot.setGravity(Gravity.CENTER); foot.setPadding(0,25,0,0); root.addView(foot);
        setContentView(scroll);
    }

    void addQuick(LinearLayout r,String label,String cmd){ Button b=btn(label); b.setOnClickListener(v->{ if(cmd.equals("wake")) wake(); else send(cmd); }); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,46,1); p.setMargins(3,0,3,0); r.addView(b,p); }

    String base(){ String h=hostBox.getText().toString().trim(); if(h.startsWith("http://")) h=h.substring(7); if(h.endsWith("/")) h=h.substring(0,h.length()-1); if(!h.contains(":"))h += ":8000"; return "http://"+h; }
    void setStatus(String s){ main.post(()->status.setText(s)); }
    void msg(String s){ main.post(()->response.setText(s)); }

    void pair(){ host=hostBox.getText().toString().trim(); String pin=pinBox.getText().toString().trim().toUpperCase(Locale.US); if(host.isEmpty()||pin.isEmpty()){msg("Enter the PC address and the PIN shown by JARVIS.");return;} setStatus("● PAIRING…"); net.execute(()->{ try{ JSONObject body=new JSONObject(); body.put("pin",pin); String out=post(base()+"/login",body.toString(),null); JSONObject j=new JSONObject(out); token=j.optString("token",""); if(token.isEmpty()) throw new Exception("No token returned"); prefs.edit().putString("host",host).putString("token",token).apply(); setStatus("● CONNECTED"); msg("JARVIS remote link established."); }catch(Exception e){token="";setStatus("● OFFLINE");msg("Could not connect. Make sure the PC and phone are on the same Wi‑Fi/hotspot and JARVIS is running.");}}); }

    void send(String command){ if(token.isEmpty()){msg("Pair the phone first.");return;} setStatus("● SENDING"); net.execute(()->{try{JSONObject b=new JSONObject();b.put("text",command);post(base()+"/api/command",b.toString(),token);setStatus("● CONNECTED");msg("Command sent: "+command);}catch(Exception e){setStatus("● CONNECTION LOST");msg("Command failed. Check the PC connection.");}}); }
    void sendText(){String s=commandBox.getText().toString().trim(); if(!s.isEmpty()){commandBox.setText("");send(s);}}
    void wake(){if(token.isEmpty()){msg("Pair the phone first.");return;} net.execute(()->{try{post(base()+"/api/wake","{}",token);setStatus("● CONNECTED");msg("Wake signal sent.");}catch(Exception e){setStatus("● CONNECTION LOST");msg("Wake signal failed.");}});}

    String post(String url,String json,String tok)throws Exception{ HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection(); c.setRequestMethod("POST"); c.setConnectTimeout(4500); c.setReadTimeout(7000); c.setDoOutput(true); c.setRequestProperty("Content-Type","application/json"); if(tok!=null)c.setRequestProperty("Authorization","Bearer "+tok); try(OutputStream o=c.getOutputStream()){o.write(json.getBytes("UTF-8"));} int code=c.getResponseCode(); InputStream in=code>=400?c.getErrorStream():c.getInputStream(); ByteArrayOutputStream out=new ByteArrayOutputStream(); byte[] buf=new byte[1024];int n;while((n=in.read(buf))>0)out.write(buf,0,n); if(code>=400)throw new IOException("HTTP "+code); return out.toString("UTF-8"); }

    void startVoice(){ if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},MIC_PERMISSION);return;} if(!SpeechRecognizer.isRecognitionAvailable(this)){msg("Voice recognition is not available on this phone.");return;} Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,Locale.getDefault());i.putExtra(RecognizerIntent.EXTRA_PROMPT,"Speak to JARVIS"); startActivityForResult(i,SPEECH); }
    @Override protected void onActivityResult(int req,int res,Intent data){super.onActivityResult(req,res,data);if(req==SPEECH&&res==RESULT_OK&&data!=null){ArrayList<String>a=data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);if(a!=null&&!a.isEmpty()){commandBox.setText(a.get(0));send(a.get(0));}}}
    @Override public void onRequestPermissionsResult(int r,String[]p,int[]g){super.onRequestPermissionsResult(r,p,g);if(r==MIC_PERMISSION&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED)startVoice();}
    @Override protected void onDestroy(){super.onDestroy();net.shutdownNow();if(recognizer!=null)recognizer.destroy();}
}
