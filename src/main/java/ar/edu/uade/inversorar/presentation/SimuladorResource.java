package ar.edu.uade.inversorar.presentation;

import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/** Alias HTTP anterior; utiliza la misma conversación del componente Portfolio. */
@Deprecated
@RequestScoped
@Path("simulador") @Produces(MediaType.APPLICATION_JSON)
public class SimuladorResource extends SimulacionPortfolioResource { }
