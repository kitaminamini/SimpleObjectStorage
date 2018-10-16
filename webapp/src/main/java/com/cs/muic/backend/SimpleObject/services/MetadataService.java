package com.cs.muic.backend.SimpleObject.services;

import com.cs.muic.backend.SimpleObject.Bucket;
import com.cs.muic.backend.SimpleObject.BucketRepository;
import com.cs.muic.backend.SimpleObject.Content;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
public class MetadataService {

    private BucketRepository repository;

    @Autowired
    public MetadataService(BucketRepository repository){
        this.repository = repository;
    }

    public boolean update(String bucketName, String objectName, String key, String value){
        Bucket bucket = repository.findBucketByName(bucketName);
        if (bucket == null){
            return false;
        }
        String objectKey = objectName.replace('.', '/');
        if (!bucket.objects.containsKey(objectKey)){
            return false;
        }
        Content object = bucket.objects.get(objectKey);

        if (key == null || key.isEmpty()){
            return true;
        }

        object.metaData.put(key, value);
        object.setModified();
        bucket.objects.put(objectKey, object);
        bucket.setModified();
        repository.save(bucket);
        return true;
    }

    public boolean remove(String bucketName, String objectName, String key){
        Bucket bucket = repository.findBucketByName(bucketName);
        if (bucket == null){
            return false;
        }
        String objectKey = objectName.replace('.', '/');
        Content object = bucket.objects.get(objectKey);
        if (object == null){
            return false;
        }

        if (key == null || key.isEmpty()){
            return true;
        }

        object.metaData.remove(key);
        object.setModified();
        bucket.objects.put(objectKey, object);
        bucket.setModified();
        repository.save(bucket);

        return true;
    }

    public HashMap<String, String> getMeta(String bucketName, String objectName, String key){
        Bucket bucket = repository.findBucketByName(bucketName);
        if (bucket == null){
            return null;
        }
        String objectKey = objectName.replace('.', '/');
        Content object = bucket.objects.get(objectKey);
        if (object == null){
            return null;
        }

        HashMap<String, String> json = new HashMap<>();

        if (key == null){

            return object.metaData;
        }

        if (key.isEmpty() || !object.metaData.containsKey(key)){
            return json;
        }

        json.put(key, object.metaData.get(key));
        return json;
    }



}
