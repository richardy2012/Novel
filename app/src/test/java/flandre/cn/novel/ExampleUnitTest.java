package flandre.cn.novel;

import android.service.autofill.FieldClassification;
import android.speech.tts.TextToSpeech;
import org.junit.Test;

import java.net.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {

    @Test
    public void addition_isCorrect() {
        Pattern pattern = Pattern.compile("《(.*?)》");
        Matcher matcher = pattern.matcher("《帝霸》全集下载");
        matcher.find();
        System.out.println(matcher.groupCount());
        System.out.println(matcher.group(0));
        System.out.println(matcher.group(1));
    }

}
