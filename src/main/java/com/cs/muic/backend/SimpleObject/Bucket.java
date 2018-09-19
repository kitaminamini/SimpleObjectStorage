package com.cs.muic.backend.SimpleObject;

import jdk.internal.dynalink.linker.LinkerServices;
import jdk.nashorn.internal.objects.annotations.Getter;
import jdk.nashorn.internal.objects.annotations.Setter;
import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Data
public class Bucket {
    private ObjectId id;
    private String name;

    private int created;
    private int modified;
    public HashMap<String, Content> objects;

    public Bucket(String name){
        this.name = name;
        this.objects = new HashMap<>();
//        this.created = this.id.getTimestamp();
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

//    public HashMap<String, Content> getObjects() {
//        return objects;
//    }
//
//    public void setObjects(List<Content> objects) {
//        this.objects = objects;
//    }

//    public Content getObjectByName(String objectName){
//        for (Content c: objects){
//            if (c.getName().equals(objectName)){
//                return c;
//            }
//        }
//        return null;
//    }
}
