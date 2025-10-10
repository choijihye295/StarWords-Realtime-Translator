package com.afteredit.starwords;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class Planets extends Service {
    WindowManager wm;
    LayoutInflater inflater;
    ImageView p_main, p_home, p_trans, p_lang;
    private float touchX, touchY;
    private int mX, mY, direction; // 메인 버튼의 좌표, 애니메이션 동작 방향
    private boolean buttons_flag;   // 서브 버튼 등장 여부 표시
    private boolean is_translating = false; //번역 시작 종료 시 이미지 변경

    //지혜
    private boolean wasIn; // 경계에 있나?

    private WindowManager.LayoutParams paramsM, paramsH, paramsT, paramsL;
    private int screenWidth, screenHeight, leftEdge, rightEdge, dtleft, dtright;
    private String bg_color, text_color;

    SharedPreferences lang_theme;

    // translate

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        //지혜
        makeForegroundNoti();

        lang_theme = getApplicationContext().getSharedPreferences("LANG_THEME", Context.MODE_PRIVATE);
        buttons_flag = false;
        wasIn = false;
        super.onCreate();
        inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        // 지혜
        Display display = wm.getDefaultDisplay();
        Point sizeP = new Point();
        display.getRealSize(sizeP);
        screenWidth = sizeP.x;
        display.getSize(sizeP);
        screenHeight = sizeP.y;

        // 메인 버튼의 초기 윈도우 매개변수 설정
        paramsM = new WindowManager.LayoutParams(
                150,
                150,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE| WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );
        paramsM.gravity = Gravity.LEFT | Gravity.TOP;
        paramsM.x = screenWidth / 2;
        paramsM.y = screenHeight / 2;
        paramsH = new WindowManager.LayoutParams(
                110,
                110,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE| WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );
        paramsH.gravity = Gravity.LEFT | Gravity.TOP;
        paramsH.x = paramsM.x + 20;
        paramsH.y = paramsM.y + 20;
        paramsT = new WindowManager.LayoutParams(
                110,
                110,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE| WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );
        paramsT.gravity = Gravity.LEFT | Gravity.TOP;
        paramsT.x = paramsM.x + 20;
        paramsT.y = paramsM.y + 20;
        paramsL = new WindowManager.LayoutParams(
                110,
                110,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE| WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );
        paramsL.gravity = Gravity.LEFT | Gravity.TOP;
        paramsL.x = paramsM.x + 20;
        paramsL.y = paramsM.y + 20;

        // 서브 버튼 초기화 및 리스너 등록
        p_home = (ImageView) inflater.inflate(R.layout.planet_home, null); //메인으로 돌아가기
        p_home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(Planets.this, "home 메인 눌림", Toast.LENGTH_SHORT).show();
                Context context = getApplicationContext();
                Intent intent = new Intent(context, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.putExtra("FROM_S", true);
                context.startActivity(intent);
            }
        });

        p_trans = (ImageView) inflater.inflate(R.layout.planet_trans, null); //번역시작/종료
        p_trans.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { //버튼 이미지 변경
                Toast.makeText(Planets.this, "trans 눌림", Toast.LENGTH_SHORT).show();
                Context context = getApplicationContext();
                Intent intent = new Intent(context, CaptureActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);

                if (!is_translating) { //캡쳐시작
                    p_trans.setImageResource(R.drawable.start_trans_off);
                    bg_color = lang_theme.getString("BACKGROUND", "FFFFFFFF");
                    text_color = lang_theme.getString("TEXT", "FF000000");
                    Log.d("텍스트테마", "배경: " + bg_color);
                    Log.d("텍스트테마", "텍스트: " + text_color);
                    intent.putExtra("Capture", true);
                    context.startActivity(intent);

                } else { //캡쳐끝
                    p_trans.setImageResource(R.drawable.start_trans_on);
                    intent.putExtra("Capture", false);
                    context.startActivity(intent);
                }
                is_translating = !is_translating;
            }
        });

        p_lang = (ImageView) inflater.inflate(R.layout.planet_lang, null); //언어선택
        p_lang.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(Planets.this, "lang 언어선택 눌림", Toast.LENGTH_SHORT).show();

                // 팝업 메뉴를 생성
                final PopupMenu popupMenu = new PopupMenu(getApplicationContext(), view);

                // 팝업 메뉴를 인플레이트
                MenuInflater inflater = popupMenu.getMenuInflater();
                inflater.inflate(R.menu.pop, popupMenu.getMenu());

                // 팝업 메뉴 아이템 클릭 리스너를 설정
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        String from = menuItem.getTitle().toString();
                        Toast.makeText(Planets.this, from, Toast.LENGTH_SHORT).show();
                        SharedPreferences.Editor editor = lang_theme.edit();
                        switch (from) {
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

        // 메인 버튼 초기화 및 리스너 등록
        p_main = (ImageView) inflater.inflate(R.layout.planet_main, null);
        p_main.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!buttons_flag) {
                    // 경계에 수납되어 있을 경우 버튼 튀어나오기
                    if (paramsM.x <= dtleft) {
                        wasIn = true; // 다시 들어갈 때를 위해 상태 반영
                        ValueAnimator popM = ValueAnimator.ofInt(paramsM.x, dtleft + 30);
                        popM.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(@NonNull ValueAnimator valueAnimator) {
                                int currentX = (int) popM.getAnimatedValue();
                                paramsM.x = currentX;
                                wm.updateViewLayout(p_main, paramsM);
                            }
                        });
                        popM.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationStart(Animator animation) {
                                p_main.setImageResource(R.drawable.sun_floating_on_btn_re);
                            }
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                setSubs();
                                showSubs();
                            }
                        });
                        popM.setDuration(150); // 속도감 있게 튀어나오도록 조정
                        popM.start();
                    }
                    else if (paramsM.x >= dtright) {
                        wasIn = true;
                        ValueAnimator popM = ValueAnimator.ofInt(paramsM.x, dtright - 30);
                        popM.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(@NonNull ValueAnimator valueAnimator) {
                                int currentX = (int) popM.getAnimatedValue();
                                paramsM.x = currentX;
                                wm.updateViewLayout(p_main, paramsM);
                            }
                        });
                        popM.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationStart(Animator animation) {
                                p_main.setImageResource(R.drawable.sun_floating_on_btn_re);
                            }

                            @Override
                            public void onAnimationEnd(Animator animation) {
                                setSubs();
                                showSubs();
                            }
                        });
                        popM.setDuration(150);
                        popM.start();
                    }
                    else {
                        setSubs();
                        showSubs();
                    }
                }
                else {
                    hideSubs();
                    if (wasIn) {
                        // 경계에서 튀어나온 상태라면 다시 경계로 수납
                        putM();
                        p_main.setImageResource(R.drawable.sun_floating_off_btn_re);
                    }
                }
            }
        });

        // 메인을 드래그할 때 버튼 위치 이동
        p_main.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        touchX = motionEvent.getRawX();
                        touchY = motionEvent.getRawY();
                        mX = paramsM.x;
                        mY = paramsM.y;
                        break;
                    case MotionEvent.ACTION_UP:
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (buttons_flag) {
                            hideSubs(); // 버튼 나와있었으면 집어넣기
                        }
                        int gapX = (int) (motionEvent.getRawX() - touchX);
                        int gapY = (int) (motionEvent.getRawY() - touchY);
                        paramsM.x = mX + gapX;
                        paramsM.y = mY + gapY;
                        Log.d("메인좌표", "x "+paramsM.x);
                        Log.d("메인좌표", "y "+paramsM.y);
                        wm.updateViewLayout(p_main, paramsM);

                        // 세로 경계 빠져나가지 않게 처리
                        if (paramsM.y > screenHeight - paramsM.height) {
                            paramsM.y = screenHeight - paramsM.height;
                            wm.updateViewLayout(p_main, paramsM);
                        }
                        if (paramsM.y < 0) {
                            paramsM.y = 0;
                            wm.updateViewLayout(p_main, paramsM);
                        }
                        //지혜
                        if (paramsM.x <= leftEdge + 30 || paramsM.x >= rightEdge - 30 && !wasIn) {
                            putM();
                            wasIn = true;
                        }
                        else {
                            wasIn = false;
                            p_main.setImageResource(R.drawable.sun_floating_on_btn_re);
                        }
                        break;
                }
                return false;
            }
        });

        // 지혜
        leftEdge = 0;
        rightEdge = (int) (screenWidth - paramsM.width);

        // 경계에서 수납되는 위치
        dtleft = (int) (leftEdge - (paramsM.width / 2.5));
        dtright = (int) (rightEdge + (paramsM.width / 2.5));
        Log.d("스크린크기", "넓이 "+screenWidth);
        Log.d("스크린크기", "높이 "+screenHeight);

        // 버튼들을 윈도우 매니저에 추가
        wm.addView(p_home, paramsH);
        wm.addView(p_trans, paramsT);
        wm.addView(p_lang, paramsL);
        wm.addView(p_main, paramsM);

        Log.d("메인좌표", "x "+paramsM.x);
        Log.d("메인좌표", "y "+paramsM.y);
        Log.d("서브1좌표", "x "+paramsH.x);
        Log.d("서브1좌표", "y "+paramsH.y);
        Log.d("서브2좌표", "x "+paramsT.x);
        Log.d("서브2좌표", "y "+paramsT.y);
        Log.d("서브3좌표", "x "+paramsL.x);
        Log.d("서브3좌표", "y "+paramsL.y);

        // 초기에는 서브 버튼 보이지 않음
        removeSubs();
    }

    private void makeForegroundNoti() {
        Notification.Builder noti;
        if (Build.VERSION.SDK_INT > 26) {
            NotificationChannel nc = new NotificationChannel("starwordsch", "starwords", NotificationManager.IMPORTANCE_LOW);
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(nc);
            noti = new Notification.Builder(this, "starwordsch");
        }
        else {
            noti = new Notification.Builder(this);
        }

        noti.setSmallIcon(R.mipmap.ic_launcher);
        noti.setContentTitle("Starwords!");
        noti.setContentText("서비스 실행중");
        startForeground(1, noti.build());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (wm != null) {
            if (p_main != null) {
                // 서비스 종료 시, 뷰를 제거하고 관련 리소스 해제
                wm.removeView(p_main);
                p_main = null;
            }
            if (p_home != null) {
                // 서비스 종료 시, 뷰를 제거하고 관련 리소스 해제
                wm.removeView(p_home);
                p_home = null;
            }
            if (p_trans != null) {
                // 서비스 종료 시, 뷰를 제거하고 관련 리소스 해제
                wm.removeView(p_trans);
                p_trans = null;
            }
            if (p_lang != null) {
                // 서비스 종료 시, 뷰를 제거하고 관련 리소스 해제
                wm.removeView(p_lang);
                p_lang = null;
            }
            wm = null;
        }
    }

    //서브 버튼 퇴장 애니메이션
    public void hideSubs() {
        // y축 (숨길 때)
        ValueAnimator hidehomeY = ValueAnimator.ofInt(paramsH.y, paramsH.y + 130);
        hidehomeY.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(@NonNull ValueAnimator valueAnimator) {
                int currentY = (int) hidehomeY.getAnimatedValue();
                paramsH.y = currentY;
                wm.updateViewLayout(p_home, paramsH);
            }
        });
        ValueAnimator hidelangY = ValueAnimator.ofInt(paramsL.y, paramsL.y - 130);
        hidelangY.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(@NonNull ValueAnimator valueAnimator) {
                int currentY = (int) hidelangY.getAnimatedValue();
                paramsL.y = currentY;
                wm.updateViewLayout(p_lang, paramsL);
            }
        });
        // x축(오른쪽으로)
        ValueAnimator movetransXR = ValueAnimator.ofInt(paramsT.x, paramsT.x + 180);
        movetransXR.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(@NonNull ValueAnimator valueAnimator) {
                int currentX = (int) movetransXR.getAnimatedValue();
                paramsT.x = currentX;
                wm.updateViewLayout(p_trans, paramsT);
            }
        });
        ValueAnimator movehomeXR = ValueAnimator.ofInt(paramsH.x, paramsH.x + 90);
        movehomeXR.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(@NonNull ValueAnimator valueAnimator) {
                int currentX = (int) movehomeXR.getAnimatedValue();
                paramsH.x = currentX;
                wm.updateViewLayout(p_home, paramsH);
            }
        });
        ValueAnimator movelangXR = ValueAnimator.ofInt(paramsL.x, paramsL.x + 90);
        movelangXR.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(@NonNull ValueAnimator valueAnimator) {
                int currentX = (int) movelangXR.getAnimatedValue();
                paramsL.x = currentX;
                wm.updateViewLayout(p_lang, paramsL);
            }
        });
        // x축(왼쪽으로)
        ValueAnimator movetransXL = ValueAnimator.ofInt(paramsT.x, paramsT.x - 180);
        movetransXL.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(@NonNull ValueAnimator valueAnimator) {
                int currentX = (int) movetransXL.getAnimatedValue();
                paramsT.x = currentX;
                wm.updateViewLayout(p_trans, paramsT);
            }
        });
        ValueAnimator movehomeXL = ValueAnimator.ofInt(paramsH.x, paramsH.x - 90);
        movehomeXL.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(@NonNull ValueAnimator valueAnimator) {
                int currentX = (int) movehomeXL.getAnimatedValue();
                paramsH.x = currentX;
                wm.updateViewLayout(p_home, paramsH);
            }
        });
        ValueAnimator movelangXL = ValueAnimator.ofInt(paramsL.x, paramsL.x - 90);
        movelangXL.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(@NonNull ValueAnimator valueAnimator) {
                int currentX = (int) movelangXL.getAnimatedValue();
                paramsL.x = currentX;
                wm.updateViewLayout(p_lang, paramsL);
            }
        });

        AnimatorSet hideSubs = new AnimatorSet();
        if (direction == 0) {
            hideSubs.playTogether(hidehomeY, hidelangY, movehomeXR, movelangXR, movetransXR);
        }
        else if (direction == 1){
            hideSubs.playTogether(hidehomeY, hidelangY, movehomeXL, movelangXL, movetransXL);
        }
        hideSubs.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                removeSubs();
                buttons_flag = false;
            }
        });
        hideSubs.start();
    }
    //서브 버튼 등장 애니메이션
    public void showSubs() {
        // y축 (나올 때)
        ValueAnimator showhomeY = ValueAnimator.ofInt(paramsH.y, paramsH.y - 130);
        showhomeY.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(@NonNull ValueAnimator valueAnimator) {
                int currentY = (int) showhomeY.getAnimatedValue();
                paramsH.y = currentY;
                wm.updateViewLayout(p_home, paramsH);
            }
        });
        ValueAnimator showlangY = ValueAnimator.ofInt(paramsL.y, paramsL.y + 130);
        showlangY.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(@NonNull ValueAnimator valueAnimator) {
                int currentY = (int) showlangY.getAnimatedValue();
                paramsL.y = currentY;
                wm.updateViewLayout(p_lang, paramsL);
            }
        });
        // x축(오른쪽으로)
        ValueAnimator movetransXR = ValueAnimator.ofInt(paramsT.x, paramsT.x + 180);
        movetransXR.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(@NonNull ValueAnimator valueAnimator) {
                int currentX = (int) movetransXR.getAnimatedValue();
                paramsT.x = currentX;
                wm.updateViewLayout(p_trans, paramsT);
            }
        });
        ValueAnimator movehomeXR = ValueAnimator.ofInt(paramsH.x, paramsH.x + 90);
        movehomeXR.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(@NonNull ValueAnimator valueAnimator) {
                int currentX = (int) movehomeXR.getAnimatedValue();
                paramsH.x = currentX;
                wm.updateViewLayout(p_home, paramsH);
            }
        });
        ValueAnimator movelangXR = ValueAnimator.ofInt(paramsL.x, paramsL.x + 90);
        movelangXR.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(@NonNull ValueAnimator valueAnimator) {
                int currentX = (int) movelangXR.getAnimatedValue();
                paramsL.x = currentX;
                wm.updateViewLayout(p_lang, paramsL);
            }
        });
        // x축(왼쪽으로)
        ValueAnimator movetransXL = ValueAnimator.ofInt(paramsT.x, paramsT.x - 180);
        movetransXL.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(@NonNull ValueAnimator valueAnimator) {
                int currentX = (int) movetransXL.getAnimatedValue();
                paramsT.x = currentX;
                wm.updateViewLayout(p_trans, paramsT);
            }
        });
        ValueAnimator movehomeXL = ValueAnimator.ofInt(paramsH.x, paramsH.x - 90);
        movehomeXL.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(@NonNull ValueAnimator valueAnimator) {
                int currentX = (int) movehomeXL.getAnimatedValue();
                paramsH.x = currentX;
                wm.updateViewLayout(p_home, paramsH);
            }
        });
        ValueAnimator movelangXL = ValueAnimator.ofInt(paramsL.x, paramsL.x - 90);
        movelangXL.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(@NonNull ValueAnimator valueAnimator) {
                int currentX = (int) movelangXL.getAnimatedValue();
                paramsL.x = currentX;
                wm.updateViewLayout(p_lang, paramsL);
            }
        });

        AnimatorSet showSubs = new AnimatorSet();
        setDirection();
        if (direction == 0) {
            showSubs.playTogether(showhomeY, showlangY, movelangXL, movehomeXL, movetransXL);
        }
        else {
            showSubs.playTogether(showhomeY, showlangY, movelangXR, movehomeXR, movetransXR);
        }
        showSubs.setStartDelay(500);
        showSubs.setDuration(500);
        showSubs.start();
    }

    // 등장 전에 뷰 위치 맞추고 VISIBLE 처리
    public void setSubs() {
        paramsH.x = paramsM.x + 20;
        paramsH.y = paramsM.y + 20;
        wm.updateViewLayout(p_home, paramsH);
        paramsT.x = paramsM.x + 20;
        paramsT.y = paramsM.y + 20;
        wm.updateViewLayout(p_trans, paramsT);
        paramsL.x = paramsM.x + 20;
        paramsL.y = paramsM.y + 20;
        wm.updateViewLayout(p_lang, paramsL);

        p_home.setVisibility(View.VISIBLE);
        p_trans.setVisibility(View.VISIBLE);
        p_lang.setVisibility(View.VISIBLE);
        buttons_flag = true;
    }

    // 퇴장 후엔 GONE 처리
    public void removeSubs() {
        p_home.setVisibility(View.GONE);
        p_trans.setVisibility(View.GONE);
        p_lang.setVisibility(View.GONE);
        buttons_flag = false;
    }

    // 경계 수납 애니메이션
    public void putM() {
        setDirection();
        ValueAnimator putM;
        if (direction == 0) {
            putM = ValueAnimator.ofInt(paramsM.x, dtright);
        }
        else {
            putM = ValueAnimator.ofInt(paramsM.x, dtleft);
        }
        putM.setDuration(150);
        putM.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(@NonNull ValueAnimator valueAnimator) {
                int currentX = (int) putM.getAnimatedValue();
                paramsM.x = currentX;
                wm.updateViewLayout(p_main, paramsM);
            }
        });
        putM.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                p_main.setImageResource(R.drawable.sun_floating_off_btn_re);
            }
        });
        putM.start();
    }

    // 애니메이션 동작 방향 결정
    public void setDirection() {
        if (paramsM.x > screenWidth / 2.0) {
            direction = 0;
        }
        else {
            direction = 1;
        }
    }

    //언어 선택 관련 메서드 - 버튼의 텍스트를 변경하는 도우미 메서드입니다.
    private void changeButtonText(Button button, String newText) {
        button.setText(newText);
    }

}

