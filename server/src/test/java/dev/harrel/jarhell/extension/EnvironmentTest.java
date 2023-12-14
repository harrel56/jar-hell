package dev.harrel.jarhell.extension;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@ExtendWith(EnvironmentExtension.class)
public @interface EnvironmentTest {}
