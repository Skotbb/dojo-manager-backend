# src/main/java/com/dojomanager/data/forms/student/

## Responsibility

Form Layer — create and update payload for student management, validated at the REST boundary and translated into `Student`/`StudentRank` state by `StudentService`.

## Design

- `StudentRequestForm` — Lombok `@Data`; `jakarta.validation` constraints with explicit messages.
- Fields:
  - `firstName` — `@NotBlank`, `@Size(max = 100)`.
  - `lastName` — `@NotBlank`, `@Size(max = 100)`.
  - `email` — `@NotBlank`, `@Email`, `@Size(max = 254)`.
  - `rankId` (`Long`, optional) — selects an existing `StudentRank`; `null` on create means "no rank yet", `null` on update leaves the current rank untouched.
  - `dojoId` (`Long`, optional) — a selector, not an ownership assignment, for owners with multiple dojos.
- The Javadoc documents the security decision: there is no free-form ownership field; `dojoId` may only select one of the dojos already owned by the authenticated principal, and `StudentService` rejects anything else (403). The dojo always comes from the principal.
- The same form is used for both create and update, so update is a full replace of the field set rather than a patch.

## Flow

1. `POST /api/v1/students` and `PUT /api/v1/students/{id}` bind `@Valid @RequestBody StudentRequestForm`.
2. `StudentService.createStudent` validates the selected `dojoId`/`rankId` against the owner's dojos, normalizes/trims the email, constructs the `Student`, optionally attaches the rank, and saves.
3. `StudentService.updateStudent` loads the owner-scoped student, re-applies the same ownership checks, updates name/email/dojo/rank, and saves.
4. The resulting entity is projected to `StudentDto` via `toDto` and returned by the controller (`201 Created` on create).

## Integration

- Consumed by `com.dojomanager.rest.v1.students.StudentController`.
- Used by `com.dojomanager.services.StudentService` (`createStudent`, `updateStudent`, private rank/dojo validation helpers).
- Maps to `com.dojomanager.data.entities.people.Student` and `com.dojomanager.data.entities.rank.StudentRank`, with dojo selection from `DojoOwner.getManagedDojos()`; output is `com.dojomanager.data.dto.student.StudentDto`.
