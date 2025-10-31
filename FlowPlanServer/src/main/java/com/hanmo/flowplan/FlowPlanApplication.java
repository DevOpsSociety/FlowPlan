package com.hanmo.flowplan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class FlowPlanApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlowPlanApplication.class, args);
    }

}
