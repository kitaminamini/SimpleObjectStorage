package com.cs.muic.backend.SimpleObject;


import com.mongodb.operation.BatchCursor;
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
        return repository.save(new Bucket(bucketName));
    }

    @GetMapping("/buckets/{id}")
    Bucket one(@PathVariable Long id){
        return repository.findById(id).orElseThrow(()-> new BucketNotFoundException(id));
    }

    @GetMapping("/buckets")
    List<Bucket> all(){
        return repository.findAll();
    }

    @DeleteMapping("/{bucketname}?delete")
    void delete(@PathVariable String bucketname, Long id){
        Bucket B = repository.findById(id).orElseThrow(()-> new BucketNotFoundException(id));
        repository.delete(B);
    }

    @GetMapping("/{bucketname}?list")
    List<String> getObjects(@PathVariable String bucketname, Long id){
        Bucket B = repository.findById(id).orElseThrow(() -> new BucketNotFoundException(id));
        return B.getContents();
    }

    @PostMapping("/{bucketName}/{objectName}?create")
    void createObject(@PathVariable String bucketName, @PathVariable String objectName, Long id){
        Bucket B = repository.findById(id).orElseThrow(() -> new BucketNotFoundException(id));
        List<String> objects = B.getContents();
        if (!objects.contains(objectName)){
            objects.add(objectName);
            //TODO: Add object

            B.setContents(objects);
        }
    }

    @PutMapping("/{bucketName}/{objectName}?partNumber=1 Content-Length: {partSize:required} Content-MD5: {partMd5:required}")
    void UploadAllParts(){}

    @PostMapping("/{bucketName}/{objectName}?complete Content-Length: {totalLength:required} Content-MD5: {eTag:required}")
    void completeMultiPartUpload(){}

    @DeleteMapping("/{bucketName}/{objectName}?partNumber=1")
    void deletePart(){}

    @DeleteMapping("/{bucketName}/{objectName}?delete")
    void deleteObject(){}

    @GetMapping("/{bucketName}/{objectName}")
    void downloadObject(@PathVariable String bucketName, @PathVariable String objectName){}

    @PutMapping("/{bucketName}/{objectName}?metadata&key={key}")
    void updateObjectMetadata(){}

    @DeleteMapping("/{bucketName}/{objectName}?metadata&key={key}")
    void removeObjectMetadata(){}

    @GetMapping("/{bucketName}/{objectName}?metadata&key={key}")
    void getObjectMetadata(){}

    @GetMapping("/{bucketName}/{objectName}?metadata")
    void getAllMetadata(){}




}
