package com.cs.muic.backend.SimpleObject;

import org.bson.types.ObjectId;

public class Content {
    private ObjectId id;
    private String name;

    private int created;
    private int modified;
    public Content(String name){
        this.name = name;
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

}
