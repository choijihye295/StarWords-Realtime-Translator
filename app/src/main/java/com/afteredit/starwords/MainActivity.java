package com.afteredit.starwords;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Context;
import android.graphics.Typeface;

import java.util.prefs.Preferences;

public class MainActivity extends AppCompatActivity {
    private static final int ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE = 1;
    ImageView p_service;
    boolean service_running = false;

    AppCompatButton start_lang;
    AppCompatButton destin_lang;
    SharedPreferences lang_theme;
    SharedPreferences.OnSharedPreferenceChangeListener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 레이아웃에서 VideoView와 Button을 가져옵니다.
        MyVideoView myVideoView = findViewById(R.id.openningvideoView);
        ImageView skipButton = findViewById(R.id.skipButton);

        // 서비스에서부터 실행됐을 땐 비디오 스킵
        Intent intent = getIntent();
        if (intent != null) {
            boolean from_service = intent.getBooleanExtra("FROM_S", false);
            if (!from_service) {
                // 비디오 URI 설정 및 재생 시작
                String videoPath = "android.resource://" + getPackageName() + "/raw/openningvideo";
                Uri uri = Uri.parse(videoPath);
                myVideoView.setVideoURI(uri);
                myVideoView.start();

                // VideoView의 재생이 완료되면 처리하는 리스너를 설정합니다.
                myVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        // 비디오 재생이 완료되면 VideoView와 skipButton을 숨깁니다.
                        myVideoView.setVisibility(View.GONE);
                        skipButton.setVisibility(View.GONE);
                        LinearLayout home = findViewById(R.id.home);
                        home.setVisibility(View.VISIBLE);
                    }
                });

                // skip 버튼의 클릭 리스너 설정
                skipButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // 비디오를 스킵합니다. (원하는 시간으로 조절 가능)
                        myVideoView.seekTo(myVideoView.getDuration());
                    }
                });
            }
            else {
                myVideoView.setVisibility(View.GONE);
                skipButton.setVisibility(View.GONE);
                LinearLayout home = findViewById(R.id.home);
                home.setVisibility(View.VISIBLE);
            }
        }

        p_service = findViewById(R.id.p_service);
        start_lang = findViewById(R.id.start_lang);
        destin_lang = findViewById(R.id.destin_lang);

        //서비스가 돌아가고 있는지 확인
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (Planets.class.getName().equals(service.service.getClassName())) {
                p_service.setImageResource(R.drawable.sun_stop_btn);
                service_running = true;
            }
        }

        //언어 및 테마 선택 정보
        lang_theme = getSharedPreferences("LANG_THEME", MODE_PRIVATE);
        listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String s) {
                if (s.equals("FROM")) {
                    String from = lang_theme.getString("FROM", "en");
                    switch (from) {
                        case "en":
                            changeButtonText(start_lang, "영어");
                            break;
                        case "ko":
                            changeButtonText(start_lang, "한국어");
                            break;
                        case "ja":
                            changeButtonText(start_lang, "일본어");
                            break;
                        case "zh":
                            changeButtonText(start_lang, "중국어");
                            break;
                        case "es":
                            changeButtonText(start_lang, "스페인어");
                            break;
                    }
                }
            }
        };
        lang_theme.registerOnSharedPreferenceChangeListener(listener);
        //초기 언어선택 반영
        String from = lang_theme.getString("FROM", "en");
        String to = lang_theme.getString("TO", "ko");
        switch (from) {
            case "en":
                changeButtonText(start_lang, "영어");
                break;
            case "ko":
                changeButtonText(start_lang, "한국어");
                break;
            case "ja":
                changeButtonText(start_lang, "일본어");
                break;
            case "zh":
                changeButtonText(start_lang, "중국어");
                break;
            case "es":
                changeButtonText(start_lang, "스페인어");
                break;
        }
        switch (to) {
            case "en":
                changeButtonText(destin_lang, "영어");
                break;
            case "ko":
                changeButtonText(destin_lang, "한국어");
                break;
            case "ja":
                changeButtonText(destin_lang, "일본어");
                break;
            case "zh":
                changeButtonText(destin_lang, "중국어");
                break;
            case "es":
                changeButtonText(destin_lang, "스페인어");
                break;
        }

        //언어선택
        start_lang.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                // 팝업 메뉴를 생성합니다.
                final PopupMenu popupMenu = new PopupMenu(getApplicationContext(), view);
                getMenuInflater().inflate(R.menu.pop, popupMenu.getMenu());

                // 팝업 메뉴 아이템 클릭 리스너를 설정합니다.
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        // 선택된 메뉴에 따라 버튼1의 텍스트를 변경합니다.
                        String update_st = menuItem.getTitle().toString();
                        changeButtonText(start_lang, update_st);
                        SharedPreferences.Editor editor = lang_theme.edit();
                        switch (update_st) {
                            case "영어":
                                editor.putString("FROM", "en");
                                break;
                            case "한국어":
                                editor.putString("FROM", "ko");
                                break;
                            case "일본어":
                                editor.putString("FROM", "ja");
                                break;
                            case "중국어":
                                editor.putString("FROM", "zh");
                                break;
                            case "스페인어":
                                editor.putString("FROM", "es");
                                break;
                        }
                        editor.apply();
                        return true;
                    }
                });

                // 팝업 메뉴를 표시합니다.
                popupMenu.show();
            }
        });

        destin_lang.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                // 팝업 메뉴를 생성합니다.
                final PopupMenu popupMenu = new PopupMenu(getApplicationContext(), view);
                getMenuInflater().inflate(R.menu.pop, popupMenu.getMenu());

                // 팝업 메뉴 아이템 클릭 리스너를 설정합니다.
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        // 선택된 메뉴에 따라 버튼2의 텍스트를 변경합니다.
                        String update_dt = menuItem.getTitle().toString();
                        changeButtonText(destin_lang, update_dt);
                        SharedPreferences.Editor editor = lang_theme.edit();
                        switch (update_dt) {
                            case "영어":
                                editor.putString("TO", "en");
                                break;
                            case "한국어":
                                editor.putString("TO", "ko");
                                break;
                            case "일본어":
                                editor.putString("TO", "ja");
                                break;
                            case "중국어":
                                editor.putString("TO", "zh");
                                break;
                            case "스페인어":
                                editor.putString("TO", "es");
                                break;
                        }
                        editor.apply();
                        return true;
                    }
                });

                // 팝업 메뉴를 표시합니다.
                popupMenu.show();
            }
        });
    }

    //언어 선택 관련 메서드 - 버튼의 텍스트를 변경하는 도우미 메서드입니다.
    private void changeButtonText(Button button, String newText) {
        button.setText(newText);
    }

    // "메인" 버튼 클릭 이벤트 처리 메서드
    public void serviceOnClick(View v) {
        if (service_running) {
            stopService(new Intent(MainActivity.this, Planets.class));
            service_running = false;
            p_service.setImageResource(R.drawable.sun_start_btn);
        }
        else {
            checkPermission(); // 오버레이 권한을 확인
        }
    }

    // 텍스트테마 변경 버튼 클릭 이벤트 처리 메서드
    public void themeOnClick(View v) {
        // 커스텀 다이얼로그 생성
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View ctView =inflater.inflate(R.layout.dialog_changetheme, null);
        AlertDialog changeTheme = new AlertDialog.Builder(MainActivity.this).setView(ctView).create();

        // 리스너 등록
        TextView darkTheme = ctView.findViewById(R.id.darktheme);
        darkTheme.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 동작
                Toast.makeText(MainActivity.this, "어두운 테마 선택", Toast.LENGTH_SHORT).show();
                SharedPreferences.Editor editor = lang_theme.edit();
                editor.putString("BACKGROUND", "FF000000");
                editor.putString("TEXT", "FFFFFFFF");
                editor.apply();
                changeTheme.dismiss(); // 테마 선택 시 다이얼로그 사라짐
            }
        });
        TextView lightTheme = ctView.findViewById(R.id.lighttheme);
        lightTheme.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 동작
                Toast.makeText(MainActivity.this, "밝은 테마 선택", Toast.LENGTH_SHORT).show();
                SharedPreferences.Editor editor = lang_theme.edit();
                editor.putString("BACKGROUND", "FFFFFFFF");
                editor.putString("TEXT", "FF000000");
                editor.apply();
                changeTheme.dismiss(); // 테마 선택 시 다이얼로그 사라짐
            }
        });
        changeTheme.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        changeTheme.show();
    }

    // 도움말 버튼 클릭 이벤트 처리 메서드 도움말
    public void helpOnClick(View v) {
        Intent intent = new Intent(this, helpActivity.class);
        startActivity(intent);
    }

    // 오버레이 권한을 확인하는 메서드
    public void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                // 오버레이 권한이 없으면 설정 화면으로 이동
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE);
            } else {
                // 오버레이 권한이 있으면 Planets 서비스 시작
                startService(new Intent(MainActivity.this, Planets.class));
                p_service.setImageResource(R.drawable.sun_stop_btn);
                service_running = true;

            }
        } else {
            // 안드로이드 버전이 M 미만인 경우, Planets 서비스 시작
            startService(new Intent(MainActivity.this, Planets.class));
            p_service.setImageResource(R.drawable.sun_stop_btn);
            service_running = true;
        }
    }

    // 오버레이 권한 설정 화면에서 결과를 처리하는 메서드
    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE) {
            if (!Settings.canDrawOverlays(this)) {
                // 오버레이 권한이 여전히 없으면 아무것도 하지 않음
            } else {
                // 오버레이 권한이 부여되면 Planets 서비스 시작
                startService(new Intent(MainActivity.this, Planets.class));
                p_service.setImageResource(R.drawable.sun_stop_btn);
                service_running = true;
            }
        }
    }
}
