package com.santacecilia.api.config;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
@Configuration
public class CorreoConfig {
    @Bean(name="correoExecutor") ThreadPoolTaskExecutor correoExecutor(){
        ThreadPoolTaskExecutor executor=new ThreadPoolTaskExecutor();executor.setCorePoolSize(2);executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);executor.setThreadNamePrefix("sc-correo-");executor.initialize();return executor;
    }
}
