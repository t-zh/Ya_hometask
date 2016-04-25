package ru.tzh.http;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.Display;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import ru.tzh.http.Singers;

public class DetailsActivity extends AppCompatActivity {

    String error;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);
        // Создаем экземпляр класса Singers
        Singers singer = new Singers();
        //Получаем из интента строку соответсвующую JSON объекту
        String sJSONObject = getIntent().getStringExtra("Singer_Details");
        try {
            singer = getJSONObject(sJSONObject);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //меняем название Activity
        this.setTitle(singer.name);
        //Заполняем Views значениями из элемента класса Singer
        ImageView imageView=(ImageView)findViewById(R.id.imageView2);
        TextView genresView = (TextView)findViewById(R.id.textView5);
        TextView albumsView = (TextView)findViewById(R.id.textView6);
        TextView descriptionView = (TextView)findViewById(R.id.textView8);

        imageView.setImageBitmap(singer.image);
        genresView.setText(singer.genres);
        albumsView.setText("альбомов "+ singer.albums+ " . " + "трэков " + singer.tracks);
        descriptionView.setText(singer.description);

    }

    // ***********************************************************
    // Парсер JSON объекта
    Singers getJSONObject(String content){

        Bitmap largeImage=null;
        Singers singer=new Singers();
        try {

            JSONObject jsonObject =  new JSONObject(content);
            JSONArray genresArray = jsonObject.getJSONArray("genres");
            String genres = "";
            //Получаем значение поля жанры (genres) из массива JSON
            for (int j = 0; j < genresArray.length(); j++) {
                genres += genresArray.getString(j);
                if (j!= genresArray.length()-1 ) {genres +=", "; }
            }
            // Запускаем AsyncTask на загрузку изображения (вызов метода getImage)
            DownloadImageAsyncTask getImage = new DownloadImageAsyncTask();
            // Ссылка на загружаемую картинку
            String s =jsonObject.getJSONObject("cover").getString("big").toString();
            getImage.execute(s);

            largeImage = getImage.get();
            // Заполняем экземпляр класса Singers, для вывода на экран
            singer = new Singers(largeImage, jsonObject.getString("name"), genres, jsonObject.getInt("albums"), jsonObject.getInt("tracks"), jsonObject.getString("description"));
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            error = e.getMessage();
        } catch (JSONException e) {
            error=e.getMessage();
        }
        return singer;
    }

    // **********************************************************************
    // Загрузка изображения
    class DownloadImageAsyncTask extends AsyncTask<String, Void, Bitmap> {
        public DownloadImageAsyncTask() {
            super();
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
        }

        @Override
        protected Bitmap doInBackground(String  ... path) {
            Bitmap bitmap=null;
            //устанавливаем Http соединение
            HttpURLConnection httpConnect;
            URL url;
            try {
                url = new URL(path[0]);
                httpConnect= (HttpURLConnection) url.openConnection();
                InputStream inStream = new BufferedInputStream(httpConnect.getInputStream());
                // настраиваем опции BitmapFactory,оперделяем оптимальные размеры картинки
                final BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                // Определяем размеры экрана
                Display display = getWindowManager().getDefaultDisplay();
                DisplayMetrics metrics = new DisplayMetrics();
                display.getMetrics(metrics);
                float heightScreen = metrics.heightPixels;
                int widthScreen = metrics.widthPixels;
                // Размеры изображения:
                float w = options.outWidth;
                float h = options.outHeight;

              //  final BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = false;
                options.inSampleSize=Math.round(w/widthScreen);
                // Получаем изображение
                bitmap = BitmapFactory.decodeStream(inStream, null, options);

                inStream.close();
                httpConnect.disconnect();
                return bitmap;

            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }

        }

    }
}
