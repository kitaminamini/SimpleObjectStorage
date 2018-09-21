package com.cs.muic.backend.SimpleObject;


import com.mongodb.operation.BatchCursor;
import com.oracle.javafx.jmx.json.impl.JSONMessages;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.data.domain.Range;
import org.springframework.data.repository.query.Param;
import org.springframework.http.ResponseEntity;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.*;
import java.net.SocketTimeoutException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
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
    ResponseEntity newBucket(@PathVariable String bucketName, HttpServletResponse response){
        if (repository.existsBucketByName(bucketName)){
            return ResponseEntity.badRequest().build();
        }
        try{
            Bucket bucket = new Bucket(bucketName);
            new File("./buckets/"+bucketName).mkdirs();
            Bucket resBucket = repository.save(bucket);
            return ResponseEntity.ok(resBucket);
        }
        catch (Exception e){
            return ResponseEntity.badRequest().build();
        }
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
    ResponseEntity delete(@PathVariable String bucketname){
        Bucket B = repository.findBucketByName(bucketname);
        if (B==null){
            return ResponseEntity.badRequest().build();
        }

        if (new File("./buckets/"+bucketname).delete()){
            repository.delete(B);
            return ResponseEntity.ok().build();
        }
        else{
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping(value = "/{bucketname}", params="list")
    ResponseEntity getObjects(@PathVariable String bucketname){
        Bucket B = repository.findBucketByName(bucketname);
        if (B == null){
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(B);
    }

    @PostMapping(value = "/{bucketName}/{objectName}", params = "create")
    ResponseEntity createObject(@PathVariable String bucketName, @PathVariable String objectName){
        Bucket B = repository.findBucketByName(bucketName);
        if (B == null){
            return ResponseEntity.badRequest().build();
        }

        String objectKey = objectName.replace('.', '/');

        if (!B.objects.containsKey(objectKey)){
            Content newObj = new Content(objectName);

            B.objects.put(objectKey, newObj);
            //TODO: Add object
            B.setModified();
            repository.save(B);
            return ResponseEntity.ok().build();
        }

        else{
            return ResponseEntity.badRequest().build();
        }

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
    ResponseEntity UploadAllParts(@PathVariable("bucketName") String bucketName,
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



                FileInputStream fileInputStream = new FileInputStream(target);
                String md5 = DigestUtils.md5DigestAsHex(fileInputStream);
                String targetLen = Long.toString(target.length());
                fileInputStream.close();

                HashMap<String, String> json = new HashMap<>();
                json.put("md5", md5);
                json.put("length", targetLen);
                json.put("partNumber", partNumber);


                boolean md5Match = md5.equals(contentMD5);
                boolean lenMatch = targetLen.equals(contentLength);

                if (md5Match && lenMatch){
                    System.out.println("200 OK");

                    return ResponseEntity.ok(json);
                }
                else if (!lenMatch){
                    System.out.println("Content-Length not matched");
                    target.delete();
                    json.put("error", "LengthMismatched");
                    return ResponseEntity.badRequest().body(json);
                }
                else{
                    System.out.println("MD5 not matched");
                    target.delete();
                    json.put("error", "MD5Mismatched");
                    return ResponseEntity.badRequest().body(json);
                }

            }catch (IOException e){
                System.out.println("+++++++++++++failed to process request++++++++++++");
            }

            return ResponseEntity.badRequest().build();
        }

        else{
            return ResponseEntity.badRequest().build();
        }
    }


// ------- TODO havent done MD5 -------------
    @PostMapping(value = "/{bucketName}/{objectName}",params = "complete")
    ResponseEntity completeMultiPartUpload(@PathVariable("bucketName") String bucketName,
                                            @PathVariable("objectName") String objectName){
        HashMap<String, String> json = new HashMap<>();
        json.put("name", objectName);

        Bucket bucket = repository.findBucketByName(bucketName);
        if (bucket  == null){
            json.put("error", "InvalidBucket");
            return ResponseEntity.badRequest().body(json);
        }

        String objectKey = objectName.replace('.', '/');
        Content object = bucket.objects.get(objectKey);
        if (object == null){
            json.put("error", "InvalidObjectName");
            return ResponseEntity.badRequest().body(json);
        }
        File directory = new File("./buckets/"+bucketName);
        File[] filesInDir = directory.listFiles();
        List<File> parts = Arrays.stream(filesInDir)
                .filter(f -> f.getName().split(".part")[0].equals(objectName))
                .collect(Collectors.toList());
        Collections.sort(parts);
        long contentLength = 0;
        if (!parts.isEmpty()){
            try{
                MessageDigest md5 = MessageDigest.getInstance("MD5");
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                for (File part : parts){
                    contentLength+=part.length();
                    byte[] partMD5 = md5.digest(IOUtils
                            .toByteArray(new FileInputStream(part)));
                    outputStream.write(partMD5);
                    object.partMD5s.put(part.getName().replace('.', '/'), partMD5);
                }
                byte[] fullMD5 = md5.digest(outputStream.toByteArray());
                String eTag = Base64.getEncoder().encodeToString(fullMD5)
                        +"-"+Integer.toString(parts.size());
                object.seteTag(eTag);
                object.setContentLength(contentLength);
                object.setComplete(true);
                object.setModified();
                bucket.objects.put(objectKey, object);
                bucket.setModified();
                repository.save(bucket);

                json.put("eTag", eTag);
                json.put("length", Long.toString(contentLength));
                return ResponseEntity.ok(json);
            }
            catch (NoSuchAlgorithmException | IOException e ){
                json.put("error", "NoSuchAlgorithmException|IOException");
            }
        }
        return ResponseEntity.badRequest().body(json);
    }

    @DeleteMapping(value = "/{bucketName}/{objectName}",params = "partNumber")
    ResponseEntity deletePart(@PathVariable("bucketName") String bucketName,
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
    ResponseEntity deleteObject(@PathVariable("bucketName") String bucketName,
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
            bucket.setModified();
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
                Pattern pattern = Pattern.compile("(-?[0-9]*)-(-?[0-9]*)");
                Matcher matcher = pattern.matcher(range);
                matcher.find();
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

                // convert negative to positive
                if (from < 0){
                    from = from + contentLength +1;
                }
                if (to < 0){
                    to = to + contentLength + 1;
                }

                if (from > to || to > contentLength){
                    return ResponseEntity.badRequest().build();
                }

                // Find from which part to which part is in range
                File directory = new File("./buckets/"+bucketName);
                File[] filesInDir = directory.listFiles();
                List<File> parts = Arrays.stream(filesInDir)
                        .filter(f -> f.getName().split(".part")[0].equals(objectName))
                        .collect(Collectors.toList());
                Collections.sort(parts);
                List<InputStream> PartsToBeRead = new ArrayList<>();
                int firstPart = 0;
                int lastPart = 0;
                long startAtFirstPart = 0;
                long endAtLastPart = 0;
                long acc = 0;
                int i = 0;
                long diffAtEnd = 0;
                boolean inRange = false;
                try {
                    for (File part: parts){
                        acc+=part.length();
                        i++;
                        if (!inRange && from <= acc){
                            inRange = true;
                            firstPart = i;
                            startAtFirstPart = part.length() - (acc - from);
                            InputStream inputStream = new FileInputStream(part);
                            if (startAtFirstPart != 0){
                                inputStream.skip(startAtFirstPart - 1);
                            }
                            PartsToBeRead.add(inputStream);
                            if (to <= acc){
                                lastPart = i;
                                endAtLastPart = part.length() - (acc - to);
                                diffAtEnd = acc - to;
                                break;
                            }
                        }

                        else if (to <= acc){
                            lastPart = i;
                            endAtLastPart = part.length() - (acc - to);
                            diffAtEnd = acc - to;
                            InputStream inputStream = new FileInputStream(part);
                            PartsToBeRead.add(inputStream);
                            break;
                        }

                        else if (inRange){
                            InputStream inputStream = new FileInputStream(part);
                            PartsToBeRead.add(inputStream);
                        }
                    }
                    SequenceInputStream combinedIS = new SequenceInputStream(
                            Collections.enumeration(PartsToBeRead));


//                    i = 0;
//                    acc = 0;

                    OutputStream outputStream = new BufferedOutputStream(response.getOutputStream());
                    byte[] buffer = new byte[2048];
                    int read;
//                        for (File part : PartsToBeRead) {
//                            InputStream inputStream =
//                        }
                    long toRead = to - from + 1;
                    while ((read = combinedIS.read(buffer)) > 0){
                        if ((toRead -= read) > 0){
                            outputStream.write(buffer, 0, read);
                            outputStream.flush();
                        }
                        else{
                            outputStream.write(buffer, 0, (int)toRead+read);
                            outputStream.flush();
                            break;
                        }
                    }
//                    IOUtils.copyLarge(outputStream, response.getOutputStream());
                    return ResponseEntity.ok().build();

                }
                catch (IOException e){
                    return ResponseEntity.notFound().build();
                }

            }
            else {
                return ResponseEntity.notFound().build();
            }
        }
        else{
            return ResponseEntity.notFound().build();
        }


    }

    @PutMapping(value = "/{bucketName}/{objectName}", params = "metadata")
    ResponseEntity updateObjectMetadata(@PathVariable("bucketName") String bucketName,
                                      @PathVariable("objectName") String objectName,
                                      @RequestParam(value = "key") String key,
                                      @RequestBody String value){

        Bucket bucket = repository.findBucketByName(bucketName);
        if (bucket == null){
            return ResponseEntity.notFound().build();
        }
        String objectKey = objectName.replace('.', '/');
        Content object = bucket.objects.get(objectKey);
        if (object == null){
            return ResponseEntity.notFound().build();
        }

        if (key == null || key.isEmpty()){
            return ResponseEntity.ok().build();
        }

        object.metaData.put(key, value);
        object.setModified();
        bucket.objects.put(objectKey, object);
        bucket.setModified();
        repository.save(bucket);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping(value = "/{bucketName}/{objectName}", params = "metadata")
    ResponseEntity removeObjectMetadata(@PathVariable("bucketName") String bucketName,
                                      @PathVariable("objectName") String objectName,
                                      @RequestParam(value = "key") String key){
        Bucket bucket = repository.findBucketByName(bucketName);
        if (bucket == null){
            return ResponseEntity.notFound().build();
        }
        String objectKey = objectName.replace('.', '/');
        Content object = bucket.objects.get(objectKey);
        if (object == null){
            return ResponseEntity.notFound().build();
        }

        if (key == null || key.isEmpty()){
            return ResponseEntity.ok().build();
        }

        object.metaData.remove(key);
        object.setModified();
        bucket.objects.put(objectKey, object);
        bucket.setModified();
        repository.save(bucket);

        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/{bucketName}/{objectName}", params = "metadata")
    ResponseEntity getObjectMetadata(@PathVariable("bucketName") String bucketName,
                                     @PathVariable("objectName") String objectName,
                                     @RequestParam(value = "key", required = false) String key){

        Bucket bucket = repository.findBucketByName(bucketName);
        if (bucket == null){
            return ResponseEntity.notFound().build();
        }
        String objectKey = objectName.replace('.', '/');
        Content object = bucket.objects.get(objectKey);
        if (object == null){
            return ResponseEntity.notFound().build();
        }

        HashMap<String, String> json = new HashMap<>();

        if (key == null){

            return ResponseEntity.ok(object.metaData);
        }

        if (key.isEmpty() || !object.metaData.containsKey(key)){
            return ResponseEntity.ok(json);
        }

        json.put(key, object.metaData.get(key));
        return ResponseEntity.ok(json);

    }


}
