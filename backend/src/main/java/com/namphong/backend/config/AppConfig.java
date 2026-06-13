package com.namphong.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class AppConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        return mapper;
    } // tu tao bean objectmapper de fix loi spring k tim dc bean

    @Bean
    public MongoClient mongoClient() {
        return MongoClients.create(
                "mongodb://admin:password@localhost:27017/study_app_chat?authSource=admin"
        );
    }
    // Object đại diện cho toàn bộ kết nối mongo
    // kết nối tới localhost:27017, gửi username password và xác thực ở database, tạo connection pool để tái sử dụng 


    @Bean
    public MongoTemplate mongoTemplate(MongoClient mongoClient) {
        return new MongoTemplate(mongoClient, "study_app_chat");
    }
    // Nó giúp:
    //save()
    //find()
    //update()
    //delete()

    //Ví dụ:
    //mongoTemplate.save(chatMessage);
    //Spring sẽ:
    //db.chatMessage.insertOne(...)
//   new MongoTemplate(client, "study_app_chat");
//            "Hãy thao tác trên database này."
//    Tương đương:
//    use study_app_chat
}
