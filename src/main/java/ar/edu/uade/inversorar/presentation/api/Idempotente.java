package ar.edu.uade.inversorar.presentation.api;

import jakarta.ws.rs.NameBinding;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Marca un POST que acepta el encabezado Idempotency-Key (ver {@link FiltroIdempotencia}). */
@NameBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Idempotente { }
