package com.cs.muic.backend.SimpleObject.services;

import com.cs.muic.backend.SimpleObject.Bucket;
import com.cs.muic.backend.SimpleObject.BucketRepository;
import com.mongodb.MongoException;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
public class BucketService {

    private BucketRepository repository;

    @Autowired
    public BucketService(BucketRepository repository){
        this.repository = repository;
    }

    public Bucket createBucket(String bucketName) throws
            DataAccessResourceFailureException, MongoException {
        if (repository.existsBucketByName(bucketName)){
            return null;
        }
        Bucket bucket = new Bucket(bucketName);
        new File("/tmp/buckets/"+bucketName).mkdirs();
        Bucket resBucket = repository.save(bucket);
        return resBucket;
    }

    public Bucket find(String bucketName) throws
            DataAccessResourceFailureException, MongoException{
        return repository.findBucketByName(bucketName);
    }

    public int delete(String bucketName) throws IOException {
        Bucket B = repository.findBucketByName(bucketName);
        if (B==null){
            return 400;
        }
        FileUtils.deleteDirectory(new File("/tmp/buckets/"+bucketName));
        repository.delete(B);
        return 200;
    }

}
