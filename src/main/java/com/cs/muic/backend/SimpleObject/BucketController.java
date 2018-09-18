package com.cs.muic.backend.SimpleObject;


import com.mongodb.operation.BatchCursor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController

public class BucketController {

    private final BucketRepository repository;

    BucketController(BucketRepository repository){
        this.repository = repository;
    }

    @PostMapping(value = "/{bucketName}", params = "create")
    Bucket newBucket(@PathVariable String bucketName){
        Bucket bucket = new Bucket(bucketName);
//        bucket.setCreated(bucket.getId().getTimestamp());
//        bucket.setModified(bucket.getId().getTimestamp());
        return repository.save(bucket);
    }

    @GetMapping("/buckets/{id}")
    Bucket one(@PathVariable Long id){
        return repository.findById(id).orElseThrow(()-> new BucketNotFoundException(id));
    }

    @GetMapping("/buckets")
    List<Bucket> all(){
        return repository.findAll();
    }

    @DeleteMapping(value = "/{bucketname}", params = "delete")
    void delete(@PathVariable String bucketname){
        Bucket B = repository.findBucketByName(bucketname);
        repository.delete(B);
    }

    @GetMapping(value = "/{bucketname}", params="list")
    Bucket getObjects(@PathVariable String bucketname){
        Bucket B = repository.findBucketByName(bucketname);
        return B;
    }

    @PostMapping(value = "/{bucketName}/{objectName}", params = "create")
    void createObject(@PathVariable String bucketName, @PathVariable String objectName){
        Bucket B = repository.findBucketByName(bucketName);
        List<Content> objects = B.getObjects();

        if (!contains(objects, objectName)){
            Content newObj = new Content(objectName);
            objects.add(newObj);
            //TODO: Add object

            B.setObjects(objects);
        }
        repository.save(B);
    }

    public boolean contains(List<Content> objs, String objectName){
        if (objs.isEmpty()){ return false; }
        for (Content obj : objs){
            if (obj.getName().equals(objectName)){
                return true;
            }
        }
        return false;
    }

    @PutMapping(value = "/{bucketName}/{objectName}",params = "partNumber")
    public ResponseEntity UploadAllParts(@PathVariable("bucketName") String bucketName,
                                         @PathVariable("objectName") String objectName,
                                         @RequestParam("partNumber") String partNumber,
                                         @RequestHeader("Content-Length") String contentLength,
                                         @RequestHeader("Content-MD5") String contentMD5){


        return null;
    }

    @PostMapping(value = "/{bucketName}/{objectName}",params = "complete")
    public ResponseEntity completeMultiPartUpload(@PathVariable("bucketName") String bucketName,
                                                @PathVariable("objectName") String objectName,
                                                @RequestHeader("Content-Length") String contentLength,
                                                @RequestHeader("Content-MD5") String contentMD5){
        return null;
    }

    @DeleteMapping(value = "/{bucketName}/{objectName}",params = "partNumber")
    public ResponseEntity deletePart(@PathVariable("bucketName") String bucketName,
                                    @PathVariable("objectName") String objectName,
                                    @RequestParam("partNumber") String partNumber){
        return null;
    }

    @DeleteMapping(value = "/{bucketName}/{objectName}",params = "delete")
    public ResponseEntity deleteObject(@PathVariable("bucketName") String bucketName,
                                       @PathVariable("objectName") String objectName){
        return null;
    }

    @GetMapping("/{bucketName}/{objectName}")
    void downloadObject(@PathVariable String bucketName, @PathVariable String objectName){}

    @PutMapping("/{bucketName}/{objectName}?metadata&key={key}")
    void updateObjectMetadata(){}

    @DeleteMapping("/{bucketName}/{objectName}?metadata&key={key}")
    void removeObjectMetadata(){}

    @GetMapping("/{bucketName}/{objectName}?metadata&key={key}")
    void getObjectMetadata(){}

    @GetMapping(value = "/{bucketName}/{objectName}",params = "metadata")
    void getAllMetadata(){}




}
