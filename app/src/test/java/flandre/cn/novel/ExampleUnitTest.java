package flandre.cn.novel;

import android.net.Uri;
import android.service.autofill.FieldClassification;
import android.speech.tts.TextToSpeech;
import org.jsoup.Connection;
import org.junit.Test;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {

    @Test
    public void addition_isCorrect() throws IOException {
        List<Long> longs = new ArrayList<>();
        longs.add(123l);
        longs.add(122l);
        longs.add(112l);
        longs.add(152l);
        Collections.shuffle(longs);
        System.out.println(longs);
    }

}
