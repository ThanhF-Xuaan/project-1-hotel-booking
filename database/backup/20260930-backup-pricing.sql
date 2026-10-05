--
-- PostgreSQL database dump
--

\restrict STwj5CGpuQuXltdAd0v9eFzyVagf7iE1ioYvYnrfRaW0hbhgdF4pTcKS1YLO1J0

-- Dumped from database version 16.15
-- Dumped by pg_dump version 16.15

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Data for Name: discount_rules; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.discount_rules OVERRIDING SYSTEM VALUE VALUES (1, 1, NULL, 'LONG_STAY', '{"minNights": 3}', 'PERCENT', 10.00, '2026-06-01', '2027-06-30', 'ACTIVE', false, '2026-09-22 11:28:28.434704+00', '2026-09-22 11:28:28.434704+00');
INSERT INTO public.discount_rules OVERRIDING SYSTEM VALUE VALUES (2, 2, NULL, 'LONG_STAY', '{"minNights": 3}', 'PERCENT', 10.00, '2026-06-01', '2027-06-30', 'ACTIVE', false, '2026-09-22 11:28:28.434704+00', '2026-09-22 11:28:28.434704+00');
INSERT INTO public.discount_rules OVERRIDING SYSTEM VALUE VALUES (3, 3, NULL, 'LONG_STAY', '{"minNights": 3}', 'PERCENT', 10.00, '2026-06-01', '2027-06-30', 'ACTIVE', false, '2026-09-22 11:28:28.434704+00', '2026-09-22 11:28:28.434704+00');
INSERT INTO public.discount_rules OVERRIDING SYSTEM VALUE VALUES (4, 1, NULL, 'EARLY_BIRD', '{"minAdvanceBookingDays": 30}', 'PERCENT', 15.00, '2026-06-01', '2027-06-30', 'ACTIVE', false, '2026-09-22 11:28:28.437693+00', '2026-09-22 11:28:28.437693+00');
INSERT INTO public.discount_rules OVERRIDING SYSTEM VALUE VALUES (5, 2, NULL, 'EARLY_BIRD', '{"minAdvanceBookingDays": 30}', 'PERCENT', 15.00, '2026-06-01', '2027-06-30', 'ACTIVE', false, '2026-09-22 11:28:28.437693+00', '2026-09-22 11:28:28.437693+00');
INSERT INTO public.discount_rules OVERRIDING SYSTEM VALUE VALUES (6, 3, NULL, 'EARLY_BIRD', '{"minAdvanceBookingDays": 30}', 'PERCENT', 15.00, '2026-06-01', '2027-06-30', 'ACTIVE', false, '2026-09-22 11:28:28.437693+00', '2026-09-22 11:28:28.437693+00');
INSERT INTO public.discount_rules OVERRIDING SYSTEM VALUE VALUES (7, 1, 1, 'SPECIAL_CAMPAIGN', '{"promoCode": "SUMMER2026"}', 'FIXED', 200000.00, '2026-06-01', '2026-08-31', 'ACTIVE', false, '2026-09-22 11:28:28.44023+00', '2026-09-22 11:28:28.44023+00');
INSERT INTO public.discount_rules OVERRIDING SYSTEM VALUE VALUES (8, 2, 1, 'SPECIAL_CAMPAIGN', '{"promoCode": "SUMMER2026"}', 'FIXED', 200000.00, '2026-06-01', '2026-08-31', 'ACTIVE', false, '2026-09-22 11:28:28.44023+00', '2026-09-22 11:28:28.44023+00');
INSERT INTO public.discount_rules OVERRIDING SYSTEM VALUE VALUES (9, 3, 1, 'SPECIAL_CAMPAIGN', '{"promoCode": "SUMMER2026"}', 'FIXED', 200000.00, '2026-06-01', '2026-08-31', 'ACTIVE', false, '2026-09-22 11:28:28.44023+00', '2026-09-22 11:28:28.44023+00');


--
-- Data for Name: pricing_rules; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.pricing_rules OVERRIDING SYSTEM VALUE VALUES (1, 1, 1, 'HOLIDAY', 'PERCENT', 30.00, '2026-09-02', '2026-09-02', 'ACTIVE', false, '2026-09-22 11:28:28.415301+00', '2026-09-22 11:28:28.415301+00');
INSERT INTO public.pricing_rules OVERRIDING SYSTEM VALUE VALUES (2, 1, 4, 'HOLIDAY', 'PERCENT', 30.00, '2027-02-06', '2027-02-06', 'ACTIVE', false, '2026-09-22 11:28:28.415301+00', '2026-09-22 11:28:28.415301+00');
INSERT INTO public.pricing_rules OVERRIDING SYSTEM VALUE VALUES (3, 1, 5, 'HOLIDAY', 'PERCENT', 30.00, '2027-02-07', '2027-02-07', 'ACTIVE', false, '2026-09-22 11:28:28.415301+00', '2026-09-22 11:28:28.415301+00');
INSERT INTO public.pricing_rules OVERRIDING SYSTEM VALUE VALUES (4, 1, 6, 'HOLIDAY', 'PERCENT', 30.00, '2027-02-08', '2027-02-08', 'ACTIVE', false, '2026-09-22 11:28:28.415301+00', '2026-09-22 11:28:28.415301+00');
INSERT INTO public.pricing_rules OVERRIDING SYSTEM VALUE VALUES (5, 2, 1, 'HOLIDAY', 'PERCENT', 30.00, '2026-09-02', '2026-09-02', 'ACTIVE', false, '2026-09-22 11:28:28.415301+00', '2026-09-22 11:28:28.415301+00');
INSERT INTO public.pricing_rules OVERRIDING SYSTEM VALUE VALUES (6, 2, 4, 'HOLIDAY', 'PERCENT', 30.00, '2027-02-06', '2027-02-06', 'ACTIVE', false, '2026-09-22 11:28:28.415301+00', '2026-09-22 11:28:28.415301+00');
INSERT INTO public.pricing_rules OVERRIDING SYSTEM VALUE VALUES (7, 2, 5, 'HOLIDAY', 'PERCENT', 30.00, '2027-02-07', '2027-02-07', 'ACTIVE', false, '2026-09-22 11:28:28.415301+00', '2026-09-22 11:28:28.415301+00');
INSERT INTO public.pricing_rules OVERRIDING SYSTEM VALUE VALUES (8, 2, 6, 'HOLIDAY', 'PERCENT', 30.00, '2027-02-08', '2027-02-08', 'ACTIVE', false, '2026-09-22 11:28:28.415301+00', '2026-09-22 11:28:28.415301+00');
INSERT INTO public.pricing_rules OVERRIDING SYSTEM VALUE VALUES (9, 3, 1, 'HOLIDAY', 'PERCENT', 30.00, '2026-09-02', '2026-09-02', 'ACTIVE', false, '2026-09-22 11:28:28.415301+00', '2026-09-22 11:28:28.415301+00');
INSERT INTO public.pricing_rules OVERRIDING SYSTEM VALUE VALUES (10, 3, 4, 'HOLIDAY', 'PERCENT', 30.00, '2027-02-06', '2027-02-06', 'ACTIVE', false, '2026-09-22 11:28:28.415301+00', '2026-09-22 11:28:28.415301+00');
INSERT INTO public.pricing_rules OVERRIDING SYSTEM VALUE VALUES (11, 3, 5, 'HOLIDAY', 'PERCENT', 30.00, '2027-02-07', '2027-02-07', 'ACTIVE', false, '2026-09-22 11:28:28.415301+00', '2026-09-22 11:28:28.415301+00');
INSERT INTO public.pricing_rules OVERRIDING SYSTEM VALUE VALUES (12, 3, 6, 'HOLIDAY', 'PERCENT', 30.00, '2027-02-08', '2027-02-08', 'ACTIVE', false, '2026-09-22 11:28:28.415301+00', '2026-09-22 11:28:28.415301+00');
INSERT INTO public.pricing_rules OVERRIDING SYSTEM VALUE VALUES (13, 5, 1, 'HOLIDAY', 'PERCENT', 30.00, '2026-09-02', '2026-09-02', 'ACTIVE', false, '2026-09-22 11:28:28.415301+00', '2026-09-22 11:28:28.415301+00');
INSERT INTO public.pricing_rules OVERRIDING SYSTEM VALUE VALUES (14, 5, 4, 'HOLIDAY', 'PERCENT', 30.00, '2027-02-06', '2027-02-06', 'ACTIVE', false, '2026-09-22 11:28:28.415301+00', '2026-09-22 11:28:28.415301+00');
INSERT INTO public.pricing_rules OVERRIDING SYSTEM VALUE VALUES (15, 5, 5, 'HOLIDAY', 'PERCENT', 30.00, '2027-02-07', '2027-02-07', 'ACTIVE', false, '2026-09-22 11:28:28.415301+00', '2026-09-22 11:28:28.415301+00');
INSERT INTO public.pricing_rules OVERRIDING SYSTEM VALUE VALUES (16, 5, 6, 'HOLIDAY', 'PERCENT', 30.00, '2027-02-08', '2027-02-08', 'ACTIVE', false, '2026-09-22 11:28:28.415301+00', '2026-09-22 11:28:28.415301+00');
INSERT INTO public.pricing_rules OVERRIDING SYSTEM VALUE VALUES (17, 6, 1, 'HOLIDAY', 'PERCENT', 30.00, '2026-09-02', '2026-09-02', 'ACTIVE', false, '2026-09-22 11:28:28.415301+00', '2026-09-22 11:28:28.415301+00');
INSERT INTO public.pricing_rules OVERRIDING SYSTEM VALUE VALUES (18, 6, 4, 'HOLIDAY', 'PERCENT', 30.00, '2027-02-06', '2027-02-06', 'ACTIVE', false, '2026-09-22 11:28:28.415301+00', '2026-09-22 11:28:28.415301+00');
INSERT INTO public.pricing_rules OVERRIDING SYSTEM VALUE VALUES (19, 6, 5, 'HOLIDAY', 'PERCENT', 30.00, '2027-02-07', '2027-02-07', 'ACTIVE', false, '2026-09-22 11:28:28.415301+00', '2026-09-22 11:28:28.415301+00');
INSERT INTO public.pricing_rules OVERRIDING SYSTEM VALUE VALUES (20, 6, 6, 'HOLIDAY', 'PERCENT', 30.00, '2027-02-08', '2027-02-08', 'ACTIVE', false, '2026-09-22 11:28:28.415301+00', '2026-09-22 11:28:28.415301+00');
INSERT INTO public.pricing_rules OVERRIDING SYSTEM VALUE VALUES (21, 4, NULL, 'WEEKEND', 'PERCENT', 15.00, '2026-07-01', '2027-06-30', 'ACTIVE', false, '2026-09-22 11:28:28.432101+00', '2026-09-22 11:28:28.432101+00');
INSERT INTO public.pricing_rules OVERRIDING SYSTEM VALUE VALUES (22, 5, NULL, 'WEEKEND', 'PERCENT', 15.00, '2026-07-01', '2027-06-30', 'ACTIVE', false, '2026-09-22 11:28:28.432101+00', '2026-09-22 11:28:28.432101+00');


--
-- Data for Name: surcharge_rules; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.surcharge_rules OVERRIDING SYSTEM VALUE VALUES (1, 1, 2, 'EXTRA_PERSON', NULL, 'FIXED', 300000.00, '2026-06-01', '2027-06-30', 'ACTIVE', false, '2026-09-22 11:28:28.444798+00', '2026-09-22 11:28:28.444798+00');
INSERT INTO public.surcharge_rules OVERRIDING SYSTEM VALUE VALUES (2, 2, 2, 'EXTRA_PERSON', NULL, 'FIXED', 300000.00, '2026-06-01', '2027-06-30', 'ACTIVE', false, '2026-09-22 11:28:28.444798+00', '2026-09-22 11:28:28.444798+00');
INSERT INTO public.surcharge_rules OVERRIDING SYSTEM VALUE VALUES (3, 3, 2, 'EXTRA_PERSON', NULL, 'FIXED', 300000.00, '2026-06-01', '2027-06-30', 'ACTIVE', false, '2026-09-22 11:28:28.444798+00', '2026-09-22 11:28:28.444798+00');
INSERT INTO public.surcharge_rules OVERRIDING SYSTEM VALUE VALUES (4, 1, NULL, 'EXTRA_BED', NULL, 'FIXED', 1000000.00, '2026-06-01', '2027-06-30', 'ACTIVE', false, '2026-09-22 11:28:28.448925+00', '2026-09-22 11:28:28.448925+00');
INSERT INTO public.surcharge_rules OVERRIDING SYSTEM VALUE VALUES (5, 2, NULL, 'EXTRA_BED', NULL, 'FIXED', 1000000.00, '2026-06-01', '2027-06-30', 'ACTIVE', false, '2026-09-22 11:28:28.448925+00', '2026-09-22 11:28:28.448925+00');
INSERT INTO public.surcharge_rules OVERRIDING SYSTEM VALUE VALUES (6, 3, NULL, 'EXTRA_BED', NULL, 'FIXED', 1000000.00, '2026-06-01', '2027-06-30', 'ACTIVE', false, '2026-09-22 11:28:28.448925+00', '2026-09-22 11:28:28.448925+00');
INSERT INTO public.surcharge_rules OVERRIDING SYSTEM VALUE VALUES (7, 4, NULL, 'EARLY_CHECKIN', '{"time_tiers": [{"up_to_hours": 4.0, "adjustment_type": "PERCENT", "adjustment_value": 30.00}, {"up_to_hours": 8.0, "adjustment_type": "PERCENT", "adjustment_value": 50.00}, {"up_to_hours": null, "adjustment_type": "PERCENT", "adjustment_value": 100.00}]}', 'PERCENT', 0.00, '2026-06-01', '2027-06-30', 'ACTIVE', false, '2026-09-22 11:28:28.451803+00', '2026-09-22 11:28:28.451803+00');
INSERT INTO public.surcharge_rules OVERRIDING SYSTEM VALUE VALUES (8, 5, NULL, 'EARLY_CHECKIN', '{"time_tiers": [{"up_to_hours": 4.0, "adjustment_type": "PERCENT", "adjustment_value": 30.00}, {"up_to_hours": 8.0, "adjustment_type": "PERCENT", "adjustment_value": 50.00}, {"up_to_hours": null, "adjustment_type": "PERCENT", "adjustment_value": 100.00}]}', 'PERCENT', 0.00, '2026-06-01', '2027-06-30', 'ACTIVE', false, '2026-09-22 11:28:28.451803+00', '2026-09-22 11:28:28.451803+00');
INSERT INTO public.surcharge_rules OVERRIDING SYSTEM VALUE VALUES (9, 6, NULL, 'EARLY_CHECKIN', '{"time_tiers": [{"up_to_hours": 4.0, "adjustment_type": "PERCENT", "adjustment_value": 30.00}, {"up_to_hours": 8.0, "adjustment_type": "PERCENT", "adjustment_value": 50.00}, {"up_to_hours": null, "adjustment_type": "PERCENT", "adjustment_value": 100.00}]}', 'PERCENT', 0.00, '2026-06-01', '2027-06-30', 'ACTIVE', false, '2026-09-22 11:28:28.451803+00', '2026-09-22 11:28:28.451803+00');
INSERT INTO public.surcharge_rules OVERRIDING SYSTEM VALUE VALUES (10, 4, NULL, 'LATE_CHECKOUT', '{"time_tiers": [{"up_to_hours": 3.0, "adjustment_type": "PERCENT", "adjustment_value": 30.00}, {"up_to_hours": 6.0, "adjustment_type": "PERCENT", "adjustment_value": 50.00}, {"up_to_hours": null, "adjustment_type": "PERCENT", "adjustment_value": 100.00}]}', 'PERCENT', 0.00, '2026-06-01', '2027-06-30', 'ACTIVE', false, '2026-09-22 11:28:28.454937+00', '2026-09-22 11:28:28.454937+00');
INSERT INTO public.surcharge_rules OVERRIDING SYSTEM VALUE VALUES (11, 5, NULL, 'LATE_CHECKOUT', '{"time_tiers": [{"up_to_hours": 3.0, "adjustment_type": "PERCENT", "adjustment_value": 30.00}, {"up_to_hours": 6.0, "adjustment_type": "PERCENT", "adjustment_value": 50.00}, {"up_to_hours": null, "adjustment_type": "PERCENT", "adjustment_value": 100.00}]}', 'PERCENT', 0.00, '2026-06-01', '2027-06-30', 'ACTIVE', false, '2026-09-22 11:28:28.454937+00', '2026-09-22 11:28:28.454937+00');
INSERT INTO public.surcharge_rules OVERRIDING SYSTEM VALUE VALUES (12, 6, NULL, 'LATE_CHECKOUT', '{"time_tiers": [{"up_to_hours": 3.0, "adjustment_type": "PERCENT", "adjustment_value": 30.00}, {"up_to_hours": 6.0, "adjustment_type": "PERCENT", "adjustment_value": 50.00}, {"up_to_hours": null, "adjustment_type": "PERCENT", "adjustment_value": 100.00}]}', 'PERCENT', 0.00, '2026-06-01', '2027-06-30', 'ACTIVE', false, '2026-09-22 11:28:28.454937+00', '2026-09-22 11:28:28.454937+00');


--
-- Name: discount_rules_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.discount_rules_id_seq', 9, true);


--
-- Name: pricing_rules_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.pricing_rules_id_seq', 22, true);


--
-- Name: surcharge_rules_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.surcharge_rules_id_seq', 12, true);


--
-- PostgreSQL database dump complete
--

\unrestrict STwj5CGpuQuXltdAd0v9eFzyVagf7iE1ioYvYnrfRaW0hbhgdF4pTcKS1YLO1J0

