package com.cs.muic.backend.SimpleObject;

import com.cs.muic.backend.SimpleObject.services.BucketService;
import com.cs.muic.backend.SimpleObject.services.MetadataService;
import com.cs.muic.backend.SimpleObject.services.ObjectService;
import com.mongodb.MongoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

@RestController
public class BucketController {

//    private final BucketRepository repository;
    private final BucketService bucketService;
    private final ObjectService objectService;
    private final MetadataService metadataService;

//    BucketController(BucketRepository repository){
//        this.repository = repository;
//        this.bucketService = new BucketService(repository);
//        this.objectService = new ObjectService(repository);
//        this.metadataService = new MetadataService(repository);
//    }

    @Autowired
    public BucketController(BucketService bucketService, ObjectService objectService, MetadataService metadataService){
        this.bucketService = bucketService;
        this.objectService = objectService;
        this.metadataService = metadataService;
    }

    boolean isBucketNameValid(String bucketName){
        return bucketName.matches("^[a-zA-Z0-9\\-_]+$");
    }

    boolean isObjectNameValid(String objectName){
        return objectName.matches("^([a-zA-Z0-9\\-_]+[.]*)+[a-zA-Z0-9\\-_]+$");
    }

    @PostMapping(value = "/{bucketName}", params = "create")
    ResponseEntity newBucket(@PathVariable String bucketName, HttpServletResponse response){
        if (!isBucketNameValid(bucketName)){
            return ResponseEntity.badRequest().build();
        }
        try{
            Bucket res = bucketService.createBucket(bucketName);
            if (res == null){
                return ResponseEntity.badRequest().build();
            }
            else{
                return ResponseEntity.ok(res);
            }
        }
        catch (DataAccessResourceFailureException | MongoException e){
            return ResponseEntity.badRequest().build();
        }
    }

//    @GetMapping("/buckets/{id}")
//    Bucket one(@PathVariable Long id){
//        return repository.findById(id).orElseThrow(()-> new BucketNotFoundException(id));
//    }

//    ======== for testing & checking ==============
//    @GetMapping("/buckets")
//    List<Bucket> all(){
//        return repository.findAll();
//    }
//    ===============================================

    @DeleteMapping(value = "/{bucketname}", params = "delete")
    ResponseEntity delete(@PathVariable String bucketname){
        if (bucketname == null || bucketname.isEmpty()){
            return ResponseEntity.badRequest().build();
        }
        else{
            try{
                int res_code = bucketService.delete(bucketname);
                if (res_code == 200){
                    return ResponseEntity.ok().build();
                }
                else{
                    return ResponseEntity.badRequest().build();
                }
            }
            catch (IOException e){
                return ResponseEntity.badRequest().build();
            }
        }
    }

    @GetMapping(value = "/{bucketname}", params="list")
    ResponseEntity getObjects(@PathVariable String bucketname){
        try{
            Bucket B = bucketService.find(bucketname);
            if (B == null || !isBucketNameValid(bucketname)){
                return ResponseEntity.badRequest().build();
            }
            return ResponseEntity.ok(B);
        }
        catch (DataAccessResourceFailureException | MongoException e){
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping(value = "/{bucketName}/{objectName}", params = "create")
    ResponseEntity createObject(@PathVariable String bucketName, @PathVariable String objectName){

        if (!isBucketNameValid(bucketName) || !isObjectNameValid(objectName)){
            return ResponseEntity.badRequest().build();
        }
        int res_code = objectService.createObject(bucketName, objectName);

        if (res_code == 200){
            return ResponseEntity.ok().build();
        }
        else{
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping(value = "/{bucketName}/{objectName}",params = "partNumber")
    ResponseEntity UploadAllParts(@PathVariable("bucketName") String bucketName,
                                 @PathVariable("objectName") String objectName,
                                 @RequestParam("partNumber") String partNumber,
                                 @RequestHeader("Content-Length") String contentLength,
                                 @RequestHeader("Content-MD5") String contentMD5,
                                 HttpServletRequest request){
        HashMap<String, String> json = new HashMap<>();
        long P;
        try {
            P = Long.parseLong(partNumber);
            if (P < 1 || P > 10000){
                json.put("partNumber", partNumber);
                json.put("error", "InvalidPartNumber");
                return ResponseEntity.badRequest().body(json);
            }
        }
        catch(Exception e){
            json.put("partNumber", partNumber);
            json.put("error", "InvalidPartNumber");
            return ResponseEntity.badRequest().body(json);
        }
        if (!isBucketNameValid(bucketName)){
            json.put("partNumber", partNumber);
            json.put("error", "InvalidBucket");
            return ResponseEntity.badRequest().body(json);
        }
        else if (!isObjectNameValid(objectName)){
            json.put("partNumber", partNumber);
            json.put("error", "InvalidObject");
            return ResponseEntity.badRequest().body(json);
        }
        try{
            json = objectService.upload(bucketName, objectName,
                    partNumber, contentLength, contentMD5, request, json);

            if (json.containsKey("error")){
                return ResponseEntity.badRequest().body(json);
            }
            else{
                return ResponseEntity.ok(json);
            }

        }catch (IOException e){
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping(value = "/{bucketName}/{objectName}",params = "complete")
    ResponseEntity completeMultiPartUpload(@PathVariable("bucketName") String bucketName,
                                            @PathVariable("objectName") String objectName){
        HashMap<String, String> json = new HashMap<>();
        json.put("name", objectName);

        if (!isObjectNameValid(objectName)){
            json.put("error", "InvalidObject");
            return ResponseEntity.badRequest().body(json);
        }

        try {
            json = objectService.complete(bucketName, objectName, json);
            if (json.containsKey("error")){
                return ResponseEntity.badRequest().body(json);
            }
            else{
                return ResponseEntity.ok(json);
            }
        }
        catch (NoSuchAlgorithmException | IOException e){
            json.put("error", "NoSuchAlgorithmException|IOException");
        }
        return ResponseEntity.badRequest().body(json);
    }

    @DeleteMapping(value = "/{bucketName}/{objectName}",params = "partNumber")
    ResponseEntity deletePart(@PathVariable("bucketName") String bucketName,
                                    @PathVariable("objectName") String objectName,
                                    @RequestParam("partNumber") String partNumber){
        boolean isDeleted = objectService.deletePart(bucketName, objectName, partNumber);
        if (isDeleted){
            return ResponseEntity.ok().build();
        }
        else {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping(value = "/{bucketName}/{objectName}",params = "delete")
    ResponseEntity deleteObject(@PathVariable("bucketName") String bucketName,
                                       @PathVariable("objectName") String objectName){

        if (!isBucketNameValid(bucketName) || !isObjectNameValid(objectName)){
            return ResponseEntity.badRequest().build();
        }

        boolean isDeleted = objectService.deleteObject(bucketName, objectName);
        if (isDeleted){
            return ResponseEntity.ok().build();
        }
        else{
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping(value = "/{bucketName}/{objectName}")
    ResponseEntity downloadObject(@PathVariable("bucketName") String bucketName,
                        @PathVariable("objectName") String objectName,
                        HttpServletRequest request,
                        HttpServletResponse response){
        if (!isBucketNameValid(bucketName) || !isObjectNameValid(objectName)){
            return ResponseEntity.badRequest().build();
        }
        try{
            boolean isDownloaded = objectService.download(bucketName, objectName, request, response);
            if (isDownloaded){
                return ResponseEntity.ok().build();
            }
            else {
                return ResponseEntity.badRequest().build();
            }
        }
        catch (IOException e){
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping(value = "/{bucketName}/{objectName}", params = "metadata")
    ResponseEntity updateObjectMetadata(@PathVariable("bucketName") String bucketName,
                                      @PathVariable("objectName") String objectName,
                                      @RequestParam(value = "key") String key,
                                      @RequestBody String value){
        if (!isBucketNameValid(bucketName) || !isObjectNameValid(objectName)){
            return ResponseEntity.notFound().build();
        }
        boolean isUpdated = metadataService.update(bucketName, objectName, key, value);
        if (isUpdated){
            return ResponseEntity.ok().build();
        }
        else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping(value = "/{bucketName}/{objectName}", params = "metadata")
    ResponseEntity removeObjectMetadata(@PathVariable("bucketName") String bucketName,
                                      @PathVariable("objectName") String objectName,
                                      @RequestParam(value = "key") String key){
        if (!isBucketNameValid(bucketName) || !isObjectNameValid(objectName)){
            return ResponseEntity.notFound().build();
        }
        boolean isRemoved = metadataService.remove(bucketName, objectName, key);
        if(isRemoved){
            return ResponseEntity.ok().build();
        }
        else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping(value = "/{bucketName}/{objectName}", params = "metadata")
    ResponseEntity getObjectMetadata(@PathVariable("bucketName") String bucketName,
                                     @PathVariable("objectName") String objectName,
                                     @RequestParam(value = "key", required = false) String key){
        if (!isBucketNameValid(bucketName) || !isObjectNameValid(objectName)){
            return ResponseEntity.notFound().build();
        }
        HashMap<String, String> output = metadataService.getMeta(bucketName, objectName, key);
        if (output == null){
            return ResponseEntity.notFound().build();
        }
        else {
            return ResponseEntity.ok(output);
        }
    }

}
