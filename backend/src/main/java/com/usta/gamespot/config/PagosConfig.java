package com.usta.gamespot.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** CACS-21 (HU-03): registra las propiedades {@code app.pagos.*}. */
@Configuration
@EnableConfigurationProperties(PropiedadesPagos.class)
public class PagosConfig {}
