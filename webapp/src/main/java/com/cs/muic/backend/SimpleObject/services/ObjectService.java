package com.cs.muic.backend.SimpleObject.services;

import com.cs.muic.backend.SimpleObject.Bucket;
import com.cs.muic.backend.SimpleObject.BucketRepository;
import com.cs.muic.backend.SimpleObject.Content;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ObjectService {


    private BucketRepository repository;

    @Autowired
    public ObjectService(BucketRepository repository){
        this.repository = repository;
    }

    public int createObject(String bucketName, String objectName){
        Bucket B = repository.findBucketByName(bucketName);
        if (B == null){
            return 400;
        }
        String objectKey = objectName.replace('.', '/');

        if (!B.objects.containsKey(objectKey)){
            Content newObj = new Content(objectName);

            B.objects.put(objectKey, newObj);
            B.setModified();
            repository.save(B);
            return 200;
        }
        else{
            return 400;
        }
    }

    public HashMap<String, String> upload(String bucketName,
                                          String objectName,
                                          String partNumber,
                                          String contentLength,
                                          String contentMD5,
                                          HttpServletRequest request,
                                          HashMap<String, String> json) throws IOException{
        String objectKey = objectName.replace('.', '/');
        if (repository.findBucketByName(bucketName).objects.containsKey(objectKey)){

            ServletInputStream inputStream = request.getInputStream();

            String prefixZeros = new String(new char[5-partNumber.length()]).replace("\0", "0");

            File target = new File("/tmp/buckets/"+bucketName+"/"+objectName+".part"+prefixZeros+partNumber);

            FileUtils.copyInputStreamToFile(inputStream, target);

            FileInputStream fileInputStream = new FileInputStream(target);
            String md5 = DigestUtils.md5DigestAsHex(fileInputStream);
            String targetLen = Long.toString(target.length());
            fileInputStream.close();

            json.put("md5", md5);
            json.put("length", targetLen);
            json.put("partNumber", partNumber);

            boolean md5Match = md5.equals(contentMD5);
            boolean lenMatch = targetLen.equals(contentLength);

            if (md5Match && lenMatch){
                return json;
            }
            else if (!lenMatch){
                System.out.println("Content-Length not matched");
                target.delete();
                json.put("error", "LengthMismatched");
            }
            else{
                System.out.println("MD5 not matched");
                target.delete();
                json.put("error", "MD5Mismatched");
            }
        }
        else{
            json.put("error", "BucketOrObjectNotFound");
        }
        return json;
    }

    public HashMap<String, String> complete(String bucketName, String objectName, HashMap<String, String> json)throws
            NoSuchAlgorithmException, IOException{
        Bucket bucket = repository.findBucketByName(bucketName);
        if (bucket  == null){
            json.put("error", "InvalidBucket");
            return json;
        }

        String objectKey = objectName.replace('.', '/');
        if (!bucket.objects.containsKey(objectKey)){
            json.put("error", "InvalidObjectName");
            return json;
        }

        Content object = bucket.objects.get(objectKey);
        File directory = new File("/tmp/buckets/"+bucketName);
        File[] filesInDir = directory.listFiles();
        List<File> parts = Arrays.stream(filesInDir)
                .filter(f -> f.getName().split(".part")[0].equals(objectName))
                .collect(Collectors.toList());
        Collections.sort(parts);
        long contentLength = 0;
        if (!parts.isEmpty()){
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
            return json;
        }
        else{
            json.put("error", "NoParts");
        }
        return json;
    }


    public boolean deletePart(String bucketName, String objectName, String partNumber){
        File file = new File("/tmp/buckets/"+bucketName+"/"+objectName+".part"+partNumber);
        boolean isDeleted = file.delete();
        return isDeleted;
    }

    public boolean deleteObject(String bucketName, String objectName){
        String objectKey = objectName.replace('.', '/');
        Bucket bucket = repository.findBucketByName(bucketName);
        if (bucket == null){
            return false;
        }
        if (bucket.objects.containsKey(objectKey)){
            bucket.objects.remove(objectKey);
            bucket.setModified();
            repository.save(bucket);
            File directory = new File("/tmp/buckets/"+bucketName);
            File[] filesInDir = directory.listFiles();
            if (filesInDir != null){
                for (File f : filesInDir){
                    String filename = f.getName();
                    if (filename.split(".part")[0].equals(objectName)){
                        File deleteFile = new File("/tmp/buckets/"+bucketName+"/"+filename);
                        deleteFile.delete();
                    }
                }
            }
            return true;
        }
        else{
            return false;
        }
    }

    public boolean download(String bucketName, String objectName,
                         HttpServletRequest request,
                         HttpServletResponse response) throws IOException{
        String objectKey = objectName.replace('.', '/');
        Content object = repository.findBucketByName(bucketName)
                .objects.get(objectKey);

        if (object == null){
            return false;
        }

        if (object.getComplete()){
            String ranges = request.getHeader("Range");
            if (ranges == null || ranges.isEmpty()){
                ranges = "bytes=0-";
            }
            response.setHeader("Content-Range", ranges);
            String range = ranges.substring(6);
            Pattern pattern = Pattern.compile("(-?[0-9]*)-(-?[0-9]*)");
            Matcher matcher = pattern.matcher(range);
            matcher.find();
            String fromstr = matcher.group(1);
            String tostr = matcher.group(2);
            long contentLength = object.getContentLength();
            if (fromstr.length()==0 && tostr.length()==0){
                return false;
            }
            long[] fromTo = getRange(fromstr, tostr, contentLength);
            long from = fromTo[0];
            long to = fromTo[1];
            if (from > to || to > contentLength){
                return false;
            }

            // Find from which part to which part is in range
            File directory = new File("/tmp/buckets/"+bucketName);
            File[] filesInDir = directory.listFiles();
            List<File> parts = Arrays.stream(filesInDir)
                    .filter(f -> f.getName().split(".part")[0].equals(objectName))
                    .collect(Collectors.toList());
            Collections.sort(parts);
            List<InputStream> PartsToBeRead = new ArrayList<>();
            long startAtFirstPart = 0;
            long acc = 0;
            int i = 0;
            boolean inRange = false;
            for (File part: parts){
                acc+=part.length();
                i++;
                if (!inRange && from <= acc){
                    inRange = true;
                    startAtFirstPart = part.length() - (acc - from);
                    InputStream inputStream = new FileInputStream(part);
                    if (startAtFirstPart != 0){
                        inputStream.skip(startAtFirstPart);
                    }
                    PartsToBeRead.add(inputStream);
                    if (to <= acc){
                        break;
                    }
                }

                else if (to <= acc){
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

            OutputStream outputStream = new BufferedOutputStream(response.getOutputStream());
            byte[] buffer = new byte[2048];
            int read;

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
            outputStream.close();
            combinedIS.close();
//                    IOUtils.copyLarge(outputStream, response.getOutputStream());
            return true;
        }
        else{
            return false;
        }
    }

    private long[] getRange(String fromstr, String tostr, long contentLength){
        long from;
        long to;
        long[] range = new long[2];

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
        range[0] = from;
        range[1] = to;
        return range;
    }

}
