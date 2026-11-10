package com.azkary.azkar2026;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Random;

public class WakeUpActivity extends Activity {

    private int correctAnswer;
    private int questionsCount = 0;
    private static final int REQUIRED_QUESTIONS = 3;
    
    private TextView questionTv, progressTv;
    private Button btn1, btn2, btn3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // ✅ إظهار الشاشة فوق القفل - جميع الإصدارات
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        
        // ✅ إلغاء قفل الشاشة (للإصدارات القديمة)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        }

        setContentView(R.layout.activity_wake_up);

        questionTv = findViewById(R.id.questionText);
        progressTv = findViewById(R.id.progressText);
        btn1 = findViewById(R.id.answer1);
        btn2 = findViewById(R.id.answer2);
        btn3 = findViewById(R.id.answer3);

        if (progressTv == null) {
            progressTv = new TextView(this);
        }

        updateProgress();
        generateQuestion();
    }

    private void updateProgress() {
        progressTv.setText("السؤال: " + (questionsCount + 1) + " من " + REQUIRED_QUESTIONS);
    }

    private void generateQuestion() {
        Random r = new Random();
        int a = r.nextInt(15) + 1;
        int b = r.nextInt(15) + 1;
        correctAnswer = a + b;

        questionTv.setText("أجب ليتوقف الأذان: " + a + " + " + b + " = ؟");

        int pos = r.nextInt(3);
        Button[] buttons = {btn1, btn2, btn3};
        
        for (int i = 0; i < 3; i++) {
            if (i == pos) {
                buttons[i].setText(String.valueOf(correctAnswer));
                buttons[i].setOnClickListener(v -> handleCorrectAnswer());
            } else {
                int wrong = correctAnswer + (r.nextBoolean() ? 2 : -2) + r.nextInt(5);
                if (wrong == correctAnswer) wrong += 1;
                buttons[i].setText(String.valueOf(wrong));
                buttons[i].setOnClickListener(v -> {
                    Toast.makeText(this, "إجابة خاطئة! حاول مرة أخرى", Toast.LENGTH_SHORT).show();
                    questionsCount = 0;
                    updateProgress();
                    generateQuestion();
                });
            }
        }
    }

    private void handleCorrectAnswer() {
        questionsCount++;
        if (questionsCount >= REQUIRED_QUESTIONS) {
            stopAdhanAndFinish();
        } else {
            int remaining = REQUIRED_QUESTIONS - questionsCount;
            Toast.makeText(this, "أحسنت! بقيت " + remaining + " إجابة", Toast.LENGTH_SHORT).show();
            updateProgress();
            generateQuestion();
        }
    }

    private void stopAdhanAndFinish() {
        AdhanReceiver.stopAdhan(this);
        Toast.makeText(this, "تقبل الله طاعتك ✅ تم حل الأسئلة الثلاثة بنجاح", Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
        if (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN || 
            keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP || 
            keyCode == android.view.KeyEvent.KEYCODE_POWER) {
            Toast.makeText(this, "أسئلة الإفاقة مفعّلة: أجب عن الأسئلة الثلاثة لإيقاف الأذان وإغلاق الشاشة ⚠️", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        // منع زر الرجوع
    }
}