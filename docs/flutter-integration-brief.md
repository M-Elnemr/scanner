# Flutter integration brief — Lead / Commission / Cashback / Traveler redesign

Backend: Umrah Scanner (Spring Boot 4, `/api/v1`). This document describes a **breaking** API change.
Every endpoint and field below is live on the backend — nothing here is aspirational.

---

## 0. TL;DR for the app

1. The trip's primary CTA is **"Preserve the journey"** — client service reaches out to the customer,
   rather than the customer being handed a phone number. Tapping it reveals nothing on its own: the
   user must first pick **adults / children / infants**. Submitting that creates the *lead*, the
   button turns green **"Preserved"**, and only from then on does the trip endpoint return the
   company block **and** `GET /api/v1/companies/{id}` return the full profile (§7).
2. A customer may hold **one preserved journey at a time**. Preserving a second trip returns
   **409 `ACTIVE_LEAD_EXISTS`** naming the trip in the way; they must cancel that one first (§5.9).
   `GET /api/v1/customers/me/leads/active` tells you which trip is being held, so the button can be
   rendered correctly before the user taps anything.
3. Cancelling is a **new customer action**, `CANCEL`, legal from every status except `CASHBACK_PAID`,
   and it requires a reason. It moves the lead to the new **`CANCELLED`** status, frees the slot, and
   **takes the company's details away again**.
4. A lead permanently stores its **commission** and **cashback** at the moment it is created. Nothing
   recalculates them later. Customers must **never** be shown commission — only cashback.
5. The status flow is **8 progress states plus `CANCELLED`**, with a report → confirm handshake on
   each payment step.
6. The app must **never post a target status**. Each step is its own named endpoint. Render buttons
   from the `availableActions` array the server returns on every lead.

---

## 1. Endpoints that were DELETED

Remove these call sites entirely — they now 404/405.

| Gone | Replaced by |
|---|---|
| `PATCH /api/v1/companies/me/leads/{id}/status` | the four named company/admin endpoints in §5 |
| `PATCH /api/v1/customers/me/leads/{id}/confirm-payment` | `report-deposit` and `report-full-payment` |
| `PATCH /api/v1/admin/leads/{id}/release-commission` | `PATCH /api/v1/admin/leads/{id}/confirm-commission` |
| `PATCH /api/v1/admin/leads/{id}/send-cashback` | `PATCH /api/v1/admin/leads/{id}/pay-cashback` |

The old `LeadStatus` values are gone too: `COMPANY_CONTACTED`, `CUSTOMER_CONFIRMED`,
`PAYMENT_PENDING`, `COMPANY_MARKED_PAID`, `ADMIN_REVIEW`, `COMMISSION_RELEASED`, `CASHBACK_SENT`.
Delete every reference, including local enums, switch statements and status-label maps.

---

## 2. Envelopes and errors (unchanged, restated)

Every success body is wrapped:

```json
{ "data": { ... } }
```

Paginated:

```json
{ "data": { "content": [ ... ], "page": 0, "size": 20, "totalElements": 1, "totalPages": 1, "last": true } }
```

Errors are RFC 7807 `application/problem+json`:

```json
{ "type": "about:blank", "title": "Conflict", "status": 409, "detail": "Human readable reason", "instance": "" }
```

Bean-validation failures (400) add a `fieldErrors` map:

```json
{ "status": 400, "detail": "Request failed validation", "fieldErrors": { "adultCount": "At least one adult traveler is required" } }
```

Some failures add a stable `code` plus extra fields, so you can branch on the failure instead of
parsing English. Today there is exactly one — preserving a trip while another is already held:

```json
{
  "status": 409,
  "detail": "You already have a preserved journey. Cancel it before preserving another.",
  "code": "ACTIVE_LEAD_EXISTS",
  "activeLead": {
    "leadId": "…", "tripId": "…", "tripTitle": "September Umrah - 12 Nights", "status": "DEPOSIT_PAID"
  }
}
```

Branch on `code` when it is present; fall back to showing `detail`. Treat an unknown `code` as a
plain error — more will be added.

Show `detail` to the user. Status codes you must handle on lead calls:

| Code | Meaning | Suggested UI |
|---|---|---|
| 400 | field validation | inline field errors from `fieldErrors` |
| 403 | not your lead / wrong role | generic "not allowed", pop the screen |
| 404 | lead or trip gone | pop + refresh list |
| 409 | **action illegal from the current status**, or travelers locked, or already paid, or `ACTIVE_LEAD_EXISTS` | with a `code`, handle it (see above); otherwise refresh the lead and re-render from `availableActions` — the app's state was stale |
| 422 | domain rule (profile incomplete, trip unpublished, no wallet on file) | show `detail`, route to the fix (e.g. profile screen) |

Auth is unchanged: `Authorization: Bearer <accessToken>`.

---

## 3. The status flow

```
INTERESTED
  ├─ customer reports ──► PENDING_DEPOSIT_CONFIRMATION ──► (company confirms) ──► DEPOSIT_PAID
  └─ company records ───────────────────────────────────────────────────────────► DEPOSIT_PAID

DEPOSIT_PAID
  ├─ customer reports ──► PENDING_FULL_PAYMENT_CONFIRMATION ──► (company confirms) ──► FULLY_PAID
  └─ company records ────────────────────────────────────────────────────────────────► FULLY_PAID

FULLY_PAID
  ├─ company reports ───► PENDING_COMMISSION_CONFIRMATION ──► (admin confirms) ──► COMMISSION_PAID
  └─ admin records ────────────────────────────────────────────────────────────────► COMMISSION_PAID

COMMISSION_PAID ──► (admin pays) ──► CASHBACK_PAID   [terminal]

ANY of the above except CASHBACK_PAID ──► (customer cancels, reason required) ──► CANCELLED [terminal]
```

Dart enum — order matters for the eight progress states, it is the progress order:

```dart
enum LeadStatus {
  interested,                     // INTERESTED
  pendingDepositConfirmation,     // PENDING_DEPOSIT_CONFIRMATION
  depositPaid,                    // DEPOSIT_PAID
  pendingFullPaymentConfirmation, // PENDING_FULL_PAYMENT_CONFIRMATION
  fullyPaid,                      // FULLY_PAID
  pendingCommissionConfirmation,  // PENDING_COMMISSION_CONFIRMATION
  commissionPaid,                 // COMMISSION_PAID
  cashbackPaid,                   // CASHBACK_PAID
  cancelled,                      // CANCELLED — NOT on the ladder, see below
}
```

Use `index` comparisons for "has it got at least this far" checks (e.g. review eligibility), never
string matching. Treat an unrecognised status string as a non-fatal fallback rather than throwing —
the backend may add states.

> ⚠️ **`cancelled` is off the ladder.** It is listed last only because Dart enums need an order; it
> does not mean "further along than `cashbackPaid`". Every progress comparison must exclude it
> first — `if (status == LeadStatus.cancelled) { … } else if (status.index >= …)`. The backend
> mirrors this: `CANCELLED` carries a negative stage server-side for exactly this reason. A
> cancelled lead is finished: it has no `availableActions`, its travelers cannot be edited, and it
> no longer entitles the customer to the company's contact details.

**Suggested user-facing copy** (customer side):

| Status | Customer sees | Company sees |
|---|---|---|
| `INTERESTED` | "Awaiting your deposit" | "New enquiry" |
| `PENDING_DEPOSIT_CONFIRMATION` | "Deposit reported — awaiting company confirmation" | "Confirm the customer's deposit" |
| `DEPOSIT_PAID` | "Deposit confirmed" | "Deposit confirmed" |
| `PENDING_FULL_PAYMENT_CONFIRMATION` | "Full payment reported — awaiting confirmation" | "Confirm the full payment" |
| `FULLY_PAID` | "Paid in full — cashback pending" | "Settle your commission" |
| `PENDING_COMMISSION_CONFIRMATION` | "Cashback being processed" | "Commission reported — awaiting platform" |
| `COMMISSION_PAID` | "Cashback on its way" | "Commission confirmed" |
| `CASHBACK_PAID` | "Cashback sent 🎉" | "Complete" |
| `CANCELLED` | "Journey cancelled" | "Customer cancelled" |

---

## 4. Drive the UI from `availableActions` — do not hardcode

Every lead response carries the exact set of actions **the calling role** may perform **right now**:

```json
"availableActions": ["REPORT_DEPOSIT"]
```

Map action → button, and render only what is present. This is the single most important integration
rule: it removes any need for the app to replicate the state machine, and it survives future backend
workflow changes without an app release.

```dart
const _actionLabels = {
  'REPORT_DEPOSIT':          'I paid the deposit',
  'MARK_DEPOSIT_PAID':       'Confirm deposit received',
  'REPORT_FULL_PAYMENT':     'I paid in full',
  'MARK_FULLY_PAID':         'Confirm full payment',
  'REPORT_COMMISSION_PAID':  'I paid the commission',
  'CONFIRM_COMMISSION_PAID': 'Confirm commission',   // admin
  'PAY_CASHBACK':            'Send cashback',        // admin
  'CANCEL':                  'Cancel this journey',  // customer only
};
```

An empty `availableActions` means "nothing for you to do — wait for the other side". Show a status
chip, not a disabled button.

`CANCEL` is present for the customer on every status except `CASHBACK_PAID`, so it will appear
alongside the step button (e.g. `["REPORT_DEPOSIT", "CANCEL"]`). Give it destructive styling and a
confirm dialog rather than putting it next to the primary action — and remember it needs a reason
(§5.9), unlike every other action where the note is optional.

---

## 5. Endpoint reference

### 5.1 Trip details — cashback, four airports, currency object, company gate

`GET /api/v1/trips/{id}` — public; send the token if the user is logged in.

New field `cashbackPerTraveler`. The `company` object is **`null` until this customer has a lead on
this trip that they have not cancelled**; that is the reveal gate described in §6. It goes back to
`null` after a cancel, so re-fetch the trip whenever a lead's status changes.

**Breaking:** `departureAirport`/`arrivalAirport` (strings) are replaced by **four airport objects**
covering both legs, and `currency` is now an object rather than a 3-letter string. There is also a
new `fastTrainIncluded` flag for the include list. See §12.

```json
{
  "data": {
    "id": "…", "tripCode": "UMR-001", "title": "Ramadan Umrah Package - 10 Nights",
    "departureDate": "2026-03-01", "returnDate": "2026-03-11",
    "outboundDepartureAirport": { "id": "…", "iataCode": "CAI", "name": "Cairo International Airport",
                                  "city": "Cairo", "countryId": "…", "countryName": "Egypt", "countryIso2": "EG" },
    "outboundArrivalAirport":   { "id": "…", "iataCode": "JED", "name": "King Abdulaziz International Airport",
                                  "city": "Jeddah", "countryId": "…", "countryName": "Saudi Arabia", "countryIso2": "SA" },
    "returnDepartureAirport":   { "id": "…", "iataCode": "MED", "city": "Madinah", "countryIso2": "SA", "…": "…" },
    "returnArrivalAirport":     { "id": "…", "iataCode": "CAI", "city": "Cairo",   "countryIso2": "EG", "…": "…" },
    "airline": "EgyptAir",
    "transitCount": 0, "transitCity": null, "transitDuration": null,
    "daysInMakkah": 6, "daysInMadinah": 4,
    "visaIncluded": true, "transportationIncluded": true, "mealsIncluded": true,
    "guideIncluded": true, "zamzamIncluded": true, "fastTrainIncluded": true,
    "description": "…",
    "currency": { "id": "…", "code": "EGP", "name": "Egyptian Pound", "symbol": "E£" },
    "availableSeats": 20,
    "status": "PUBLISHED", "tier": "PREMIUM",

    "cashbackPerTraveler": 500.00,

    "lastUpdate": "2026-07-20T08:00:00Z",
    "hotels": [ … ], "roomPrices": [ … ],

    "company": null
  }
}
```

Once a lead exists, `company` becomes:

```json
"company": { "companyId": "…", "companyName": "Al Noor Travel & Tourism", "whatsapp": "+201000000000", "logoUrl": "…" }
```

> **Show `cashbackPerTraveler` prominently on the program page** — "Earn 500 EGP cashback per
> traveller". Multiply by the adult count in the picker for a live total.
>
> **Never display commission to a customer.** It is not in this payload and must not be derived.

### 5.2 Create the lead (the traveler picker submit)

`POST /api/v1/trips/{tripId}/contact-company` — role `CUSTOMER` → **201**

```json
{ "adultCount": 2, "childCount": 1, "infantCount": 0 }
```

Rules: `adultCount >= 1`, `childCount >= 0`, `infantCount >= 0`.

**Idempotent for the same trip.** Calling it again for the trip the customer is already holding
returns the existing lead rather than erroring — safe to retry on timeout. If the lead is still
before `FULLY_PAID` the counts are refreshed; otherwise the existing counts stand and the call still
succeeds.

**But only one trip at a time.** If the customer is holding a *different* trip, this returns
`409` with `code: "ACTIVE_LEAD_EXISTS"` and an `activeLead` object (§2). Show the warning naming
`activeLead.tripTitle`, and offer to cancel it (§5.9) and retry. Check
`GET /api/v1/customers/me/leads/active` (§5.10) when the trip screen loads so you can warn *before*
the user fills in the picker rather than after.

A previously **cancelled** trip can be preserved again: the call succeeds and returns a **new**
`leadId`, priced at today's rates. Do not assume the lead id for a trip is stable across a cancel.

Failure modes: `422 "Complete your profile before contacting a company"` → route to profile;
`422 "This trip is not available"` → trip unpublished, pop and refresh.

### 5.3 Lead payload (customer projection)

```json
{
  "data": {
    "id": "…", "customerId": "…",
    "tripId": "…", "tripTitle": "Ramadan Umrah Package - 10 Nights",
    "companyId": "…", "companyName": "Al Noor Travel & Tourism",
    "customer": { "id": "…", "fullName": "Ahmed Hassan", "phone": "+201234567890" },
    "status": "INTERESTED",

    "adultCount": 2, "childCount": 1, "infantCount": 0,
    "travelerCount": 2,
    "travelersEditable": true,

    "cashbackAmount": 1000.00,
    "commissionAmount": null,
    "commissionPerTraveler": null,

    "confirmedRoomType": null, "confirmedPrice": null,

    "availableActions": ["REPORT_DEPOSIT"],

    "audit": {
      "depositReportedBy": null,  "depositReportedAt": null,
      "depositConfirmedBy": null, "depositConfirmedAt": null,
      "fullPaymentReportedBy": null,  "fullPaymentReportedAt": null,
      "fullPaymentConfirmedBy": null, "fullPaymentConfirmedAt": null,
      "commissionReportedBy": null, "commissionReportedAt": null,
      "commissionPaidBy": null,     "commissionPaidAt": null,
      "cashbackPaidBy": null,       "cashbackPaidAt": null
    },

    "createdAt": "2026-07-20T08:00:00Z",
    "updatedAt": "2026-07-20T08:30:00Z"
  }
}
```

Field notes:

- **`travelerCount`** = **`adultCount`**. It is the *billable* count: commission and cashback are
  charged per adult; children and infants are free. Label it "travellers charged" or similar, and
  show the full party (`adultCount + childCount + infantCount`) separately if you need a head count.
- **`commissionAmount` / `commissionPerTraveler` are always `null` for customers** — the server
  redacts them. They are populated for `COMPANY` and `ADMIN` callers. Do not build a UI that assumes
  they are present.
- **`travelersEditable`** — see §5.5 and §9. Gate the edit affordance on this boolean, never on the status.
- **`audit`** — use for a timeline widget ("You reported the deposit on 20 Jul, confirmed 21 Jul").
  All fields are nullable until the corresponding step happens.

Same shape for company/admin, with the commission fields filled in and role-appropriate
`availableActions`.

### 5.4 Lists and single reads

| Method | Path | Role | Notes |
|---|---|---|---|
| `GET` | `/api/v1/customers/me/leads` | CUSTOMER | `?status=&page=&size=` — paginated |
| `GET` | `/api/v1/companies/me/leads` | COMPANY | same params |
| `GET` | `/api/v1/admin/leads` | ADMIN | same params |
| `GET` | `/api/v1/leads/{id}` | any | **new** — projected for the caller's role |
| `GET` | `/api/v1/leads/{id}/history` | any | append-only transition log |

`GET /api/v1/leads/{id}` is the one to call after any action if you want a clean refresh, though
every action endpoint already returns the updated lead — prefer using that response directly.

History payload:

```json
{ "data": [
  { "fromStatus": null, "toStatus": "INTERESTED", "changedBy": "…", "changedAt": "…", "note": null },
  { "fromStatus": "INTERESTED", "toStatus": "PENDING_DEPOSIT_CONFIRMATION", "changedBy": "…", "changedAt": "…", "note": "Transferred via InstaPay, ref 88213." }
] }
```

### 5.5 Edit traveler counts

`PUT /api/v1/customers/me/leads/{id}/travelers` — role `CUSTOMER`

```json
{ "adultCount": 3, "childCount": 1, "infantCount": 1 }
```

- Allowed **only while `travelersEditable == true`** (backend: before `FULLY_PAID` — see §9).
- **Does not reprice the lead.** `commissionAmount` and `cashbackAmount` stay exactly as fixed at
  creation. Make this explicit in the UI: *"Changing traveller numbers will not change your cashback
  — it was fixed when you contacted the company."* Otherwise users will expect the cashback to move
  and file bugs.
- `409 "Traveler counts cannot be changed once the booking is paid in full"` if it has locked, or
  `409 "Traveler counts cannot be changed on a cancelled journey"` once cancelled. Gate the affordance
  on `travelersEditable`, which covers both, rather than comparing statuses.

### 5.6 Lifecycle actions

All are `PATCH`. The body is **optional** — send it only when you have a note to attach:

```json
{ "note": "Transferred via InstaPay, ref 88213." }
```

All return the updated lead in the standard envelope.

| Role | Path | Action | From → To |
|---|---|---|---|
| CUSTOMER | `/api/v1/customers/me/leads/{id}/report-deposit` | `REPORT_DEPOSIT` | `INTERESTED` → `PENDING_DEPOSIT_CONFIRMATION` |
| CUSTOMER | `/api/v1/customers/me/leads/{id}/report-full-payment` | `REPORT_FULL_PAYMENT` | `DEPOSIT_PAID` → `PENDING_FULL_PAYMENT_CONFIRMATION` |
| COMPANY | `/api/v1/companies/me/leads/{id}/deposit-paid` | `MARK_DEPOSIT_PAID` | `INTERESTED` **or** `PENDING_DEPOSIT_CONFIRMATION` → `DEPOSIT_PAID` |
| COMPANY | `/api/v1/companies/me/leads/{id}/fully-paid` | `MARK_FULLY_PAID` | `DEPOSIT_PAID` **or** `PENDING_FULL_PAYMENT_CONFIRMATION` → `FULLY_PAID` |
| COMPANY | `/api/v1/companies/me/leads/{id}/report-commission-paid` | `REPORT_COMMISSION_PAID` | `FULLY_PAID` → `PENDING_COMMISSION_CONFIRMATION` |
| ADMIN | `/api/v1/admin/leads/{id}/confirm-commission` | `CONFIRM_COMMISSION_PAID` | `FULLY_PAID` **or** `PENDING_COMMISSION_CONFIRMATION` → `COMMISSION_PAID` |
| ADMIN | `/api/v1/admin/leads/{id}/pay-cashback` | `PAY_CASHBACK` | `COMMISSION_PAID` → `CASHBACK_PAID` |
| CUSTOMER | `/api/v1/customers/me/leads/{id}/cancel` | `CANCEL` | anything except `CASHBACK_PAID` → `CANCELLED` — **note required**, see §5.9 |

Note the company's two "or" rows: **one button covers both confirming the customer's report and
recording the payment first**. Label it contextually from the current status
("Confirm deposit" vs "Mark deposit received") but call the same endpoint.

`409` on any of these means the app's copy of the lead was stale. Re-fetch and re-render.

### 5.7 Reviews — gate moved earlier

`POST /api/v1/leads/{id}/rating` — role `CUSTOMER` → **201**

```json
{ "stars": 5, "comment": "Excellent service" }
```

Now allowed from **`DEPOSIT_PAID` onwards** (it used to require the whole payout chain to finish).
Gate the review CTA on `status.index >= LeadStatus.depositPaid.index`. One review per lead;
`409` if already reviewed, `422` if too early.

### 5.8 Admin: company commission

`PATCH /api/v1/admin/companies/{id}/commission` — role `ADMIN`

```json
{ "commissionPerTraveler": 2000.00 }
```

Returns the full company object. Applies to **future** leads only. Admin-only — the company profile
screen must render `commissionPerTraveler` **read-only**; there is no company-facing write path and
attempting one returns 403.

`GET /api/v1/companies/me` now includes `commissionPerTraveler` for the company's own profile view.

### 5.9 Cancel a preserved journey

`PATCH /api/v1/customers/me/leads/{id}/cancel` — role `CUSTOMER` → **200**

```json
{ "note": "Changed my travel dates." }
```

The body is **required here**, unlike every other action in §5.6, and `note` must be non-blank —
a blank or missing reason is `422 "Tell us why you are cancelling this journey"`. Max 500 chars.
Collect it in the confirm dialog.

Returns the updated lead with `status: "CANCELLED"` and an empty `availableActions`.

What changes on the server, so the app knows what to invalidate:

- the customer's preserved-trip slot is freed — they may immediately preserve another trip;
- `GET /trips/{id}` for that trip returns `company: null` again, and `GET /companies/{id}` goes back
  to `403` unless another live lead with that company exists. **Pop any open company screen** and
  refresh the trip;
- traveler counts freeze (`travelersEditable: false`);
- any commission the company owed the platform on this lead is voided;
- the company is notified, and admins too if the lead had reached `DEPOSIT_PAID` or beyond.

`409` means it is already cancelled, or the cashback has already been paid. Re-fetch the lead.

**Money already paid is not refunded by the platform.** Cancelling a lead at `DEPOSIT_PAID` or later
is allowed, but the refund is between the customer and the company. Say so in the confirm dialog
when `status` is at least `DEPOSIT_PAID` — do not present it as a clean undo.

### 5.10 The journey I am currently holding

`GET /api/v1/customers/me/leads/active` — role `CUSTOMER` → **200**

```json
{ "data": null }
```

…or the full lead payload of §5.3 when one is held. `null` data is the normal empty case, not an
error. Cancelled and `CASHBACK_PAID` leads never appear here — both free the slot.

This is the one call that drives the trip-details button (§6.1). Fetch it with the trip detail:

| `active` | `activeLead.tripId == thisTrip` | Button |
|---|---|---|
| null | — | grey **"Preserve the journey"** → opens the traveler picker |
| present | yes | green **"Preserved"** → opens the lead screen; cancel lives there |
| present | no | **"Preserve the journey"**, but tapping warns first, naming `activeLead.tripTitle` |

---

## 6. Screen-by-screen

### 6.1 Program details → traveler picker → preserved

```
[Trip details screen]
   shows: itinerary, hotels, room prices, cashbackPerTraveler
   company section: locked placeholder
   on load: GET /trips/{id}  +  GET /customers/me/leads/active
        │
        ├── active == null ────────────► grey  [ Preserve the journey ]
        ├── active.tripId == this trip ► green [ ✓ Preserved ] ──► lead screen
        └── active.tripId == other ────► grey  [ Preserve the journey ] but tap → warning ↓
        │
        │  user taps "Preserve the journey"
        ▼
[Warning dialog — only when another trip is held]
   "You are already preserving <activeLead.tripTitle>. Cancel it to preserve this one instead."
   [Keep the current one]   [Cancel it and continue]
        │                          │
        │                          │  PATCH /customers/me/leads/{activeLead.leadId}/cancel { note }
        │                          ▼
        └──────────────────────────┴──► continue to the picker
        ▼
[Traveler picker sheet]   adults (min 1) · children · infants
   live preview: "Cashback: cashbackPerTraveler × adults"
        │
        │  POST /trips/{tripId}/contact-company  { adultCount, childCount, infantCount }
        ▼
[201 → journey preserved]
   button turns green [ ✓ Preserved ]
   copy: "Client service will reach you soon."
   re-fetch GET /trips/{id}  → `company` is now populated
   GET /api/v1/companies/{companyId} → full profile: branches, licence, rating, description
   deep-link to the new lead
```

Implementation notes:

- The button's three states come from **one** call, `GET /customers/me/leads/active` (§5.10) — do not
  infer them from whether `company` is populated on the trip, which stays true for a completed
  journey the customer is no longer holding.
- Still handle `409 ACTIVE_LEAD_EXISTS` on the submit even though you checked up front: the slot may
  have been taken on another device between the two calls. The 409 carries the same `activeLead`
  object, so route it into the same warning dialog.
- The submit is idempotent **for the trip already held**, so a user who backs out and retaps simply
  re-enters the picker and the same lead comes back. Do not show "you already contacted this
  company" as an error.
- Guard the picker behind a completed profile check to avoid a 422 round trip, but still handle the
  422 (profile can be completed on another device).
- Invalidate the trip screen and any cached company profile after a cancel — both go back to locked.

### 6.2 Lead detail screen (customer)

Sections: status stepper (8 states, current highlighted) · traveler counts with an **Edit** affordance
shown only when `travelersEditable` · cashback amount · action buttons from `availableActions` ·
timeline from `audit` · review CTA once `status >= DEPOSIT_PAID` · company card backed by
`GET /api/v1/companies/{companyId}`.

When `status == CANCELLED`, replace the stepper with a terminal "Journey cancelled" state (the
stepper's progress order does not apply — see §3), drop the company card, and offer "Preserve
another journey" back to browse. `availableActions` is empty, so the action row disappears on its
own. The `CANCEL` button lives here, at the bottom, styled destructively — see §5.9 for the required
reason and the refund caveat.

### 6.3 Lead detail screen (company)

Same skeleton, plus `commissionAmount` and `commissionPerTraveler`, and company-side
`availableActions`. Include a "what you owe the platform" line once the lead is `FULLY_PAID`.
Title each lead with `customer.fullName` and make `customer.phone` a tap-to-call / WhatsApp action —
see §8.

---

## 7. Company profile for the customer

`GET /api/v1/companies/{id}` — any authenticated user.

**Authorisation:** a customer may read this **only while they hold a lead with that company that
they have not cancelled**. Before that — and again after cancelling — it returns
`403 "Contact this company about a trip to see its full details"`. A company can always read its
own; admins can read any. This is the same reveal rule that already gates company identity on the
trip screen, extended to the full profile.

Note the asymmetry with the customer's preserved-trip slot: a **completed** (`CASHBACK_PAID`)
journey no longer occupies the slot but *does* still grant access to the company, so a past
traveller keeps their operator's contact details. Only cancelling withdraws access.

```json
{
  "data": {
    "id": "…",
    "companyName": "Al Noor Travel & Tourism",
    "licenseNumber": "TRV-2024-8891",
    "logoUrl": "http://…/uploads/logos/al-noor.png",
    "whatsapp": "+201000000000",
    "description": "Licensed Umrah operator since 2009.",
    "ratingAvg": 4.50,
    "ratingCount": 12,
    "addresses": [
      { "id": "…", "cityId": "…", "cityName": "Cairo",
        "addressText": "12 Talaat Harb St, Downtown", "mobileNumber": "+201000000001" }
    ]
  }
}
```

Notes:

- `addresses` is the **branch list**; each branch carries its own city and its own `mobileNumber`.
  Render it as a list of branches, not a single address line.
- This is a **different shape** from the company's own `GET /api/v1/companies/me`. It deliberately
  omits `status`, `rejectionReason`, approval metadata and `commissionPerTraveler` — generate a
  separate `PublicCompany` model rather than reusing the company-profile model with nullable fields.
- Pair with `GET /api/v1/companies/{id}/ratings` (public, paginated) for the reviews list.
- Use this for **both** the post-picker company reveal (§6.1) and the company card on the lead
  screen (§6.2). The trip detail's small `company` block is still fine for a compact header.

> Route ordering note: `/api/v1/companies/me` is a separate, `COMPANY`-only endpoint. Never
> substitute the literal `me` into this path.

## 8. Customer details on the lead

`LeadResponse` now carries a `customer` block, present for **all three roles**:

```json
"customer": { "id": "…", "fullName": "Ahmed Hassan", "phone": "+201234567890" }
```

This is what lets the company screen show and contact the client behind a lead — surface `fullName`
as the lead's title and `phone` as a tap-to-call / WhatsApp action.

`cashbackWalletNumber` and `walletType` are deliberately **not** included: the payout wallet is
between the customer and the platform, and the company has no reason to see it. Do not expect them
here.

Both fields are nullable in practice (a profile can be incomplete), so guard the UI.

## 9. Traveler-edit cut-off — resolved

Travellers are editable up to but not including **`FULLY_PAID`**, matching the product intent. So
`INTERESTED`, `PENDING_DEPOSIT_CONFIRMATION`, `DEPOSIT_PAID` and
`PENDING_FULL_PAYMENT_CONFIRMATION` all allow edits; everything from `FULLY_PAID` onwards is frozen.

Still: **bind the edit affordance to the `travelersEditable` boolean, never to a status comparison.**
The cut-off is one constant on the backend and may move again; reading the flag means no client
release when it does.

`409 "Traveler counts cannot be changed once the booking is paid in full"` if you attempt it late.

---

## 10. Migration checklist for the app

- [ ] Delete the 4 removed endpoints and the 7 removed status values, including local enums/labels.
- [ ] Regenerate the lead model: traveler counts, `travelerCount`, `travelersEditable`,
      `cashbackAmount`, nullable `commissionAmount`/`commissionPerTraveler`, `availableActions`, `audit`.
- [ ] Make `commissionAmount` nullable in the Dart model — it is `null` on every customer response.
- [ ] Add `cashbackPerTraveler` to the trip detail model.
- [ ] Replace the two airport strings with four `Airport` objects; render both legs (§12.2).
- [ ] Replace the `currency` string with a `Currency` object; format prices from its symbol (§12.4).
- [ ] Add `fastTrainIncluded` to the include list (§12.5).
- [ ] Scope the trip-form airport pickers with `GET /api/v1/airports?countryId=` (§12.2).
- [ ] Add `commissionPerTraveler` (read-only) to the company profile model.
- [ ] Add a `PublicCompany` model + `GET /api/v1/companies/{id}` client call (§7).
- [ ] Add the `customer { id, fullName, phone }` block to the lead model; use it on company screens (§8).
- [ ] Replace the status-picker call with the seven named action endpoints.
- [ ] Render all lifecycle buttons from `availableActions`.
- [ ] Add the traveler picker as a mandatory gate before the company reveal.
- [ ] Move the review CTA gate to `DEPOSIT_PAID`.
- [ ] Handle 409 on every action by re-fetching the lead.
- [ ] Copy for "changing travellers does not change your cashback".
- [ ] Handle 403 on the company profile call as "contact the company first", not as an auth error.
- [ ] Push-notification `type` strings changed — see §11.
- [ ] Rename the trip CTA to **"Preserve the journey"**; add the green **"Preserved"** state and the
      "client service will reach you soon" copy (§6.1).
- [ ] Add `CANCELLED` to the status enum **and audit every `index`/`>=` comparison** so it is
      excluded first — it is not a progress state (§3).
- [ ] Call `GET /customers/me/leads/active` on the trip screen to drive the button's three states (§5.10).
- [ ] Handle `409 ACTIVE_LEAD_EXISTS` with the "cancel your current journey first" dialog, reading
      `activeLead.tripTitle` from the error body (§2).
- [ ] Add the cancel flow: destructive button, confirm dialog with a **required** reason, refund
      caveat once `status >= DEPOSIT_PAID` (§5.9).
- [ ] Invalidate the trip detail and company profile caches after a cancel — both re-lock.
- [ ] Stop assuming a trip's `leadId` is stable: re-preserving a cancelled trip yields a new lead.

---

## 11. Notification types

In-app notification rows (and future FCM pushes) carry a `type` plus a `data` map with `leadId` and
`tripId`. Handle these; route each to the lead screen.

| `type` | Recipient | Trigger |
|---|---|---|
| `NEW_LEAD` | company, admin | customer contacted the company — admins get their own title/body ("حجز جديد") naming the company, same `type` and `data` shape |
| `DEPOSIT_CONFIRMATION_REQUIRED` | company | customer reported the deposit |
| `DEPOSIT_CONFIRMED` | customer | company confirmed the deposit |
| `FULL_PAYMENT_CONFIRMATION_REQUIRED` | company | customer reported full payment |
| `FULL_PAYMENT_CONFIRMED` | customer | company confirmed full payment |
| `COMMISSION_CONFIRMATION_REQUIRED` | admin | company reported paying its commission |
| `COMMISSION_PAID` | company | admin confirmed the commission |
| `CASHBACK_PAID` | customer | admin sent the cashback |
| `COMMISSION_RATE_UPDATED` | company | admin changed the per-traveller rate |
| `LEAD_CANCELLED` | company | customer cancelled their preserved journey |
| `PAID_LEAD_CANCELLED` | admin | the cancelled journey had reached `DEPOSIT_PAID` or beyond — a refund needs following up |

Old types `LEAD_ADMIN_REVIEW`, `COMMISSION_RELEASED` and `CASHBACK_SENT` no longer occur.

---

## 12. Airports, currencies and the fast train

Three pieces of free text on a trip became reference data. This is **breaking** for any screen that
reads a trip.

### 12.1 New public lookup endpoints

| Method | Path | Returns |
|---|---|---|
| `GET` | `/api/v1/countries` | `[{ id, name, iso2 }]` — Egypt and Saudi Arabia |
| `GET` | `/api/v1/airports?countryId=` | `[{ id, iataCode, name, city, countryId, countryName, countryIso2 }]` |
| `GET` | `/api/v1/currencies` | `[{ id, code, name, symbol }]` — EGP, SAR, USD |

All three are public (no token) and are fixed lists — fetch once and cache for the session.

Airports currently seeded: **Egypt** — Cairo (CAI), Borg El Arab (HBE), Assiut (ATZ), Luxor (LXR).
**Saudi Arabia** — Jeddah (JED), Madinah (MED).

### 12.2 A trip now has four airports, not two

A round trip has two legs, so it has four airports. The return leg is stored explicitly rather than
assumed to mirror the outbound, which means a company can sell *out to Jeddah, home from Madinah*:

| Field | Country | Meaning |
|---|---|---|
| `outboundDepartureAirport` | Egypt | leg 1 origin |
| `outboundArrivalAirport` | Saudi Arabia | leg 1 destination |
| `returnDepartureAirport` | Saudi Arabia | leg 2 origin — **not necessarily** where they landed |
| `returnArrivalAirport` | Egypt | leg 2 destination |

**Company trip form — this is the filtering rule you asked for.** Load countries once, then scope each
picker with `?countryId=`:

- outbound **departure** → Egyptian airports
- outbound **arrival** → Saudi airports
- return **departure** → Saudi airports
- return **arrival** → Egyptian airports

The server enforces the same shape and returns `422` with a readable `detail` if it is violated
(e.g. *"The return flight must depart from Saudi Arabia, the country the outbound flight arrives in"*).
Do not hardcode the country names client-side — read `countryIso2` off the airport objects, so adding
a third country later needs no app change.

**Customer trip screen:** render the itinerary as two legs. Showing only the outbound pair hides the
open-jaw case, which is exactly the information a pilgrim needs.

### 12.3 Create / update trip — request changes

Replace in `POST /api/v1/companies/me/trips` and `PUT /api/v1/companies/me/trips/{id}`:

```jsonc
// removed
"departureAirport": "CAI",
"arrivalAirport": "JED",
"currency": "EGP",

// added — all five are required (UUIDs from the lookup endpoints)
"outboundDepartureAirportId": "…",
"outboundArrivalAirportId":   "…",
"returnDepartureAirportId":   "…",
"returnArrivalAirportId":     "…",
"currencyId":                 "…",
"fastTrainIncluded":          true
```

`currencyId` is now **required** — there is no server-side default to EGP any more. The company picks
the currency per trip.

### 12.4 Currency is an object

`trip.currency` is `{ id, code, name, symbol }` instead of `"EGP"`. Format prices with `symbol` (or
`code`) from the trip itself — never a hardcoded `E£`, since the same list can now hold SAR and USD
trips side by side. Room prices carry no currency of their own; they inherit the trip's.

There are no exchange rates and nothing converts between currencies: a price is quoted and paid in
the trip's own currency.

### 12.5 `flightNumber` is removed

A trip describes two legs but only ever held one flight number, so it could be right for at most
one of them. It is gone from every trip payload and from the create/update requests — delete the
field from your models and any UI that renders it. The airline, the four airports and the transit
details still describe the journey.

### 12.6 Fast train

`fastTrainIncluded` (boolean) joins `visaIncluded`, `transportationIncluded`, `mealsIncluded`,
`guideIncluded` and `zamzamIncluded` in the include list. It means the **Haramain high-speed rail
between Makkah and Madinah** is in the package price — worth its own icon, travellers care about it.

---

## 13. Hotels — now a catalogue, not free text (BREAKING)

Hotels used to be typed out by hand on every trip. They are now a platform-curated catalogue a
company **picks from** — the same shift that airports and currencies went through earlier in this
brief. This changes both the read and the write shape.

### Read: `TripHotel` is nested, not flat

```jsonc
{
  "city": "MAKKAH",
  "hotel": {
    "id": "d78ef4f0-…",
    "city": "MAKKAH",
    "name": "Swissotel Al Maqam Makkah",
    "nameAr": null,
    "stars": 5,
    "distanceToHaramM": 350,
    "canWalk": true,
    "locationUrl": "https://maps.google.com/?q=Swissotel+Al+Maqam+Makkah",
    "active": true
  },
  "freeBusIncluded": false
}
```

Field-by-field migration for the model: `hotelName` → `hotel.name`, `stars` → `hotel.stars`,
`distanceToHaramM` → `hotel.distanceToHaramM`, `canWalk` → `hotel.canWalk`, `locationUrl` →
`hotel.locationUrl`. New: `hotel.id`, `hotel.nameAr` (nullable — not every hotel has a translation
yet), `hotel.active`. `city` and `freeBusIncluded` are unchanged, still flat on the outer object.

As before, `distanceToHaramM`/`canWalk` mean **distance to the Prophet's Mosque**, not the Haram, on
the `MADINAH` row — label per row rather than reusing one string. `freeBusIncluded` stays independent
of `canWalk`: a company can offer both, neither, or just one.

### Write: the company's trip form becomes a picker, not a text field

`POST/PUT` on a trip's `hotels` array now sends:

```jsonc
{ "hotelId": "d78ef4f0-…", "freeBusIncluded": false }
```

No `city`, no `hotelName`, no `stars` — the city is derived from the hotel you picked (the server
rejects the same city twice), and every other fact belongs to the hotel record, not the trip.

**New endpoint, needed before this form works at all:**

`GET /api/v1/hotels?city=MAKKAH` — public, no auth required. Returns every **active** hotel in that
city as `HotelResponse` (the same shape embedded above). Omit `city` for the full list. This is the
picker's data source — replace the old free-text hotel-name field with an autocomplete/dropdown
bound to this call, scoped to whichever city slot (Makkah/Madinah) the company is filling in.

A hotel that has been retired (`active: false`) never appears here, so it silently drops out of the
picker for new trips without touching trips that already reference it.

**Checklist:**
- [ ] Replace the hotel-name text field on the trip form with a picker backed by `GET /hotels?city=`.
- [ ] Update the trip detail model: nest the nine hotel fields under `hotel`, keep `city` and
      `freeBusIncluded` flat.
- [ ] Update the create/update trip request: send `{ hotelId, freeBusIncluded }` per hotel, not the
      old six-field shape.
- [ ] Handle 404 `"Hotel not found"` and 422 `"<name> is no longer offered"` on trip save — a hotel
      picked earlier may have been retired since.

---

## 14. Arabic — switch language on countries, airports, currencies and cities

Every response from the four fixed reference lists now carries **both** languages in the same
payload — no `Accept-Language` header, no locale query param, no second request. Pick whichever
field the app's current language setting calls for at render time, and switching language in the
app is instant with zero network round-trips for this data.

| Endpoint | New fields |
|---|---|
| `GET /api/v1/countries` | `nameAr` alongside `name` |
| `GET /api/v1/airports` | `nameAr`, `cityAr` alongside `name`, `city` (each airport also nests `countryNameAr`) |
| `GET /api/v1/currencies` | `nameAr` alongside `name` — `code` and `symbol` are **not** localized, a currency symbol reads the same in either language |
| `GET /api/v1/cities` | `nameAr` alongside `name` |
| Company address objects (`addresses[]` wherever they appear — `/companies/me`, `/companies/{id}`, admin company routes) | `cityNameAr` alongside `cityName`, since an address's city comes from the same cities table |

```json
{
  "id": "…", "iataCode": "CAI",
  "name": "Cairo International Airport", "nameAr": "مطار القاهرة الدولي",
  "city": "Cairo", "cityAr": "القاهرة",
  "countryId": "…", "countryName": "Egypt", "countryNameAr": "مصر", "countryIso2": "EG"
}
```

**What this does *not* cover:** enum values — `TripStatus`, `TripTier`, `RoomType`, `LeadStatus`,
`LeadAction`, the `MAKKAH`/`MADINAH` hotel `city` enum, and so on — are still plain codes, unchanged.
Those were already meant to be localized client-side with your own string tables (that's how enums
work everywhere in this API), so nothing about them changed here. This section is specifically the
four *fixed lookup tables* — free-text reference data that had no client-side translation to fall
back on.

**Checklist addition:**
- [ ] Add `nameAr` to the Country, Airport (plus `cityAr`), Currency and City models.
- [ ] Add `cityNameAr` to the company-address model.
- [ ] Wire the language toggle to pick `name`/`nameAr` (and `city`/`cityAr`) instead of hardcoding
      English everywhere these four lists are rendered: the airport pickers, the currency picker,
      the company address form, and the tour details itinerary line from §12.3/§13.

---

## 15. Reference

- Swagger UI: `/swagger-ui.html` (bearer auth configured; the lead lifecycle is described on the
  landing page).
- Postman: `postman/Umrah-Scanner.postman_collection.json` — the **Lead** folder walks one lead
  through all seven numbered steps in order and is the fastest way to see real payloads.
- Seed data: `src/main/resources/db/seed/dev-seed.sql` gives the dev database 5 companies with
  10 published trips each (50 trips, real Egyptian governorates, real airlines and Makkah/Madinah
  hotels, prices spanning ECONOMIC/PREMIUM/VIP and off-peak through Ramadan). Every company has a
  different `commissionPerTraveler`, so `cashbackPerTraveler` varies between 300 and 625 EGP across
  the catalogue — useful for checking the UI does not hardcode a value. Sign in as any of them in
  the dev profile with `POST /api/v1/auth/dev-google-test`, e.g.
  `{"email": "nour-al-haram@seed.test", "role": "COMPANY"}`.
