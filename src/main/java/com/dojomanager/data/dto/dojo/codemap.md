# src/main/java/com/dojomanager/data/dto/dojo/

## Responsibility

DTO Layer — dojo/owner API payloads: flattened dojo reads, the owner profile response, and the dojo create/update body.

## Design

- `DojoDto` — new flattened dojo response (Lombok-based) with `id`, `name`, `website`, and a flattened address; static `from(Dojo)` mapper. Never exposes the JPA entity graph.
- `DojoResponse` — plain POJO wrapper. Single field `List<DojoDto> dojos` (changed from `List<Dojo>` — it no longer serializes entities directly). Constructed by `DojoController.getMyDojos`.
- `OwnerResponse` — new flat owner profile response with `id`, names, email, and `managedDojos` as `DojoDto[]`; static `from(DojoOwner)` mapper. Replaces the raw `DojoOwner`/`Collection<Dojo>` responses the old `/owner` endpoints returned.
- `DojoUpdateRequest` — plain POJO with `name`, `website`, `address`. Fields are optional (only non-null values are applied by `DojoService.applyRequest`, wrapping the address string in `new Address(address, null, null, null, null)`). The `contact` field was removed — the `Dojo` entity has no contact field, so it was accepted-but-never-applied dead weight.
- `OwnerDTO` — deleted. It was a legacy, unused signup-shaped DTO (imported but never referenced).
- No `jakarta.validation` annotations; the controller does not `@Valid` these bodies yet.

## Flow

1. `GET /api/v1/dojos` → `DojoController.getMyDojos` maps each owned `Dojo` through `DojoDto::from` into `DojoResponse`.
2. `POST /api/v1/dojos` → `DojoUpdateRequest` bound from JSON → `DojoService.createDojo` applies non-null fields → `DojoDto.from(created)` returned with `201 Created`.
3. `PUT /api/v1/dojos/{id}` → same request shape → `DojoService.updateDojo` applies to an owned dojo → `DojoDto.from(updated)`.
4. `GET /api/v1/owner` → `OwnerResponse.from(getOwnerProfile(principal))`, with `managedDojos` flattened to `DojoDto[]`.

## Integration

- Consumed by `com.dojomanager.rest.v1.dojo.DojoController` (`DojoDto`, `DojoResponse`, `DojoUpdateRequest`) and `DojoOwnerController` (`OwnerResponse`).
- Maps from `com.dojomanager.data.entities.dojo.Dojo` / `DojoOwner` and `com.dojomanager.data.entities.people.Address`.
- Services involved: `DojoService` (`listDojos`/`createDojo`/`updateDojo` via `applyRequest`), `DojoOwnerService.getOwnerProfile`.
