package vn.edu.utc.hotel_booking.modules.ai.demo.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import vn.edu.utc.hotel_booking.modules.ai.forecast.model.DailyOccupancySample;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.dto.GroupRoomAssignmentPreviewRequest;
import vn.edu.utc.hotel_booking.modules.ai.groupassignment.port.GroupAssignmentDataPort.Snapshot;

import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.function.Function;

public final class FixtureDemoDataProvider implements DemoDataProvider {
    public static final String OCCUPANCY_HASH = "8c1f3e81019238948be47a61c49ae4821b92752381a7d99d70216e8437651c20";
    public static final String GROUP_HASH = "8d569695a769c55b9bc94ec465d47063555a87917eb47b415a1341405d1994cf";
    public static final Instant OCCUPANCY_REFERENCE = Instant.parse("2026-10-29T17:00:00Z");
    public static final Instant GROUP_REFERENCE = Instant.parse("2026-10-31T02:00:00Z");
    private final JsonNode occupancy;
    private final JsonNode group;
    public FixtureDemoDataProvider() { this(read("group-preview-v1.json",128_000)); }
    public FixtureDemoDataProvider(byte[] groupBytes) {
        occupancy = parse(read("occupancy-history-v1.json",512_000), OCCUPANCY_HASH,512_000);
        group = parse(groupBytes,GROUP_HASH,128_000);
        if (occupancy.path("hotels").size()!=2 || group.path("cases").size()!=7
                || !OCCUPANCY_REFERENCE.toString().equals(occupancy.path("referenceInstant").asText())
                || !GROUP_REFERENCE.toString().equals(group.path("referenceInstant").asText())) throw unavailable();
    }
    private static byte[] read(String name,int limit) {
        try(var input=new ClassPathResource("ai/demo/"+name).getInputStream()) { return input.readNBytes(limit+1); }
        catch(Exception e) { throw unavailable(); }
    }
    private static JsonNode parse(byte[] raw,String hash,int limit) {
        try {
            if(raw==null || raw.length>limit || !hash.equals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw)))) throw unavailable();
            var node=new ObjectMapper().readTree(raw);
            if(!"SYNTHETIC".equals(node.path("dataMode").asText())) throw unavailable();
            return node;
        } catch(Exception e) { throw unavailable(); }
    }
    public static IllegalStateException unavailable() { return new IllegalStateException("Synthetic demo data unavailable"); }
    JsonNode expectedPayload(String key) {
        var mapper=new ObjectMapper();
        var result=mapper.createObjectNode();
        if ("occupancy-v1".equals(key)) {
            var hotels=result.putArray("hotels");
            for(var hotel:occupancy.path("hotels")) {
                var copy=hotels.addObject();
                for(String field:new String[]{"hotelId","regionId","samples"}) copy.set(field,hotel.path(field).deepCopy());
            }
        } else if ("group-v1".equals(key)) {
            var cases=new java.util.ArrayList<JsonNode>(); group.path("cases").forEach(cases::add);
            cases.sort(java.util.Comparator.comparing(node->node.path("id").asText()));
            var array=result.putArray("cases"); cases.forEach(node->array.add(node.deepCopy()));
        } else throw unavailable();
        return result;
    }
    static OccupancyData occupancy(JsonNode payload,String scenario) {
        try {
            int index=switch(scenario) { case "HOTEL_A"->0; case "HOTEL_B"->1; default->throw unavailable(); };
            var hotel=payload.path("hotels").get(index);
            if(hotel==null || hotel.path("hotelId").asInt()!=index+1 || hotel.path("regionId").asInt()!=(index+1)*10 || hotel.path("samples").size()!=420) throw unavailable();
            var rows=new ArrayList<DailyOccupancySample>();
            LocalDate expected=LocalDate.parse("2025-09-05");
            for(var row:hotel.path("samples")) {
                var item=new DailyOccupancySample(row.path("hotelId").asInt(),LocalDate.parse(row.path("date").asText()),
                    row.path("occupiedRooms").isNull()?null:row.path("occupiedRooms").intValue(),
                    row.path("capacityRooms").isNull()?null:row.path("capacityRooms").intValue(),
                    DailyOccupancySample.Coverage.valueOf(row.path("coverage").asText()));
                if(item.hotelId()!=index+1 || !item.date().equals(expected)) throw unavailable();
                rows.add(item); expected=expected.plusDays(1);
            }
            return new OccupancyData(index+1,(index+1)*10,OCCUPANCY_REFERENCE,rows);
        } catch(Exception e) { throw unavailable(); }
    }
    static GroupData group(JsonNode payload,String id) {
        try {
            var ids=new HashSet<String>(); JsonNode found=null;
            for(var node:payload.path("cases")) {
                if(node.has("expected") || !ids.add(node.path("id").asText())) throw unavailable();
                if(id.equals(node.path("id").asText())) found=node;
            }
            if(found==null || ids.size()!=7) throw unavailable();
            var mapper=new ObjectMapper().findAndRegisterModules();
            var request=mapper.treeToValue(found.path("request"),GroupRoomAssignmentPreviewRequest.class);
            var snapshot=mapper.treeToValue(found.path("snapshot"),Snapshot.class);
            if(request==null || snapshot==null || snapshot.scope()==null || snapshot.scope().hotelId()!=1 || snapshot.scope().regionId()!=10
                    || snapshot.scope().contractId()!=501 || !"SYNTHETIC".equals(snapshot.dataMode())
                    || !"SYNTHETIC_PREVIEWABLE_ROOMS_V1".equals(snapshot.basis())
                    || snapshot.candidates().size()>300 || snapshot.quotaRows().size()>9300 || snapshot.nightEligibility().size()>9300) throw unavailable();
            return new GroupData(request,snapshot);
        } catch(Exception e) { throw unavailable(); }
    }
    public <T> T occupancy(String scenario,Function<OccupancyData,T> action) { return action.apply(occupancy(occupancy,scenario)); }
    public <T> T group(String id,Function<GroupData,T> action) { return action.apply(group(group,id)); }
}
