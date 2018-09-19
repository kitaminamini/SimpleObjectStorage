package com.cs.muic.backend.SimpleObject;


import com.mongodb.operation.BatchCursor;
import org.apache.commons.io.FileUtils;
import org.springframework.data.domain.Range;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.*;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@RestController

public class BucketController {

    private final BucketRepository repository;

    BucketController(BucketRepository repository){
        this.repository = repository;
    }

    @PostMapping(value = "/{bucketName}", params = "create")
    Bucket newBucket(@PathVariable String bucketName, HttpServletResponse response){
        Bucket bucket = new Bucket(bucketName);
//        bucket.setCreated(bucket.getId().getTimestamp());
//        bucket.setModified(bucket.getId().getTimestamp());
        new File("./buckets/"+bucketName).mkdirs();
        Bucket resBucket = repository.save(bucket);
        return resBucket;
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
        String objectKey = objectName.replace('.', '/');

        if (!B.objects.containsKey(objectKey)){
            Content newObj = new Content(objectName);

            B.objects.put(objectKey, newObj);
            //TODO: Add object
        }
        repository.save(B);
    }

//    private boolean contains(List<Content> objs, String objectName){
//        if (objs.isEmpty()){ return false; }
//        for (Content obj : objs){
//            if (obj.getName().equals(objectName)){
//                return true;
//            }
//        }
//        return false;
//    }

    @PutMapping(value = "/{bucketName}/{objectName}",params = "partNumber")
    public ResponseEntity UploadAllParts(@PathVariable("bucketName") String bucketName,
                                         @PathVariable("objectName") String objectName,
                                         @RequestParam("partNumber") String partNumber,
                                         @RequestHeader("Content-Length") String contentLength,
                                         @RequestHeader("Content-MD5") String contentMD5,
                                         HttpServletRequest request){

        String objectKey = objectName.replace('.', '/');
        if (repository.findBucketByName(bucketName).objects.containsKey(objectKey)){
            try{
                ServletInputStream inputStream = request.getInputStream();

                String prefixZeros = new String(new char[5-partNumber.length()]).replace("\0", "0");

                File target = new File("./buckets/"+bucketName+"/"+objectName+".part"+prefixZeros+partNumber);

                FileUtils.copyInputStreamToFile(inputStream, target);

            }catch (IOException e){
                System.out.println("+++++++++++++failed to process request++++++++++++");
                return ResponseEntity.badRequest().build();
            }

            return ResponseEntity.ok().build();
        }

        else{
            return ResponseEntity.badRequest().build();
        }
    }


// ------- TODO havent done MD5 -------------
    @PostMapping(value = "/{bucketName}/{objectName}",params = "complete")
    public ResponseEntity completeMultiPartUpload(@PathVariable("bucketName") String bucketName,
                                                @PathVariable("objectName") String objectName){
        File directory = new File("./buckets/"+bucketName);
        File[] filesInDir = directory.listFiles();
        List<File> parts = Arrays.stream(filesInDir)
                .filter(f -> f.getName().split(".part")[0].equals(objectName))
                .collect(Collectors.toList());
        long contentLength = 0;
        if (!parts.isEmpty()){
            for (File part : parts){
                contentLength+=part.length();
            }
        }

        Bucket bucket = repository.findBucketByName(bucketName);
        String objectKey = objectName.replace('.', '/');
        Content object = bucket.objects.get(objectKey);
        object.setContentLength(contentLength);
        object.setComplete(true);
        bucket.objects.put(objectKey, object);
        repository.save(bucket);



        return null;
    }

    @DeleteMapping(value = "/{bucketName}/{objectName}",params = "partNumber")
    public ResponseEntity deletePart(@PathVariable("bucketName") String bucketName,
                                    @PathVariable("objectName") String objectName,
                                    @RequestParam("partNumber") String partNumber){
        File file = new File("./buckets/"+bucketName+"/"+objectName+".part"+partNumber);
        if (file.delete()){
            return ResponseEntity.ok().build();
        }
        else {
            return ResponseEntity.badRequest().build();
        }

    }

    @DeleteMapping(value = "/{bucketName}/{objectName}",params = "delete")
    public ResponseEntity deleteObject(@PathVariable("bucketName") String bucketName,
                                       @PathVariable("objectName") String objectName){
        File directory = new File("./buckets/"+bucketName);
        File[] filesInDir = directory.listFiles();
        if (filesInDir != null){
            for (File f : filesInDir){
                String filename = f.getName();
                if (filename.split(".part")[0] == objectName){
                    File deleteFile = new File("./buckets/"+bucketName+"/"+filename);
                    deleteFile.delete();
                }
            }
            String objectKey = objectName.replace('.', '/');
            Bucket bucket = repository.findBucketByName(bucketName);
            bucket.objects.remove(objectKey);
            repository.save(bucket);
        }
        return null;
    }

    @GetMapping(value = "/{bucketName}/{objectName}")
    ResponseEntity downloadObject(@PathVariable("bucketName") String bucketName,
                        @PathVariable("objectName") String objectName,
                        HttpServletRequest request,
                        HttpServletResponse response){

        String objectKey = objectName.replace('.', '/');
        Content object = repository.findBucketByName(bucketName)
                .objects.get(objectKey);

        if (object.getComplete()){
            String ranges = request.getHeader("Range");
            response.setHeader("Content-Range", ranges);
            if (ranges != null && !ranges.isEmpty()){
//            String[] rs = ranges.substring(6).split(",");
//            for (String range : rs){
                String range = ranges.substring(6);
                Pattern pattern = Pattern.compile("(-?\\d*)-(-?\\d*)");
                Matcher matcher = pattern.matcher(range);
                String fromstr = matcher.group(1);
                String tostr = matcher.group(2);
                long contentLength = object.getContentLength();
                long from;
                long to;
                if (fromstr.length()==0 && tostr.length()==0){
                    return ResponseEntity.badRequest().build();
                }

                if (fromstr.length() == 0){
                    from = -1;
                }
                else{
                    from = Long.parseLong(fromstr);
                }

                if (tostr.length() == 0){
                    to = -1;
                }
                else {
                    to = Long.parseLong(tostr);
                }


                if (from < 0){
                    from = from + contentLength +1;
                }
                if (to < 0){
                    to = to + contentLength + 1;
                }



//                OutputStream outputStream = response.getOutputStream();
                byte[] buffer = new byte[2048];
                int read;




//            }



            }
            else {
                return ResponseEntity.notFound().build();
            }
            return null;
        }
        else{
            return ResponseEntity.notFound().build();
        }


    }

    @PutMapping("/{bucketName}/{objectName}?metadata&key={key}")
    void updateObjectMetadata(){}

    @DeleteMapping("/{bucketName}/{objectName}?metadata&key={key}")
    void removeObjectMetadata(){}

    @GetMapping("/{bucketName}/{objectName}?metadata&key={key}")
    void getObjectMetadata(){}

    @GetMapping(value = "/{bucketName}/{objectName}",params = "metadata")
    void getAllMetadata(){}




}
