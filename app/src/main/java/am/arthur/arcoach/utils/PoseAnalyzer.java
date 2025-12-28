package am.arthur.arcoach.utils;

import android.content.Context;
import android.graphics.Color;

import com.google.mlkit.vision.common.PointF3D;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseLandmark;

import am.arthur.arcoach.R;

public class PoseAnalyzer {

    private final Context context;
    private final String exerciseType;
    private int repCount = 0;
    private int goodReps = 0;
    private boolean isDown = false;
    private boolean isGoodForm = false;
    private float minAngleDuringRep = Float.MAX_VALUE;
    private float lastAngle = -1f;
    private static final float ANGLE_SMOOTHING = 0.6f;

    private boolean hadBadBodyForm = false;

    private long plankSegmentStart = 0;
    private long plankAccumulatedMs = 0;
    private boolean isInPlankPosition = false;

    private static final float SQUAT_DOWN_THRESHOLD  = 110f;
    private static final float SQUAT_UP_THRESHOLD    = 160f;
    private static final float SQUAT_PERFECT_MIN     = 80f;
    private static final float SQUAT_PERFECT_MAX     = 110f;
    private static final float SQUAT_TOO_DEEP        = 70f;

    private static final float PUSHUP_DOWN_THRESHOLD    = 100f;
    private static final float PUSHUP_UP_THRESHOLD      = 155f;
    private static final float PUSHUP_PERFECT_MIN       = 70f;
    private static final float PUSHUP_PERFECT_MAX       = 100f;
    // Минимальный угол между плечом, бедром и лодыжкой (shoulder->hip->ankle) на протяжении всего
    // повторения; если он ниже, то нижняя часть тела будет лежать на полу.
    private static final float PUSHUP_BODY_STRAIGHT_MIN = 150f;

    // Plank
    private static final float PLANK_PERFECT_MIN = 155f;
    // При правильном выполнении планки точка опоры запястье/локоть (wrist/elbow) должна находиться ниже плеча:
    // ratio = (supportY − shoulderY) / bodyHorizontalSpan; тело приподнимается, когда
    // коэффициент превышает пороговое значение (ratio > threshold).
    // В горизонтальном положении все находится на уровне пола, ratio ≈ 0, планка отбраковывается.
    private static final float PLANK_ARM_DROP_RATIO = 0.08f;

    private static final float MIN_CONFIDENCE = 0.7f;

    // Jumping Jacks - минимальный реальный размер тела (в пикселях) и минимальное время между переходами состояний.
    private long lastJJStateChangeTime = 0;
    private static final long JJ_MIN_TRANSITION_MS = 400;
    private static final float JJ_MIN_BODY_SIZE_PX = 50f;

    public PoseAnalyzer(Context context, String exerciseType) {
        this.context = context;
        this.exerciseType = exerciseType;
    }

    public AnalysisResult analyze(Pose pose) {
        switch (exerciseType) {
            case "SQUATS":        return analyzeSquats(pose);
            case "PUSHUPS":       return analyzePushups(pose);
            case "JUMPING_JACKS": return analyzeJumpingJacks(pose);
            case "PLANK":         return analyzePlank(pose);
            default:              return new AnalysisResult(0, "Unknown exercise", Color.GRAY, 0);
        }
    }

    private AnalysisResult analyzeSquats(Pose pose) {
        PoseLandmark leftHip    = pose.getPoseLandmark(PoseLandmark.LEFT_HIP);
        PoseLandmark leftKnee   = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE);
        PoseLandmark leftAnkle  = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE);
        PoseLandmark rightHip   = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP);
        PoseLandmark rightKnee  = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE);
        PoseLandmark rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE);

        if (!visible(leftHip) || !visible(leftKnee) || !visible(leftAnkle) ||
                !visible(rightHip) || !visible(rightKnee) || !visible(rightAnkle)) {
            isDown = false;
            minAngleDuringRep = Float.MAX_VALUE;
            lastAngle = -1f; // сброс фильтра: не тянуть старый угол в новую сессию
            return new AnalysisResult(repCount, "Position yourself in frame", Color.GRAY, goodReps);
        }

        float leftKneeAngle  = calculateAngle(leftHip.getPosition3D(),  leftKnee.getPosition3D(),  leftAnkle.getPosition3D());
        float rightKneeAngle = calculateAngle(rightHip.getPosition3D(), rightKnee.getPosition3D(), rightAnkle.getPosition3D());
        float avgKneeAngle   = smoothAngle((leftKneeAngle + rightKneeAngle) / 2f);

        if (avgKneeAngle < SQUAT_DOWN_THRESHOLD && !isDown) {
            isDown = true;
            minAngleDuringRep = avgKneeAngle;
            isGoodForm = (minAngleDuringRep >= SQUAT_PERFECT_MIN && minAngleDuringRep <= SQUAT_PERFECT_MAX);
        }

        // While DOWN: непрерывно отслеживаем достигнутую максимальную глубину.
        if (isDown && avgKneeAngle < minAngleDuringRep) {
            minAngleDuringRep = avgKneeAngle;
            isGoodForm = (minAngleDuringRep >= SQUAT_PERFECT_MIN && minAngleDuringRep <= SQUAT_PERFECT_MAX);
        }

        if (avgKneeAngle > SQUAT_UP_THRESHOLD && isDown) {
            isDown = false;
            repCount++;
            if (isGoodForm) goodReps++;
            minAngleDuringRep = Float.MAX_VALUE;
        }

        // isAscending: user уже достиг дна и теперь поднимается обратно.
        boolean isAscending = isDown && avgKneeAngle > minAngleDuringRep + 5f;

        String feedback;
        int qualityColor;
        String voiceTip = null;
        if (isDown) {
            if (avgKneeAngle < SQUAT_TOO_DEEP) {
                feedback = "Too deep ⚠️";
                qualityColor = Color.RED;
                voiceTip = context.getString(R.string.voice_tip_squat_too_deep);
            } else if (avgKneeAngle >= SQUAT_PERFECT_MIN && avgKneeAngle <= SQUAT_PERFECT_MAX) {
                feedback = isAscending ? "Stand up straight ✓" : "Perfect depth! 👍";
                qualityColor = Color.GREEN;
            } else if (avgKneeAngle > SQUAT_PERFECT_MAX) {
                feedback = isAscending ? "Stand up straight ✓" : "Go lower 💪";
                qualityColor = isAscending ? Color.GREEN : Color.YELLOW;
            } else {
                // 70-80°
                feedback = "Slightly too deep";
                qualityColor = Color.YELLOW;
            }
        } else {
            feedback = "Stand up straight ✓";
            qualityColor = Color.GREEN;
        }

        return new AnalysisResult(repCount, feedback, qualityColor, goodReps, voiceTip);
    }

    private AnalysisResult analyzePushups(Pose pose) {
        PoseLandmark leftShoulder  = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER);
        PoseLandmark leftElbow     = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW);
        PoseLandmark leftWrist     = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST);
        PoseLandmark leftHip       = pose.getPoseLandmark(PoseLandmark.LEFT_HIP);
        PoseLandmark leftAnkle     = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE);
        PoseLandmark rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER);
        PoseLandmark rightElbow    = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW);
        PoseLandmark rightWrist    = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST);
        PoseLandmark rightHip      = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP);
        PoseLandmark rightAnkle    = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE);

        if (!visible(leftShoulder) || !visible(leftElbow) || !visible(leftWrist) ||
                !visible(rightShoulder) || !visible(rightElbow) || !visible(rightWrist)) {
            isDown = false;
            minAngleDuringRep = Float.MAX_VALUE;
            lastAngle = -1f;
            hadBadBodyForm = false;
            return new AnalysisResult(repCount, "Position yourself in frame", Color.GRAY, goodReps);
        }

        float leftElbowAngle  = calculateAngle(leftShoulder.getPosition3D(),  leftElbow.getPosition3D(),  leftWrist.getPosition3D());
        float rightElbowAngle = calculateAngle(rightShoulder.getPosition3D(), rightElbow.getPosition3D(), rightWrist.getPosition3D());
        float avgElbowAngle   = smoothAngle((leftElbowAngle + rightElbowAngle) / 2f);

        // Камера находится сбоку, поэтому используем ту сторону, где расположены все три ориентира.
        // Когда нижняя часть тела лежит ровно, а работает только верхняя часть, бедро опускается
        // ниже линии плеча-лодыжки, и этот угол становится значительно меньше 150°.
        float bodyAlignAngle = -1f;
        boolean leftBodyOk  = visible(leftShoulder)  && visible(leftHip)  && visible(leftAnkle);
        boolean rightBodyOk = visible(rightShoulder) && visible(rightHip) && visible(rightAnkle);
        if (leftBodyOk) {
            bodyAlignAngle = calculateAngle(
                    leftShoulder.getPosition3D(), leftHip.getPosition3D(), leftAnkle.getPosition3D());
        } else if (rightBodyOk) {
            bodyAlignAngle = calculateAngle(
                    rightShoulder.getPosition3D(), rightHip.getPosition3D(), rightAnkle.getPosition3D());
        }

        if (avgElbowAngle < PUSHUP_DOWN_THRESHOLD && !isDown) {
            isDown = true;
            minAngleDuringRep = avgElbowAngle;
            isGoodForm = (minAngleDuringRep >= PUSHUP_PERFECT_MIN && minAngleDuringRep <= PUSHUP_PERFECT_MAX);
        }

        if (isDown) {
            if (avgElbowAngle < minAngleDuringRep) {
                minAngleDuringRep = avgElbowAngle;
                isGoodForm = (minAngleDuringRep >= PUSHUP_PERFECT_MIN && minAngleDuringRep <= PUSHUP_PERFECT_MAX);
            }
            if (bodyAlignAngle >= 0 && bodyAlignAngle < PUSHUP_BODY_STRAIGHT_MIN) {
                hadBadBodyForm = true;
            }
        }

        // требуется как правильная глубина, так и прямая форма тела.
        if (avgElbowAngle > PUSHUP_UP_THRESHOLD && isDown) {
            isDown = false;
            repCount++;
            if (isGoodForm && !hadBadBodyForm) goodReps++;
            minAngleDuringRep = Float.MAX_VALUE;
            hadBadBodyForm = false;
        }

        boolean isAscending = isDown && avgElbowAngle > minAngleDuringRep + 5f;

        String feedback;
        int qualityColor;
        String voiceTip = null;

        if (isDown && bodyAlignAngle >= 0 && bodyAlignAngle < PUSHUP_BODY_STRAIGHT_MIN) {
            feedback = "Keep body straight! Hips are sagging ⬇️";
            qualityColor = Color.RED;
            voiceTip = context.getString(R.string.voice_tip_pushup_body_sagging);
        } else if (isDown) {
            if (avgElbowAngle < PUSHUP_PERFECT_MIN) {
                feedback = "Too deep ⚠️";
                qualityColor = Color.RED;
                voiceTip = context.getString(R.string.voice_tip_pushup_too_deep);
            } else if (avgElbowAngle <= PUSHUP_PERFECT_MAX) {
                feedback = isAscending ? "Push up ✓" : "Perfect form! 👍";
                qualityColor = Color.GREEN;
            } else {
                feedback = isAscending ? "Push up ✓" : "Go lower 💪";
                qualityColor = isAscending ? Color.GREEN : Color.YELLOW;
            }
        } else {
            feedback = "Push up ✓";
            qualityColor = Color.GREEN;
        }

        return new AnalysisResult(repCount, feedback, qualityColor, goodReps, voiceTip);
    }

    private AnalysisResult analyzeJumpingJacks(Pose pose) {
        PoseLandmark leftShoulder  = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER);
        PoseLandmark rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER);
        PoseLandmark leftWrist     = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST);
        PoseLandmark rightWrist    = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST);
        PoseLandmark leftHip       = pose.getPoseLandmark(PoseLandmark.LEFT_HIP);
        PoseLandmark rightHip      = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP);
        PoseLandmark leftAnkle     = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE);
        PoseLandmark rightAnkle    = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE);

        if (!visible(leftShoulder) || !visible(rightShoulder) ||
                !visible(leftWrist) || !visible(rightWrist) ||
                !visible(leftHip) || !visible(rightHip) ||
                !visible(leftAnkle) || !visible(rightAnkle)) {
            isDown = false;
            return new AnalysisResult(repCount, "Position yourself in frame", Color.GRAY, goodReps);
        }

        // Горизонтальный размах плеч и бёдер
        float shoulderSpan = Math.abs(leftShoulder.getPosition3D().getX() - rightShoulder.getPosition3D().getX());
        float hipSpan      = Math.abs(leftHip.getPosition3D().getX()      - rightHip.getPosition3D().getX());

        // Вертикальный размер туловища - среднее по двум сторонам: плечи двигаются при прыжке,
        // поэтому одна сторона может давать завышенное значение.
        float bodyHeightLeft  = Math.abs(leftShoulder.getPosition3D().getY()  - leftHip.getPosition3D().getY());
        float bodyHeightRight = Math.abs(rightShoulder.getPosition3D().getY() - rightHip.getPosition3D().getY());
        float bodyHeight      = (bodyHeightLeft + bodyHeightRight) / 2f;

        // маленький hipSpan делает legsApart истинным.
        if (shoulderSpan < JJ_MIN_BODY_SIZE_PX || bodyHeight < JJ_MIN_BODY_SIZE_PX
                || hipSpan < JJ_MIN_BODY_SIZE_PX * 0.4f) {
            return new AnalysisResult(repCount, "Move closer to camera", Color.GRAY, goodReps);
        }

        float leftArmRise  = leftShoulder.getPosition3D().getY()  - leftWrist.getPosition3D().getY();
        float rightArmRise = rightShoulder.getPosition3D().getY() - rightWrist.getPosition3D().getY();
        boolean armsUp = leftArmRise > bodyHeight * 0.4f && rightArmRise > bodyHeight * 0.4f;

        float ankleSpan = Math.abs(leftAnkle.getPosition3D().getX() - rightAnkle.getPosition3D().getX());
        boolean legsApart = ankleSpan > hipSpan * 1.5f;

        String feedback;
        int qualityColor;

        long nowJJ = System.currentTimeMillis();
        if (armsUp && legsApart) {
            if (!isDown && (nowJJ - lastJJStateChangeTime) >= JJ_MIN_TRANSITION_MS) {
                isDown = true;
                lastJJStateChangeTime = nowJJ;
            }
            feedback = "Great jump! 🤸";
            qualityColor = Color.GREEN;
        } else if (!armsUp && !legsApart) {
            if (isDown && (nowJJ - lastJJStateChangeTime) >= JJ_MIN_TRANSITION_MS) {
                repCount++;
                goodReps++;
                isDown = false;
                lastJJStateChangeTime = nowJJ;
            }
            feedback = "Starting position ✓";
            qualityColor = Color.GREEN;
        } else {
            feedback = !armsUp ? "Raise your arms! 💪" : "Spread your legs! 🦵";
            qualityColor = Color.YELLOW;
        }

        return new AnalysisResult(repCount, feedback, qualityColor, goodReps);
    }

    private AnalysisResult analyzePlank(Pose pose) {
        PoseLandmark leftShoulder  = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER);
        PoseLandmark leftElbow     = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW);
        PoseLandmark leftWrist     = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST);
        PoseLandmark leftHip       = pose.getPoseLandmark(PoseLandmark.LEFT_HIP);
        PoseLandmark leftAnkle     = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE);
        PoseLandmark rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER);
        PoseLandmark rightElbow    = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW);
        PoseLandmark rightWrist    = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST);
        PoseLandmark rightHip      = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP);
        PoseLandmark rightAnkle    = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE);

        boolean leftOk  = visible(leftShoulder)  && visible(leftHip)  && visible(leftAnkle);
        boolean rightOk = visible(rightShoulder) && visible(rightHip) && visible(rightAnkle);

        if (!leftOk && !rightOk) {
            plankPauseTimer();
            lastAngle = -1f;
            return new AnalysisResult(repCount, "Position yourself in frame", Color.GRAY, goodReps);
        }

        PoseLandmark shoulder = leftOk ? leftShoulder : rightShoulder;
        PoseLandmark elbow    = leftOk ? leftElbow    : rightElbow;
        PoseLandmark wrist    = leftOk ? leftWrist    : rightWrist;
        PoseLandmark hip      = leftOk ? leftHip      : rightHip;
        PoseLandmark ankle    = leftOk ? leftAnkle    : rightAnkle;

        float bodyAngle = smoothAngle(calculateAngle(
                shoulder.getPosition3D(),
                hip.getPosition3D(),
                ankle.getPosition3D()
        ));

        boolean bodyIsStraight = bodyAngle > PLANK_PERFECT_MIN;
        boolean armsSupportBody = isArmSupporting(shoulder, hip, ankle, elbow, wrist);

        if (bodyIsStraight && armsSupportBody) {
            if (!isInPlankPosition) {
                isInPlankPosition = true;
                plankSegmentStart = System.currentTimeMillis();
            }
            long elapsedSeconds = (plankAccumulatedMs + (System.currentTimeMillis() - plankSegmentStart)) / 1000;
            repCount = (int) elapsedSeconds;
            goodReps = repCount;
            return new AnalysisResult(repCount, "Perfect plank! Hold it! ⏱️", Color.GREEN, goodReps);
        }

        plankPauseTimer();

        if (!armsSupportBody) {
            return new AnalysisResult(repCount, "Get into plank! Raise your body ⬆️",
                    Color.RED, goodReps, context.getString(R.string.voice_tip_plank_not_in_position));
        }

        float shoulderY    = shoulder.getPosition3D().getY();
        float ankleY       = ankle.getPosition3D().getY();
        float shoulderX    = shoulder.getPosition3D().getX();
        float ankleX       = ankle.getPosition3D().getX();
        float hipX         = hip.getPosition3D().getX();
        float totalLen     = Math.abs(ankleX - shoulderX);
        float ratio        = totalLen > 0 ? Math.abs(hipX - shoulderX) / totalLen : 0.5f;
        float expectedHipY = shoulderY + (ankleY - shoulderY) * ratio;
        float hipY         = hip.getPosition3D().getY();

        String feedback;
        String voiceTip;
        if (hipY < expectedHipY - 20) {
            feedback = "Hips too high! Lower down ⬇️";
            voiceTip = context.getString(R.string.voice_tip_plank_hips_high);
        } else {
            feedback = "Hips too low! Lift up ⬆️";
            voiceTip = context.getString(R.string.voice_tip_plank_hips_low);
        }
        return new AnalysisResult(repCount, feedback, Color.RED, goodReps, voiceTip);
    }

    /**
     * Returns true when the arm support point (wrist, or elbow for forearm plank) is
     * meaningfully below the shoulder in the image - i.e. the body is elevated off the floor.
     *
     * Image Y increases downward: supportY > shoulderY means the support is physically lower.
     * We normalise by the horizontal body span so the check is scale-invariant.
     */
    private boolean isArmSupporting(PoseLandmark shoulder, PoseLandmark hip, PoseLandmark ankle,
                                    PoseLandmark elbow, PoseLandmark wrist) {
        float shoulderX = shoulder.getPosition3D().getX();
        float ankleX    = ankle.getPosition3D().getX();
        float bodySpanX = Math.abs(ankleX - shoulderX);
        if (bodySpanX < 30f) return false;

        float shoulderY = shoulder.getPosition3D().getY();

        float supportY;
        if (visible(wrist)) {
            supportY = wrist.getPosition3D().getY();
        } else if (visible(elbow)) {
            supportY = elbow.getPosition3D().getY();
        } else {
            return false;
        }

        float dropRatio = (supportY - shoulderY) / bodySpanX;
        return dropRatio > PLANK_ARM_DROP_RATIO;
    }

    private void plankPauseTimer() {
        if (isInPlankPosition) {
            plankAccumulatedMs += System.currentTimeMillis() - plankSegmentStart;
            isInPlankPosition = false;
        }
    }

    private boolean visible(PoseLandmark lm) {
        return lm != null && lm.getInFrameLikelihood() >= MIN_CONFIDENCE;
    }

    private float calculateAngle(PointF3D firstPoint, PointF3D midPoint, PointF3D lastPoint) {
        float angle = (float) Math.toDegrees(
                Math.atan2(lastPoint.getY()  - midPoint.getY(), lastPoint.getX()  - midPoint.getX()) -
                        Math.atan2(firstPoint.getY() - midPoint.getY(), firstPoint.getX() - midPoint.getX())
        );
        angle = Math.abs(angle);
        if (angle > 180) angle = 360 - angle;
        return angle;
    }

    private float smoothAngle(float newAngle) {
        if (lastAngle < 0f) {
            lastAngle = newAngle;
            return newAngle;
        }
        float smoothed = lastAngle * (1 - ANGLE_SMOOTHING) + newAngle * ANGLE_SMOOTHING;
        lastAngle = smoothed;
        return smoothed;
    }

    public int getRepCount() { return repCount; }
    public int getGoodReps() { return goodReps; }

    public static class AnalysisResult {
        public int repCount;
        public String feedback;
        public int qualityColor;
        public int goodReps;
        public String voiceTip;

        public AnalysisResult(int repCount, String feedback, int qualityColor, int goodReps) {
            this.repCount     = repCount;
            this.feedback     = feedback;
            this.qualityColor = qualityColor;
            this.goodReps     = goodReps;
            this.voiceTip     = null;
        }

        public AnalysisResult(int repCount, String feedback, int qualityColor, int goodReps, String voiceTip) {
            this(repCount, feedback, qualityColor, goodReps);
            this.voiceTip = voiceTip;
        }
    }
}
