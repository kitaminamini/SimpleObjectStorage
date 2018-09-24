package com.cs.muic.backend.SimpleObject.config;


import com.cs.muic.backend.SimpleObject.Bucket;
import com.cs.muic.backend.SimpleObject.BucketRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;


@EnableMongoRepositories(basePackageClasses = BucketRepository.class)
@Configuration
public class MongoDBConfig {

    @Bean
    CommandLineRunner commandLineRunner(BucketRepository bucketRepository){
        return  new CommandLineRunner() {
            @Override
            public void run(String... args) throws Exception {
//                bucketRepository.save(new Bucket("test"));
            }
        };
    }
}
