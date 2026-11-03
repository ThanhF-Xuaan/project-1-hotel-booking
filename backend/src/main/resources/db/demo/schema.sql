CREATE TABLE m1_demo.datasets (
 dataset_key text PRIMARY KEY CHECK (dataset_key IN ('occupancy-v1','group-v1')),
 version integer NOT NULL CHECK (version=1), data_mode text NOT NULL CHECK (data_mode='SYNTHETIC'),
 reference_instant timestamptz NOT NULL, source_sha256 text NOT NULL CHECK (length(source_sha256)=64),
 content_digest text NOT NULL, state text NOT NULL CHECK (state IN ('STAGING','READY')),
 created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE m1_demo.occupancy_samples (
 dataset_key text NOT NULL REFERENCES m1_demo.datasets(dataset_key) ON DELETE CASCADE,
 scenario text NOT NULL CHECK (scenario IN ('HOTEL_A','HOTEL_B')), hotel_id integer NOT NULL CHECK (hotel_id>0),
 region_id integer NOT NULL CHECK (region_id>0), sample_date date NOT NULL,
 occupied_rooms integer, capacity_rooms integer, coverage text NOT NULL CHECK (coverage IN ('COMPLETE','MISSING')),
 created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now(),
 PRIMARY KEY(dataset_key,scenario,sample_date),
 CHECK ((coverage='MISSING' AND occupied_rooms IS NULL AND capacity_rooms IS NULL) OR
 (coverage='COMPLETE' AND occupied_rooms IS NOT NULL AND capacity_rooms IS NOT NULL AND occupied_rooms>=0 AND occupied_rooms<=capacity_rooms))
);
CREATE TABLE m1_demo.group_scenarios (
 dataset_key text NOT NULL REFERENCES m1_demo.datasets(dataset_key) ON DELETE CASCADE,
 scenario text NOT NULL, request jsonb NOT NULL CHECK (jsonb_typeof(request)='object'),
 snapshot_metadata jsonb NOT NULL CHECK (jsonb_typeof(snapshot_metadata)='object'),
 created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now(),
 PRIMARY KEY(dataset_key,scenario)
);
CREATE TABLE m1_demo.group_candidates (
 dataset_key text NOT NULL, scenario text NOT NULL, ordinal integer NOT NULL CHECK (ordinal>=0),
 room_id integer NOT NULL CHECK (room_id>0), hotel_id integer NOT NULL CHECK(hotel_id>0),
 block_id bigint NOT NULL CHECK(block_id>0), room_type_id integer NOT NULL CHECK(room_type_id>0),
 capacity_persons integer NOT NULL CHECK(capacity_persons>0), bed_units integer NOT NULL CHECK(bed_units>0),
 floor_number integer, eligible boolean NOT NULL,
 created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now(),
 PRIMARY KEY(dataset_key,scenario,room_id), UNIQUE(dataset_key,scenario,ordinal),
 FOREIGN KEY(dataset_key,scenario) REFERENCES m1_demo.group_scenarios ON DELETE CASCADE
);
CREATE TABLE m1_demo.group_quotas (
 dataset_key text NOT NULL, scenario text NOT NULL, ordinal integer NOT NULL CHECK(ordinal>=0),
 hotel_id integer NOT NULL CHECK(hotel_id>0), contract_id bigint NOT NULL CHECK(contract_id>0),
 block_id bigint NOT NULL CHECK(block_id>0), room_type_id integer NOT NULL CHECK(room_type_id>0),
 stay_date date NOT NULL, room_count integer NOT NULL CHECK(room_count>=0),
 created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now(),
 PRIMARY KEY(dataset_key,scenario,hotel_id,contract_id,block_id,room_type_id,stay_date),
 UNIQUE(dataset_key,scenario,ordinal), FOREIGN KEY(dataset_key,scenario) REFERENCES m1_demo.group_scenarios ON DELETE CASCADE
);
CREATE TABLE m1_demo.group_night_eligibility (
 dataset_key text NOT NULL, scenario text NOT NULL, ordinal integer NOT NULL CHECK(ordinal>=0),
 room_id integer NOT NULL, hotel_id integer NOT NULL CHECK(hotel_id>0), contract_id bigint NOT NULL CHECK(contract_id>0),
 block_id bigint NOT NULL CHECK(block_id>0), room_type_id integer NOT NULL CHECK(room_type_id>0),
 stay_date date NOT NULL, state text NOT NULL CHECK(state IN ('ELIGIBLE','ROOM_UNAVAILABLE','BLOCK_INACTIVE','CUTOFF_EXPIRED')),
 created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now(),
 PRIMARY KEY(dataset_key,scenario,room_id,stay_date), UNIQUE(dataset_key,scenario,ordinal),
 FOREIGN KEY(dataset_key,scenario,room_id) REFERENCES m1_demo.group_candidates(dataset_key,scenario,room_id) ON DELETE CASCADE
);
CREATE FUNCTION m1_demo.touch_updated_at() RETURNS trigger LANGUAGE plpgsql AS $fn$
BEGIN NEW.updated_at=now(); RETURN NEW; END $fn$;
DO $triggers$ DECLARE t text; BEGIN
 FOREACH t IN ARRAY ARRAY['datasets','occupancy_samples','group_scenarios','group_candidates','group_quotas','group_night_eligibility'] LOOP
 EXECUTE format('CREATE TRIGGER touch_updated_at BEFORE UPDATE ON m1_demo.%I FOR EACH ROW EXECUTE FUNCTION m1_demo.touch_updated_at()',t);
 END LOOP;
END $triggers$;
CREATE FUNCTION m1_demo.dataset_payload(k text) RETURNS jsonb LANGUAGE sql STABLE AS $payload$
 SELECT CASE k WHEN 'occupancy-v1' THEN
 jsonb_build_object('hotels', (SELECT jsonb_agg(h.payload ORDER BY h.scenario) FROM (
  SELECT scenario,jsonb_build_object('hotelId',min(hotel_id),'regionId',min(region_id),'samples',
   jsonb_agg(jsonb_build_object('hotelId',hotel_id,'date',sample_date,'occupiedRooms',occupied_rooms,'capacityRooms',capacity_rooms,'coverage',coverage) ORDER BY sample_date)) payload
  FROM m1_demo.occupancy_samples WHERE dataset_key=k GROUP BY scenario) h))
 WHEN 'group-v1' THEN jsonb_build_object('cases',(SELECT jsonb_agg(jsonb_build_object('id',g.scenario,'request',g.request,'snapshot',
  g.snapshot_metadata || jsonb_build_object(
   'candidates',(SELECT coalesce(jsonb_agg(jsonb_build_object('roomInstanceId',c.room_id,'hotelId',c.hotel_id,'roomBlockId',c.block_id,'hotelRoomTypeId',c.room_type_id,'capacityPersons',c.capacity_persons,'bedUnits',c.bed_units,'floorNumber',c.floor_number,'eligibleForEntireStay',c.eligible) ORDER BY c.ordinal),'[]'::jsonb) FROM m1_demo.group_candidates c WHERE c.dataset_key=g.dataset_key AND c.scenario=g.scenario),
   'quotaRows',(SELECT coalesce(jsonb_agg(jsonb_build_object('hotelId',q.hotel_id,'contractId',q.contract_id,'roomBlockId',q.block_id,'hotelRoomTypeId',q.room_type_id,'stayDate',q.stay_date,'previewableRoomCount',q.room_count) ORDER BY q.ordinal),'[]'::jsonb) FROM m1_demo.group_quotas q WHERE q.dataset_key=g.dataset_key AND q.scenario=g.scenario),
   'nightEligibility',(SELECT coalesce(jsonb_agg(jsonb_build_object('roomInstanceId',n.room_id,'hotelId',n.hotel_id,'contractId',n.contract_id,'roomBlockId',n.block_id,'hotelRoomTypeId',n.room_type_id,'stayDate',n.stay_date,'state',n.state) ORDER BY n.ordinal),'[]'::jsonb) FROM m1_demo.group_night_eligibility n WHERE n.dataset_key=g.dataset_key AND n.scenario=g.scenario))) ORDER BY g.scenario)
  FROM m1_demo.group_scenarios g WHERE g.dataset_key=k)) END
$payload$;
