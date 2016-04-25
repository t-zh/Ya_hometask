package ru.tzh.http;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.DownloadListener;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import ru.tzh.http.Singers;
import ru.tzh.http.DetailsActivity;

import static android.widget.Toast.*;


public class MainActivity extends AppCompatActivity {
    String contentText; //строка содержит текст из файла JSON
    private Integer lengthOfList;   // Количество исполнителей
    // массив содержит объекты класса Singers и предназначен для вывода на экран через адаптер
    private static ArrayList<Singers> singersListToAdapter;
    // массив содержит объекты класса Singers и предназначен для загрузки данных об исполнителях
    private static ArrayList<Singers> loadingSingers;
    ArrayAdapter<Singers> adapter;
    //Объект расширенного класса типа AsynckTask, предназначен для загрузки данных об исполнителях в массив  loadingSingers
    ParserInAsyncTask getParser;
    ProgressBar progressBar;

    @Override
    public void setFinishOnTouchOutside(boolean finish) {
        super.setFinishOnTouchOutside(finish);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView listView = (ListView)findViewById(R.id.listView);

        singersListToAdapter = new ArrayList<Singers>();
        singersListToAdapter.clear();
        loadingSingers = new ArrayList<Singers>();
        loadingSingers.clear();
        lengthOfList = 0;

        //Создаем адаптер и привязываем массив через адаптер к ListView
        adapter=new newAdapter(this,singersListToAdapter);
        if (listView != null)    {
           listView.setAdapter(adapter);
        }
        progressBar = (ProgressBar)findViewById(R.id.progressBar);
        progressBar.setVisibility(ProgressBar.INVISIBLE);
        // Запуск AcyncTask - загрузка контента  JSON, и сохраняем в строку contentText
        DownloadAsyncTask myAcyncTask =  new DownloadAsyncTask();
        myAcyncTask.execute();
        try {
            contentText = myAcyncTask.get();
        }
        catch (Exception e)        {
            contentText = e.getMessage();
        }

        // Запускаем парсер, в качестве данных передается файл JSON,
        // в классе ParserInAsyncTask осуществляется парсер JSON-данных, загрузка изображений,
        // и формирование массива элеметов класса Singers, в котором содержатся данные об исполнителях
        getParser = new ParserInAsyncTask();
        getParser.execute(contentText);

        //Обработка нажатий на элементы ListView
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //Приостанавливаем задачу getParser (класс ParserInAsyncTask), в которой осуществляется загрузка
                //и парсинг информации об исполнителях
                if (getParser.isCancelled()== false) getParser.cancel(true);

                //Направляем интент в активити DetailsActivity и запускаем ее
                Intent intent = new Intent(MainActivity.this, DetailsActivity.class);
                try {
                    intent.putExtra("Singer_Details", getJSONObject(contentText, position));
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });

        // Устанавливаем OnScrollListener, если была прокруткаэкрана  и количество загруженных элементов
        // меньше общего числа элементовобновляем адаптер
       listView.setOnScrollListener(new AbsListView.OnScrollListener() {
           @Override
           public void onScrollStateChanged(AbsListView view, int scrollState) {
               if (scrollState==0){
                   if (loadingSingers.size() <= lengthOfList) {
                           singersListToAdapter.clear();
                           singersListToAdapter.addAll(loadingSingers);
                           adapter.notifyDataSetChanged();
                   }
               }
           }
           @Override
           public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
           }
       });

    }
    // Метод предназначен для получения объекта JSON из файла JSON, для передачи в DetailsActivity,
    // при нажатии на элемент ListView(вызывается в onItemClick)
    String getJSONObject(String content, int position)throws Exception{
        JSONArray array = new JSONArray(content);
        JSONObject jsonObject =  array.getJSONObject(position);
        return jsonObject.toString();

    }

    //В случае если не все данные загружены создаем заново задачу getParser (класс ParserInAsyncTask),
    // в которой осуществляется загрузка и парсинг информации об исполнителях
    @Override
    protected void onRestart() {
        super.onRestart();
        if (loadingSingers.size() < lengthOfList) {
            getParser = new ParserInAsyncTask();
            getParser.execute(contentText);
        }
    }



//*********************************************************************************
    //Переопределение адаптера
    private class newAdapter extends ArrayAdapter<Singers> {

        ArrayList<Singers> list;

        public newAdapter (Context context, ArrayList<Singers> singers) {
            super(context, R.layout.list_view_item, singers);
            this.list=singers;
        }
    //Создаем класс ViewHolder
    class ViewHolder {
        public ImageView imageView;
        public TextView nameView, genresView, albumsView;
    }

    @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            Singers singer = getItem(position);

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_view_item, null);
                holder = new ViewHolder();
                holder.imageView = ((ImageView) convertView.findViewById(R.id.imageView));
                holder.nameView =((TextView) convertView.findViewById(R.id.textView));
                holder.genresView = ((TextView) convertView.findViewById(R.id.textView2));
                holder.albumsView = ((TextView) convertView.findViewById(R.id.textView3));
                convertView.setTag(holder);
            }else
            {   holder = (ViewHolder) convertView.getTag();}

            holder.imageView.setImageBitmap(singer.image);
            holder.nameView.setText(singer.name);
            holder.genresView.setText(singer.genres);
            holder.albumsView.setText("альбомов "+singer.albums+", "+"песен " + singer.tracks);

            return convertView;
        }


        @Override
        public int getCount() {
            return super.getCount();
        }

        @Override
        public long getItemId(int position) {
            return super.getItemId(position);
        }

        @Override
        public Singers getItem(int position) {
            return super.getItem(position);
        }

    }
//*********************************************************************************
// Переопределяем клас AsyncTask для загрузки данных JSON
    class DownloadAsyncTask extends AsyncTask<String, Void, String> {
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);

        public DownloadAsyncTask() {
            super();
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
         //   makeText(getApplicationContext(), "загрузка начата", LENGTH_SHORT).show();
            progressBar.setVisibility(ProgressBar.VISIBLE);
        }
        @Override
        protected String doInBackground(String... path) {
            String content;
            try  {
                content = getContent("http://cache-default01d.cdn.yandex.net/download.cdn.yandex.net/mobilization-2016/artists.json");
            }
            catch (IOException e) {
                content = e.getMessage();
            }
            return content;
        }

        @Override
        protected void onPostExecute(String content) {
            super.onPostExecute(content);
            progressBar.setVisibility(ProgressBar.INVISIBLE);
        }
    }

    //*******************************************************************************************
    // Метод устанавливает соединение и загружает данные JSON
    private String getContent(String path) throws IOException {
        BufferedReader reader = null;
        try {
            URL url = new URL(path);
            HttpURLConnection httpConnect = (HttpURLConnection) url.openConnection();
            httpConnect.setRequestMethod("GET");
            httpConnect.setReadTimeout(10000);
            httpConnect.connect();
            reader = new BufferedReader(new InputStreamReader(httpConnect.getInputStream()));
            StringBuilder buf = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                buf.append(line + "\n");
            }
            httpConnect.disconnect();
            return (buf.toString());
        }
        finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    //***********************************************************************************
    //  Переопределяем  класс AsyncTask, в методе doInBackground которого запускаем Парсер JSON, загружаем картинку
    //  при загрузку 20 элементов списка шлем адаптеру информацию об обновлении данных (метод onProgressUpdate) и выводим на экран
    //  в результате возращем массив объектов класса Singers, в котором содержатся данные об исполнителях
    class ParserInAsyncTask extends AsyncTask <String, ArrayList<Singers>, ArrayList<Singers>> {
        public ParserInAsyncTask() {
            super();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            }

        @Override
        protected ArrayList<Singers> doInBackground(String... content) {
            Bitmap bitmap = null;
            String genres;

            try {
                JSONArray array = new JSONArray(content[0]);
                lengthOfList = array.length();
                // Запускаем цикл по всем объектам JSON, т.е. по всем исполнителям
                for (int i = loadingSingers.size(); i < array.length(); i++) {
                    JSONObject object = array.getJSONObject(i);
                    //Выгрузка данных о жанрах
                    JSONArray genresArray = object.getJSONArray("genres");
                    genres = "";
                    for (int j = 0; j < genresArray.length(); j++) {
                        genres += genresArray.getString(j);
                        if (j != genresArray.length() - 1) {
                            genres += ",";
                        }
                    }

                    // Запускаем  на загрузку изображения
                    String s = object.getJSONObject("cover").getString("small").toString();
                    bitmap = getImage(s);

                    //в массив loadingSingers элементов класса Singers добавляем информацию о новом исполнителе
                    loadingSingers.add(new Singers(bitmap, object.getString("name"), genres, object.getInt("albums"), object.getInt("tracks")));

                    // Когда загрузится инфомрацию о 20 первых исполнителях, шлем массив в ProgressUpdate, чтобы передать в адаптер
                    if (loadingSingers.size()==20)
                        publishProgress(loadingSingers);

                    if (isCancelled()){ return null; }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return loadingSingers;
        }

        @Override
        protected void onPostExecute(ArrayList<Singers> loadingSingers) {
            super.onPostExecute(loadingSingers);
        }

        // В методе onProgressUpdate обновляем адаптер, загружаем информацию об исполнителях
        @Override
        protected void onProgressUpdate( ArrayList<Singers>... singers) {
            super.onProgressUpdate(singers[0]);
            singersListToAdapter.clear();
            singersListToAdapter.addAll(singers[0]);

            progressBar.setVisibility(ProgressBar.INVISIBLE);
            adapter.notifyDataSetChanged();
       }
    }

//***********************************************************************************
    //Метод устанавливает соединение и загружает изображение
    protected Bitmap getImage (String  ... path) {
        Bitmap bitmap=null;
        HttpURLConnection httpConnect;
        URL url;
        try {
            url = new URL(path[0]);
            httpConnect= (HttpURLConnection) url.openConnection();
            InputStream inStream = new BufferedInputStream(httpConnect.getInputStream());
            // настраиваем опции BitmapFactory, сжимаем картинку в два раза
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize=2;
            bitmap = BitmapFactory.decodeStream(inStream, null, options);
            inStream.close();
            httpConnect.disconnect();
            } catch (IOException e) {
                e.getMessage();
            }
        return bitmap;
    }
}

