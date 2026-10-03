--
-- PostgreSQL database dump
--

\restrict cf63IbtDwAxFgHw405dug6TeGFBkV7NVSgK2cuxeF8hdac9cclMbJ4Pnj6sOT8x

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
-- Data for Name: hotel_room_type_catalog_items; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.hotel_room_type_catalog_items VALUES (1, 2, 'MANDATORY', 'PER_NIGHT', 0.00, '2026-09-22 11:28:28.36931+00', '2026-09-22 11:28:28.36931+00');
INSERT INTO public.hotel_room_type_catalog_items VALUES (1, 1, 'MANDATORY', 'PER_NIGHT', 0.00, '2026-09-22 11:28:28.36931+00', '2026-09-22 11:28:28.36931+00');
INSERT INTO public.hotel_room_type_catalog_items VALUES (2, 2, 'MANDATORY', 'PER_NIGHT', 0.00, '2026-09-22 11:28:28.36931+00', '2026-09-22 11:28:28.36931+00');
INSERT INTO public.hotel_room_type_catalog_items VALUES (2, 1, 'MANDATORY', 'PER_NIGHT', 0.00, '2026-09-22 11:28:28.36931+00', '2026-09-22 11:28:28.36931+00');
INSERT INTO public.hotel_room_type_catalog_items VALUES (3, 2, 'MANDATORY', 'PER_NIGHT', 0.00, '2026-09-22 11:28:28.36931+00', '2026-09-22 11:28:28.36931+00');
INSERT INTO public.hotel_room_type_catalog_items VALUES (3, 1, 'MANDATORY', 'PER_NIGHT', 0.00, '2026-09-22 11:28:28.36931+00', '2026-09-22 11:28:28.36931+00');
INSERT INTO public.hotel_room_type_catalog_items VALUES (4, 12, 'MANDATORY', 'PER_NIGHT', 0.00, '2026-09-22 11:28:28.36931+00', '2026-09-22 11:28:28.36931+00');
INSERT INTO public.hotel_room_type_catalog_items VALUES (4, 11, 'MANDATORY', 'PER_NIGHT', 0.00, '2026-09-22 11:28:28.36931+00', '2026-09-22 11:28:28.36931+00');
INSERT INTO public.hotel_room_type_catalog_items VALUES (5, 12, 'MANDATORY', 'PER_NIGHT', 0.00, '2026-09-22 11:28:28.36931+00', '2026-09-22 11:28:28.36931+00');
INSERT INTO public.hotel_room_type_catalog_items VALUES (5, 11, 'MANDATORY', 'PER_NIGHT', 0.00, '2026-09-22 11:28:28.36931+00', '2026-09-22 11:28:28.36931+00');
INSERT INTO public.hotel_room_type_catalog_items VALUES (6, 12, 'MANDATORY', 'PER_NIGHT', 0.00, '2026-09-22 11:28:28.36931+00', '2026-09-22 11:28:28.36931+00');
INSERT INTO public.hotel_room_type_catalog_items VALUES (6, 11, 'MANDATORY', 'PER_NIGHT', 0.00, '2026-09-22 11:28:28.36931+00', '2026-09-22 11:28:28.36931+00');
INSERT INTO public.hotel_room_type_catalog_items VALUES (3, 5, 'MANDATORY', 'PER_STAY', 0.00, '2026-09-22 11:28:28.376802+00', '2026-09-22 11:28:28.376802+00');
INSERT INTO public.hotel_room_type_catalog_items VALUES (7, 21, 'OPTIONAL', 'PER_NIGHT', 250000.00, '2026-09-22 11:28:28.381175+00', '2026-09-22 11:28:28.381175+00');
INSERT INTO public.hotel_room_type_catalog_items VALUES (8, 21, 'MANDATORY', 'PER_NIGHT', 0.00, '2026-09-22 11:28:28.385258+00', '2026-09-22 11:28:28.385258+00');
INSERT INTO public.hotel_room_type_catalog_items VALUES (7, 28, 'OPTIONAL', 'PER_NIGHT', 150000.00, '2026-09-22 11:28:28.388001+00', '2026-09-22 11:28:28.388001+00');
INSERT INTO public.hotel_room_type_catalog_items VALUES (8, 28, 'OPTIONAL', 'PER_NIGHT', 150000.00, '2026-09-22 11:28:28.388001+00', '2026-09-22 11:28:28.388001+00');
INSERT INTO public.hotel_room_type_catalog_items VALUES (1, 7, 'OPTIONAL', 'PER_STAY', 350000.00, '2026-09-22 11:28:28.390011+00', '2026-09-22 11:28:28.390011+00');
INSERT INTO public.hotel_room_type_catalog_items VALUES (2, 7, 'OPTIONAL', 'PER_STAY', 350000.00, '2026-09-22 11:28:28.390011+00', '2026-09-22 11:28:28.390011+00');
INSERT INTO public.hotel_room_type_catalog_items VALUES (3, 7, 'OPTIONAL', 'PER_STAY', 350000.00, '2026-09-22 11:28:28.390011+00', '2026-09-22 11:28:28.390011+00');
INSERT INTO public.hotel_room_type_catalog_items VALUES (4, 17, 'OPTIONAL', 'PER_STAY', 350000.00, '2026-09-22 11:28:28.390011+00', '2026-09-22 11:28:28.390011+00');
INSERT INTO public.hotel_room_type_catalog_items VALUES (5, 17, 'OPTIONAL', 'PER_STAY', 350000.00, '2026-09-22 11:28:28.390011+00', '2026-09-22 11:28:28.390011+00');
INSERT INTO public.hotel_room_type_catalog_items VALUES (6, 17, 'OPTIONAL', 'PER_STAY', 350000.00, '2026-09-22 11:28:28.390011+00', '2026-09-22 11:28:28.390011+00');
INSERT INTO public.hotel_room_type_catalog_items VALUES (1, 10, 'OPTIONAL', 'PER_STAY', 1000000.00, '2026-09-22 11:28:28.392474+00', '2026-09-22 11:28:28.392474+00');
INSERT INTO public.hotel_room_type_catalog_items VALUES (2, 10, 'OPTIONAL', 'PER_STAY', 1000000.00, '2026-09-22 11:28:28.392474+00', '2026-09-22 11:28:28.392474+00');
INSERT INTO public.hotel_room_type_catalog_items VALUES (3, 10, 'OPTIONAL', 'PER_STAY', 1000000.00, '2026-09-22 11:28:28.392474+00', '2026-09-22 11:28:28.392474+00');
INSERT INTO public.hotel_room_type_catalog_items VALUES (4, 20, 'OPTIONAL', 'PER_STAY', 1000000.00, '2026-09-22 11:28:28.392474+00', '2026-09-22 11:28:28.392474+00');
INSERT INTO public.hotel_room_type_catalog_items VALUES (5, 20, 'OPTIONAL', 'PER_STAY', 1000000.00, '2026-09-22 11:28:28.392474+00', '2026-09-22 11:28:28.392474+00');
INSERT INTO public.hotel_room_type_catalog_items VALUES (6, 20, 'OPTIONAL', 'PER_STAY', 1000000.00, '2026-09-22 11:28:28.392474+00', '2026-09-22 11:28:28.392474+00');
INSERT INTO public.hotel_room_type_catalog_items VALUES (7, 30, 'OPTIONAL', 'PER_STAY', 1000000.00, '2026-09-22 11:28:28.392474+00', '2026-09-22 11:28:28.392474+00');
INSERT INTO public.hotel_room_type_catalog_items VALUES (8, 30, 'OPTIONAL', 'PER_STAY', 1000000.00, '2026-09-22 11:28:28.392474+00', '2026-09-22 11:28:28.392474+00');


--
-- PostgreSQL database dump complete
--

\unrestrict cf63IbtDwAxFgHw405dug6TeGFBkV7NVSgK2cuxeF8hdac9cclMbJ4Pnj6sOT8x

