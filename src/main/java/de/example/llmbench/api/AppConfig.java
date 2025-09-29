package de.example.llmbench.api;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * Konfiguriert die Basis-URL für die REST-API.
 * 
 * Die Annotation @ApplicationPath legt fest, dass alle REST-Endpunkte
 * unter dem Pfad "/api" erreichbar sind.
 * 
 * Durch das Erben von jakarta.ws.rs.core.Application wird diese Klasse
 * als Einstiegspunkt für die JAX-RS-Anwendung verwendet.
 */
@ApplicationPath("/api")
public class AppConfig extends Application { }
