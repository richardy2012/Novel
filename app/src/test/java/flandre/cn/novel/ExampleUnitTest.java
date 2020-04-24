package flandre.cn.novel;

import android.net.Uri;
import android.service.autofill.FieldClassification;
import android.speech.tts.TextToSpeech;
import org.jsoup.Connection;
import org.junit.Test;

import java.io.IOException;
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
    public void addition_isCorrect() throws IOException {
        URL url = new URL("https://www.kutun.net/book/122615/");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setDoOutput(false);
        connection.connect();
        connection.getInputStream();
    }

}
