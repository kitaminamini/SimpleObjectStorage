package com.cs.muic.backend.SimpleObject;

import jdk.internal.dynalink.linker.LinkerServices;
import jdk.nashorn.internal.objects.annotations.Getter;
import jdk.nashorn.internal.objects.annotations.Setter;
import lombok.Data;
import org.bson.types.ObjectId;

import java.util.List;

@Data
public class Bucket {
    private ObjectId id;
    private String name;
    private String created;
    private String modified;
    private List<String> contents;

    public Bucket(String name){
        this.name = name;
    }

//    public String getName() {
//        return name;
//    }
//
//    public long getId() {
//        return id;
//    }
//
//    public String getCreated() {
//        return created;
//    }
//
//    public String getModified() {
//        return modified;
//    }
//
//    public void setId(long id) {
//        this.id = id;
//    }
//
//    public void setName(String name) {
//        this.name = name;
//    }
//
//    public void setCreated(String created) {
//        this.created = created;
//    }
//
//    public void setModified(String modified) {
//        this.modified = modified;
//    }

    public List<String> getContents() {
        return contents;
    }

    public void setContents(List<String> contents) {
        this.contents = contents;
    }
}
