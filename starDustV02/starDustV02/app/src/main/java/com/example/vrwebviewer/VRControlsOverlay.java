// VR CONTROLS OVERLAY - Enhanced VR controls
package com.example.vrwebviewer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.view.MotionEvent;
import android.view.View;

public class VRControlsOverlay extends View {
    
    private Paint crosshairPaint, buttonPaint, textPaint;
    private PointF crosshairPos;
    private VRSettings vrSettings;
    private boolean showControls = true;
    
    public interface VRControlListener {
        void onCrosshairMove(float x, float y);
        void onTriggerClick();
        void onMenuClick();
        void onBackClick();
    }
    
    private VRControlListener controlListener;
    
    public VRControlsOverlay(Context context, VRSettings vrSettings) {
        super(context);
        this.vrSettings = vrSettings;
        initPaints();
        crosshairPos = new PointF();
    }
    
    private void initPaints() {
        crosshairPaint = new Paint();
        crosshairPaint.setColor(vrSettings.getCrosshairColor());
        crosshairPaint.setStrokeWidth(3f);
        crosshairPaint.setAntiAlias(true);
        
        buttonPaint = new Paint();
        buttonPaint.setColor(Color.WHITE);
        buttonPaint.setAlpha(180);
        buttonPaint.setAntiAlias(true);
        
        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(24f);
        textPaint.setAntiAlias(true);
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (!showControls) return;
        
        int width = getWidth();
        int height = getHeight();
        
        // Draw crosshair in center
        drawCrosshair(canvas, width / 2f, height / 2f);
        
        // Draw VR controls
        drawVRButtons(canvas, width, height);
        
        // Draw status info
        drawStatusInfo(canvas, width, height);
    }
    
    private void drawCrosshair(Canvas canvas, float x, float y) {
        float size = vrSettings.getCrosshairSize() * 20f;
        
        // Center dot
        canvas.drawCircle(x, y, 4f, crosshairPaint);
        
        // Cross lines
        canvas.drawLine(x - size, y, x + size, y, crosshairPaint);
        canvas.drawLine(x, y - size, x, y + size, crosshairPaint);
        
        // Outer circle
        crosshairPaint.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(x, y, size, crosshairPaint);
        crosshairPaint.setStyle(Paint.Style.FILL);
        
        crosshairPos.set(x, y);
    }
    
    private void drawVRButtons(Canvas canvas, int width, int height) {
        float buttonSize = 80f;
        float margin = 40f;
        
        // Menu button (top left)
        canvas.drawCircle(margin + buttonSize/2, margin + buttonSize/2, buttonSize/2, buttonPaint);
        canvas.drawText("⚙️", margin + buttonSize/2 - 15, margin + buttonSize/2 + 8, textPaint);
        
        // Back button (top right)
        canvas.drawCircle(width - margin - buttonSize/2, margin + buttonSize/2, buttonSize/2, buttonPaint);
        canvas.drawText("←", width - margin - buttonSize/2 - 10, margin + buttonSize/2 + 8, textPaint);
        
        // Trigger area (bottom center)
        float triggerWidth = 200f;
        float triggerHeight = 60f;
        float triggerX = (width - triggerWidth) / 2;
        float triggerY = height - margin - triggerHeight;
        
        canvas.drawRoundRect(triggerX, triggerY, triggerX + triggerWidth, triggerY + triggerHeight, 20f, 20f, buttonPaint);
        canvas.drawText("TAP TO CLICK", triggerX + 40, triggerY + 35, textPaint);
    }
    
    private void drawStatusInfo(Canvas canvas, int width, int height) {
        if (!vrSettings.getShowDebug()) return;
        
        String fps = "FPS: " + vrSettings.getVrFps();
        String sensitivity = "Sensitivity: " + vrSettings.getGyroSensitivity();
        
        canvas.drawText(fps, 20, height - 80, textPaint);
        canvas.drawText(sensitivity, 20, height - 50, textPaint);
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float x = event.getX();
            float y = event.getY();
            
            // Check button hits
            if (isInMenuButton(x, y)) {
                if (controlListener != null) controlListener.onMenuClick();
                return true;
            } else if (isInBackButton(x, y)) {
                if (controlListener != null) controlListener.onBackClick();
                return true;
            } else if (isInTriggerArea(x, y)) {
                if (controlListener != null) controlListener.onTriggerClick();
                return true;
            }
            
            // Update crosshair position
            if (controlListener != null) {
                controlListener.onCrosshairMove(x, y);
            }
        }
        
        return super.onTouchEvent(event);
    }
    
    private boolean isInMenuButton(float x, float y) {
        float buttonSize = 80f;
        float margin = 40f;
        return x >= margin && x <= margin + buttonSize && y >= margin && y <= margin + buttonSize;
    }
    
    private boolean isInBackButton(float x, float y) {
        float buttonSize = 80f;
        float margin = 40f;
        int width = getWidth();
        return x >= width - margin - buttonSize && x <= width - margin && y >= margin && y <= margin + buttonSize;
    }
    
    private boolean isInTriggerArea(float x, float y) {
        float triggerWidth = 200f;
        float triggerHeight = 60f;
        float margin = 40f;
        int width = getWidth();
        int height = getHeight();
        
        float triggerX = (width - triggerWidth) / 2;
        float triggerY = height - margin - triggerHeight;
        
        return x >= triggerX && x <= triggerX + triggerWidth && y >= triggerY && y <= triggerY + triggerHeight;
    }
    
    public void setControlListener(VRControlListener listener) {
        this.controlListener = listener;
    }
    
    public void setShowControls(boolean show) {
        this.showControls = show;
        invalidate();
    }
    
    public void updateCrosshairPosition(float x, float y) {
        crosshairPos.set(x, y);
        invalidate();
    }
}