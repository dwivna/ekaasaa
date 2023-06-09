package com.learning.dwivna.ekaasaa.service.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.learning.dwivna.ekaasaa.data.User;
import com.learning.dwivna.ekaasaa.repositories.UserRepository;
import com.learning.dwivna.ekaasaa.service.PublisherService;
import com.learning.dwivna.ekaasaa.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.ReactiveSubscription;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Autowired
    private ReactiveRedisMessageListenerContainer reactiveMsgListenerContainer;

    @Autowired
    private ChannelTopic channelTopic;

    @Autowired
    private PublisherService publisherService;

    @Autowired
    private UserRepository userRepository;

    private final Gson gson = new GsonBuilder().create();

    @Override
    public Mono<User> getUser(String id) {
        return userRepository.findById(id);
    }

    @Override
    public Mono<List<User>> getUsers() {
        return userRepository.findAll();
    }

    @Override
    public Mono<User> putUser(User user) {
        return userRepository.save(user);
    }

    @Override
    public Mono<User> updateUser(String id, User user) {
        return userRepository.update(id,user);
    }

    @Override
    public Mono<String> deleteUser(String id) {
        return userRepository.deleteById(id);
    }

    @Override
    public Publisher<List<User>> userSub() {
        return this.subscriptionResponse();
    }

    @Scheduled(fixedDelay = 30000)
    public void subscribeApp() {
        List<User> userList = Objects.requireNonNull(this.getUsers().block());
        log.info("Size of fetched keys is {}", userList.size());
        publisherService.publish(gson.toJson(userList));
    }

    private Flux<String> getSubscriptionMessage() {
        return reactiveMsgListenerContainer.receive(this.channelTopic)
                .map(ReactiveSubscription.Message::getMessage)
                .map(msg -> {
                    System.out.println(msg);
                    return msg;
                });
    }

    private Flux<List<User>> subscriptionResponse() {
        return this.getSubscriptionMessage().flatMap(subscribeData -> {
            log.info("UserSub Message:{}", subscribeData);
            Type typeOfObjectsList = new TypeToken<List<User>>() {
            }.getType();
            List<User> users = gson.fromJson(subscribeData, typeOfObjectsList);
            log.info("Message List:{}", users);
            return Mono.just(users);
        });
    }
}
