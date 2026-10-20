# src/main/java/com/dojomanager/data/dto/dojo/

## Responsibility

DTO Layer — dojo/owner API payloads: flattened dojo reads, the owner profile response, the dojo update body, and the create-with-ranks request.

## Design

- `CreateDojoRequest` — Lombok `@Data` body for `POST /api/v1/dojos`, kept separate from `DojoUpdateRequest` so updates cannot rewrite the rank graph. Top level: `name` (`@NotBlank`), `website`, `address`, and `@Valid List<RankRequest> ranks` (nullable/empty allowed). Nested static classes: `RankRequest` (`@NotBlank name`, `@Valid levels: List<RankLevelRequest>`) and `RankLevelRequest` (`sortOrder`, `number`, `hoursRequired` as primitives defaulting to 0, `beltColor` `@NotBlank`, nullable `stripeColor`, `Integer stripeCount` — null-safe at persistence).
- `DojoDto` — flattened dojo response (Lombok-based) with `id`, `name`, `website`, flattened address, and optional `ranks: List<RankDto>`; static `from(Dojo)` and `from(Dojo, List<RankDto>)` mappers. Never exposes the JPA entity graph.
- `RankDto` and `RankLevelDto` — structured DTO representations of ranks and levels (`id`, `name`, `levels` containing `sortOrder`, `number`, `hoursRequired`, `beltColor`, `stripeColor`, `stripeCount`, `rankSettingId`).
- `DojoResponse` — plain POJO wrapper. Single field `List<DojoDto> dojos`. Constructed by `DojoController.getMyDojos`.
- `OwnerResponse` — flat owner profile response with `id`, names, email, and `managedDojos` as `DojoDto[]`; static `from(DojoOwner)` mapper.
- `DojoUpdateRequest` — POJO with `name`, `website`, `address`, and optional `@Valid List<CreateDojoRequest.RankRequest> ranks`. Fields are optional (only non-null values are applied).
- `OwnerDTO` — deleted. It was a legacy, unused signup-shaped DTO (imported but never referenced).
- No `jakarta.validation` annotations; the controller does not `@Valid` these bodies yet.

## Flow

1. `GET /api/v1/dojos` → `DojoController.getMyDojos` maps each owned `Dojo` through `DojoDto::from` into `DojoResponse`.
2. `POST /api/v1/dojos` → `CreateDojoRequest` bound from JSON with cascade validation → `DojoService.createDojo` applies `name`/`website`/`address`, persists the dojo, then builds the rank graph (one `RankName` per rank; per level `RankLevel` → `BeltColor` → `RankSetting`) → `DojoDto.from(created)` returned with `201 Created`.
3. `PUT /api/v1/dojos/{id}` → same request shape → `DojoService.updateDojo` applies to an owned dojo → `DojoDto.from(updated)`.
4. `GET /api/v1/owner` → `OwnerResponse.from(getOwnerProfile(principal))`, with `managedDojos` flattened to `DojoDto[]`.

## Integration

- Consumed by `com.dojomanager.rest.v1.dojo.DojoController` (`CreateDojoRequest` for POST, `DojoUpdateRequest` for PUT, `DojoDto`, `DojoResponse`) and `DojoOwnerController` (`OwnerResponse`).
- Maps from `com.dojomanager.data.entities.dojo.Dojo` / `DojoOwner` and `com.dojomanager.data.entities.people.Address`.
- Services involved: `DojoService` (`listDojos`/`createDojo`/`updateDojo` via `applyRequest`), `DojoOwnerService.getOwnerProfile`.
