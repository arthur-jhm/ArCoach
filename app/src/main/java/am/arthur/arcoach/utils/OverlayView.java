package am.arthur.arcoach.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseLandmark;

import java.util.List;

public class OverlayView extends View {

    private Pose pose;
    private Paint linePaint;
    private Paint pointPaint;
    private int qualityColor = Color.GREEN;
    private boolean mirrored = false;
    private int mirrorWidth = 0;
    private static final float MIN_CONFIDENCE = 0.6f;

    public OverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        linePaint = new Paint();
        linePaint.setColor(Color.WHITE);
        linePaint.setStrokeWidth(8f);
        linePaint.setStyle(Paint.Style.STROKE);

        pointPaint = new Paint();
        pointPaint.setColor(Color.YELLOW);
        pointPaint.setStrokeWidth(12f);
        pointPaint.setStyle(Paint.Style.FILL);
    }

    public void setPose(Pose pose, int qualityColor) {
        this.pose = pose;
        this.qualityColor = qualityColor;
        invalidate();
    }

    public void setMirrored(boolean mirrored) {
        this.mirrored = mirrored;
    }

    public void setMirrorWidth(int width) {
        this.mirrorWidth = width;
    }

    private float mirrorX(float x) {
        return (mirrored && mirrorWidth > 0) ? mirrorWidth - x : x;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (pose == null) return;

        // цвет линий в зависимости от качества
        linePaint.setColor(qualityColor);

        List<PoseLandmark> landmarks = pose.getAllPoseLandmarks();
        if (landmarks.isEmpty()) return;

        // скелет
        drawLine(canvas, PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER);
        drawLine(canvas, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW);
        drawLine(canvas, PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST);
        drawLine(canvas, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW);
        drawLine(canvas, PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST);

        drawLine(canvas, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP);
        drawLine(canvas, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP);
        drawLine(canvas, PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP);

        drawLine(canvas, PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE);
        drawLine(canvas, PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE);
        drawLine(canvas, PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE);
        drawLine(canvas, PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE);

        // точки суставов
        for (PoseLandmark landmark : landmarks) {
            if (landmark.getInFrameLikelihood() >= MIN_CONFIDENCE) {
                canvas.drawCircle(
                        mirrorX(landmark.getPosition().x),
                        landmark.getPosition().y,
                        8f,
                        pointPaint
                );
            }
        }
    }

    private void drawLine(Canvas canvas, int startLandmarkType, int endLandmarkType) {
        PoseLandmark startLandmark = pose.getPoseLandmark(startLandmarkType);
        PoseLandmark endLandmark = pose.getPoseLandmark(endLandmarkType);

        if (startLandmark != null && endLandmark != null
                && startLandmark.getInFrameLikelihood() >= MIN_CONFIDENCE
                && endLandmark.getInFrameLikelihood() >= MIN_CONFIDENCE)
        {
            canvas.drawLine(
                    mirrorX(startLandmark.getPosition().x),
                    startLandmark.getPosition().y,
                    mirrorX(endLandmark.getPosition().x),
                    endLandmark.getPosition().y,
                    linePaint
            );
        }
    }
}
