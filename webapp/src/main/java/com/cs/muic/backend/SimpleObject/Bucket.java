package com.cs.muic.backend.SimpleObject;

import jdk.internal.dynalink.linker.LinkerServices;
import jdk.nashorn.internal.objects.annotations.Getter;
import jdk.nashorn.internal.objects.annotations.Setter;
import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Data
public class Bucket {
//    private ObjectId id;
    @Id
    private String name;

    private long created;
    private long modified;
    public HashMap<String, Content> objects;

    public Bucket(String name){
        this.name = name;
        this.objects = new HashMap<>();
        Long now = Instant.now().toEpochMilli();
        this.created = now;
        this.modified = now;
//        this.created = this.id.getTimestamp();
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

//    public void setId(long id) {
//        this.id = id;
//    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCreated(int created) {
        this.created = created;
    }

    public void setModified() {
        this.modified = Instant.now().toEpochMilli();
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
