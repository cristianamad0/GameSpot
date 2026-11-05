package com.usta.gamespot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Habilita las tareas programadas con {@code @Scheduled}. */
@Configuration
@EnableScheduling
public class ProgramacionConfig {}
