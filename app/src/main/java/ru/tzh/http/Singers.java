package ru.tzh.http;

import android.graphics.Bitmap;

import java.util.ArrayList;

/**
 * Created by 1 on 16.04.16.
 */
public class Singers extends ArrayList{
    Integer length;
//    public Integer id;
    public Bitmap image;
    public String name;
    public String genres;
    public Integer tracks;
    public Integer albums;
    String description;

    public Singers (){
        this.image =  null;
        this.name = null;
        this.genres = null;
        this.albums = null;
        this.tracks = null;
        this.description = null;
    }
    public Singers (Bitmap image, String name, String genres, Integer albums, Integer tracks){
        this.image =  image;
     //   this.id = id;
        this.name = name;
        this.genres = genres;
        this.albums = albums;
        this.tracks = tracks;
        this.description = null;
    }
    public Singers (Bitmap image, String name, String genres, Integer albums, Integer tracks, String description){
        this.image =  image;
     //   this.id = id;
        this.name = name;
        this.genres = genres;
        this.albums = albums;
        this.tracks = tracks;
        this.description = description;
    }

}
