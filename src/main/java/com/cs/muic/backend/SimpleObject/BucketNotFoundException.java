package com.cs.muic.backend.SimpleObject;

public class BucketNotFoundException extends RuntimeException {
    BucketNotFoundException(Long id){
        super("Could not find bucket id: "+id);
    }
}
