package com.redditclone.redditservice.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.ReplayProcessor;

@Configuration
public class ReplayProcessorConfiguration {

    @Bean
    FluxSink<Object> replayProcessor() {
         return ReplayProcessor
                 .create()
                 .serialize()
                 .sink();
    }
}
