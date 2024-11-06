package com.tool.draw.views.widget;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

import com.tool.draw.fragment.SecondFragment;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Stack;

public class DrawingView extends View {
    private static final Stack<Bitmap> UNDO_STACK = new Stack<>();
    private static final Stack<Bitmap> REDO_STACK = new Stack<>();
    private Paint drawPaint, erasePaint, canvasPaint;
    private Bitmap canvasBitmap;
    private Canvas drawCanvas;
    private float brushSize = 10;
    private Path drawPath;
    private int toolMode = SecondFragment.TOOL_NEUTRAL;
    private Bitmap backgroundBitmap;

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupDrawing();
    }

    private void setupDrawing() {
        drawPath = new Path();
        drawPaint = new Paint();
        drawPaint.setColor(Color.BLACK);
        drawPaint.setAntiAlias(true);
        drawPaint.setStrokeWidth(brushSize);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);

        erasePaint = new Paint();
        erasePaint.setAntiAlias(true);
        erasePaint.setStrokeWidth(brushSize);
        erasePaint.setStyle(Paint.Style.FILL);
        erasePaint.setStrokeJoin(Paint.Join.ROUND);
        erasePaint.setStrokeCap(Paint.Cap.ROUND);
        erasePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        canvasPaint = new Paint(Paint.DITHER_FLAG);

    }

    public Bitmap getBitmapFromDrawingView() {
        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        draw(canvas);
        return bitmap;

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        drawCanvas = new Canvas(canvasBitmap);
    }

    public void setPaintColor(int color) {
        drawPaint.setColor(color);
        invalidate();
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (backgroundBitmap != null) {
            int bitmapWidth = backgroundBitmap.getWidth();
            int bitmapHeight = backgroundBitmap.getHeight();
            float viewWidth = getWidth();
            float viewHeight = getHeight();
            float scale = Math.min(viewWidth / bitmapWidth, viewHeight / bitmapHeight);
            int newWidth = (int) (bitmapWidth * scale);
            int newHeight = (int) (bitmapHeight * scale);
            int left = (int) ((viewWidth - newWidth) / 2);
            int top = (int) ((viewHeight - newHeight) / 2);
            canvas.drawBitmap(backgroundBitmap, null, new Rect(left, top, left + newWidth, top + newHeight), null);
        }
        canvas.drawBitmap(canvasBitmap, 0, 0, canvasPaint);
        if (toolMode == SecondFragment.TOOL_NEUTRAL)
            return;
        if (toolMode == SecondFragment.TOOL_ERASER) {
            canvas.drawPath(drawPath, erasePaint);
        } else {
            canvas.drawPath(drawPath, drawPaint);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float touchX = event.getX();
        float touchY = event.getY();
        float eraserSize = 50;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startDrawing(touchX, touchY, eraserSize);
                break;
            case MotionEvent.ACTION_MOVE:
                drawing(touchX, touchY, eraserSize);
                break;
            case MotionEvent.ACTION_UP:
                stopDrawing(touchX, touchY, eraserSize);
                break;
            default:
                return false;
        }
        invalidate();
        return true;
    }

    private void stopDrawing(float touchX, float touchY, float eraserSize) {
        if (toolMode == SecondFragment.TOOL_ERASER) {
            drawCanvas.drawCircle(touchX, touchY, eraserSize, erasePaint);
        } else if (toolMode == SecondFragment.TOOL_BLACK_PEN) {
            drawPath.lineTo(touchX, touchY);
            drawCanvas.drawPath(drawPath, drawPaint);
        }
        drawPath.reset();
    }

    private void drawing(float touchX, float touchY, float eraserSize) {
        if (toolMode == SecondFragment.TOOL_ERASER) {
            drawCanvas.drawCircle(touchX, touchY, eraserSize, erasePaint);
        } else if (toolMode == SecondFragment.TOOL_BLACK_PEN) {
            drawPath.lineTo(touchX, touchY);
        }
    }

    private void startDrawing(float touchX, float touchY, float eraserSize) {
        if (toolMode == SecondFragment.TOOL_ERASER) {
            saveCanvasState();
            drawCanvas.drawCircle(touchX, touchY, eraserSize, erasePaint);
        } else if (toolMode == SecondFragment.TOOL_BLACK_PEN) {
            saveCanvasState();
            drawPath.moveTo(touchX, touchY);
        }
    }

    public void saveCanvasState() {
        Bitmap saveBitmap = canvasBitmap.copy(Bitmap.Config.ARGB_8888, true);
        UNDO_STACK.push(saveBitmap);
        REDO_STACK.clear();
    }

    public void undo() {
        if (!UNDO_STACK.isEmpty()) {
            REDO_STACK.push(canvasBitmap.copy(Bitmap.Config.ARGB_8888, true));
            canvasBitmap = UNDO_STACK.pop();
            drawCanvas.setBitmap(canvasBitmap);
            invalidate();
        }
    }

    public void redo() {
        if (!REDO_STACK.isEmpty()) {
            UNDO_STACK.push(canvasBitmap.copy(Bitmap.Config.ARGB_8888, true));
            canvasBitmap = REDO_STACK.pop();
            drawCanvas.setBitmap(canvasBitmap);
            invalidate();
        }
    }

    public void setToolMode(int isToolMode) {
        toolMode = isToolMode;
    }

    public void setBrushThickness(float thickness) {
        this.brushSize = thickness;
        drawPaint.setStrokeWidth(thickness);
        erasePaint.setStrokeWidth(thickness);
        invalidate();
    }

    public void setBackgroundBitmap(Bitmap bitmap) {
        this.backgroundBitmap = bitmap;
        invalidate();
    }

    public void loadImage(Uri imageUri) {
        try {
            InputStream inputStream = getContext().getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            int viewWidth = getWidth();
            int viewHeight = getHeight();
            float imageRatio = (float) bitmap.getWidth() / bitmap.getHeight();
            float viewRatio = (float) viewWidth / viewHeight;
            int newWidth, newHeight;
            if (imageRatio > viewRatio) {
                newWidth = viewWidth;
                newHeight = Math.round(viewWidth / imageRatio);
            } else {
                newHeight = viewHeight;
                newWidth = Math.round(viewHeight * imageRatio);
            }
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
            setBackgroundBitmap(resizedBitmap);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void clear() {
        if (drawCanvas != null) {
            drawCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            backgroundBitmap = null;
            invalidate();
        }
    }

    public void resetUndoRedoStacks() {
        UNDO_STACK.clear();
        REDO_STACK.clear();
    }

    public Bitmap getBitmap() {
        Bitmap combinedBitmap = Bitmap.createBitmap(canvasBitmap.getWidth(), canvasBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas combinedCanvas = new Canvas(combinedBitmap);
        if (backgroundBitmap != null) {
            combinedCanvas.drawBitmap(backgroundBitmap, 0, 0, null);
        }
        combinedCanvas.drawBitmap(canvasBitmap, 0, 0, null);
        return combinedBitmap;
    }

    public void saveImage(Context context) {
        Bitmap bitmap = getBitmap();
        String filename = "Drawing_" + System.currentTimeMillis() + ".png";
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MyApp");
            Uri uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                try (FileOutputStream outputStream = (FileOutputStream) context.getContentResolver().openOutputStream(uri)) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                    context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            bitmap.recycle();
        }
    }
}