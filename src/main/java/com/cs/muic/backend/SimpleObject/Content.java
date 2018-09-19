package com.cs.muic.backend.SimpleObject;

import org.bson.types.ObjectId;

import java.io.File;

public class Content {
    private ObjectId id;
    private String name;
    private int created;
    private int modified;
    private boolean complete;
    private long contentLength;

    public Content(String name){
        this.name = name;
        this.complete = false;
    }

    public String getName() {
        return name;
    }

    public ObjectId getId() {
        return id;
    }

    public int getCreated() {
        return created;
    }

    public int getModified() {
        return modified;
    }

    public boolean getComplete(){return complete; }

    public long getContentLength() {
        return contentLength;
    }

    //    public void setId(long id) {
//        this.id = id;
//    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCreated(int created) {
        this.created = created;
    }

    public void setModified(int modified) {
        this.modified = modified;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }
}
