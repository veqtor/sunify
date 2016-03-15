package com.responcity.dataplayer.dataplayer.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.Date;
import java.util.HashMap;
import java.util.Random;

public class MultiBallView extends View implements View.OnTouchListener {

    Ball temp = new Ball();
    Ball wind = new Ball();
    Ball prec = new Ball();

    Ball[] balls = {temp,wind,prec};

    BallTouchListener listener;

    public void setListener(BallTouchListener listener) {
        this.listener = listener;
    }

    HashMap<Integer, Ball> ballTouchMap = new HashMap<>(3);

    public MultiBallView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setAntiAlias(true);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(8);
        temp.color = Color.YELLOW;
        wind.color = Color.GRAY;
        prec.color = Color.BLUE;
        this.setOnTouchListener(this);

    }

    Paint paint = new Paint();
    Paint textPaint = new Paint();
    float ballRadius;
    int maxDim;

    Random r = new Random(new Date().getTime());

    @Override
    protected void onDraw(Canvas canvas) {
        //super.onDraw(canvas);
        int viewWidthHalf = this.getMeasuredWidth()/2;
        int viewHeightHalf = this.getMeasuredHeight()/2;
        maxDim = 0;
        if(viewWidthHalf>viewHeightHalf)
            maxDim=viewHeightHalf-10;
        else
            maxDim=viewWidthHalf-10;

        ballRadius = maxDim / 12;
        textPaint.setTextSize(ballRadius);

        for(Ball b : balls) {
            paint.setColor(b.color);
            if(b.xpos == 0) {
                b.xpos = r.nextInt(maxDim);
                b.ypos = r.nextInt(maxDim);
            }
            canvas.drawCircle(b.xpos, b.ypos, ballRadius, textPaint);
            canvas.drawCircle(b.xpos, b.ypos, ballRadius * 0.9f, paint);
            //canvas.drawText(b.label,b.xpos, b.ypos,textPaint);
        }

    }

    public void setBallPos(String ball, float x, float y) {
        x = (x + 1.f);
        y = (y + 1.f);
        if(ball.contentEquals("temp")) {
            if(ballTouchMap.containsValue(temp)) {
                return;
            }
            temp.xpos = x * maxDim;
            temp.ypos = y * maxDim;
        }

        if(ball.contentEquals("wind")) {
            if(ballTouchMap.containsValue(wind)) {
                return;
            }
            wind.xpos = x * maxDim;
            wind.ypos = y * maxDim;
        }

        if(ball.contentEquals("prec")) {
            if(ballTouchMap.containsValue(prec)) {
                return;
            }
            prec.xpos = x * maxDim;
            prec.ypos = y * maxDim;
        }
        invalidate();

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        float brd2 = ballRadius /2.f;
        boolean handled = false;
        for(int i = 0; i < event.getPointerCount(); i++) {
            if(!ballTouchMap.isEmpty() && ballTouchMap.containsKey(event.getPointerId(i))) {
                if(event.getAction() == MotionEvent.ACTION_UP) {
                    Ball b = ballTouchMap.get(event.getPointerId(i));
                    ballTouchMap.remove(event.getPointerId(i));
                    dispatchBallTouchChanged(b,false);
                    invalidate();
                    handled = true;
                }
                else if(event.getAction() == MotionEvent.ACTION_MOVE) {
                    Ball b = ballTouchMap.get(event.getPointerId(i));
                    b.xpos = event.getX();
                    b.ypos = event.getY();
                    dispatchBallMovedEvent(b);
                    invalidate();
                    handled = true;
                }
            }
            else if (event.getAction() == MotionEvent.ACTION_DOWN) {
                for (Ball b : balls) {
                    if (event.getX() > b.xpos - brd2 && event.getX() < b.xpos + brd2) {
                        if (event.getY() > b.ypos - brd2 && event.getY() < b.ypos + brd2) {
                            b.xpos = event.getX();
                            b.ypos = event.getY();
                            ballTouchMap.put(event.getPointerId(i),b);
                            dispatchBallTouchChanged(b,true);
                            invalidate();
                            handled = true;
                        }
                    }
                }
            }
        }
        return handled;
    }

    private void dispatchBallMovedEvent(Ball b) {
        float normX = Math.max(-1.f, Math.min(1.f,((b.xpos / maxDim)) - 1.f));
        float normY = Math.max(-1.f, Math.min(1.f,((b.ypos / maxDim)) - 1.f));
        if(listener != null) {
            String ball = "";
            if(b == temp) {
                ball = "temp";
            }
            else if(b == wind) {
                ball = "wind";
            }
            else if(b == prec) {
                ball = "prec";
            }
            if(listener!=null) {
                listener.onBallMoved(ball, normX, normY);
            }
        }
    }

    private void dispatchBallTouchChanged(Ball b, boolean down) {
        String ball = "";
        if(b == temp) {
            ball = "temp";
        }
        else if(b == wind) {
            ball = "wind";
        }
        else if(b == prec) {
            ball = "prec";
        }
        if(listener != null) {
            listener.onBallTouchUpDown(ball, down);
        }
    }

    public interface BallTouchListener {
        void onBallMoved(String ball, float x, float y);
        void onBallTouchUpDown(String ball, boolean down);
    }

    private class Ball {
        float xpos = 0;
        float ypos = 0;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Ball ball = (Ball) o;

            if (Float.compare(ball.xpos, xpos) != 0) return false;
            if (Float.compare(ball.ypos, ypos) != 0) return false;
            if (color != ball.color) return false;
            return label.equals(ball.label);

        }

        @Override
        public int hashCode() {
            int result = (xpos != +0.0f ? Float.floatToIntBits(xpos) : 0);
            result = 31 * result + (ypos != +0.0f ? Float.floatToIntBits(ypos) : 0);
            result = 31 * result + color;
            result = 31 * result + label.hashCode();
            return result;
        }

        int color;
        String label = "0";
    }
}
