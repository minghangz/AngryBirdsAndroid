package com.example.angrybirds;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import androidx.core.content.res.ResourcesCompat;

import java.util.LinkedList;
import java.util.List;

/**
 * 动态显示游戏内容
 * @author ZhengMinghang
 */
public class GameSurfaceView extends SurfaceView implements SurfaceHolder.Callback,
        UiInterface, Runnable, View.OnTouchListener {
    public static final int GAME_NOT_READY = 0;
    public static final int GAME_READY = 1;
    public static final int GAME_WIN = 2;
    public static final int GAME_LOSS = 3;
    private int status; // 游戏状态
    private boolean flag; // 刷新画面线程的结束标志

    private List<Body> bodyList; // 显示的所有物品
    private Body shotBody; // 在弹弓上的物品
    private boolean focusOnShot; // 是否正在拖动弹弓上的物品

    private ShotListener shotListener; // 弹弓发射时间监听
    private ClickListener clickListener; // 点击事件监听
    private DestroyListener destroyListener; // 退出事件监听
    private ResumeListener resumeListener; // 重新开始事件监听
    private CreateListener createListener; // 游戏创建事件监听

    private Bitmap bgBmp; // 背景图
    private Canvas canvas; // 画布
    private SurfaceHolder sfh;

    private int screenW, screenH; // 屏幕尺寸
    private float shotWScale = 370f/2560f, shotHScale = 942f/1440f; // 弹弓相对位置
    private float groundHScale = 1236f/1440f; // 地面相对位置

    public GameSurfaceView(final Context context) {
        super(context);
        sfh = this.getHolder();
        sfh.addCallback(this);
        setOnTouchListener(this);
        setFocusable(true);
        bodyList = new LinkedList<>();
        status = GAME_NOT_READY;
    }

    /**
     * 绘图
     */
    private void myDraw() {
        try {
            canvas = sfh.lockCanvas();
            if(canvas != null) {
                // 绘制游戏内容
                Paint paint = new Paint();
                paint.setColor(Color.parseColor("#55280f"));
                paint.setStrokeWidth(30);
                paint.setStrokeCap(Paint.Cap.ROUND);
                canvas.drawBitmap(bgBmp, 0, 0, paint);
                for(Body body: bodyList){
                    if(body == null) continue;
                    if(body == shotBody) { // 弹弓上的物品
                        canvas.drawLine(screenW * 410f / 2560f, screenH * 940f / 1440f,
                                body.x - body.getWidth()/4, body.y, paint);
                        body.draw(canvas, paint);
                        canvas.drawLine(screenW * 338f / 2560f, screenH * 945f / 1440f,
                                body.x - body.getWidth()/4, body.y, paint);
                    }
                    else body.draw(canvas, paint); // 非弹弓上的物品
                }

                // 绘制游戏结束状态
                paint = new Paint();
                paint.setTextSize(Math.min(screenH / 3, screenW / 3));
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setTypeface(ResourcesCompat.getFont(getContext(), R.font.angrybirds));
                paint.setColor(Color.parseColor("#aa6d8346"));
                if(status == GAME_WIN){
                    canvas.drawText("WIN", screenW / 2, screenH * 0.6f, paint);
                }
                else if(status == GAME_LOSS){
                    canvas.drawText("LOSS", screenW / 2, screenH * 0.6f, paint);
                }
            }
        } catch (Exception e) {
            Log.v("myDraw", e.toString());
        } finally {
            if(canvas != null) {
                sfh.unlockCanvasAndPost(canvas);
            }
        }
    }

    @Override
    public int getScreenW() {
        // 等待画布被创建（否则screenW为0）
        while(status == GAME_NOT_READY){
            try { Thread.sleep(100); } catch (Exception e) { }
        }
        return screenW;
    }

    @Override
    public int getScreenH() {
        // 等待画布被创建（否则screenH为0）
        while(status == GAME_NOT_READY){
            try { Thread.sleep(100); } catch (Exception e) { }
        }
        return screenH;
    }

    @Override
    public int getGroundY() {
        while(status == GAME_NOT_READY){
            try { Thread.sleep(100); } catch (Exception e) { }
        }
        return (int) (screenH * groundHScale);
    }

    @Override
    public void addBody(Body body) {
        while(status == GAME_NOT_READY){
            try { Thread.sleep(100); } catch (Exception e) { }
        }
        bodyList.add(body);
    }

    @Override
    public void deleteBody(Body body) {
        if(shotBody == body)
            shotBody = null;
        bodyList.remove(body);
    }

    @Override
    public void resume() {
        bodyList.clear();
        shotBody = null;
        if(status != GAME_NOT_READY)
            status = GAME_READY;
        if(resumeListener != null)
            resumeListener.resumePerformed();
    }

    @Override
    public void putOnSlingshot(Body body, ShotListener listener) {
        addBody(body);
        shotBody = body;
        body.x = (float) (screenW * shotWScale);
        body.y = (float) (screenH * shotHScale);
        shotListener = listener;
    }

    @Override
    public void setClickListener(ClickListener listener) {
        clickListener = listener;
    }

    @Override
    public void gameOver(boolean result) {
        if(result)
            status = GAME_WIN;
        else
            status = GAME_LOSS;
    }

    @Override
    public void setDestroyListener(DestroyListener listener) {
        destroyListener = listener;
    }

    @Override
    public void setResumeListener(ResumeListener listener) {
        resumeListener = listener;
    }

    @Override
    public void setCreateListener(CreateListener listener) {
        createListener = listener;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        screenW = this.getWidth();
        screenH = this.getHeight();
        bgBmp = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(
                this.getResources(), R.drawable.bg_game),
                screenW, screenH, true);
        flag = true;
        new Thread(this).start();
        status = GAME_READY;
        if(createListener != null)
            createListener.createPerformed();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        flag = false;
        if(destroyListener != null)
            destroyListener.destroyPerformed();
    }

    @Override
    public void run() {
        while (flag) {
            myDraw();
            try {
                Thread.sleep(50);
            } catch (Exception e) {
               e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        float x = event.getX(), y = event.getY();
        if(event.getAction() == MotionEvent.ACTION_DOWN){
            if(shotBody != null && shotBody.pointIn(x, y))
                focusOnShot = true;
        }
        if(event.getAction() == MotionEvent.ACTION_MOVE){
            if(focusOnShot && shotBody != null){
                // 限制弹弓在一定范围内拖动
                float ox = screenW * shotWScale, oy = screenH * shotHScale;
                float limit = screenH * (groundHScale - shotHScale) - shotBody.getHeight() / 2;
                if(ViewUtils.dist(x, y, ox, oy) <= limit){
                    shotBody.x = x;
                    shotBody.y = y;
                } else {
                    shotBody.x = (x - ox) / ViewUtils.dist(x, y, ox, oy) * limit + ox;
                    shotBody.y = (y - oy) / ViewUtils.dist(x, y, ox, oy) * limit + oy;
                }
            }
        }
        if(event.getAction() == MotionEvent.ACTION_UP){
            if(focusOnShot && shotBody != null){
                if(shotListener != null){
                    shotListener.shotPerformed(shotBody,
                            screenW * shotWScale, screenH * shotHScale);
                }
                shotBody = null;
            } else if(clickListener != null){
                clickListener.clickPerformed(event.getX(), event.getY());
            }
            focusOnShot = false;
        }
        return true;
    }
}
