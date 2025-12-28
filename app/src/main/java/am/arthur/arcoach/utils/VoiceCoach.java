package am.arthur.arcoach.utils;

import android.content.Context;
import android.os.Build;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

import am.arthur.arcoach.R;

public class VoiceCoach {
    private final Context context;
    private TextToSpeech tts;
    private float volume = 1.0f;
    private boolean isReady = false;
    private int lastRepCount = 0;

    public VoiceCoach(Context context) {
        this.context = context;
        tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    Locale locale = Locale.getDefault();
                    int result = tts.setLanguage(locale);

                    if (result == TextToSpeech.LANG_MISSING_DATA ||
                            result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        tts.setLanguage(Locale.ENGLISH);
                    }

                    isReady = true;
                }
            }
        });
    }

    public void setVolume(float volume) {
        this.volume = Math.max(0.0f, Math.min(1.0f, volume));
    }

    public void announceRep(int repCount) {
        if (!isReady || repCount == lastRepCount) return;

        lastRepCount = repCount;

        // Каждые 5 повторений - мотивация
        if (repCount % 5 == 0) {
            String[] motivations = {
                    context.getString(R.string.voice_motivation_1),
                    context.getString(R.string.voice_motivation_2),
                    context.getString(R.string.voice_motivation_3),
                    context.getString(R.string.voice_motivation_4)
            };
            int random = (int) (Math.random() * motivations.length);
            speak(motivations[random]);
        } else {
            speak(String.valueOf(repCount));
        }
    }

    public void speak(String text) {
        if (!isReady || tts == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        } else {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    public boolean isSpeaking() {
        return tts != null && tts.isSpeaking();
    }

    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
}
