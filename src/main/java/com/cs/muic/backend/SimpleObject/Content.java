package com.cs.muic.backend.SimpleObject;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;

import java.io.File;
import java.time.Instant;
import java.util.HashMap;

public class Content {
//    private ObjectId id;
    @Id
    private String name;

    private long created;
    private long modified;
    private boolean complete;
    private long contentLength;
    private String eTag;
    public HashMap<String, byte[]> partMD5s;
    public HashMap<String, String> metaData;

    public Content(String name){
        this.name = name;
        this.complete = false;
        Long now = Instant.now().toEpochMilli();
        this.created = now;
        this.modified = now;
        this.partMD5s = new HashMap<>();
        this.metaData = new HashMap<>();
    }

    public String getName() {
        return name;
    }

//    public ObjectId getId() {
//        return id;
//    }

    public long getCreated() {
        return created;
    }

    public long getModified() {
        return modified;
    }

    public boolean getComplete(){return complete; }

    public long getContentLength() {
        return contentLength;
    }

    public String geteTag() {
        return eTag;
    }

    //    public void setId(long id) {
//        this.id = id;
//    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public void setModified() {
        this.modified = Instant.now().toEpochMilli();
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }

    public void seteTag(String eTag) {
        this.eTag = eTag;
    }
}
