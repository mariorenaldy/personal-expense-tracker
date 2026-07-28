# Design Decisions

## 1. Money as NUMERIC(12,2), not FLOAT
Floating-point math produces rounding errors (0.1 + 0.2 ≠ 0.3), which is unacceptable for currency. 
Maps to BigDecimal in Java.

## 2. expenses carries user_id even though categories already has it
Ownership checks and queries scope directly by user_id from the JWT without
joining through categories.
**Trade-off:** the service layer must verify the category belongs to the same
user, otherwise the two columns could disagree.

## 3. Category deletion: ON DELETE RESTRICT
CASCADE would silently delete the user's expenses (data loss). RESTRICT blocks
deletion; the frontend shows "move or delete N expenses first".
**Consequence:** deleting a user is also blocked if they have expenses. Acceptable since account deletion is out of scope for v1.

## 4. IDs as BIGINT identity, not INT or UUID
- vs INT: only 4 extra bytes per row, avoids a painful PK type migration later
  (every referencing FK changes too).
- vs UUID: 16 bytes vs 8, and random UUIDs (v4) fragment indexes. UUIDv7 fixes
  fragmentation, but a single-database app doesn't need distributed ID
  generation. Enumeration risk is handled by ownership checks instead.

## 5. Login accepts email or username
Email is the unique account identifier (recovery, notifications). Username
exists so login doesn't require typing a full email address.
**Trade-off:** two uniqueness checks at signup.

## 6. Plural table names (users, not user)
Consistent with common convention, and `user` is a reserved keyword in
PostgreSQL that would need quoting in every raw query.

## 7. timestamptz over timestamp
Stores absolute instants; avoids timezone bugs (server UTC vs client WIB).

## 8. Default categories seeded per user at registration
Created in service code in the same transaction as the user, not a global
shared table, not migration seed data. Migrations contain schema only.

## 9. Login as the landing page
The project does not have a dedicated landing page and uses the login page as
the front door. A private tool has nothing to market to visitors, so a marketing
page would just be an extra click. Route: `/` goes to the dashboard if
authenticated, otherwise to login.

## 10. Single password field with eye toggle, no confirm password
Sign up has one password field with a show/hide (eye) toggle instead of a
confirm password input. Confirm password exists to catch typos in a field you
can't see; the toggle solves the same problem with one less field, since the
user can just reveal and check what they typed.
**Note:** these two come as a pair. Dropping confirm password is only okay
because the toggle exists. A single hidden field with no reveal would be worse
than either option.

## 11. $ (USD) as the display currency
Amounts are displayed in $ instead of any other currencies to keep the demo readable for global audience.