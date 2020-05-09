package flandre.cn.novel;

import android.graphics.Paint;
import flandre.cn.novel.Tools.NovelConfigure;
import org.junit.Test;

import java.io.IOException;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {

    @Test
    public void addition_isCorrect() throws IOException {
        String addr = "https://www.ymxxs.com/book/144/144575/index.html";
        addr = addr.replace("/index.html", "");
        addr = "https://www.ymxxs.com/" + "text_" + addr.substring(addr.lastIndexOf("/") + 1) + ".html";
        System.out.println(addr);
    }

}
