-- A trip now describes two flight legs (outbound and return), but flight_number was a single
-- value shared by both — so it could only ever be right for one of them. Rather than carry a
-- field that is misleading half the time, drop it. The airline, the four airports and the
-- transit details still describe the journey.

ALTER TABLE trips DROP COLUMN flight_number;
