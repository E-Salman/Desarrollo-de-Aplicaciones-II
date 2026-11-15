# ADR 003 — Repository para acceso a datos

**Estado:** aceptado · Segunda entrega parcial.

**Contexto.** Compra, Venta, Portfolio y el consumidor JMS consultan entidades persistidas y comparten límites transaccionales. Parte del catálogo MySQL tiene un esquema propio.

**Decisión.** Los repositorios encapsulan consultas JPA y consultas específicas del catálogo. `OrdenRepository` concentra las consultas de órdenes y el bloqueo pesimista que coordina al usuario con el ejecutor. La aplicación utiliza el `EntityManager` JTA del contenedor.

**Alternativas.** Colocar SQL en los recursos REST mezclaría persistencia con HTTP. Un repositorio genérico de CRUD no expresaría la búsqueda por propietario ni el bloqueo de la orden como operaciones de dominio.

**Consecuencias.** Las consultas, el mapeo y los bloqueos se revisan en la capa de datos. Las pruebas unitarias pueden sustituir los repositorios. El comportamiento transaccional se comprueba contra MySQL y WildFly.

**Evidencia.** `OrdenRepository`, `OperacionRepository`, `CatalogoMercadoRepository` y `smoke_segunda_entrega.mjs`.
