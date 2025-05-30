package com.xwrl.mvvm.demo.custom;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.xwrl.mvvm.demo.R;

import java.util.Random;

public class GifView {
    private float dX;
    private float dY;
    private ImageView imageView;
    private Context context;
    private static final @DrawableRes int HAPPY = R.drawable.di;
    private static final @DrawableRes int SLEEP = R.drawable.didi;
    private boolean isHappy = false;
    private Handler handler = new Handler();
    private static final long INACTIVITY_TIME_MS = 10000; // 设置无活动时间为5000毫秒(5秒)
    private static final long FADE_DURATION = 300; // 淡入淡出动画的持续时间
    private static final long MOVE_DURATION = 3000; // 移动动画时间
    private int[] positions = {840, 700, 560, 420}; // 可能的 Y 位置
    private boolean isDragging = false;  // 新增一个标记来判断是否正在拖动
    private boolean firstLoad = true;  // 新增一个标记来判断是否为首次加载
    private Handler isHappyHandler = new Handler();
    private Random random = new Random();

    private Runnable inactivityRunnable = new Runnable() {
        @Override
        public void run() {
            if (isHappy) {
                isHappy = false;
                updateGifWithFade();
            }
        }
    };

    private Runnable moveImageRunnable = new Runnable() {
        @Override
        public void run() {
            int randomIndex = random.nextInt(positions.length);
            int newY = positions[randomIndex];
            imageView.animate()
                    .y(newY)
                    .setDuration(MOVE_DURATION)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            isHappy = true; // Assuming you want to set this here
                            updateGifImage(); // Update the GIF image

                            // Remove any existing callbacks to avoid stacking multiple animations
                            handler.removeCallbacks(isHappyTimeoutRunnable);

                            // Post a delay to execute updateGifWithFade
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    updateGifWithFade(); // Call updateGifWithFade after 1 second
                                }
                            }, 10000); // 10 seconds delay
                        }
                    })
                    .start();
        }
    };

    private Runnable isHappyTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            isHappy = false;
            updateGifImage(); // 更新 GIF 图片以反映状态变化
        }
    };

    private Runnable updateGifWithFadeRunnable = new Runnable() {
        @Override
        public void run() {
            updateGifWithFade();
        }
    };

    public GifView(AppCompatActivity activity, int num) {
        this.context = activity;
        this.imageView = activity.findViewById(R.id.gifImageView);
        isHappy = (num != 1);
        setupGif();
    }

    private void setupGif() {
        if (imageView == null) {
            Log.e("GifManager", "ImageView with id 'gifImageView' not found.");
            return;
        }
        updateGifWithFade();
        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                v.getParent().requestDisallowInterceptTouchEvent(true);

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = v.getX() - event.getRawX();
                        dY = v.getY() - event.getRawY();
                        isDragging = false;
                        Log.d("GifManager", "ACTION_DOWN");
                        handler.removeCallbacks(inactivityRunnable);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        float moveX = event.getRawX() + dX;
                        float moveY = event.getRawY() + dY;
                        if (Math.abs(moveX - v.getX()) > 5 || Math.abs(moveY - v.getY()) > 5) {
                            isDragging = true;  // 检测到明显移动
                        }
                        v.setX(moveX);
                        v.setY(moveY);
                        Log.d("GifManager", "ACTION_MOVE");
                        break;
                    case MotionEvent.ACTION_UP:
                        Log.d("GifManager", "ACTION_UP");
                        if (!isDragging) {
                            toggleGifWithFade();
                        }
                        handler.postDelayed(inactivityRunnable, INACTIVITY_TIME_MS);
                        break;
                    case MotionEvent.ACTION_CANCEL:
                        Log.d("GifManager", "ACTION_CANCEL");
                        handler.postDelayed(inactivityRunnable, INACTIVITY_TIME_MS);
                        break;
                }
                return true;
            }
        });
    }

    private void toggleGifWithFade() {
        if (!isHappy) {
            isHappy = true;
            updateGifWithFade();
        }
    }

    private void updateGifWithFade() {
        imageView.animate().alpha(0f).setDuration(FADE_DURATION).withEndAction(new Runnable() {
            @Override
            public void run() {
                Glide.with(context)
                        .asGif()
                        .load(isHappy ? HAPPY : SLEEP)
                        .into(imageView);
                imageView.animate().alpha(1f).setDuration(FADE_DURATION).withStartAction(new Runnable() {
                    @Override
                    public void run() {
                        if (!isHappy) {
                            // 根据是否首次加载选择位置
                            if (firstLoad) {
                                imageView.setX(600);
                                imageView.setY(1000);
                                firstLoad = false;  // 更新首次加载标志
                            }
                        }

                        if (isHappy) {
                            handler.post(moveImageRunnable);  // 确保在更换图像后开始移动
                        }

                        handler.postDelayed(moveImageRunnable, 10000); // 5秒后开始移动
                    }
                }).start();
            }
        }).start();
    }

    private void updateGifImage() {
        if (isHappy) {
            Glide.with(context)
                    .asGif()
                    .load(R.drawable.di) // 假设这是当 isHappy 为 true 时要显示的 GIF
                    .into(imageView);
        } else {
            Glide.with(context)
                    .asGif()
                    .load(R.drawable.didi) // 假设这是 isHappy 为 false 时的 GIF
                    .into(imageView);
        }
    }
}
