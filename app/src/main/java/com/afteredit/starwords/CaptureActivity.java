package com.afteredit.starwords;

/*
작성자: 최우석
작성일: 23년 12월 02일
내  용: 번역 다언어로 변경

translate 함수로 번역
매개변수 ( 원래 언어, 번역할 언어, 번역할 내용, data index )

("en"); // 영어
("ko"); // 한국어
("ja"); // 일본어
("zh"); // 중국어
("es"); // 스페인어
 */


import static java.lang.Thread.sleep;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaPlayer;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

class Data{
    int leftTop_textView_x;
    int leftTop_textView_y;
    int txtWidth;
    int txtHeight;
    String textStr;
    String backgrounColor;
    String textColor;
    public Data(int leftTop_textView_x, int leftTop_textView_y, int txtWidth, int txtHeight, String textStr, String backgrounColor, String textColor) {
        this.leftTop_textView_x = leftTop_textView_x;
        this.leftTop_textView_y = leftTop_textView_y;
        this.txtWidth = txtWidth;
        this.txtHeight = txtHeight;
        this.textStr = textStr;
        this.backgrounColor = "#"+backgrounColor;
        this.textColor = "#"+textColor;
    }
}

public class CaptureActivity extends AppCompatActivity {
    private Button captureButton;
    private MediaProjectionManager projectionManager;
    private static final int REQUEST_CODE = 100;
    public int screenWidth, screenHeight, screenDensity;
    private boolean isCapturing = false;
    public ImageReader imageReader;
    TextView tv_status;

    SharedPreferences lang_theme;

    public void supportedLanguagesAdd(){
        // 지원 언어 추가
        supportedLanguages.add("en"); // 영어
        supportedLanguages.add("ko"); // 한국어
        supportedLanguages.add("ja"); // 일본어
        supportedLanguages.add("zh"); // 중국어
        supportedLanguages.add("es"); // 스페인어
    }

    /////////////////////////////////////////////////////
    // 추출한 텍스트와 위치를 저장
    private String resultText="";

    // 텍스트 뷰들 저장
    Vector<TextView> textViews = new Vector<>();

    // 텍스트 + 위치 저장
    List<Data> dataList = new ArrayList<>();

    // Translator 객체를 저장할 맵
    private Map<String, Translator> translators = new HashMap<>();
    // 지원할 언어 코드 목록
    private List<String> supportedLanguages = new ArrayList<>();

    /////////////////////////////////////////////////////
    public String sourceLanguage = "en", targetLanguage = "ko";

    public WindowManager mManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture);

        tv_status = findViewById(R.id.tv_status);

        lang_theme = getApplicationContext().getSharedPreferences("LANG_THEME", Context.MODE_PRIVATE);
        sourceLanguage = lang_theme.getString("FROM", "en");
        targetLanguage = lang_theme.getString("TO", "ko");

        ///////////////// 번역 //////////////////////
        supportedLanguagesAdd(); // 지원 언어 설정
        // 각 언어에 대한 TranslatorOptions 생성 및 Translator 초기화
        for (String sourceLanguage : supportedLanguages) {
            for (String targetLanguage : supportedLanguages) {
                if (!sourceLanguage.equals(targetLanguage)) {
                    String translatorKey = sourceLanguage + "-" + targetLanguage;
                    TranslatorOptions translatorOptions = new TranslatorOptions.Builder()
                            .setSourceLanguage(sourceLanguage)
                            .setTargetLanguage(targetLanguage)
                            .build();

                    Translator translator = Translation.getClient(translatorOptions);
                    translators.put(translatorKey, translator);
                }
            }
        }
        // 번역 모듈 다운로드
        downloadModel();

        Intent serviceIntent = new Intent(this, ScreenCaptureService.class);
        ContextCompat.startForegroundService(this, serviceIntent);

//        captureButton = findViewById(R.id.capture_button);
        projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        // 서비스에서부터 실행됐을 땐 비디오 스킵
        Intent intent2 = getIntent();
        if (intent2 != null) {
            boolean isCapturing = intent2.getBooleanExtra("Capture", false);
            if (isCapturing) {
                //Toast.makeText(CaptureActivity.this, "Capture True? " + isCapturing, Toast.LENGTH_SHORT).show();
                tv_status.setText("화면 캡처 중입니다...");
                Intent intent = projectionManager.createScreenCaptureIntent();
                startActivityForResult(intent, REQUEST_CODE);
            }
            else {
                //Toast.makeText(CaptureActivity.this, "Capture False? " + isCapturing, Toast.LENGTH_SHORT).show();
                tv_status.setText("캡처 종료.");
                stopScreenCapture();
            }
        }


        // DisplayMetrics 객체를 사용하여 화면 해상도 가져오기
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        screenDensity = metrics.densityDpi;

        mManager = (WindowManager) getSystemService(WINDOW_SERVICE);
    }

    @Override
    protected void onNewIntent(Intent intent3) {
        super.onNewIntent(intent3);
        if (intent3 != null) {
            boolean isCapturing = intent3.getBooleanExtra("Capture", false);
            if (isCapturing) {
                //Toast.makeText(CaptureActivity.this, "Capture True? " + isCapturing, Toast.LENGTH_SHORT).show();
                tv_status.setText("화면 캡처 중입니다...");
                Intent intent = projectionManager.createScreenCaptureIntent();
                startActivityForResult(intent, REQUEST_CODE);
            }
            else {
                //Toast.makeText(CaptureActivity.this, "Capture False? " + isCapturing, Toast.LENGTH_SHORT).show();
                tv_status.setText("캡처 종료.");
                stopScreenCapture();
                //finish();
            }
        }
    }

    private void stopScreenCapture() {
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
        isCapturing = false;
    }

    MediaProjection mediaProjection;
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            mediaProjection = projectionManager.getMediaProjection(resultCode, data);
            startScreenCapture();

        }

    }

    public boolean isDeleted = true;
    private void startScreenCapture() {
        // 이미지 리더 및 가상 디스플레이 생성 로직
        // 여기에서 mediaProjection을 사용하여 화면 캡처 구현

        MediaProjection.Callback projectionCallback = new MediaProjection.Callback() {
            @Override
            public void onStop() {
                // MediaProjection이 중지될 때 필요한 정리 작업
            }
        };
        mediaProjection.registerCallback(projectionCallback, null);
        // 화면 캡쳐를 위한 ImageReader 설정
        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 1);
        mediaProjection.createVirtualDisplay("ScreenCapture",
                screenWidth, screenHeight, screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(), null, null);

        HandlerThread handlerThread = new HandlerThread("ImageListenerThread");
        handlerThread.start();
        Handler handler = new Handler(handlerThread.getLooper());

            // 이미지 캡쳐 및 처리//////////////////////////////////////////////////////////////////////////////////////////
            imageReader.setOnImageAvailableListener(reader -> {
                Image image = null;
                Bitmap bitmap = null;
                try {
                    image = reader.acquireLatestImage();
                    if (image != null) {
                        Image.Plane[] planes = image.getPlanes();
                        ByteBuffer buffer = planes[0].getBuffer();
                        int pixelStride = planes[0].getPixelStride();
                        int rowStride = planes[0].getRowStride();
                        int rowPadding = rowStride - pixelStride * screenWidth;

                        // Bitmap 생성
                        bitmap = Bitmap.createBitmap(screenWidth + rowPadding / pixelStride, screenHeight, Bitmap.Config.ARGB_8888);
                        bitmap.copyPixelsFromBuffer(buffer);

                        if (bitmap != null) {
                            // 캡처된 비트맵을 ImageView에 표시합니다.
                            Bitmap bitmap_ = bitmap;
                            _extractTextFromUri(bitmap_);
                        }

                        // 대기 상태 진입 (동기화 블록 내에서 호출)
                        synchronized (imageReader) {
                            imageReader.wait();
                        }

                        //                    // 파일로 저장
    //                    File file = new File(getExternalFilesDir(null), "screenshot.png");
    //                    fos = new FileOutputStream(file);
    //                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
    //                    Toast.makeText(CaptureActivity.this, "스크린샷 저장됨: " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                        // 대기 상태 진입 (동기화 블록 내에서 호출)

                        Toast.makeText(CaptureActivity.this, "remove함수 호출", Toast.LENGTH_SHORT).show();
                        removeButton();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (bitmap != null) {
                        bitmap.recycle();
                    }
                    if (image != null) {
                        image.close();
                    }
                }
            }, handler);

        isCapturing = true;
    }

    public void _extractTextFromUri(Bitmap bitmap_) {
        FirebaseVisionTextRecognizer recognizer = FirebaseVision.getInstance()
                .getOnDeviceTextRecognizer();

        FirebaseVisionImage image;
        image = FirebaseVisionImage.fromBitmap(bitmap_);

        recognizer.processImage(image)
                .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                    @Override
                    public void onSuccess(FirebaseVisionText firebaseVisionText) {
                        // Toast.makeText(CaptureActivity.this, "스크린샷 저장됨: ", Toast.LENGTH_SHORT).show();
                        parsingStart(firebaseVisionText);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        e.printStackTrace();
                    }
                });
    }

    // 이미지에서 텍스트와 텍스트 블록의 위치를 추출
    private void parsingStart(FirebaseVisionText firebaseVisionText) {
        for (FirebaseVisionText.TextBlock block: firebaseVisionText.getTextBlocks()) {
            Block2Line(block);
        }
    }
    public int block_max_num = 0;
    public int block_count = 0;
    public void Block2Line(FirebaseVisionText.TextBlock block) {
        block_count = 0;
        block_max_num = block.getLines().size();
        for (FirebaseVisionText.Line line : block.getLines()) {
            getLineInfo(line);
        }
    }

    public String _background_color;
    public String _text_color;

    public void getLineInfo(FirebaseVisionText.Line line){
        Point[] blockCornerPoints = line.getCornerPoints(); // 좌표 추출
        resultText = line.getText();         // 문자열 추출

        int txtWidth = blockCornerPoints[2].x - blockCornerPoints[0].x + 10;
        int txtHeight = blockCornerPoints[2].y - blockCornerPoints[0].y + 10;
        int leftTop_textView_x = blockCornerPoints[0].x + txtWidth/2;
        int leftTop_textView_y = blockCornerPoints[0].y + txtHeight/2;
        _background_color = lang_theme.getString("BACKGROUND", "FFFFFFFF");
        _text_color = lang_theme.getString("TEXT", "FF000000");

        Data data = new Data(leftTop_textView_x, leftTop_textView_y, txtWidth, txtHeight, "", _background_color, _text_color);
        dataList.add(data);

        // Toast.makeText(CaptureActivity.this, "스크린샷 저장됨: ", Toast.LENGTH_SHORT).show();

        translate(sourceLanguage, targetLanguage, resultText, dataList.size()-1);
    }

    // Translate ML-kit

    // 예시: 특정 언어에서 다른 언어로 번역
    public void translate(String sourceLanguage, String targetLanguage, String inputText, int i) {
        String translatorKey = sourceLanguage + "-" + targetLanguage;
        Translator translator = translators.get(translatorKey);

        translator.translate(inputText)
                .addOnSuccessListener(new OnSuccessListener<String>() {
                    @Override
                    public void onSuccess(String s) {
                        Data data = dataList.get(i);
                        data.textStr = s;
                        makeButton(data.leftTop_textView_x, data.leftTop_textView_y, data.txtWidth, data.txtHeight, data.textStr, data.backgrounColor, data.textColor);
                        // Utils.showToast(getApplicationContext(), "번역 성공: "+ s);
                        // Toast.makeText(CaptureActivity.this, s, Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
//                        Utils.showToast(getApplicationContext(), "번역 실패");
                        Toast.makeText(CaptureActivity.this, "번역 실패", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // 텍스트 위치에 번역된 내용을 올리는 함수

    // string으로 전달받은 데이터 파싱
    // 형식 왼쪽위 x1,y1 오른쪽아래 x2, y2 내용\n
    // input Format:  x1 y1 x2 y2 text\n
    // input Example: "200 200 200 200 T 1\n400 600 300 200 T2 a\n"
    // output Format: data.leftTop_textView_x, data.leftTop_textView_y, data.txtWidth, data.txtHeight, data.textStr

    private void makeButton(int leftTop_textView_x, int leftTop_textView_y, int txtWidth, int txtHeight, String text, String BackgroundColor, String TextColor) { //버튼생성 메서드
        try {
            final WindowManager mManager = (WindowManager) getSystemService(WINDOW_SERVICE);

            //디스플레이 좌측 상단
            final int leftTop_x = -screenWidth/2 - 20;
            final int leftTop_y = -screenHeight/2 + 20;

            //오버레이 기능 -> 안드로이드 버전에 따라 작동방식 다르게 만듦.
            int flags;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {  //안드로이드 8 이상
                flags = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {  //안드로이드 8 미만
                flags = WindowManager.LayoutParams.TYPE_PHONE;
            }
            //오버레이 창의 넓이와 높이
            final WindowManager.LayoutParams mParams = new WindowManager.LayoutParams(
                    -2, -2, //-2: wrap_content
                    flags, //오버레이 유형
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, //외부 터치 가능
                    PixelFormat.TRANSLUCENT); //창 포맷으로 반투명(translucent) 값을 지정

            TextView txt = new TextView(this); //버튼 인스턴스 생성
            //txt.setPadding(8, 8, 8, 8); // 패딩을 넣고 싶으면 textview 전체 크기를 키우고 넣어야함
            txt.setWidth(txtWidth);
            txt.setHeight(txtHeight);
            TextViewCompat.setAutoSizeTextTypeWithDefaults(txt, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM);
            txt.setGravity(Gravity.CENTER);
            txt.setText(text);
            txt.setBackgroundColor(Color.parseColor(BackgroundColor));
            txt.setTextColor(Color.parseColor(TextColor));

            //주의 - 중심 기준이라 textview 띄울 때 조심해야 함
            //상단 툴바 포함 위치
            mParams.x = leftTop_x + leftTop_textView_x; // x 좌표를 100 픽셀로 설정
            mParams.y = leftTop_y + leftTop_textView_y; // y 좌표를 200 픽셀로 설정
            mManager.addView(txt, mParams); //WindowManager를 사용하여 화면에 뷰 추가

            textViews.add(txt);

            // Toast.makeText(this, "make text", Toast.LENGTH_SHORT).show();
            block_count++;
            if (block_max_num == block_count){
                Toast.makeText(this, "전역 변수 접근 성공", Toast.LENGTH_SHORT).show();
                synchronized (imageReader) {
                    // 어떤 조건이 충족되면 다른 스레드에게 알리고 대기 상태에서 깨움
                    imageReader.notify();
                }
            }

        } catch (Exception e) { //오류 발생시 토스트메시지로 오류내용 출력
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }
    private void removeButton() {
        try {
            // 이용자가 볼수 있는 시간
            sleep(3000);
            // Toast.makeText(this, "제거 시작", Toast.LENGTH_SHORT).show();
            // Toast.makeText(this, Integer.toString(textViews.size()), Toast.LENGTH_SHORT).show();
            for (TextView textView : textViews) {
                mManager.removeView(textView); // 추가된 뷰 제거
                // Toast.makeText(this, "**************", Toast.LENGTH_SHORT).show();
            }
            textViews.clear(); // TextView 목록 비우기
            // 이용자가 볼수 있는 시간
            sleep(1000);
            // Toast.makeText(this, "제거 완료", Toast.LENGTH_SHORT).show();
//            synchronized (imageReader) {
//                imageReader.wait();
//            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void downloadModel(){
        // Toast.makeText(CaptureActivity.this, "모델 다운 시작", Toast.LENGTH_SHORT).show();
        DownloadConditions downloadConditions = new DownloadConditions.Builder()
                .requireWifi()
                .build();

        for ( String key : translators.keySet() ) {
            translators.get(key).downloadModelIfNeeded(downloadConditions)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        public void onSuccess(Void Unused) {
//                            Utils.showToast(getApplicationContext(), "모델 다운 성공");
                            // Toast.makeText(CaptureActivity.this, "모델 다운 성공: " + key, Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
//                            Utils.showToast(getApplicationContext(), "모델 다운 실패");
                            Toast.makeText(CaptureActivity.this, "모델 다운 실패: " + key, Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

}