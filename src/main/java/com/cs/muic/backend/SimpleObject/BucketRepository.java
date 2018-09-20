package com.cs.muic.backend.SimpleObject;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface BucketRepository extends MongoRepository<Bucket, Long> {

    Bucket findBucketByName(String name);

    boolean existsBucketByName(String name);

}
