# src/main/java/com/dojomanager/data/dto/dojo/

## Responsibility

DTO Layer — dojo/owner API payloads: read wrapper for an owner's dojos, the dojo update body, and a legacy owner transfer object.

## Design

- `DojoResponse` — plain POJO (no Lombok, manual ctor/getter/setter). Single field `List<com.dojomanager.data.entities.dojo.Dojo> dojos`. Null is used as the "owner not found" signal by callers. It serializes JPA `Dojo` entities directly (including `owner`/`address`/`leadInstructor`), so it is the one DTO here that does not flatten its entity graph; `Dojo.students` is `@JsonIgnore`d on the entity.
- `DojoUpdateRequest` — plain POJO with `name`, `website`, `contact`, `address`. Fields are treated as optional: `DojoController.updateDojo` applies only non-null `name`, `website`, and `address` (wrapping the address string in `new Address(address, null, null, null, null)`). Notably `contact` is accepted but never applied to the `Dojo` entity (the entity has no contact field).
- `OwnerDTO` — Lombok `@Data` with `firstName`, `lastName`, `email`, `password`. It is imported by `DojoOwnerController` but not referenced anywhere in the current code; treat it as a legacy/unused signup-shaped DTO.
- No `jakarta.validation` annotations; controllers do not `@Valid` these bodies.

## Flow

1. `GET /dojo/api/v1/dojo` → `DojoController.getMyDojo()` resolves the principal's email, loads the `DojoOwner`, and returns `new DojoResponse(List.copyOf(owner.getManagedDojos()))` (or `null` list when absent).
2. `PUT /dojo/api/v1/dojo` → `DojoUpdateRequest` bound from JSON, selectively copied into a new `Dojo`, persisted via `DojoService.saveDojo`, and returned inside `DojoResponse`.
3. `OwnerDTO` has no active flow.

## Integration

- Consumed by `com.dojomanager.rest.v1.dojo.DojoController`; `OwnerDTO` is only imported by `com.dojomanager.rest.v1.dojo.DojoOwnerController` (unused).
- Maps to/from `com.dojomanager.data.entities.dojo.Dojo` and `com.dojomanager.data.entities.people.Address`; reads `DojoOwner.getManagedDojos()`.
- Services involved: `DojoService.saveDojo`, `DojoOwnerService.getOwnerByEmail`.
