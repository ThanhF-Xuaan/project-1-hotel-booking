-- Explicit maintenance context only; Liquibase owns the single transaction.
-- Quiesce all writers first. Conflicts abort, never merge/delete business rows.
SET LOCAL lock_timeout = '5s';
LOCK TABLE public.bookings, public.booking_details, public.booking_rooms, public.room_instances IN SHARE MODE;
LOCK TABLE public.stay_guests IN ACCESS EXCLUSIVE MODE;

CREATE FUNCTION public.m1_normalize_stay_token(value text) RETURNS text
LANGUAGE sql IMMUTABLE PARALLEL SAFE SET search_path = pg_catalog AS $$
    SELECT nullif(btrim(value, chr(9)||chr(10)||chr(11)||chr(12)||chr(13)||' '), '')
$$;

CREATE FUNCTION public.m1_active_stay_key(
    p_hotel smallint, p_booking bigint, p_booking_room bigint, p_room integer,
    p_room_number text, p_document text, p_checkin timestamptz, p_checkout timestamptz, p_deleted boolean
) RETURNS bytea LANGUAGE plpgsql STABLE SET search_path = pg_catalog, public AS $$
DECLARE
    effective_booking bigint := p_booking;
    effective_room integer := p_room;
    parent_booking bigint;
    parent_hotel smallint;
    assigned_room integer;
    physical_hotel smallint;
    physical_number text;
    physical_deleted boolean;
    normalized_document text := public.m1_normalize_stay_token(p_document);
    normalized_room text := public.m1_normalize_stay_token(p_room_number);
BEGIN
    IF p_checkin IS NULL OR p_checkout IS NOT NULL OR coalesce(p_deleted,false) THEN RETURN NULL; END IF;
    IF p_hotel IS NULL OR p_hotel < 1 OR normalized_room IS NULL THEN
        RAISE EXCEPTION 'M1_ACTIVE_STAY_INVALID_LINK' USING ERRCODE='23514';
    END IF;
    IF p_booking IS NOT NULL THEN
        SELECT hotel_id INTO parent_hotel FROM public.bookings WHERE id=p_booking;
        IF NOT FOUND OR parent_hotel IS DISTINCT FROM p_hotel THEN
            RAISE EXCEPTION 'M1_ACTIVE_STAY_INVALID_LINK' USING ERRCODE='23514';
        END IF;
    END IF;
    IF p_booking_room IS NOT NULL THEN
        SELECT bd.booking_id,b.hotel_id,br.room_instance_id INTO parent_booking,parent_hotel,assigned_room
        FROM public.booking_rooms br JOIN public.booking_details bd ON bd.id=br.booking_detail_id
        JOIN public.bookings b ON b.id=bd.booking_id WHERE br.id=p_booking_room;
        IF NOT FOUND OR parent_hotel IS DISTINCT FROM p_hotel OR assigned_room IS NULL
                OR (p_booking IS NOT NULL AND p_booking IS DISTINCT FROM parent_booking)
                OR (p_room IS NOT NULL AND p_room IS DISTINCT FROM assigned_room) THEN
            RAISE EXCEPTION 'M1_ACTIVE_STAY_INVALID_LINK' USING ERRCODE='23514';
        END IF;
        effective_booking := parent_booking;
        effective_room := coalesce(p_room,assigned_room);
    END IF;
    IF effective_room IS NOT NULL THEN
        SELECT hotel_id,room_number,is_deleted INTO physical_hotel,physical_number,physical_deleted
        FROM public.room_instances WHERE id=effective_room;
        IF NOT FOUND OR physical_hotel IS DISTINCT FROM p_hotel OR physical_deleted
                OR public.m1_normalize_stay_token(physical_number) IS DISTINCT FROM normalized_room THEN
            RAISE EXCEPTION 'M1_ACTIVE_STAY_INVALID_LINK' USING ERRCODE='23514';
        END IF;
    END IF;
    IF normalized_document IS NULL OR upper(normalized_document)='N/A' THEN RETURN NULL; END IF;
    RETURN sha256(convert_to(jsonb_build_array(p_hotel,
        CASE WHEN effective_booking IS NULL THEN 'WALKIN' ELSE 'BOOKING' END,
        CASE WHEN effective_booking IS NULL THEN normalized_room ELSE effective_booking::text END,
        normalized_document)::text,'UTF8'));
END $$;

-- Report numeric IDs only. No documents, hashes, names or automatic reconciliation.
DO $$
DECLARE row record; duplicate_ids bigint[];
BEGIN
    IF (SELECT tgenabled FROM pg_trigger WHERE tgrelid='public.stay_guests'::regclass
            AND tgname='trg_stay_guests_updated_at') IS DISTINCT FROM 'O'::"char" THEN
        RAISE EXCEPTION 'M1_ACTIVE_STAY_TIMESTAMP_TRIGGER_BASELINE_MISMATCH';
    END IF;
    FOR row IN SELECT * FROM public.stay_guests WHERE check_in_time IS NOT NULL
            AND actual_check_out_time IS NULL AND NOT is_deleted LOOP
        BEGIN
            PERFORM public.m1_active_stay_key(row.hotel_id,row.booking_id,row.booking_room_id,row.room_id,
                row.room_number,row.document_number,row.check_in_time,row.actual_check_out_time,row.is_deleted);
        EXCEPTION WHEN check_violation THEN
            RAISE EXCEPTION 'M1_ACTIVE_STAY_INVALID_LINK id=%', row.id USING ERRCODE='23514';
        END;
    END LOOP;
    WITH keys AS MATERIALIZED (
        SELECT id,public.m1_active_stay_key(hotel_id,booking_id,booking_room_id,room_id,room_number,
            document_number,check_in_time,actual_check_out_time,is_deleted) AS key FROM public.stay_guests
    ), duplicates AS (
        SELECT unnest(array_agg(id)) AS id FROM keys WHERE key IS NOT NULL GROUP BY key HAVING count(*)>1
    ) SELECT array_agg(id ORDER BY id) INTO duplicate_ids FROM (SELECT id FROM duplicates ORDER BY id LIMIT 100) limited;
    IF duplicate_ids IS NOT NULL THEN
        RAISE EXCEPTION 'M1_ACTIVE_STAY_DUPLICATES ids=% (maximum first 100)', duplicate_ids USING ERRCODE='23505';
    END IF;
END $$;

-- Existing service contract permits walk-ins without a BookingRoom; the renamed legacy table did not.
ALTER TABLE public.stay_guests ALTER COLUMN booking_room_id DROP NOT NULL;
ALTER TABLE public.stay_guests ADD COLUMN active_dedup_key bytea
    CONSTRAINT chk_stay_guest_active_dedup_length CHECK(active_dedup_key IS NULL OR octet_length(active_dedup_key)=32);
-- Only the technical column changes; preserve historical updated_at and all export fields.
ALTER TABLE public.stay_guests DISABLE TRIGGER trg_stay_guests_updated_at;
UPDATE public.stay_guests SET active_dedup_key=public.m1_active_stay_key(hotel_id,booking_id,booking_room_id,room_id,
    room_number,document_number,check_in_time,actual_check_out_time,is_deleted);
ALTER TABLE public.stay_guests ENABLE TRIGGER trg_stay_guests_updated_at;

CREATE FUNCTION public.m1_set_active_stay_key() RETURNS trigger
LANGUAGE plpgsql SET search_path = pg_catalog, public AS $$
BEGIN
    NEW.active_dedup_key := public.m1_active_stay_key(NEW.hotel_id,NEW.booking_id,NEW.booking_room_id,NEW.room_id,
        NEW.room_number,NEW.document_number,NEW.check_in_time,NEW.actual_check_out_time,NEW.is_deleted);
    RETURN NEW;
END $$;
CREATE TRIGGER trg_m1_active_stay_dedup BEFORE INSERT OR UPDATE ON public.stay_guests
    FOR EACH ROW EXECUTE FUNCTION public.m1_set_active_stay_key();
CREATE UNIQUE INDEX uq_stay_guest_active_dedup ON public.stay_guests(active_dedup_key) WHERE active_dedup_key IS NOT NULL;
