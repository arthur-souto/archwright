package com.acme.legado.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Composition root. Fica em {@code bootstrap}, então o scan precisa apontar explicitamente para
 * {@code com.acme.legado}: sem isso só beans, entidades e repositórios dentro de {@code bootstrap} seriam encontrados.
 * <ul>
 *   <li>{@code scanBasePackages}: {@code @Component} e {@code @Configuration} de todos os módulos.</li>
 *   <li>{@code @AutoConfigurationPackage}: {@code @Entity} e repositórios Spring Data de todos os módulos.</li>
 * </ul>
 * Testes {@code @SpringBootTest} devem declarar {@code classes = LegadoApplication.class}.
 */
@SpringBootApplication(scanBasePackages = "com.acme.legado")
@AutoConfigurationPackage(basePackages = "com.acme.legado")
public class LegadoApplication {

    public static void main(String[] args) {
        SpringApplication.run(LegadoApplication.class, args);
    }

}
