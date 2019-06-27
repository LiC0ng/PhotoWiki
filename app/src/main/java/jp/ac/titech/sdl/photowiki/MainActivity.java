package jp.ac.titech.sdl.photowiki;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import jp.ac.titech.sdl.photowiki.db.Page;
import jp.ac.titech.sdl.photowiki.db.Root;

public class MainActivity extends AppCompatActivity {

    private TextView wikiContent;
    private Button getWiki;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        wikiContent = findViewById(R.id.wikiContent);
        getWiki = findViewById(R.id.getWiki);

        getWiki.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onButtonGet();
            }
        });
    }

    public void onButtonGet() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("https://en.wikipedia.org/w/api.php?format=json&action=query&prop=extracts&exintro&explaintext&redirects=1&titles=Stack%20Overflow");
                    // 処理開始時刻
                    HttpURLConnection con = (HttpURLConnection)url.openConnection();
                    final String str = InputStreamToString(con.getInputStream());
                    // 処理終了時刻
                    Log.d("HTTP", str);
                    Root root = new Gson().fromJson(str, Root.class);
                    for (final Page page : root.getQuery().getPages().values()) {
                        System.out.println(page.getTitle());
                        System.out.println("  " + page.getExtract());
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                wikiContent.setText(page.getExtract());
                            }
                        });
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }).start();
    }

    // InputStream -> String
    static String InputStreamToString(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();
        return sb.toString();
    }
}
