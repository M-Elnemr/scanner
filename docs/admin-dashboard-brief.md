# Admin dashboard integration brief

Backend: Umrah Scanner (Spring Boot 4, `/api/v1`). This is the API contract for an **admin
dashboard** — there is no dashboard UI in this repository; it is meant to be built against the
endpoints below, the same way `docs/flutter-integration-brief.md` specifies the customer/company app.

---

## 0. TL;DR

1. Admin gets **full CRUD** on companies, trips and a new **hotels catalogue** trips pick from —
   plus the power to **reassign a trip to a different company** and **force a lead to any status**.
2. **Hotels are no longer free text.** A trip picks a hotel per city from a platform-curated
   catalogue. See `docs/flutter-integration-brief.md` §13 for the customer/company app's side of
   this — it is the same breaking change described from the other app's point of view.
3. Two buttons on a lead compose a **WhatsApp message** (trip details, or company details) addressed
   to the customer, with a `wa.me` link ready to open. Nothing is sent automatically.
4. **Reassigning a trip's company does not touch existing leads.** A lead keeps the company (and
   pricing) it was created against — only leads created *after* the reassignment follow the new
   owner. This is deliberate, not a bug: see §5.3.
5. **The status override bypasses the normal workflow entirely** and does not move money. See §7.2
   before building the UI for it — this is the one endpoint in the whole API that can put a lead in
   a state the rest of the system wouldn't otherwise allow.
6. Admin accounts are still seeded directly in the database — there is no "create an admin" endpoint
   here, and none is planned.

---

## 1. Envelopes and errors

Identical to the customer app — see `docs/flutter-integration-brief.md` §2 for the full reference.
Summary: every success body is `{ "data": ... }`; paginated lists add `page`/`size`/`totalElements`/
`totalPages`/`last`; errors are RFC 7807 problem+json, and some carry a stable `code` plus extra
fields (e.g. `ACTIVE_LEAD_EXISTS` on the lead-override endpoint — see §7.2).

Auth is unchanged: `Authorization: Bearer <accessToken>`, obtained the same way as any other role.
There is no separate admin login flow.

---

## 2. Hotels catalogue

| Method | Path | Auth |
|---|---|---|
| GET | `/api/v1/hotels?city=` | public |
| GET | `/api/v1/admin/hotels` | ADMIN — includes retired hotels the public list omits |
| POST | `/api/v1/admin/hotels` | ADMIN |
| PUT | `/api/v1/admin/hotels/{id}` | ADMIN |
| DELETE | `/api/v1/admin/hotels/{id}` | ADMIN |

```json
{
  "id": "…", "city": "MAKKAH", "name": "Swissotel Al Maqam Makkah", "nameAr": null,
  "stars": 5, "distanceToHaramM": 350, "canWalk": true,
  "locationUrl": "https://maps.google.com/?q=…", "active": true
}
```

**Create** — `POST /admin/hotels`: `{ city, name, nameAr?, stars, distanceToHaramM?, canWalk,
locationUrl?, active }`. `city` is **immutable after creation** — a hotel that moved city is a
different hotel; delete and recreate it if it was miscategorized before any trip used it. 409 if a
hotel with the same (case-insensitive) name already exists in that city.

**Update** — `PUT /admin/hotels/{id}`: same body minus `city`.

**Delete vs. retire — these are two different operations, pick the right one:**

| Want | How |
|---|---|
| Take it out of the picker for new trips, trips already using it are unaffected | `PUT` with `active: false` |
| Remove a hotel created by mistake | `DELETE` |

`DELETE` returns **409** naming how many trips use it if any do — retire it instead. There is no way
to force-delete a hotel that is in use; that would break every trip (and lead, and past booking)
still pointing at it.

`canWalk` is an admin judgement call, not something the API computes from `distanceToHaramM` — set it
deliberately, don't infer a threshold client-side.

---

## 3. Companies

Endpoints already covered by `docs/flutter-integration-brief.md` §5.8 (`GET /admin/companies`,
`/{id}/approve`, `/{id}/reject`, `/{id}/suspend`, `/{id}/commission`) are unchanged. New:

| Method | Path |
|---|---|
| GET | `/api/v1/admin/companies?status=&search=` — both now optional; omit `status` for every company |
| POST | `/api/v1/admin/companies` |
| PUT | `/api/v1/admin/companies/{id}` |
| DELETE | `/api/v1/admin/companies/{id}` |
| PATCH | `/api/v1/admin/companies/{id}/reinstate` |

### 3.1 Create a company from scratch

`POST /admin/companies`:

```json
{
  "ownerEmail": "owner@example.com",
  "companyName": "Al Noor Travel", "licenseNumber": "TRV-2026-001",
  "logoUrl": null, "whatsapp": "+2010…", "description": "…",
  "addresses": [{ "cityId": "…", "addressText": "…", "mobileNumber": "+2010…" }],
  "commissionPerTraveler": 2000.00,
  "autoApprove": true
}
```

**There is no invite flow, and none is needed.** If `ownerEmail` has no account yet, one is
provisioned with a placeholder identity; the moment that person signs in with Google on that same
email, they land in the company's seat automatically — same login screen as everyone else, nothing
sent to them by this API. Tell the admin exactly that: "the owner just needs to sign in with Google
using this email — no invite link, no separate step."

Three failure cases worth surfacing distinctly in the UI:
- **A COMPANY account for that email exists with no profile yet** — silently adopted, this succeeds.
  (The common case: someone opened the app, chose "I'm a company", then never finished registering.)
- **A company profile for that email already exists** → 409.
- **A CUSTOMER or ADMIN account already owns that email** → 409. This is refused, not converted —
  that account may hold bookings, a wallet, or admin authority the API has no way to safely discard.
  Tell the admin to use a different email for this company.

`autoApprove: true` (recommended default in the UI) skips the PENDING queue — creating it *is* the
vetting step. Pass `false` to route it through the normal approve/reject flow instead.

### 3.2 Edit any company

`PUT /admin/companies/{id}` — same body as the company's own self-service update
(`docs/flutter-integration-brief.md` §7, `companyName`/`logoUrl`/`whatsapp`/`description`/
`addresses`), plus `licenseNumber`, which the self-service path treats as immutable. Works on a
suspended company; the self-service equivalent refuses.

### 3.3 Delete

`DELETE /admin/companies/{id}` → 204, or **409** naming the reason:
- `"This company has N live booking(s). Resolve or cancel them before deleting it."`
- `"This company has an unsettled commission owed to the platform."`

On success: the company, its trips, and its owner's account are all soft-deleted in one step (the
owner is signed out everywhere — every refresh token is revoked) and the email/licence become free
for reuse. Leads, ratings and commission history are kept — nothing about past bookings disappears,
only the company's ability to operate. **Consider not exposing this as a casual button** — suspend +
reinstate (below) covers "stop this company, maybe restart later" without ever touching the record.
Delete is really for "this was created by mistake five minutes ago."

### 3.4 Reinstate

`PATCH /admin/companies/{id}/reinstate` — the missing other half of suspend. 409 unless the company
is currently `SUSPENDED`. Restores straight to `APPROVED` (not back through the PENDING queue — it
was already vetted once) and clears the rejection reason.

---

## 4. Trips

A parallel surface to the company's own trip endpoints (`docs/flutter-integration-brief.md` §12),
minus ownership — an admin acts on any trip, for any company.

| Method | Path |
|---|---|
| GET | `/api/v1/admin/trips?companyId=&status=&tier=&departureFrom=&departureTo=&search=` |
| GET | `/api/v1/admin/trips/{id}` |
| POST | `/api/v1/admin/trips` |
| PUT | `/api/v1/admin/trips/{id}` |
| PATCH | `/api/v1/admin/trips/{id}/status` |
| PATCH | `/api/v1/admin/trips/{id}/company` |
| DELETE | `/api/v1/admin/trips/{id}` |

### 4.1 List

Every filter is optional and they combine. List rows use a different shape from the public browse
(`AdminTripSummary`) because the admin console needs the owning company on every row, which the
public browse deliberately never exposes:

```json
{ "id": "…", "tripCode": "…", "title": "…", "companyId": "…", "companyName": "…",
  "departureDate": "…", "returnDate": "…", "status": "PUBLISHED", "tier": "ECONOMIC", "availableSeats": 20 }
```

`search` matches trip title or trip code.

### 4.2 Create / update

Same body as the company's own `POST/PUT /companies/me/trips` — see
`docs/flutter-integration-brief.md` §12 for every field, and §13 (above) for the new hotel-picker
shape, which applies here identically. The one difference: **create** wraps it with an explicit
target company —

```json
{ "companyId": "…", "trip": { "tripCode": "…", "title": "…", … "hotels": [...], "prices": [...] } }
```

— because there is no authenticated company to imply it from. `companyId` must be `APPROVED`.

Update/status-change/delete take no `companyId` — they act on whatever company already owns the
trip. Full trip detail (`GET /admin/trips/{id}`) is the same `TripDetail` shape used everywhere else
in this API, including the newly-nested hotel shape.

### 4.3 Reassign a trip's company — read this before building the button

`PATCH /admin/trips/{id}/company`, body `{ "companyId": "…" }`. Target must be `APPROVED` and
different from the current owner; the trip must not be `CLOSED`.

**Existing leads on this trip keep their original company.** A lead's commission is a permanent
snapshot of the company's rate at the moment it was created — that is a hard rule everywhere else in
this API (see the lead lifecycle brief), and reassignment does not get a special exception. So after
this call:

- the trip's catalogue listing, and every **new** lead on it, belongs to the new company;
- every **existing** lead on it still belongs to the old company, with the old company's pricing,
  and that company still works it through the normal lifecycle;
- `lead.companyId != lead.trip.companyId` for those leads, by design, forever.

Surface this explicitly in the confirm dialog — "existing bookings on this trip stay with
`<old company>`; only new bookings go to `<new company>`" — because it is the one thing about this
endpoint that looks like a bug if nobody explains it.

---

## 5. Leads

Building on `docs/flutter-integration-brief.md` §5 — the same `Lead` shape, `availableActions`, and
lifecycle actions apply. This section is additive.

### 5.1 List, filtered

`GET /admin/leads?status=&companyId=&tripId=&customerId=&createdFrom=&createdTo=&search=` — every
param optional and combinable (e.g. `companyId` + `createdFrom`/`createdTo` for "this company's
bookings this month"). `createdFrom`/`createdTo` are ISO instants. `search` matches the customer's
name or phone, or the trip's title or code.

### 5.2 Detail — trip and company already embedded

`GET /admin/leads/{id}`:

```json
{ "data": { "lead": { ...full Lead... }, "trip": { ...full TripDetail... }, "company": { ...full Company... } } }
```

One call instead of three — no need to separately fetch the trip or company after loading the lead.

### 5.3 Force a lead to any status

`PATCH /admin/leads/{id}/status`, body `{ "status": "DEPOSIT_PAID", "reason": "…" }` — **`reason` is
required and must be non-blank.**

**Read this before wiring up the button.** Every other way a lead's status changes goes through a
typed action (`REPORT_DEPOSIT`, `MARK_FULLY_PAID`, `CANCEL`, …) that the backend's transition table
proves is legal from wherever the lead currently sits — an illegal jump is *unrepresentable*, not
merely rejected. This endpoint is the one deliberate exception: it sets the status directly,
skipping that table entirely. It exists to correct the record (data entered by hand, a booking
migrated from before this system existed), not as a second, more convenient way to run the normal
workflow.

Consequences to build the UI around:
- **It does not move money.** Setting a lead to `COMMISSION_PAID` here does **not** create or update
  a commission ledger entry, and setting it to `CASHBACK_PAID` does **not** pay out a cashback. If
  the admin also wants the ledger to move, they still need the real `confirm-commission` /
  `pay-cashback` actions. Put a visible note on the confirm dialog saying so — this is the single
  easiest thing to get wrong.
- Still writes a full audit trail: a `lead_status_history` row (with `reason` as its note, visible in
  the lead's timeline like any other step) and an admin-specific audit log entry, and it notifies
  both the customer and the company that an operator changed their booking.
- **409** if the lead is already in that status.
- **409 with `code: "ACTIVE_LEAD_EXISTS"`** if reviving a `CANCELLED` or `CASHBACK_PAID` lead back to
  an active status would give the customer a second preserved journey (a customer may hold only one
  — see the lead lifecycle brief §0). The body names the lead in the way; the fix is to cancel that
  other one first, exactly like the customer-facing `ACTIVE_LEAD_EXISTS` case.
- **422** if `reason` is blank.

### 5.4 WhatsApp: compose a message to the customer, don't send one

```
GET /admin/leads/{id}/whatsapp/trip?lang=ar
GET /admin/leads/{id}/whatsapp/company?lang=ar
```

Both return the same shape — one button component covers both:

```json
{ "data": {
    "recipientName": "…", "recipientPhone": "+2010…",
    "message": "…full formatted text…",
    "link": "https://wa.me/2010…?text=…urlencoded…"
} }
```

- `link` opens WhatsApp with `message` already typed into the compose box — **the admin still has to
  press send.** Nothing is sent by this API call itself; there is no Meta Business account, no
  access token, no per-message cost, and no 24-hour-window restriction to worry about.
- `lang` defaults to **Arabic** (`ar`); pass `lang=en` for the English version. This is the first
  Arabic text anywhere in this backend — it exists only in these two composed messages, nowhere else
  in the API.
- The **trip** message: itinerary dates, airline and route, nights in Makkah/Madinah, both hotels
  with stars/distance/free-shuttle, included inclusions, room prices, and this lead's cashback
  amount. The **company** message: name, licence, rating, description, and every branch with its
  city and phone number.
- **422** if the customer has no usable phone number on file — surface it as "add a phone number for
  this customer first" rather than a generic error.
- Recommended UI: two buttons on the lead detail screen, "Send trip details" / "Send company
  details," each opening `link` in a new tab/window (or the device's WhatsApp app) after a quick
  preview of `message` so the admin can proofread before it's already typed into the chat.

---

## 6. Screen-by-screen sketch

- **Companies** — list (search + status filter) → detail (approve/reject/suspend/reinstate,
  commission, edit) → "+ New company" opening the create form from §3.1.
- **Trips** — list (search + company/status/tier/date filters, company column always visible) →
  detail/edit (same form as the company's own, hotel picker included) → a "Reassign company" action
  showing the existing-leads caveat from §4.3 before confirming → "+ New trip" starting with a
  company picker, then the same form.
- **Hotels** — a flat catalogue table (city, name, stars, distance, active) with inline
  activate/deactivate and a delete that surfaces the in-use count from §2 when refused. "+ New
  hotel" is a simple form.
- **Leads** — a filterable table (§5.1) → detail screen combining §5.2's embedded trip/company with
  the existing lifecycle stepper and action buttons from the customer/company brief, plus: the two
  WhatsApp buttons (§5.4) and a destructive "Override status" action (§5.3) behind its own
  confirmation dialog with a required reason field and the "this does not move money" notice.

---

## 7. Migration checklist

- [ ] Build the hotels catalogue screen and wire the trip form's hotel fields to `GET /hotels?city=`.
- [ ] Update the trip detail/request models for the nested hotel shape (§4.2, and
      `docs/flutter-integration-brief.md` §13) — this affects the customer app too if it shares models.
- [ ] Build the company create/edit/delete/reinstate screens; make the "no invite needed, just sign
      in with Google" behaviour visible in the create form's copy.
- [ ] Build the trip list/create/edit/reassign screens; put the existing-leads caveat in the
      reassign confirmation, not just in a tooltip.
- [ ] Build the lead console: filters, embedded detail, the two WhatsApp buttons, and the status
      override with its required reason and "does not move money" warning.
- [ ] Handle the `ACTIVE_LEAD_EXISTS` 409 on the status-override endpoint the same way the customer
      app handles it on `contact-company`.
